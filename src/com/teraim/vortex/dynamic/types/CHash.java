package com.teraim.vortex.dynamic.types;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.utils.Expressor;
import com.teraim.vortex.utils.Expressor.EvalExpr;

public class CHash implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6300633169769731018L;

	private final Map<String, String> keyHash;
	private final String contextS;
	private final String err;
	
	
	public CHash(String err) {
		keyHash=null;
		this.err=err;
		this.contextS="";
	}
	public CHash(String c, Map<String, String> keyHash) {
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
	 * @return current context.
	 * The context may contain variables, so the evaluation might change over time. 
	 * 
	 */
	
	public static CHash evaluate(List<EvalExpr> eContext) {
		
		GlobalState gs = GlobalState.getInstance();
		if (gs==null)
			return null;
		String err = null;
		Map<String, String> keyHash = null;

		LoggerI o = gs.getLogger();
		Log.d("vortex","In evaluate Context!!");

		if (eContext==null) {
			Log.d("vortex","No change to context. Return existing");
			return GlobalState.getInstance().getCurrentKeyHash();
		} else {
			keyHash = new HashMap<String, String>();
			//rawHash = new HashMap<String, Variable>();
			Log.d("nils","Evaluating context: "+eContext);
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
						Log.d("nils","found pair: "+pair);
						if (pair!=null&&!pair.isEmpty()) {
							String[] kv = pair.split("=");
							if (kv==null||kv.length<2) {
								err = "Could not split context on equal sign (=) for context "+cContext;
								break;
							} else {
								String arg = kv[0].trim();
								String val = kv[1].trim();
								Log.d("nils","Keypair: "+arg+","+val);

								if (val.isEmpty()||arg.isEmpty()) {
									err = "Empty key or value in context keypair for context "+cContext;
									break;
								} else {
									Log.d("nils","Added "+arg+","+val+" to current context");
									keyHash.put(arg, val);
								}
							}
						}
					}
				}
			}
			if (err!=null) {
				o.addRow("");
				o.addRedText(err);
				return new CHash(err);
			} else
				return new CHash(cContext,keyHash);
		}
	}
	
}