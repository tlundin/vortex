package com.teraim.fieldapp.dynamic.types;


import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;

public class DB_Context implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6300633169769731018L;

	private final Map<String, String> keyHash;
	private final String contextS;
	private final String err;

	public DB_Context(String err) {
		keyHash=null;
		this.err=err;
		this.contextS="";
	}
	public DB_Context(String c, Map<String, String> keyHash) {
		this.keyHash = keyHash;
		this.contextS=c;
		err = null;
	}
		
	public Map<String, String> getContext() {
		return keyHash;
	}

	@Override
	public String toString() {
		if (err!=null)
			return err;
		if (contextS!=null)
			return contextS;
		if (keyHash!=null)
			return keyHash.toString();
		else 
			return "NULL";
	}
	public boolean isOk() {
		return err==null;
	}
	
	/**
	 * 
	 * @return a variable cache containing the variables for current context (as given by eContext)
	 * The context may contain variables, so the evaluation might change over time. 
	 * 
	 */
	
	public static DB_Context evaluate(List<EvalExpr> eContext) {
		
		String err = null;
		Map<String, String> keyHash = null;

		LoggerI o = GlobalState.getInstance().getLogger();
		//Log.d("vortex","In evaluate Context!!");

		if (eContext==null) {
			Log.d("vortex","No change to context: "+GlobalState.getInstance().getVariableCache().getContext());
			return GlobalState.getInstance().getVariableCache().getContext();
		} else {
			keyHash = new HashMap<String, String>();
			//rawHash = new HashMap<String, Variable>();
			//Log.d("nils","Evaluating context: "+eContext);
			//Returns fully evaluated context as a string
			String cContext = Expressor.analyze(eContext);
			if (cContext==null) {
				err = "Context syntax error when evaluating precompiled context: "+eContext.toString();

			} else {
				String[] pairs = cContext.split(",");
				if (pairs==null||pairs.length==0) {
					o.addRow("Could not split context on comma (,)");
					err = "Could not split context on comma (,). for context "+cContext;

				} else {
					for (String pair:pairs) {
						//Log.d("nils","found pair: "+pair);
						if (pair!=null&&!pair.isEmpty()) {
							String[] kv = pair.split("=");
							if (kv==null||kv.length<2) {
								err = "Could not split context on equal sign (=) for context "+cContext;
								break;
							} else {
								String arg = kv[0].trim();
								String val = kv[1].trim();
								//Log.d("nils","Keypair: "+arg+","+val);
								
								if (val.isEmpty()||arg.isEmpty()) {
									err = "Empty key or value in context keypair for context "+cContext;
									break;
								} 
								
								for (char c:val.toCharArray()) {
									if(!Character.isLetterOrDigit(c)) {
										err = "The literal "+val+"contains non alfabetic-nonnumeric characters. Did you forget braces? [ ] ";
										break;	
									}
								}
								if (err==null)
									keyHash.put(arg, val);
								//Log.d("nils","Added "+arg+","+val+" to current context");

							}
						}
					}
				}
			}
			if (err!=null) {
				o.addRow("");
				o.addRedText(err);
				return new DB_Context(err);
			} else {
				Log.d("vortex","CONTEXT CHANGE: "+keyHash);
				return new DB_Context(cContext,keyHash);
			}
		}
	}
	
}