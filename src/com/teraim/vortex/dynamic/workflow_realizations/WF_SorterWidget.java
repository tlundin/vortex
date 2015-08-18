package com.teraim.vortex.dynamic.workflow_realizations;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout.LayoutParams;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.dynamic.types.Table;
import com.teraim.vortex.dynamic.workflow_realizations.filters.WF_Column_Name_Filter;
import com.teraim.vortex.dynamic.workflow_realizations.filters.WF_Column_Name_Filter.FilterType;
import com.teraim.vortex.dynamic.workflow_realizations.filters.WF_Filter;


public class WF_SorterWidget extends WF_Widget {

	private final String[] alfabet = {
			"*","ABCD","EFGH","IJKL","MNOP","QRST","UVXY","Z���"};



	WF_Filter existing;
	WF_List targetList;

	public WF_SorterWidget(String name,WF_Context ctx, String type, final WF_List targetList,final ViewGroup container,final String selectionField, final String displayField,String selectionPattern,boolean isVisible) {
		super(name,new LinearLayout(ctx.getContext()),isVisible,ctx);
		LinearLayout buttonPanel;
		o = GlobalState.getInstance().getLogger();
		LayoutParams lp;
		int orientation =  ((LinearLayout)container).getOrientation();
		if (orientation==LinearLayout.HORIZONTAL)
			lp = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT);
		else 
			lp = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);

		buttonPanel = (LinearLayout) getWidget();
		buttonPanel.setOrientation(orientation);
		buttonPanel.setLayoutParams(lp);



		this.targetList=targetList;

		if (type.equals("alphanumeric")) {
			final OnClickListener cl = new OnClickListener(){
				@Override
				public void onClick(View v) {
					String ch = ((Button)v).getText().toString();
					Log.d("Strand","User pressed "+ch);
					//This shall apply a new Alpha filter on target.
					//First, remove any existing alpha filter.
					targetList.removeFilter(existing);

					//Wildcard? Do not add any filter.
					if(!ch.equals("*")) {							
						//Use ch string as unique id.
						existing = new WF_Column_Name_Filter(ch,ch,displayField,FilterType.prefix);
						targetList.addFilter(existing);
					}
					//running the filters will trigger redraw.
					targetList.draw();
				}
			};
			Button b;
			for (String c:alfabet) {
				b = new Button(ctx.getContext());
				//b.setLayoutParams(lp);
				b.setText(c);
				b.setOnClickListener(cl);
				buttonPanel.addView(b);
				Log.d("nils","Added button "+c);
			}

		} else if (type.equals("column")) {
			final OnClickListener dl = new OnClickListener() {
				@Override
				public void onClick(View v) {
					String ch = ((Button)v).getText().toString();
					Log.d("Strand","User pressed "+ch);
					//This shall apply a new Alpha filter on target.
					//First, remove any existing alpha filter.
					targetList.removeFilter(existing);
					existing = new WF_Column_Name_Filter(ch,ch,displayField,FilterType.exact);
					//existing = new WF_Column_Name_Filter(ch,ch,Col_Art)
					targetList.addFilter(existing);

					//running the filters will trigger redraw.
					targetList.draw();
				}
			};
			//Generate buttons from artlista. 
			//Pick fields that are of type Familj
			VariableConfiguration al = GlobalState.getInstance().getVariableConfiguration();
			Table t = al.getTable();
			List<List<String>> rows = t.getRowsContaining(selectionField,selectionPattern);
			if (rows!=null) {
				Log.d("nils","SORTERWIDGET: GETROWS RETURNED "+rows.size()+" FOR SELFIELD "+selectionField+" AND SELP: "+selectionPattern);

				int cIndex = t.getColumnIndex(displayField);
				if (cIndex != -1) {
					Set<String> txts = new TreeSet<String>();
					Button b;
					
					for(List<String>row:rows) {
						if (row.size()>cIndex)
							txts.add(row.get(cIndex));
						else {
							o.addRow("");
							o.addRedText("SorterWidget: column to sort on ["+displayField+"] was found in column# "+(cIndex+1)+" but the current row only contains "+row.size()+" elements");
							Log.e("vortex","SorterWidget: column to sort on ["+displayField+"] was found in column# "+(cIndex+1)+" but the row is shorter:"+row.size());
							Log.e("vortex","Current row: "+row.toString() );
							o.addRow("");
							o.addRow("Current Columns:"+t.getColumnHeaders().toString());
							o.addRow("Current row: "+row.toString() );
						}
					}
					for (String txt:txts)				
						if (txt !=null && txt.trim().length()>0) {
							b = new Button(ctx.getContext());
							b.setLayoutParams(lp);
							b.setText(txt);
							b.setOnClickListener(dl);
							buttonPanel.addView(b);				
							Log.d("nils","Added button "+txt+" length "+txt.length());
						}


				} else{
					o.addRow("");
					o.addRedText("Could not find column <display_field>: "+displayField+" in WF_SorterWidget. Check your xml for block_create_sort_widget");

				}
			} else {
				o.addRow("");
				o.addRedText("Found no rows for selection: ["+selectionField+"] and pattern ["+selectionPattern+"] in WF_SorterWidget. Check your xml for block_create_sort_widget");
			}
		}
		else 
			Log.e("parser","Sorry, unknown filtering type");


	}



	private void removeExistingFilter() {
		if (existing!=null) {
			targetList.removeFilter(existing);
			targetList.draw();
			existing = null;
		}

	}

	/* (non-Javadoc)
	 * @see com.teraim.vortex.dynamic.workflow_realizations.WF_Widget#hide()
	 */
	@Override
	public void hide() {
		super.hide();
		removeExistingFilter();
	}



}
