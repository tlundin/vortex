package com.teraim.vortex.dynamic.blocks;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.types.Variable.DataType;
import com.teraim.vortex.utils.RuleExecutor;
import com.teraim.vortex.utils.Tools;

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
				Set<Entry<String, DataType>> vars) {
			//assume fail
			re = RuleExecutor.getInstance(gs.getContext());
			int eval=STOP;
			Log.d("nils","Variables found: "+vars.size());
			String subst = re.substituteVariables(vars,formula,false);
			if (subst!=null) {
				String strRes = re.parseExpression(formula,subst);
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
				Log.e("nils","Substitution failed for formula ["+formula+"]");
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
