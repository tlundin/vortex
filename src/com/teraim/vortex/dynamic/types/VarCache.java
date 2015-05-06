package com.teraim.vortex.dynamic.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.dynamic.types.Variable.DataType;
import com.teraim.vortex.log.LoggerI;



public class VarCache {

	private final static String vCol = "value";	
	String varID;
	private Map<String,List<Variable>> cache = new ConcurrentHashMap<String,List<Variable>>();

	//returns a variable.
	//If no cached instance exist, it will return a new instance.

	private GlobalState gs;
	private LoggerI o;

	public VarCache(GlobalState gs) {
		this.gs = gs;
		this.o = gs.getLogger();

	}

	private class KeyException extends java.lang.Exception {

		private static final long serialVersionUID = 458116191810109227L;

	}


	public Variable getVariable(String varId) {
		return getVariable(gs.getCurrentKeyHash(),varId,null,null);
	}

	public Variable getVariable(String varId,String defaultValue) {
		return getVariable(gs.getCurrentKeyHash(),varId,defaultValue,null);
	}

	//fetch using current default keymap.
	//fetch using keychain + varId.
	public Variable getVariable(Map<String,String> context, String varId,String defaultValue, Boolean hasValueInDB) {
		//Log.d("nils","in CACHE GetVariable for "+varId);
		List<Variable> ret = cache.get(varId.toLowerCase());
		
		boolean newA = false;
		if (ret==null) {
			//Log.d("nils","Creating new CacheList entry for "+varId);			
			ret = new ArrayList<Variable>();
			cache.put(varId.toLowerCase(), ret);
			newA=true;
			//Log.d("nils","Cache now contains: ");
			//for(String s:cache.keySet())
			//	Log.d("nils",s);
		} else {
			if (ret.size()==1 && ret.get(0).getKeyChain()==null) {
				//Log.d("nils","Found Global variable in Cache: "+varId+" Value: "+ret.get(0).getValue());
				return ret.get(0);
			}
		}
		List<String> row = gs.getVariableConfiguration().getCompleteVariableDefinition(varId);
		if (row==null) {
			Log.e("nils","Variable definition missing for "+varId);
			o.addRow("");
			o.addYellowText("Variabel definition missing for "+varId);
			if (newA)
				cache.remove(ret);
			return null;
		}

		
		//Find the variable with specified key from cache.
		Map<String, String> instKey;
		try {
			instKey = buildDbKey(gs.getVariableConfiguration().getKeyChain(row),context);
		} catch (KeyException e) {
			Log.e("nils","Current context is not complete! ");
			if (context!=null)
				Log.e("nils","KeyChain: "+gs.getVariableConfiguration().getKeyChain(row)+" Context: "+context.toString());
			if (newA)
				cache.remove(ret);
			return null;
		}
		Variable v = find(ret,instKey);
		//Not there? Create new. Add to cache. 
		//Here we know that the variable is not in the cache. So here we should insert historical or default value.
		if (v == null) {
			//Log.d("nils","Variable not found. Inserting"+varId+" with chain "+(instKey==null?"null":instKey.toString()));							
			String header = gs.getVariableConfiguration().getVarLabel(row);
			DataType type = gs.getVariableConfiguration().getnumType(row);
			if (type == DataType.array)
				v= new ArrayVariable(varId,header,row,instKey,gs,vCol,defaultValue,hasValueInDB);
			else
				v = new Variable(varId,header,row,instKey,gs,vCol,defaultValue,hasValueInDB);
			//add to cache.		
			ret.add(v);
		}
//		Log.d("nils","Cache now has "+cache.size()+" rows");
		return v;
	}


	private Variable find(List<Variable> vars,Map <String,String> chainToFind) {
		for (Variable v:vars) {
			if (eq(chainToFind,v.getKeyChain())) {
				//Log.d("nils","found cached entry for VAR: "+v.getId()+" Value: "+v.getValue()+" isInvalidated: "+v.isInvalidated());
				return v;
			}
		}
		return null;
	}

