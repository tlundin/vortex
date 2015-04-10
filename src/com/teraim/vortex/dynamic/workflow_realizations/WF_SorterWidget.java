package com.teraim.vortex.dynamic.workflow_realizations;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout.LayoutParams;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.dynamic.types.Table;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Column_Name_Filter.FilterType;


public class WF_SorterWidget extends WF_Widget {

	private final String[] alfabet = {
			"*","ABC","DEF","GHI","JKL",
			"MN","OPQ","RS","T","UV",
			"WXYZ","�","�","�"};



	WF_Filter existing;
	WF_Static_List targetList;

	public WF_SorterWidget(String name,WF_Context ctx, String type, final WF_Static_List targetList,final String selectionField, final String displayField,String selectionPattern,boolean isVisible) {
		super(name,new LinearLayout(ctx.getContext()),isVisible,ctx);
		LinearLayout buttonPanel;
		o = GlobalState.getInstance().getLogger();
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
		buttonPanel = (LinearLayout) getWidget();
		buttonPanel.setOrientation(LinearLayout.VERTICAL);
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
				b.setLayoutParams(lp);
				b.setText(c);
				b.setOnClickListener(cl);
				buttonPanel.addView(b);
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
					Set<String> txts = new HashSet<String>();
					Button b;
					for(List<String>row:rows)
						txts.add(row.get(cIndex));
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
