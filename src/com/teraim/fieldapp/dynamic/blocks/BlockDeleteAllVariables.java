package com.teraim.fieldapp.dynamic.blocks;

import static com.teraim.fieldapp.utils.Expressor.preCompileExpression;

import java.util.List;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.CHash;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_SorterWidget;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Static_List;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_TextBlockWidget;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;

public class BlockDeleteAllVariables extends Block {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1134485697631003990L;
	String label,context,pattern;
	private List<EvalExpr> contextE;
	
	public BlockDeleteAllVariables(String id, String label, String target, String pattern) {
		this.blockId=id;
		this.label=label;
		this.context=target;
		this.pattern=pattern;
		
		if (context !=null)
			contextE = preCompileExpression(context);
		System.err.println("Bananas: "+((contextE == null)?"null":contextE.toString()));
	}

	
	
	
	public void create(WF_Context ctx) {
		o = GlobalState.getInstance().getLogger();
		o.addRow("Now deleting all variables under Context: ["+context+"]");
		CHash evaluatedContext = CHash.evaluate(contextE);
		if (evaluatedContext.isOk()) {
		
			int entriesDeleted = GlobalState.getInstance().getDb().deleteAllVariablesUsingKey(evaluatedContext.getContext());
			o.addRow("Deleted "+entriesDeleted+" entries!");
		} else {
			o.addRow("");
			o.addRedText("The target context in Delete block contains an error. Error: "+evaluatedContext);
		}
		
	}	
	
			
		
		
}


