package com.teraim.vortex.dynamic.blocks;

import java.util.Set;

import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.types.Workflow.Unit;
import com.teraim.vortex.dynamic.workflow_abstracts.Container;
import com.teraim.vortex.dynamic.workflow_realizations.WF_ClickableField_Selection;
import com.teraim.vortex.dynamic.workflow_realizations.WF_ClickableField_Selection_OnSave;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;

public class CreateEntryFieldBlock extends Block {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2013870148670474248L;
	String name,type,label,containerId,postLabel,initialValue;
	Unit unit;
	GlobalState gs;
	boolean isVisible = false,showHistorical;
	String format;

	public CreateEntryFieldBlock(String id,String name, 
			String containerId,boolean isVisible,String format,boolean showHistorical,String initialValue) {
		super();
		this.name = name;
		this.containerId=containerId;
		this.isVisible=isVisible;
		this.format = format;
		this.blockId=id;
		this.initialValue=initialValue;
		this.showHistorical=showHistorical;

	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}



	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}






	public Variable create(WF_Context myContext) {
		gs = GlobalState.getInstance(myContext.getContext());
		Container myContainer = myContext.getContainer(containerId);
		o = gs.getLogger();
		VariableConfiguration al = gs.getVariableConfiguration();
		Log.d("nils","NAME: "+name);
		Variable v = al.getVariableInstance(name,initialValue);
		if (v == null) {
			o.addRow("");
			o.addRedText("Variable "+name+" referenced in block_create_entry_field not found.");
			Log.d("nils","Variable "+name+" referenced in block_create_entry_field not found.");
			
			o.addRedText("Current keyChain: ["+gs.getCurrentKeyHash()+"]");
		} else	{	
			WF_ClickableField_Selection myField = new WF_ClickableField_Selection_OnSave(v.getLabel(),
					al.getBeskrivning(v.getBackingDataSet()),myContext,name,isVisible);
			Log.d("nils", "In CreateEntryField.Description: "+al.getBeskrivning(v.getBackingDataSet()));
			Log.d("nils","Backing data: "+v.getBackingDataSet().toString());
			myField.addVariable(v, true,format,true,showHistorical);
			myContext.addDrawable(v.getId(), myField);
			if(myContainer !=null) {
				myContainer.add(myField);
//				myField.refreshInputFields();	
				myField.refreshOutputFields();
			}

		}

		return v;
	}

	


}

