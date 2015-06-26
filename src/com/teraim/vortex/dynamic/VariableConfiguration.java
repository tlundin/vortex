package com.teraim.vortex.dynamic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.GlobalState.ErrorCode;
import com.teraim.vortex.dynamic.types.FixedVariable;
import com.teraim.vortex.dynamic.types.Table;
import com.teraim.vortex.dynamic.types.VarCache;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.non_generics.NamedVariables;
import com.teraim.vortex.utils.Tools;


public class VariableConfiguration implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 942330642338510319L;
	public static String Col_Variable_Name = "Variable Name";
	public static String Col_Variable_Label = "Variable Label";
	public static String Col_Variable_Keys = "Key Chain";
	public static String Type = "Type";
	public static String Col_Functional_Group = "Group Name";
	public static String Col_Variable_Scope = "Scope";
	public static String Col_Variable_Limits = "Limits";
	public static String Col_Variable_Dynamic_Limits = "D_Limits";

	public final static String KEY_YEAR = "år";
	public static final String HISTORICAL_MARKER = "H";
	public static final List<String>requiredColumns=Arrays.asList(Col_Variable_Keys,Col_Functional_Group,Col_Variable_Name,Col_Variable_Label,Type,"Unit","List Values","Description",Col_Variable_Scope,Col_Variable_Limits,Col_Variable_Dynamic_Limits);
	private static int KEY_CHAIN=0,FUNCTIONAL_GROUP=1,VARIABLE_NAME=2,VARIABLE_LABEL=3,TYPE=4,UNIT=5,LIST_VALUES=6,DESCRIPTION=7,SCOPE=8,LIMIT=9,D_LIMIT=10;

	public enum Scope {
		local_sync,
		local_nosync,
		global_nosync,
		global_sync
	}
	
	Map<String,Integer>fromNameToColumn; 


	Table myTable;
	GlobalState gs;
	private VarCache myCache;

	public VariableConfiguration(GlobalState gs,VarCache vc,Table t) {
		this.gs = gs;
		myTable = t;
		myCache=vc;
		//TODO: Call this from parser.
		validateAndInit();
	}

	public ErrorCode validateAndInit() {
		fromNameToColumn = new HashMap<String,Integer>();
		for (String c:requiredColumns) {
			int tableIndex = myTable.getColumnIndex(c);
			if (tableIndex==-1) {
				Log.e("nils","Missing column: "+c);
				Log.e("nils","Tabe has "+myTable.getColumnHeaders().toString());
				return ErrorCode.missing_required_column;
			}
			else
				//Now we can map a call to a column to the actual implementation.
				//Actual column index is decoupled.
				fromNameToColumn.put(c, tableIndex);
		}

		return ErrorCode.ok;
	}

	public Table getTable() {
		return myTable;
	}

	/*
	public String getListEntryName(List<String> row) {
		return row.get(fromNameToColumn.get(requiredColumns.get(LIST_ENTRY)));
	}
	 */

	public List<String> getListElements(List<String> row) {
		List<String> el = null;
		String listS = row.get(fromNameToColumn.get(requiredColumns.get(LIST_VALUES)));
		if (listS!=null&&listS.trim().length()>0) {
			String[] x = listS.trim().split("\\|");
			if (x!=null&&x.length>0)
				el = new ArrayList<String>(Arrays.asList(x));
		}
		return el;
	}
	

	public String getVarName(List<String> row) {
		return row.get(fromNameToColumn.get(requiredColumns.get(VARIABLE_NAME)));
	}

	public String getVarLabel(List<String> row) {
		return row.get(fromNameToColumn.get(requiredColumns.get(VARIABLE_LABEL)));
	}

	public String getVariableDescription(List<String> row) {		
		return row.get(fromNameToColumn.get(requiredColumns.get(DESCRIPTION)));
	}

	/*
	public boolean isLocal(List<String>row) {
		
	}
	*/
	
	//If the variable should be synchronized between the devices.
	public boolean isSynchronized(List<String> row) {
		String s= row.get(fromNameToColumn.get(requiredColumns.get(SCOPE)));
		return (s==null||s.length()==0||
				s.equals(Scope.global_sync.name())||
						s.equals(Scope.local_sync.name()));

	}
	
	//If the variable shall be exported via JSON to server.
	public boolean isLocal(List<String> row) {
		String s= row.get(fromNameToColumn.get(requiredColumns.get(SCOPE)));
		//Log.d("nils","getvarislocal uses string "+s);
		if (s==null||s.length()==0||s.equals(Scope.global_sync.name()))
			return false;//Scope.global_sync;
		else if (s.equals(Scope.local_nosync.name())) {
			//return Scope.local_nosync;
			return true;
		} else 
			return false;
		/*
		else if (s.equals(Scope.local_sync))
			return Scope.local_sync;
		else if (s.equals(Scope.global_nosync))
			return Scope.global_nosync;
		else {
			gs.getLogger().addRow("");
			gs.getLogger().addRedText("Unknown Scope parameter: "+s+" Will assume it is global_sync");
			return Scope.global_sync;
		}
		*/
	}
	
	public String getLimitDescription(List<String> row) {
		return row.get(fromNameToColumn.get(requiredColumns.get(LIMIT)));	
	}	
	public String getDynamicLimitExpression(List<String> row) {
		return row.get(fromNameToColumn.get(requiredColumns.get(D_LIMIT)));	
	}

	public String getKeyChain(List<String> row) {
		//Check for null or empty
		if (row==null) {
			Log.d("vortex","row was null in getKeyChain");
			return null;
		}
		//else 
		//	Log.d("vortex","Row is "+row+" length_: "+row.size()+" fromname "+fromNameToColumn);
		Pattern pattern = Pattern.compile("\\s");
		Matcher matcher = pattern.matcher(row.get(0));
		if(matcher.find()) {
			Log.e("vortex","Space char found in keychain: "+row.get(0)+" length : "+row.get(0).length()+" size: "+row.size());
			return null;
		}
			
		else
			return row.get(fromNameToColumn.get(requiredColumns.get(KEY_CHAIN)));		
	}

	public String getFunctionalGroup(List<String> row) {
		return row.get(fromNameToColumn.get(requiredColumns.get(FUNCTIONAL_GROUP)));
	}
	public Variable.DataType getnumType(List<String> row) {
		String type = row.get(fromNameToColumn.get(requiredColumns.get(TYPE)));
		if (type!=null) {		
			if (type.equals("number")||type.equals("numeric"))
				return Variable.DataType.numeric;
			else if (type.equals("boolean"))
				return Variable.DataType.bool;
			else if (type.equals("list"))
				return Variable.DataType.list;
			else if (type.equals("text")||type.equals("string"))
				return Variable.DataType.text;
			else if (type.equals("auto_increment")) 
				return Variable.DataType.auto_increment;
			else if (type.equals("array")) 
				return Variable.DataType.array;
			else
				Log.e("nils","TYPE NOT KNOWN: ["+type+"]");
		}
		gs.getLogger().addRow("");
		String myId = getVarName(row);
		gs.getLogger().addRedText("Type parameter not configured for variable "+myId+" Will default to numeric");
		return Variable.DataType.numeric;
	}


	public String getUnit(List<String> row) {
		String unit = row.get(fromNameToColumn.get(requiredColumns.get(UNIT)));
		if (unit == null) {
			gs.getLogger().addYellowText("Unit was null for variable "+getVarName(row));
			unit = "";
		}
		return unit;	
	}

	public List<String> getCompleteVariableDefinition(String varName) {
		return myTable.getRowFromKey(varName);
	}
	
	public String getAction(List<String> row) {
		return null;
	}

	public String getEntryLabel(List<String> row) {
		if (row == null)
			return null;
		String  res= myTable.getElement("Label", row);
		if (res == null) {
			Log.d("vortex","row is: ["+row.toString()+"]");
			Log.d("vortex","Column label not found, trying 'svenskt namn'");
			res= myTable.getElement("Svenskt Namn", row);
		}
		//If this is a non-art variable, use varlabel instead.
		if (res==null) {
			Log.d("vortex","failed to find column Label. Will use varlabel "+this.getVarLabel(row)+" instead.");
			gs.getLogger().addRow("");
			gs.getLogger().addYellowText("failed to find column Label. Will use varlabel "+this.getVarLabel(row)+" instead.");
			res =this.getVarLabel(row);
		}
		if (res == null)
			Log.e("nils","getEntryLabel failed to find a Label for row: "+row.toString());
		return res;
	}

	public String getDescription(List<String> row) {
		String b = myTable.getElement("Description", row);
		if(b==null) 
			b = this.getVariableDescription(row);

		return (b==null?"":b);
	}



	public String getUrl(List<String> row) {
		return myTable.getElement("Internet link", row);	
	}

	public boolean isDisplayInList(List<String> row) {
		return false;
	}

	//Map<String,Variable>varCache = new ConcurrentHashMap<String,Variable>();

	public String getVariableValue(Map<String, String> keyChain, String varId) {
		Variable v = getVariableUsingKey(keyChain,varId);
		if (v == null) {
			Log.e("nils","Varcache returned null!!");
			return null;
		}
		return v.getValue();
	}

	public Variable getVariableUsingKey(Map<String, String> keyChain, String varId) {
		return myCache.getVariable(keyChain, varId, null,null);
		//return new Variable(varId,null,null,keyChain,gs,"value",null);
	}
	
	//A variable type that will not allow its keychain to be changed.
	public Variable getFixedVariableInstance(Map<String, String> keyChain, String varId,String defaultValue) {
		List<String> row = this.getCompleteVariableDefinition(varId);
		return new FixedVariable(varId,getEntryLabel(row),row,keyChain,gs,defaultValue,true);
	}	
	
	
	//A variable that is given a value at start.	
		public Variable getCheckedVariable(Map<String, String> keyChain,String varId, String value, Boolean wasInDatabase) {
			return myCache.getVariable(keyChain,varId,value,wasInDatabase);
		}
		//A variable that is given a value at start.	
		
		public Variable getCheckedVariable(String varId, String value, Boolean wasInDatabase) {
			return myCache.getVariable(gs.getCurrentKeyHash(),varId,value,wasInDatabase);
		}
	/*
		Variable v = varCache.get(varId);
		if (v!=null) {
			//Log.d("nils","found cached var: "+varId);//+" backing: "+this.getCompleteVariableDefinition(varId));
			return v;
		}
		else {
			//use standard hash. Fetch varLabel header later when needed.
			v= new Variable(varId,null,getCompleteVariableDefinition(varId),gs.getCurrentKeyHash(),gs,vCol,value);
			varCache.put(varId, v);			
		}
		return v;
	}
	
	*/
	
	private final static String vCol = "value";
	
	public Variable getVariableInstance(String varId) {
		return myCache.getVariable(varId);
	}
	//A variable that has a defaultvalue.	
	public Variable getVariableInstance(String varId,String defaultValue) {
		return myCache.getVariable(varId,defaultValue);
	}
	

	/*
		String varLabel =null;
		String keyChain = null;
		Variable v = varCache.get(varId);
		if (v!=null) {
			//Log.d("nils","found cached var: "+varId);//+" backing: "+this.getCompleteVariableDefinition(varId));
			return v;
		}
		else {
			List<String> row = this.getCompleteVariableDefinition(varId);
			if (row!=null) {
				keyChain = this.getKeyChain(row);				
				varLabel = this.getVarLabel(row);
				v = new Variable(varId,varLabel,row,buildDbKey(keyChain,gs.getCurrentKeyHash()),gs,vCol,null);		
				varCache.put(varId, v);
			} else 
				Log.e("nils","getVariableInstance: Cannot find variable: "+varId);		
			
			return v;
		}
	}
*/

	//Create a variable with the current context and the variable's keychain.
	//public Variable getVariableInstance(String keyChain,String varId,String varLabel,List<String> row,Map<String, String> cMap,String valueColumn,String value) {	
		//find my keys in the current context.
		//Use a cache for faster access.
	//	return new Variable(varId,varLabel,row,buildDbKey(keyChain,cMap),gs,valueColumn,value);
	//} 


	private Map<String, String> buildDbKey(String keyChain,
			Map<String, String> cMap) {
		if (keyChain==null||keyChain.isEmpty()) 
			return null;
		//Log.e("nils","Keys in chain:"+keyChain);
		//Log.e("nils","Keys available: "+cMap.keySet().toString());
		//Log.e("nils","Key values:"+cMap.entrySet().toString());
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
			}
		}
		return vMap;
	}



	public Map<String, String> createRutaKeyMap() {
		String currentYear = getVariableValue(null,NamedVariables.CURRENT_YEAR);
		String currentRuta = getVariableValue(null,NamedVariables.CURRENT_RUTA);
		if (currentRuta == null)
			return null;
		return Tools.createKeyMap(KEY_YEAR,currentYear,"ruta",currentRuta);
	}
	
	public Map<String,String> createProvytaKeyMap() {
		String currentYear = getVariableValue(null,NamedVariables.CURRENT_YEAR);
		String currentRuta = getVariableValue(null,NamedVariables.CURRENT_RUTA);
		String currentProvyta = getVariableValue(null,NamedVariables.CURRENT_PROVYTA);		
		if (currentRuta == null||currentProvyta==null)
			return null;
		return Tools.createKeyMap(KEY_YEAR,currentYear,"ruta",currentRuta,"provyta",currentProvyta);
	}
	
	public Map<String, String> createSmaprovytaKeyMap() {
		String currentYear = getVariableValue(null,NamedVariables.CURRENT_YEAR);
		String currentRuta = getVariableValue(null,NamedVariables.CURRENT_RUTA);
		String currentProvyta = getVariableValue(null,NamedVariables.CURRENT_PROVYTA);		
		String currentSmayta = getVariableValue(null,NamedVariables.CURRENT_SMAPROVYTA);		
		if (currentYear == null || currentRuta == null||currentProvyta==null||currentSmayta==null)
			return null;
		return Tools.createKeyMap(KEY_YEAR,currentYear,"ruta",currentRuta,"provyta",currentProvyta,"smaprovyta",currentSmayta);
	}
	
	public Map<String, String> createDelytaKeyMap() {
		String currentYear = getVariableValue(null,NamedVariables.CURRENT_YEAR);
		String currentRuta = getVariableValue(null,NamedVariables.CURRENT_RUTA);
		String currentProvyta = getVariableValue(null,NamedVariables.CURRENT_PROVYTA);		
		String currentDelyta = getVariableValue(null,NamedVariables.CURRENT_DELYTA);		
		if (currentYear == null || currentRuta == null||currentProvyta==null||currentDelyta==null) {
			Log.e("nils","CreateDelytaKeyMap failed. Missing value");
			return null;
		}
		return Tools.createKeyMap(KEY_YEAR,currentYear,"ruta",currentRuta,"provyta",currentProvyta,"delyta",currentDelyta);
	}
	
	//note that provyta is the key for current linje.
	public Map<String, String> createLinjeKeyMap() {
		String currentYear = getVariableValue(null,NamedVariables.CURRENT_YEAR);
		String currentRuta = getVariableValue(null,NamedVariables.CURRENT_RUTA);		
		String currentLinje = getVariableValue(null,NamedVariables.CURRENT_LINJE);		
		if (currentYear == null || currentRuta == null||currentLinje==null) {
			Log.e("nils","CreateLinjeKeyMap failed. Missing value");
			return null;
		}

		return Tools.createKeyMap(VariableConfiguration.KEY_YEAR,currentYear,"ruta",currentRuta,"provyta",currentLinje);
	}

	public String getCurrentRuta() {
		return getVariableValue(null,NamedVariables.CURRENT_RUTA);
	}

	public String getCurrentProvyta() {
		return getVariableValue(null,NamedVariables.CURRENT_PROVYTA);
	}

	public void invalidateCache() {
		//myCache.invalidate();
		//for (Variable v:varCache.values())
		//	v.invalidate();
		Log.d("nils","invalidate called. Will only invalidate individual variables now.");
	}

	public void invalidateCacheKeys(String key) {
		gs.refreshKeyHash();
		for (List<Variable> vl:myCache.getVariables()) {
			if (vl!=null)
				for (Variable v:vl)
					if (v.getKeyChain()!=null && v.getKeyChain().get(key)!=null) {
						Log.d("nils","variable "+v.getId()+" contained "+key);
						v.invalidateKey();
					}
		}
	}
	
	public void destroyCache() {
		Log.d("nils","In destroy cache...will destroy");
		myCache.invalidateAll();
	}

	public void addToCache(Variable v) {
		Log.d("nils","In add to cache...will not be needed");
		myCache.put(v.getId(), v);
	}



















}

