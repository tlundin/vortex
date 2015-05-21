package com.teraim.vortex.dynamic.types;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.dynamic.types.Workflow.Unit;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.CombinedRangeAndListFilter;
import com.teraim.vortex.utils.DbHelper;
import com.teraim.vortex.utils.FilterFactory;
import com.teraim.vortex.utils.RuleExecutor;
import com.teraim.vortex.utils.Tools;
import com.teraim.vortex.utils.DbHelper.Selection;
import com.teraim.vortex.utils.DbHelper.StoredVariableData;

/**
 * 
 * @author Terje
 * Variable Class. Part of Vortex core.
 * Copyright Teraim Holding. 
 * No modification and use allowed before prior permission. 
 */

public class Variable implements Serializable {




	private static final long serialVersionUID = 6239650487891494128L;

	Map<String, String> keyChain = new HashMap <String,String>();
	Map<String,String> histKeyChain=null;
	//String value=null;
	protected String name=null;
	protected DataType myType=null;
	protected String myValue=null;

	protected Map<String, Boolean> currentRuleState;
	protected String[] myValueColumn = new String[1];
	protected Selection mySelection=null;

	protected String myLabel = null;

	protected Set<String> myRules = null;

	protected DbHelper myDb;

	protected List<String> myRow;

	protected String myStringUnit;

	protected boolean isSynchronized = true;

	protected boolean unknown=true;

	protected boolean isKeyVariable = false;

	protected String realValueColumnName;

	protected Selection histSelection;

	protected GlobalState gs;

	protected Boolean iAmIllegal=null;

	protected boolean iAmOutOfRange=false;

	protected String iAmPartOfKeyChain=null;
	
	protected String myHistory = null;
	
	protected boolean historyChecked = false;

	private Long timeStamp=null;

	private VariableConfiguration al;

	private String myDefaultValue=null;

	private CombinedRangeAndListFilter myFilter=null;

	private boolean usingDefault = false;

	public enum DataType {
		numeric,bool,list,text,existence,auto_increment, array
	}

	public String getValue() {
		if (unknown) {
			//update keyhash.
			Log.d("nils","fetching: "+System.currentTimeMillis()+" var: "+this.getId());
			myValue = myDb.getValue(name,mySelection,myValueColumn);	
			//Variable doesnt exist in database
			if (myValue==null && myDefaultValue !=null) {
				Log.d("nils","Value null! Using default: "+myDefaultValue);
				//use default value.
				myValue = myDefaultValue;
				usingDefault = true;
			}
			unknown = false;
//			Log.d("nils","done:     "+System.currentTimeMillis()+" var: "+this.getId());
			//refreshRuleState();
		}
		//Log.d("nils","Getvalue returns "+myValue+" for "+this.getId());
		return myValue;
	}

	public String getHistoricalValue() {
		if (historyChecked)
			return myHistory;
		if (keyChain == null || keyChain.get(VariableConfiguration.KEY_YEAR)==null) {
			Log.d("nils","historical keychain is null. Should contain year at least.");
			return null;
		}
		if (histKeyChain == null) {
			histKeyChain = new HashMap<String,String>(keyChain);
			histKeyChain.put(VariableConfiguration.KEY_YEAR, VariableConfiguration.HISTORICAL_MARKER);
			Log.d("nils","My historical keychain: "+histKeyChain.toString()+" my name: "+name);
			histSelection = myDb.createSelection(histKeyChain,name);
		}
		myHistory= myDb.getValue(name,histSelection,myValueColumn);
		historyChecked = true;
		return myHistory;
	}


	public String getLabel() {
		if (myLabel==null && myRow!=null)
			myLabel = al.getVarLabel(myRow);
		return myLabel;
	}

	public Set<String> getRules() {
		return myRules;
	}
	/*
	public StoredVariableData getAllFields() {
		return myDb.getVariable(name,mySelection);
	}
	 */

	//return true if change.

