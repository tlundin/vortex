package com.teraim.vortex.dynamic.blocks;

import java.util.List;

import com.teraim.vortex.expr.Expr;
import com.teraim.vortex.utils.Expressor;
import com.teraim.vortex.utils.Expressor.Token;

/**
 * Startblock.
 * @author Terje
 *
 */
public  class StartBlock extends Block {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6860379561108690650L;
	final private String workflowName;
	final private String[] args;
	private String context;
	private Expr contextE;

	public StartBlock(String id,String[] args,String wfn, String context) {
		workflowName = wfn;
		this.args = args;
		this.context = context;
		this.blockId=id;
		//contextE = Expressor.analyze(context);
	}

	public String getName() {
		return workflowName;
	}

	public String[] getArgs() {
		return args;
	}
	
	public String getWorkFlowContext() {
		return context;
	}
}