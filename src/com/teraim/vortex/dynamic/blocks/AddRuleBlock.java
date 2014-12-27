package com.teraim.vortex.dynamic.blocks;

import android.util.Log;

import com.teraim.vortex.dynamic.types.Rule;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;

public  class AddRuleBlock extends Block {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2045031005203874390L;
	private Rule r;


	public AddRuleBlock(String id,String ruleName,String target, String condition, String action, String errorMsg) {
		this.r = new Rule(id,ruleName,target,condition,action,errorMsg);
		this.blockId=id;
	}

	public Rule getRule() {
		return r;
	}

	public void create(WF_Context myContext) {
		Log.d("nils","Create called in addRuleBlock. Target name: "+r.targetName);
		myContext.addRule(r.targetName, r);
	}

}