	//Check if two maps are equal
	private boolean eq(Map<String,String> chainToFind, Map<String,String> varChain) {
//		Log.d("nils","in Varcache EQ");
		if (chainToFind==null && varChain==null)
			return true;
		if (chainToFind==null||varChain==null||chainToFind.size()<varChain.size()) {
//			Log.d("nils","eq returns false. Trying to match: "+(chainToFind==null?"null":chainToFind.toString())+" with: "+(varChain==null?"null":varChain.toString()));
			return false;
		}
//		Log.d("nils","ChainToFind: "+chainToFind.toString());
//		Log.d("nils","VarChain: "+varChain.toString());
		for (String key:varChain.keySet()) {
			if (chainToFind.get(key)==null) {
//				Log.d("nils","eq returns false. Key "+key+" is not in Chaintofind: "+chainToFind.toString());
				return false;
			}
			if (!chainToFind.get(key).equals(varChain.get(key))) {
//				Log.d("nils","eq returns false. Key "+key+" has different value than varchain with same key: "+chainToFind.get(key)+","+varChain.get(key));
				return false;
			}

		}
		return true;
	}









	private Map<String, String> buildDbKey(String keyChain,
			Map<String, String> cMap) throws KeyException {
		boolean throwE =  false;
//		Log.d("nils","Building DB key for var with keyChain "+keyChain);
		if (keyChain==null||keyChain.isEmpty()) {
//			Log.d("nils","Keychain null or empty. returning from buildDBKey");
			return null;
		}
		//if (cMap!=null){
		//	Log.d("nils","Current context: "+cMap.toString());
		//	Log.d("nils","Key values:"+cMap.entrySet().toString());
		//}
		//Log.d("nils","Keys in chain:"+keyChain);

		String[] keys = keyChain.split("\\|");
		Map<String, String> vMap = new HashMap<String,String>();
		for (String key:keys) {	
			String value = null;
			if(cMap!=null) 
				value = cMap.get(key);
			if (value!=null) {
				vMap.put(key, value);
				//Log.d("nils","Adding keychain key:"+key+" value: "+value);
			}
			else {
				Log.e("nils","Couldn't find key "+key+" in current context");
				throwE = true;
			}
		}
		if (!throwE)
			return vMap;
		else
			throw new KeyException();
	}



	public void invalidateOnName(String varId) {
		Log.d("nils","invalidating variable named "+varId);
		List<Variable> vl = cache.get(varId.toLowerCase());
		
		if (vl!=null) {
			Log.d("nils","Found "+vl.size()+" instances. Invalidating..");
			for (Variable v:vl)
				v.invalidate();
		} else {
			Log.d("nils","Could not find variable "+varId);
			Log.d("nils","Current key SET: ");
			for (String s: cache.keySet())
				Log.d("nils"," KEY: "+s);
		}

	}

	//Invalidate all variables matching the keyChain.
	public void invalidateOnKey(Map<String, String> keyChain) {
		Set<String> vars = cache.keySet();
		Log.d("nils","erasing all variables matching "+keyChain.toString());
		List<Variable>vl;
		for (String var:vars) {
			//Take first in each list.
			vl=cache.get(var.toLowerCase());
			if (vl.size()==0) {
				Log.e("nils","Size zero varlist for variable ID: "+var);
			} else {
				if (eq(vl.get(0).getKeyChain(),keyChain)) {
					//Log.d("nils","Found match for variable "+vl.get(0).getId());
					for (Variable v:vl)
						v.invalidate();
				}
			}

		}
	}
	
	public void invalidateAll() {
		cache.clear();
	}

	public  Collection<List<Variable>> getVariables() {
		return cache.values();
	}

	public void put(String varId, Variable v) {
		List<Variable> ret = cache.get(varId.toLowerCase());
		
		boolean newA = false;
		if (ret==null) {
//			Log.d("nils","Creating new CacheList entry for "+varId);			
			ret = new ArrayList<Variable>();
			cache.put(varId.toLowerCase(), ret);
			newA=true;
			//Log.d("nils","Cache now contains: ");
			//for(String s:cache.keySet())
			//	Log.d("nils",s);
		}
	}


}
