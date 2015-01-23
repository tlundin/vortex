package com.teraim.vortex.dynamic.blocks;

import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.types.Variable.DataType;
import com.teraim.vortex.utils.RuleExecutor;
import com.teraim.vortex.utils.RuleExecutor.TokenizedItem;

public class SetValueBlock extends Block {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9113802658084282749L;

	public enum ExecutionBehavior {
		constant,dynamic,update_flow
	}


	String target,expression;
	ExecutionBehavior executionBehaviour=ExecutionBehavior.update_flow;
	public SetValueBlock(String id,String target,String expression,String eb) {
		this.blockId = id;
		this.target=target;
		this.expression=expression;
		if (eb !=null) {
			for (ExecutionBehavior ex:ExecutionBehavior.values()) {
				if (eb.equals(ex.name()))
					executionBehaviour = ex;
			}
		}
		//Log.e("nils","EXECUTIONBEHAVIOR"+executionBehaviour.name());
	}


	public String getFormula() {
		return expression;
	}

	public String getMyVariable() {
		return target;
	}

	public ExecutionBehavior getBehavior() {
		return executionBehaviour;
	}


	RuleExecutor re;
	public String evaluate(GlobalState gs,String formula,
			List<TokenizedItem> tokens, boolean stringT) {
		//assume fail
		String strRes = null;
		re = RuleExecutor.getInstance(gs.getContext());
		if (stringT) 
			strRes = re.substituteForValue(tokens,formula,stringT);
		else {
			String subst =null;
			//substitute any variables or functions.
			subst = re.substituteForValue(tokens,formula,stringT);
			if (subst!=null) {
				strRes = re.parseExpression(formula,subst);

			} else 
				Log.e("nils","Formula null after substitution: ["+formula+"]");


			Log.d("nils","New eval returns "+strRes);
		}
		return strRes;
	}

}
