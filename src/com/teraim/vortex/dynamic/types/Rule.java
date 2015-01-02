package com.teraim.vortex.dynamic.types;

import java.io.Serializable;
import java.util.Set;
import java.util.Map.Entry;

import android.content.Context;
import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.types.Variable.DataType;
import com.teraim.vortex.exceptions.RuleException;
import com.teraim.vortex.expr.Expr;
import com.teraim.vortex.expr.Parser;
import com.teraim.vortex.expr.SyntaxException;
import com.teraim.vortex.utils.RuleExecutor;

public class Rule implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1965204853256767315L;
	public String targetName, condition, action, errorMsg,label,id;
	private Context ctx;
	private Type myType;
	private Set<Entry<String, DataType>> vars;
	private RuleExecutor re;
	private boolean initDone = false;

	public Rule(String id,String ruleLabel, String target, String condition,
			String action, String errorMsg) {

		this.label=ruleLabel;
		this.id=id;
		this.targetName=target;
		this.condition=condition;
		this.errorMsg=errorMsg;
		myType = Type.WARNING;
		if (action!=null && action.equals("Error_severity"))
			myType = Type.ERROR;



	}

	public enum Type {
		ERROR,
		WARNING;
	}

	public void init() {
		re = RuleExecutor.getInstance(GlobalState.getInstance(ctx).getContext());
		vars = re.parseFormula(condition,null);
		initDone=true;
	}

	//Execute Rule. Target will be colored accordingly.
	public boolean execute() throws SyntaxException {
		GlobalState gs = GlobalState.getInstance(ctx);

		if (!initDone)
			init();
		Expr result=null;
		String subst = re.substituteVariables(vars,condition,false);
		Log.d("nils","CONDITION: "+condition);
		if (vars!=null && subst!=null) {
			Log.d("nils","VARS: "+vars.toString());
			Log.d("nils","SUBST: "+subst+" isnull? "+(subst==null));		
			result = gs.getParser().parse(subst);
			if (result!=null && result.value()!=null) {
			Log.d("NILS","Result of rule eval was: "+result.value());
			return (result.value().intValue()==1);
			}
		}
		//return true if eval return null?
		Log.d("nils","Expr evaluates to null - return true");
		return true;
	}

	public String getRuleText() {
		return errorMsg;
	}

	public String getRuleHeader() {
		return label;
	}

	public Type getType() {
		return myType;
	}

}