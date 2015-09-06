package com.teraim.vortex.dynamic.types;

import java.io.Serializable;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.expr.Expr;
import com.teraim.vortex.expr.SyntaxException;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.utils.RuleExecutor;
import com.teraim.vortex.utils.RuleExecutor.SubstiResult;
import com.teraim.vortex.utils.RuleExecutor.TokenizedItem;

public class Rule implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1965204853256767316L;
	public String targetName, condition, action, errorMsg,label,id;
	private Context ctx;
	private Type myType;
	private List<TokenizedItem> tokens;
	private RuleExecutor re;
	private boolean initDone = false;
	private LoggerI o;
	private int myTarget=-1;

	public Rule(String id,String ruleLabel, String target, String condition,
			String action, String errorMsg) {

		this.label=ruleLabel;
		this.id=id;
		this.targetName=target;
		this.condition=condition;
		this.errorMsg=errorMsg;
		myType = Type.WARNING;
		if (action!=null && action.equalsIgnoreCase("Error_severity"))
			myType = Type.ERROR;
		try {
			myTarget = Integer.parseInt(target);
		} catch (NumberFormatException e) {};
	}

	public enum Type {
		ERROR,
		WARNING;
	}

	public void init(GlobalState gs) {
		re = gs.getRuleExecutor();
		tokens = re.findTokens(condition,null);
		initDone=true;
		if (o==null)
			this.o = gs.getLogger();
	}

	//Execute Rule. Target will be colored accordingly.
	public boolean execute() throws SyntaxException {
		GlobalState gs = GlobalState.getInstance();

		if (!initDone)
			init(gs);
		Expr result=null;
		SubstiResult sr = re.substituteForValue(tokens,condition,false);
		Log.d("nils","CONDITION: "+condition);
		if (!sr.iAmAString()) {
			String subst = sr.result;
			if (subst!=null) {
				Log.d("nils","SUBST: "+subst+" isnull? "+(subst==null));		
				result = gs.getParser().parse(subst);
				if (result!=null && result.value()!=null) {
					Log.d("NILS","Result of rule eval was: "+result.value());
					return (result.value().intValue()==1);
				}
			} else {

				//return true if eval return null?
				Log.d("nils","Expr evaluates to null - return true");
				return true;
			}

		} else {
			o.addRow("");
			o.addRedText("Text type in Rule Condition. Will not work. Condition: "+condition);
			throw new SyntaxException();
		}
		return false;
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
		
		public int getTarget() {
			return myTarget;
		}

	}
