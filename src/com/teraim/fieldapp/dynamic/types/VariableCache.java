package com.teraim.fieldapp.dynamic.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.Variable.DataType;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.utils.DbHelper.TmpVal;
import com.teraim.fieldapp.utils.Tools;



public class VariableCache {

	private final static String vCol = "value";	
	String varID;
	//private Map<String,List<Variable>> oldcache = new ConcurrentHashMap<String,List<Variable>>();
	private Map<Map<String,String>,Map<String,Variable>> newcache = new HashMap<Map<String,String>,Map<String,Variable>>();
	private GlobalState gs;
	private LoggerI o;
	private Map<String, Variable> currentCache,globalCache;
	private Map<String, String> currentHash;
	private DB_Context myDbContext;


	public VariableCache(GlobalState gs) {
		globalCache = this.createOrGetCache(null);
		currentCache = globalCache;
		myDbContext=new DB_Context("",null);
		this.gs = gs;
		o = gs.getLogger();
		
		scheduleTaskExecutor = Executors.newScheduledThreadPool(5);
		
		// This schedule a runnable task every 2 minutes
		scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
		  public void run() {
			  
			  if (!dbQueue.isEmpty())
	        	   GlobalState.getInstance().getDb().persistQueue(dbQueue);
		  }
		}, 0, 5, TimeUnit.SECONDS);
	}

	public void setCurrentContext(DB_Context context) {
		currentHash = context.getContext();
		currentCache = this.createOrGetCache(currentHash);
		myDbContext = context;
	}

	public DB_Context getContext() {
		return myDbContext;
	}


	public Map<String,Variable> createOrGetCache(Map<String,String> myKeyHash) {
		Map<String, Variable> ret = newcache.get(myKeyHash);
		if (ret == null) {
			if (myKeyHash!=null)
				Log.d("vortex","Creating Cache for "+myKeyHash+" hash: "+myKeyHash.hashCode());
			else
				Log.d("vortex","Creating Cache for keyhash null");
			Log.d("vortex","newCache keys: ");
			for (Map<String,String> key:newcache.keySet()) {
				if (key!=null)
						Log.d("vortex",key.toString()+", HASH: "+key.hashCode());
				else
					Log.d("vortex","*NULL*");
			}
			ret = createAllVariablesForKey(myKeyHash);
			if (ret==null) {
				Log.e("vortex","No variables found in db for "+myKeyHash+". Creating empty hash");
				ret = new HashMap<String,Variable>();
			}
			
			Map<String, String> copy = myKeyHash==null?null:Tools.copyKeyHash(myKeyHash);
			newcache.put(copy, ret);
		} else
			Log.d("vortex","Cache already exists");
		return ret;
	}

	public void refreshCache(Map<String,String> myKeyHash) {
		Map<String, Variable> ret = newcache.remove(myKeyHash);
		if (ret!=null)
			createOrGetCache(myKeyHash);
		else
			Log.e("vortex","key hash does not exist in refreshcache");
	}


	Map<String, Variable> createAllVariablesForKey(Map<String,String> myKeyHash) {
		long time = System.currentTimeMillis();
		Map<String, Variable> ret=null;
		Map<String, TmpVal> map = GlobalState.getInstance().getDb().preFetchValuesForAllMatchingKeyV(myKeyHash);
		if (map!=null) {
			//Create variables.
			Variable v;
			GlobalState gs = GlobalState.getInstance();
			if (!map.isEmpty()) {
				ret = new HashMap<String, Variable>();
				for (String varName:map.keySet()) {

					TmpVal vals = map.get(varName);
					List<String> row = gs.getVariableConfiguration().getCompleteVariableDefinition(varName);
					String header = gs.getVariableConfiguration().getVarLabel(row);
					DataType type = gs.getVariableConfiguration().getnumType(row);
					if (type == DataType.array)
						v= new ArrayVariable(varName,header,row,myKeyHash,gs,vCol,vals.norm,true,vals.hist);
					else
						v = new Variable(varName,header,row,myKeyHash,gs,vCol,vals.norm,true,vals.hist);
					ret.put(varName, v);
				}
			} else
				Log.d("vortex","Map empty in CreateAllVariablesForKey");


		}
		long ctime = System.currentTimeMillis();
		Log.d("vortex","Generating all variables took: "+(ctime-time)+" ms");
		Log.d("vortex","Key: "+myKeyHash);
		Log.d("vortex", "Variables found: "+(ret==null?"null":ret.keySet()));
		return ret;
	}


	public void put(Variable v) {
		currentCache.put(v.getId().toLowerCase(),v);
	}

	public Variable getGlobalVariable(String varId) {
		return getVariable(null,globalCache,varId,null,null);
	}


	public Variable getVariable(String varId) {
		return getVariable(currentHash,currentCache,varId,null,null);
	}

	public Variable getVariable(String varId,String defaultValue, int dummy) {
		return getVariable(currentHash,currentCache,varId,defaultValue,null);
	}

	public Variable getVariable(Map<String, String> keyChain, String varId) {
		return getVariable(keyChain,createOrGetCache(keyChain), varId, null,null);
		//return new Variable(varId,null,null,keyChain,gs,"value",null);
	}


	//A variable that is given a value at start.	
	public Variable getCheckedVariable(Map<String, String> keyChain,String varId, String value, Boolean wasInDatabase) {
		return getVariable(keyChain,createOrGetCache(keyChain),varId,value,wasInDatabase);
	}


	//A variable that is given a value at start.	

	public Variable getCheckedVariable(String varId, String value, Boolean wasInDatabase) {
		return getVariable(currentHash,currentCache,varId,value,wasInDatabase);
	}

	public String getVariableValue(Map<String, String> keyChain, String varId) {
		Variable v = getVariable(keyChain,varId);
		if (v == null) {
			Log.e("nils","Varcache returned null!!");
			return null;
		}
		return v.getValue();
	}

	//A variable type that will not allow its keychain to be changed.
	public Variable getFixedVariableInstance(Map<String, String> keyChain, String varId,String defaultValue) {
		List<String> row = gs.getVariableConfiguration().getCompleteVariableDefinition(varId);
		return new FixedVariable(varId,gs.getVariableConfiguration().getEntryLabel(row),row,keyChain,gs,defaultValue,true);
	}	


	public Variable getVariable(Map<String,String> hash, Map <String,Variable> cache, String varId,String defaultValue, Boolean hasValueInDB) {
		//Log.d("nils","in CACHE GetVariable for "+varId);
		long t0=System.currentTimeMillis();
		varId = varId.toLowerCase();
		Variable variable = cache.get(varId);
		if (variable==null) {
			//check if variable has subset of keypairs
			List<String> row = gs.getVariableConfiguration().getCompleteVariableDefinition(varId);
			if (row == null) {
				Log.e("vortex","variable "+varId+" not found");
				return null;
			}
				
			String mColumns = gs.getVariableConfiguration().getKeyChain(row);
			Map<String, String> tryThis = Tools.cutKeyMap(mColumns, hash);
			if (tryThis!=null && tryThis.isEmpty()) {
				Log.e("vortex","KEY FAILLLLL!!!");
				return null;
			}
			cache = createOrGetCache(tryThis);
			variable = cache.get(varId);
			if (variable == null) {
				Log.d("nils","Creating new CacheList entry for "+varId);
				
				if (row==null) {
					Log.e("nils","Variable definition missing for "+varId);
					o.addRow("");
					o.addYellowText("Variable definition missing for "+varId);
					return null;
				}
				String header = gs.getVariableConfiguration().getVarLabel(row);
				DataType type = gs.getVariableConfiguration().getnumType(row);
				Log.d("vortex","T1:"+(System.currentTimeMillis()-t0));
				if (type == DataType.array)
					variable= new ArrayVariable(varId,header,row,hash,gs,vCol,defaultValue,true,null);
				else
					variable = new Variable(varId,header,row,hash,gs,vCol,defaultValue,true,null);
				cache.put(varId, variable);
			} else
				Log.d("vortex","Found "+variable.getId()+" in global cache with value "+variable.getValue());

		} else
			Log.d("vortex","Found "+variable.getId()+" in cache with value "+variable.getValue());
		Log.d("vortex","Te:"+(System.currentTimeMillis()-t0));
		return variable;
	}


	//Check if two maps are equal
	private static boolean Eq(Map<String,String> chainToFind, Map<String,String> varChain) {
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

	//Check if two maps are equal
	private static boolean SubsetOf(Map<String,String> chainToFind, Map<String,String> varChain) {
		//		Log.d("nils","in Varcache EQ");
		if (chainToFind==null && varChain==null)
			return true;
		if (chainToFind==null||varChain==null||varChain.size()<chainToFind.size()) {
			//			Log.d("nils","eq returns false. Trying to match: "+(chainToFind==null?"null":chainToFind.toString())+" with: "+(varChain==null?"null":varChain.toString()));
			return false;
		}
		Log.d("nils","ChainToFind: "+chainToFind.toString());
		Log.d("nils","VarChain: "+varChain.toString());
		for (String key:chainToFind.keySet()) {
			if (chainToFind.get(key)==null) {
				Log.d("nils","SubsetOf returns false. Key "+key+" is not in Chaintofind: "+chainToFind.toString());
				return false;
			}
			if (!chainToFind.get(key).equals(varChain.get(key))) {
				Log.d("nils","SubsetOf returns false. Key "+key+" has different value than varchain with same key: "+chainToFind.get(key)+","+varChain.get(key));
				return false;
			}

		}
		return true;
	}



	public void invalidateOnName(String varId) {
		Log.d("nils","invalidating variable named "+varId);
		Variable v = currentCache.get(varId.toLowerCase());

		if (v!=null) {
			Log.d("nils","Found variable "+v.getId()+". Invalidating..");
			v.invalidate();
		} else {
			Log.d("nils","Could not find variable "+varId+" in invalidateOnName");
		}

	}

	//Invalidate all variables in any group containing at least one instance matching the keyChain.

	public void invalidateOnKey(Map<String, String> keyChain, boolean exactMatch) {
		Log.d("nils","invalidating all variables in Cache matching "+keyChain.toString()+" with exactMatch set to "+exactMatch);
		if (exactMatch) 
			this.refreshCache(keyChain);
		else {
			for (Map<String,String>chain:newcache.keySet()) {
				if (SubsetOf(keyChain,chain)) {
					Log.d("vortex","found subset chain "+chain.toString());
					this.refreshCache(chain);
				}
			}
		}
	}


	//Find all variables in cache with a given keyhash that belongs to the given group.
	public List<Variable> findVariablesBelongingToGroup(Map<String, String> keyChain, String groupName) {
		Log.d("vortex","In FindVariablesBelongingToGroup");
		//Identify matching variables.
		Map<String, Variable> cache = newcache.get(keyChain);
		if (cache!=null) {
			Log.d("vortex","findVariablesBelongingToGroup: "+keyChain.toString()+" size of cache "+cache.size());
			boolean found = false;
			groupName = groupName.toLowerCase();
			Set<String>myKeys = new HashSet<String>();
			for (String varName:cache.keySet()) {
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
			Log.d("vortex","myKeys has "+myKeys.size()+" members");
			List<Variable>resultSet = new ArrayList<Variable>();
			for (String vName:myKeys)
				resultSet.add(cache.get(vName));
			return resultSet;
		}
		Log.d("vortex","No cache exists for"+keyChain);
		return null;

	}

	private class KeyException extends java.lang.Exception {

		private static final long serialVersionUID = 458116191810109227L;

	}

	//Keep a queue of variables for saving. 
	//Trigger insert regularly 
	//Save all before sync.
	
	private final Queue<Variable> dbQueue = new ConcurrentLinkedQueue<Variable>() ;

	private final ScheduledExecutorService scheduleTaskExecutor;


	public void save(Variable variable) {
		dbQueue.add(variable);
		
	
		
	    
		
	}




}
