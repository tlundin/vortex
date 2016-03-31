package com.teraim.fieldapp.dynamic.types;

import java.io.Serializable;
import java.util.List;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.blocks.ButtonBlock;
import com.teraim.fieldapp.dynamic.blocks.RuleBlock;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;

/** Saves the relationship between status variables.
*/


public class StatusGraph implements Serializable {


	private static final long serialVersionUID = -7055792327173981534L;
	private GlobalState gs;


	public StatusGraph(GlobalState gs) {
		this.gs = gs;
	}
	
	public void parseButton(ButtonBlock bbl) {
		
		String statVar = bbl.getStatusVariable();
		 List<EvalExpr> buttonContext = bbl.getPrecompiledButtonContext();
		//use target workflow context if no button context.
		if (buttonContext==null && gs.getWfs()!=null && bbl.getTarget()!=null) {
			Workflow wf = gs.getWfs().get(bbl.getTarget());
			
		} else
			Log.e("vortex","either workflows or target null in parsebutton, statusgraph");
			
	}
	
	public void parseRule(RuleBlock rbl) {
				rbl.getRule();
	}
	
}
