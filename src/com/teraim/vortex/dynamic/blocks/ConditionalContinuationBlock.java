package com.teraim.vortex.dynamic.blocks;

import java.util.List;

import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.utils.RuleExecutor;
import com.teraim.vortex.utils.RuleExecutor.SubstiResult;
import com.teraim.vortex.utils.RuleExecutor.TokenizedItem;

public class ConditionalContinuationBlock extends Block {

	String elseID,expr;
	List<String>variables;
	public ConditionalContinuationBlock(String id, List<String> varL,
			String expr, String elseBlockId) {
		this.blockId=id;
		this.variables=varL;
		this.expr=expr;
		this.elseID=elseBlockId;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -5923203475793337276L;
	public String getFormula() {
		return expr;
	}
	public String getElseId() {
		return elseID;
	}
    public final static int STOP = 1,JUMP=2,NEXT = 3;
    
    Integer lastEval = null;
    RuleExecutor re;
	public boolean evaluate(GlobalState gs,String formula,
				List<TokenizedItem> tokens) {
			//assume fail
			re = RuleExecutor.getInstance(gs.getContext());
			int eval=STOP;
			SubstiResult sr = re.substituteForValue(tokens,formula,false);
			
			if (sr.result!=null && !sr.iAmAString()) {
				String strRes = re.parseExpression(formula,sr.result);
				if (strRes != null) {
					if (Double.parseDouble(strRes)==1) {
						Log.d("nils","Evaluates to true");
						eval=NEXT;
					} else {
						eval=JUMP;
						Log.d("nils","Evaluates to false");
					}
				} else {
					eval=STOP;
				}
			} else {
				Log.e("nils","Substitution failed for formula ["+formula+"]. Text type to blame? ["+sr.iAmAString()+"]");
				eval=STOP;
			}
			
		
		boolean ret = lastEval==null?true:eval!=lastEval;
		lastEval = eval;
		return ret;
	}
	public Integer getCurrentEval() {
		return lastEval;
	}

	
}
