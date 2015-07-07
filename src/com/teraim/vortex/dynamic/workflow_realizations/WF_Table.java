package com.teraim.vortex.dynamic.workflow_realizations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.R;
import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.dynamic.workflow_abstracts.Filter;
import com.teraim.vortex.dynamic.workflow_abstracts.Listable;
import com.teraim.vortex.dynamic.workflow_abstracts.Sorter;
import com.teraim.vortex.utils.Tools;

public class WF_Table extends WF_List {

	//protected final List<Listable> tableRows = new  ArrayList<Listable>(); //Instantiated in constructor
	protected final List<Filter> myFilters=new ArrayList<Filter>();
	protected final List<Sorter> mySorters=new ArrayList<Sorter>();
	protected List<? extends Listable> filteredList;

	protected WF_Context myContext;
	protected GlobalState gs;
	protected VariableConfiguration al;
	private String myVariator;
	private LinearLayout headerV;
	private LayoutInflater inflater ;
	private TableLayout tableView;

	private 		int rowNumber=0;
	private final String ColHeadId = "TableHeader";
	private WF_Table_Row headerRow;
	
	//How about using the Container's panel?? TODO
	public WF_Table(String id,boolean isVisible,WF_Context ctx,String namePrefix,String variatorColumn, View tableV) {
		super(id,isVisible,ctx,tableV);	
		this.tableView = (TableLayout)tableV.findViewById(R.id.table);;
		myContext = ctx;
		gs = GlobalState.getInstance();
		o = gs.getLogger();
		al = gs.getVariableConfiguration();
		myVariator = variatorColumn;
		//myTable = new GridView(ctx.getContext());
		
		Map<String, Map<String, String>> allInstances = gs.getDb().preFetchValues(ctx.getKeyHash(), namePrefix, variatorColumn);		
		Log.d("nils","in update entry fields. AllInstances contain "+allInstances.size());
		//Create rows.
		inflater = (LayoutInflater)ctx.getContext().getSystemService
				(Context.LAYOUT_INFLATER_SERVICE); 
		
		//Add the header.
		headerRow = new WF_Table_Row(ColHeadId,inflater.inflate(R.layout.header_table_row, null),myContext,true);
		//Add a first empty cell 
		headerRow.addEmptyCell();
		tableView.addView(headerRow.getWidget());
	}
	
	//Creates new rows and adds dataset to each.
	public void addRows(List<List<String>> rows) {
		for (List<String> row:rows) {
			addRow(row);
		}		
	}
	//Create a new row + dataset.
	public void addRow(List<String> row) {		
		WF_Table_Row rowWidget = new WF_Table_Row((rowNumber++)+"",inflater.inflate(R.layout.table_row, null),myContext,true);
		rowWidget.addRow(row);
		list.add(rowWidget);
	}
	
	public void addColumns(List<String> labels,
			List<String> columnKeyL, String type, String width) {

		boolean useColumKeyAsHeader = labels==null;
		
		//Add as many columns as there are keys. Check if labels are used or if the columnkey should be used.
		if (useColumKeyAsHeader&&labels.size()<columnKeyL.size()) {
			Log.e("vortex","There are too few labels in addColumns! Labels: "+labels.toString());
			o.addRow("");
			o.addRedText("There are too few labels in addColumns! Labels: "+labels.toString());
			return;
		}
		if (columnKeyL==null) {
			Log.e("vortex","columnkeys missing for addColumn!!");
			o.addRow("");
			o.addRow("columnkeys missing for addColumn!!");
			return;
		}
		String k,l;
		for (int i=0;i<columnKeyL.size();i++) {
			
			k = columnKeyL.get(i);
			l = (useColumKeyAsHeader?null:labels.get(i));
			//Add the column
			addColumn(l,k,type,width);
			
		}
		
		
	}
	
	
	
	public void addColumn(String header, String colKey, String type, String width) {
		//Copy the key and add the variator.
		Map<String, String> colHash = Tools.copyKeyHash(myContext.getKeyHash());
		colHash.put(myVariator, colKey);
		
		//Add header to the header row? Duh!!
		headerRow.addHeaderCell(header);
		
		
		//Create all row entries.
		for (Listable l:list) {
			WF_Table_Row wft = (WF_Table_Row)l;
			wft.addCell(header, colKey, colHash, type, width);
		}
	}
	



	@Override
	public TableLayout getWidget() {
		return tableView;
	}

	@Override
	protected void prepareDraw() {
		tableView.removeAllViews();
		tableView.addView(headerRow.getWidget());
		
	}

	

	

	
	

	
	


}
