package com.teraim.fieldapp.dynamic.blocks;

import java.util.Set;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Table;

public class BlockAddVariableToTable extends Block {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6892340768823185014L;
	String target=null,variableSuffix=null,format=null,id=null,initialValue=null;
	boolean displayOut=false,isVisible=true,showHistorical=false;
	
	
	public BlockAddVariableToTable(String id,String target, String variableSuffix,
			boolean displayOut,String format,boolean isVisible,boolean showHistorical, String initialValue 
			 ) {
		super();
		this.target = target;
		this.variableSuffix = variableSuffix;
		this.format = format;
		this.id = id;
		this.initialValue = initialValue;
		this.displayOut = displayOut;
		this.isVisible = isVisible;
		this.showHistorical = showHistorical;
	}
	
	
	public Set<Variable> create(WF_Context myContext) {

		final WF_Table l = myContext.getTable(target);
		o = GlobalState.getInstance().getLogger();
		if (l==null) {
			o.addRow("");
			o.addRedText("Couldn't find list with ID "+target+" in AddVariableToEveryListEntryBlock");
		} else {
			Log.d("nils","Calling AddVariableToTable for "+variableSuffix);
			l.addVariableToEveryCell(variableSuffix, displayOut,format,isVisible,showHistorical,initialValue);		
		}
		return null;
	}


}