	public boolean setValue(String value) {
		Log.d("nils","In SetValue for variable "+this.getId()+" New val: "+value+" existing val: "+myValue+" unknown? "+unknown+" using default? "+usingDefault);
		//Null values are not allowed in db.
		if (value==null)
			return false;
		if (!usingDefault && myValue != null && myValue.equals(value))
				return false;
		if (this.iAmOutOfRange) {
			Log.d("vortex","Out of range. Value not stored!");
			return false;
		}
			
		Log.e("nils","Var: "+this.getId()+" old Val: "+myValue+" new Val: "+value+" this var hash#"+this.hashCode());	
		value = Tools.removeStartingZeroes(value);
		myValue = value;		
		//will change keyset as side effect if valueKey variable.
		//reason for changing indirect is that old variable need to be erased. 
		insertVariable(value,isSynchronized);
		//If rules attached, reevaluate.

		refreshRuleState();
		timeStamp = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
		unknown=false;
		//In the case the variable was previously displaying a default value different from the DB value.
		usingDefault = false;
		return true;
	}

	protected void insertVariable(String value,
			boolean isSynchronized) {
		myDb.insertVariable(this,value,isSynchronized);
	}

	public void setValueNoSync(String value) {
		myValue = value;
		myDb.insertVariable(this, value, false);
		timeStamp = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
	}


	public String getId() {
		return name;
	}
	public void setId(String name) {
		this.name = name;
	}

	public Map<String, String> getKeyChain() {
		return keyChain;
	}


	public Unit getUnit() {
		return Tools.convertToUnit(myStringUnit);
	}


	public String getPrintedUnit() {
		return myStringUnit;
	}

	public DataType getType() {
		return myType;
	}

	public List<String> getBackingDataSet() {
		return myRow;
	}

	public Selection getSelection() {
		return mySelection;
	}

	public String getValueColumnName() {
		return realValueColumnName;
	}


	public Variable(String name,String label,List<String> row,Map<String,String>keyChain, GlobalState gs,String valueColumn, String defaultOrExistingValue, Boolean valueIsPersisted) {
		//Log.e("nils","Creating variable ["+name+"] with keychain "+((keyChain==null)?"null":printKeyChain(keyChain))+"\nthis obj: "+this);
		this.gs=gs;
		al=gs.getVariableConfiguration();
		this.name = name;
		if (row!=null) {
			myRow = row;
			myType = al.getnumType(row);		
			myStringUnit = al.getUnit(row);
			isSynchronized = al.isSynchronized(row);
			//check for rules on type level.
			addRules(al.getDynamicLimitExpression(row));
			String limitDesc = al.getLimitDescription(row);
			if (limitDesc!=null&&limitDesc.length()>0) 
				myFilter = FilterFactory.getInstance(gs.getContext()).createLimitFilter(this,limitDesc);	
		} else 
			Log.d("nils","Parameter ROW was null!!");
		this.keyChain=keyChain;		
		myDb = gs.getDb();
		mySelection = myDb.createSelection(keyChain,name);
		myLabel = label;
		realValueColumnName = valueColumn;
		myValueColumn[0]=myDb.getColumnName(valueColumn);
		//Log.d("nils","myValueColumn: "+myValueColumn[0]);
		myValue = null;
		//Defaultvalue is either a default or the current value in DB.
		setDefault(defaultOrExistingValue);
		//No information if this variable exists or not.
		if (valueIsPersisted == null) {
			unknown = true;
			usingDefault = false;
		}
		else {
			unknown = false;
			myValue = myDefaultValue;
			if (!valueIsPersisted) {
				Log.d("nils","Creating variable "+this.getId()+". Variable is not persisted: "+myValue);
				usingDefault = true;
			}
		} 
			
		if (keyChain!=null && keyChain.containsKey(valueColumn)) {
			Log.e("nils","Variable value column in keyset for valcol "+valueColumn+" varid "+name);
			isKeyVariable=true;
		}
	}

	/*
	private static boolean isHistorical(Map<String, String> kc) {
		if (kc==null) {
			Log.d("nils","No keychain - cannot be historical");
			return false;
		}
		String year = kc.get(VariableConfiguration.KEY_YEAR);
		if (year == null||year.length()==0) {
			Log.e("nils","year key missing in variable. Will assume current year");
			return false;
		}
		if (!year.equals(Integer.toString(Calendar.getInstance().get(Calendar.YEAR)))) {
			Log.d("nils","Historical value!");
			return true;
		}
		return false;
	}
	 */

	private String printKeyChain(Map<String, String> kc) {
		if (kc==null)
			return "empty";
		String ret="";
		for (String key:kc.keySet()) {
			if (key!=null) 
				ret+="{"+key+" = "+kc.get(key)+"}\n";
			
		}
		return ret;
	}

