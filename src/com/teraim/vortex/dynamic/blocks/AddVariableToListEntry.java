package com.teraim.vortex.dynamic.blocks;

import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.workflow_realizations.WF_ClickableField_Selection;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Static_List;

public class AddVariableToListEntry extends Block {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2748537558779469614L;
	boolean isVisible = true,isDisplayed=false,showHistorical=false;
	String targetField,targetList,format,varNameSuffix, initialValue;
	GlobalState gs;
	
	public AddVariableToListEntry(String id,String varNameSuffix,
			String targetList,String targetField, boolean isDisplayed,String format,boolean isVisible,boolean showHistorical,String initialValue) {
		super();
		this.blockId=id;
		this.isVisible = isVisible;
		this.targetField = targetField;
		this.targetList = targetList;
		this.format = format;
		this.varNameSuffix=varNameSuffix;
		this.isDisplayed=isDisplayed;
		this.showHistorical=showHistorical;
		this.initialValue=initialValue;
	} 


	public Variable create(WF_Context myContext) {
		gs = GlobalState.getInstance(myContext.getContext());
		o = gs.getLogger();
		VariableConfiguration al = gs.getVariableConfiguration();
		
		WF_Static_List l= myContext.getList(targetList);
			if (l!=null) {
				Log.d("nils","Found entry field in AddVariableToListEntry");
				Variable var = l.addVariableToListEntry(varNameSuffix,isDisplayed,targetField,format,isVisible,showHistorical,initialValue);
				if (var == null) {
					Log.e("nils","Didn't find list entry"+targetField+ " in AddVariableToListEntry");
				} else
					return var;
			} else
				Log.e("nils","Didn't find list in AddVariableToListEntry");
				
		return null;
	}
		
		
	
}
