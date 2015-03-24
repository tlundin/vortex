package com.teraim.vortex.dynamic.blocks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.dynamic.workflow_abstracts.Container;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Alphanumeric_Sorter;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Instance_List;
import com.teraim.vortex.dynamic.workflow_realizations.WF_List_UpdateOnSaveEvent;
import com.teraim.vortex.dynamic.workflow_realizations.WF_OnlyWithValue_Filter;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Static_List;
import com.teraim.vortex.dynamic.workflow_realizations.WF_TimeOrder_Sorter;

public class BlockCreateListEntriesFromFieldList extends Block {

	private static Map <String,List<List<String>>> cacheMap=new HashMap <String,List<List<String>>>();
	private String id,type,containerId,selectionPattern,selectionField,variatorColumn;
	boolean isVisible = true;
	public BlockCreateListEntriesFromFieldList(String id,String namn, String type,
			String containerId, String selectionPattern, String selectionField,String variatorColumn) {
		super();
		this.blockId=id;
		this.id = namn;
		this.type = type;
		this.containerId = containerId;
		this.selectionPattern = selectionPattern;
		this.selectionField = selectionField;
		this.variatorColumn=variatorColumn;
		

	}

	private static final long serialVersionUID = -5618217142115636960L;


	public void create(WF_Context myContext) {
		o = GlobalState.getInstance(myContext.getContext()).getLogger();
		WF_Static_List myList; 
		VariableConfiguration al = GlobalState.getInstance(myContext.getContext()).getVariableConfiguration();
		List<List<String>>rows = cacheMap==null?null:cacheMap.get(selectionField+selectionPattern);
		if (rows==null)
			rows  = al.getTable().getRowsContaining(selectionField, selectionPattern);
		if (rows==null||rows.size()==0) {
			Log.d("nils","no cached value found in BlockCreateEntries");
			o.addRow("");
			o.addRedText("Selectionfield: "+selectionField+" selectionPattern: "+selectionPattern+" returns zero rows! List cannot be created");
		} else {		
			cacheMap.put(selectionField+selectionPattern, rows);
			Log.d("nils","Number of rows in CreateEntrieFromList "+rows.size());
			//prefetch values from db.
			if (type.equals("selected_values_list")) {
				o.addRow("This is a selected values type list. Adding Time Order sorter.");
				myList =  new WF_List_UpdateOnSaveEvent(id,myContext,rows,isVisible);
				myList.addSorter(new WF_TimeOrder_Sorter());	
				o.addRow("Adding Filter Type: only instantiated");
				myList.addFilter(new WF_OnlyWithValue_Filter());
			}
			else { 
				if (type.equals("selection_list")) {
					o.addRow("This is a selection list. Adding Alphanumeric sorter.");
					myList = new WF_List_UpdateOnSaveEvent(id,myContext,rows,isVisible);
					myList.addSorter(new WF_Alphanumeric_Sorter());
				} 
				if (type.equals("instance_list")) {
					o.addRow("instance selection list. Time sorter.");
					myList = new WF_Instance_List(id,myContext,rows,variatorColumn,isVisible);
					myList.addSorter(new WF_TimeOrder_Sorter());	
				} else
				{
					//TODO: Find other solution
					myList = new WF_List_UpdateOnSaveEvent(id,myContext,rows,isVisible);
					myList.addSorter(new WF_Alphanumeric_Sorter());
				}
			}


			//myList.createEntriesFromRows(rows);
			//myList.draw();

			Container myContainer = myContext.getContainer(containerId);
			if (myContainer !=null) {
				myContainer.add(myList);
				myContext.addList(myList);		
				Log.d("nils","QQQ ADDED LIST "+myList.getId());
			} else {
				o.addRow("");
				o.addRedText("Failed to add listEntriesblock - could not find the container "+containerId);
			}

		}
	}
}