	private void setDefault(String defaultValue) {
		if (defaultValue == null)
			myDefaultValue = null;
		else if (defaultValue.equals(Constants.HISTORICAL_TOKEN))
			myDefaultValue = this.getHistoricalValue();
		else {
			myDefaultValue = defaultValue.equals(Constants.NO_DEFAULT_VALUE)?null:defaultValue;
		}
	}
	
	public void deleteValue() {
		myDb.deleteVariable(name,mySelection,isSynchronized);
		myValue=null;
		unknown = false;
		usingDefault = false;
	}
	
	public void deleteValueNoSync() {
		myDb.deleteVariable(name,mySelection,false);
		myValue=null;
		unknown = false;
		usingDefault = false;

	}

	public void setType(DataType type) {
		myType = type;
	}

	public void invalidate() {
		Log.d("vortex","Invalidating variable: "+this.getId());
		unknown=true;
		usingDefault = false;
		//Log.d("vortex","Test - getValue returns: "+this.getValue());
		//Log.d("vortex","My keychain is: "+this.getKeyChain().toString());

	}

	public void invalidateKey() {
		if (keyChain==null)
			return;
		keyChain = gs.getCurrentKeyHash();
		mySelection = myDb.createSelection(keyChain, name);
		//Log.d("nils","calling invalidate keyhash on "+this.getId());
		if(this.getType()!=DataType.auto_increment) 
			unknown=true;
		

	}

	public boolean isKeyVariable() {
		return isKeyVariable;
	}


	public void addRules(String rules) {
		if (rules == null||rules.length()==0)
			return;
		String[] ruleA = rules.split(",");
		Log.d("nils","In addrules with "+rules+". Rules found: "+(ruleA==null?"NULL":ruleA.length));
		for (String rule:ruleA) {
			if (myRules==null)
				myRules = new HashSet<String>();
			myRules.add(rule);
		}
		refreshRuleState();
	}


	public void refreshRuleState() {}
	/*
		Log.d("nils", "Refreshing rulestate for "+this.getId());
		iAmIllegal = false;	
		if (myRules !=null) {
			currentRuleState = new HashMap<String,Boolean>();
			Boolean evalRes;
			Iterator<String> it = myRules.iterator();
			RuleExecutor re = RuleExecutor.getInstance(gs.getContext());
			while(it.hasNext()) {
				String rule=it.next();
				evalRes = re.evaluate(rule);
				if (evalRes!=null) { 
					Log.d("nils","putting "+rule+" and evalres "+evalRes+" into currentRulestate.");
					currentRuleState.put(rule, evalRes);
					//mark variable if one of the rules fails.
					if (!evalRes)
						iAmIllegal=true;
				}
			}
		} else
			Log.d("nils","Myrules was null");
//		} else 
//			Log.d("nils","Variable "+this.myLabel+" is invalid. Will not refresh rulestate.");
	}
	*/
	public boolean hasBrokenRules() {
		/*
		if (iAmIllegal==null)
			refreshRuleState();
		if (currentRuleState!=null)
		for(String key:currentRuleState.keySet()) {
			Log.d("nils","Rule: "+key+" state: "+currentRuleState.get(key));
		}
		return iAmIllegal;
		*/
		return false;
	}

	public boolean hasValueOutOfRange() {
		return iAmOutOfRange;
	}

	public Map<String,Boolean> getRuleState() {
		return currentRuleState;
	}
	public void setOutOfRange(boolean oor) {
		iAmOutOfRange = oor;
	}

	final static String[] timeStampS = new String[] {"timestamp"};

	public Long getTimeOfInsert() {
		if (timeStamp!=null) {
			//Log.d("nils","returned cached timestamp for var "+this.getId());
			return timeStamp;
		}
			String tmp = myDb.getValue(name, mySelection,Variable.timeStampS);
			if (tmp!=null) {
				timeStamp = Long.parseLong(tmp);
				return timeStamp;
			}
			return null;
		
	}

	public void setKeyChainVariable(String key) {
		Log.d("nils","SetKeyChain called for "+this.getId());
		iAmPartOfKeyChain = key;
	}

	public String getPartOfKeyChain() {
		return iAmPartOfKeyChain;
	}
	
	public boolean isInvalidated() {
		return unknown;
	}
	
	public boolean isUsingDefault() {
		return usingDefault ;
	}

	public CombinedRangeAndListFilter getLimitFilter() {
		return myFilter;
	}

	



}
