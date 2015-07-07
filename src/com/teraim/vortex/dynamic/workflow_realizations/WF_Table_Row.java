package com.teraim.vortex.dynamic.workflow_realizations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.teraim.vortex.R;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.workflow_abstracts.Listable;

public class WF_Table_Row extends WF_Widget implements Listable,Comparable<Listable> {
	List<String> myRow;
	List<WF_Cell_Widget> myColumns;
	private WF_Context myContext;
	private LinearLayout myBody;
	private TextView headerT;
	private String id;
	
	public WF_Table_Row(String id,View v,WF_Context ctx,boolean isVisible) {
		super(id,v,isVisible,ctx);
		myColumns=null;
		myContext = ctx;
		headerT = (TextView)v.findViewById(R.id.headerT);
		this.id=id;
		//Log.d("vortex","Added header "+this.getLabel() );
		
	}
	//Add the dataprovider.
	public void addRow(List<String> row) {
		myRow=row;
		String label = getLabel();
		if (label==null) {
			Log.e("vortex","label null for row with id "+id);
			label="*null*";
		}
		//Headertext is not there for header row..
		if (headerT!=null)
			headerT.setText(label);
	}
	
	
	@Override
	public String getSortableField(String columnId) {
		return al.getTable().getElement(columnId, myRow);
	}

	@Override
	public String getKey() {
		return al.getVarName(myRow);
	}

	//Not supported.
	@Override
	public long getTimeStamp() {
		return 0;
	}

	
	
	@Override
	public boolean hasValue() {
		if (myColumns==null)
			return false;
		for (WF_Cell_Widget w:myColumns)
			if (w.hasValue())
				return true;
		
		return false;
			
	}

	@Override
	public String getLabel() {
		return al.getEntryLabel(myRow);
	}

	@Override
	public void refresh() {
		if (myColumns==null)
			return ;
		for (WF_Cell_Widget w:myColumns)
			w.refresh();
	}

	@Override
	public Set<Variable> getAssociatedVariables() {
		// TODO Auto-generated method stub
		return null;
	}

		
	@Override
	public int compareTo(Listable other) {
		return this.getLabel().compareTo(other.getLabel());
	}

	public void addEmptyCell() {
		((TableRow)this.getWidget()).addView(LayoutInflater.from(myContext.getContext()).inflate(R.layout.cell_field_normal,null));
	}
	
	//Add a cell of purely graphical nature.
	public void addHeaderCell(String label) {
		View headerC = LayoutInflater.from(myContext.getContext()).inflate(R.layout.cell_field_header,null);
		TextView headerT = (TextView)headerC.findViewById(R.id.headerT);
		headerT.setText(label);
		((TableRow)this.getWidget()).addView(headerC);
	}
	
	//Add a Vortex Cell.
	public void addCell(String colHeader, String colKey, Map<String,String> columnKeyHash, String type, String width) {
		Log.d("vortex", "in AddColumn!!");
		if (myColumns==null)
			myColumns = new ArrayList<WF_Cell_Widget>();
		WF_Cell_Widget widget = new WF_Cell_Widget(getLabel(), al.getDescription(myRow),
				myContext, this.getId()+colKey,true);
		myColumns.add(widget);
		//TODO:ADD FORMAT! NULL BELOW
		//SHOW HISTORICAL IS TRUE!
		//Variable v= al.getVariableUsingKey(columnKeyHash, this.getKey());
		//if (v!=null)
		//	widget.addVariable(v, true,null,true,true);	
		
		
		((TableRow)getWidget()).addView(widget.getWidget());
	}
	


}
