package com.teraim.fieldapp.dynamic.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.Variable.DataType;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.utils.Tools;



public class VarCache {

	private final static String vCol = "value";	
	String varID;
	private Map<String,List<Variable>> cache = new ConcurrentHashMap<String,List<Variable>>();

	//returns a variable.
	//If no cached instance exist, it will return a new instance.

	private GlobalState gs;
	private LoggerI o;
	//private VariableConfiguration al;

	public VarCache(GlobalState gs) {
		this.gs = gs;
		//al = gs.getVariableConfiguration();
		this.o = gs.getLogger();

	}

	private class KeyException extends java.lang.Exception {

		private static final long serialVersionUID = 458116191810109227L;

	}

	public void put(Variable v) {
		List<Variable> ret = cache.get(v.getId().toLowerCase());
		if (ret== null) {
			ret = new ArrayList<Variable>();
			cache.put(v.getId().toLowerCase(), ret);
			ret.add(v);
			return;
		}
		//check that it doesnt exist already.
		if (find(ret,v.getKeyChain())==null)
				ret.add(v);
		return;
				
		
	}

	public Variable getVariable(String varId) {
		return getVariable(gs.getCurrentKeyMap(),varId,null,null);
	}

	public Variable getVariable(String varId,String defaultValue) {
		return getVariable(gs.getCurrentKeyMap(),varId,defaultValue,null);
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
			o.addYellowText("Variable definition missing for "+varId);
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
			o.addRow("");
			o.addRedText("Context incomplete for " +varId+" (context and keychain do not match or there is a null value)");
			if (context!=null) {
				o.addRow("");
				o.addRedText("KeyChain: "+gs.getVariableConfiguration().getKeyChain(row)+" Context: "+context.toString());
				Log.e("nils","KeyChain: "+gs.getVariableConfiguration().getKeyChain(row)+" Context: "+context.toString());
			}
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
				Log.e("nils","Couldn't find key "+key+" in current context, or value is null");
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
			//			Log.d("nils","Current key SET: ");
			//			for (String s: cache.keySet())
			//				Log.d("nils"," KEY: "+s);
		}

	}

	//Invalidate all variables in any group containing at least one instance matching the keyChain.
	
	public void invalidateOnKey(Map<String, String> keyChain) {
		Set<String> varNames = cache.keySet();
		Log.d("nils","erasing all variables matching "+keyChain.toString());
		List<Variable>vl;
		for (String varName:varNames) {
			//Take first in each list.
			
			vl=cache.get(varName.toLowerCase());
			if (vl.size()==0) {
				Log.e("nils","Size zero varlist for variable ID: "+varName);
			} else {
				if (eq(vl.get(0).getKeyChain(),keyChain)) {
					//Log.d("nils","Found match for variable "+vl.get(0).getId());
					//invalidating the variable for all keychains
					for (Variable v:vl)
						v.invalidate();
				}
			}

		}
	}

	public void invalidateAll() {
		cache.clear();
	}

	public  Collection<List<Variable>> getAllVariablesCurrentlyInCache() {
		return cache.values();
	}

	//Find all variables in cache with a given keyhash that belongs to the given group.
	public List<Variable> findVariablesBelongingToGroup(Map<String, String> keyChain, String groupName) {
		Log.d("vortex","In FindVariablesBelongingToGroup");
		//Identify matching variables.
		Set<String> varNames = cache.keySet();
		Log.d("vortex","find variables with key "+keyChain.toString()+" and size of cache "+cache.size());
		boolean found = false;
		groupName = groupName.toLowerCase();
		Set<String>myKeys = new HashSet<String>();
		for (String varName:varNames) {
			Log.d("vortex","Looking at varName: "+varName);
			//Take first in each list.
			if (varName.startsWith(groupName)) {
				Log.d("vortex","found one: "+varName);
				myKeys.add(varName);
			} 

		}
		if (myKeys.isEmpty()) {
			Log.d("vortex","found no variable of group "+groupName);
			return null;
		}
		List<Variable>resultSet = new ArrayList<Variable>();
		//Find all variables with key above.
		Log.d("vortex","myKeys has "+myKeys.size()+" members");
		for (String key:myKeys) {
			List<Variable> vList = cache.get(key);
			//Go thro list to find var.
			for (Variable v:vList) { 
				if (Tools.sameKeys(v.getKeyChain(),keyChain)) {
					Log.d("vortex","Found match: "+v.getId());
					resultSet.add(v);
					break;
				}
			}			
		}
		return resultSet;
	}
	

	public Variable getVariableUsingKey(Map<String, String> keyChain, String varId) {
		return getVariable(keyChain, varId, null,null);
		//return new Variable(varId,null,null,keyChain,gs,"value",null);
	}

	//A variable type that will not allow its keychain to be changed.
	public Variable getFixedVariableInstance(Map<String, String> keyChain, String varId,String defaultValue) {
		List<String> row = gs.getVariableConfiguration().getCompleteVariableDefinition(varId);
		return new FixedVariable(varId,gs.getVariableConfiguration().getEntryLabel(row),row,keyChain,gs,defaultValue,true);
	}	


	//A variable that is given a value at start.	
	public Variable getCheckedVariable(Map<String, String> keyChain,String varId, String value, Boolean wasInDatabase) {
		return getVariable(keyChain,varId,value,wasInDatabase);
	}
	//A variable that is given a value at start.	

	public Variable getCheckedVariable(String varId, String value, Boolean wasInDatabase) {
		return getVariable(gs.getCurrentKeyMap(),varId,value,wasInDatabase);
	}

	public String getVariableValue(Map<String, String> keyChain, String varId) {
		Variable v = getVariableUsingKey(keyChain,varId);
		if (v == null) {
			Log.e("nils","Varcache returned null!!");
			return null;
		}
		return v.getValue();
	}

	public void destroy() {
		
	}



}
