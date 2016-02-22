package com.teraim.fieldapp.dynamic.blocks;

import java.util.List;

import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;
import com.teraim.fieldapp.utils.Expressor.Token;

import static com.teraim.fieldapp.utils.Expressor.*;

/**
 * Startblock.
 * @author Terje
 *
 */
public  class StartBlock extends Block {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6860379561128690656L;
	final private String workflowName;
	final private String[] args;
	private String context;
	private List<EvalExpr> contextE=null;

	public StartBlock(String id,String[] args,String wfn, String context) {
		workflowName = wfn;
		this.args = args;
		this.context = context;
		this.blockId=id;
		if (context !=null)
			contextE = preCompileExpression(context);
		System.err.println("Bananas: "+((contextE == null)?"null":contextE.toString()));

	}

	public String getName() {
		return workflowName;
	}

	public String[] getArgs() {
		return args;
	}
	
	public List<EvalExpr> getWorkFlowContext() {
		return contextE;
	}
}