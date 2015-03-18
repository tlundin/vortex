package com.teraim.vortex.dynamic.workflow_realizations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.workflow_abstracts.Event;
import com.teraim.vortex.dynamic.workflow_abstracts.EventGenerator;
import com.teraim.vortex.dynamic.workflow_abstracts.EventListener;
import com.teraim.vortex.dynamic.workflow_abstracts.Event.EventType;

public class WF_Instance_List extends WF_Static_List implements EventListener,EventGenerator{

	//Create variables for each instance of variables in <rows> that matches key with variable column <variatorColumn>




	Map<String,WF_ClickableField_Selection> entryFields = new HashMap<String,WF_ClickableField_Selection>();


	private int index = 0;
	String variatorColumn;
	private Set<List<String>> selectedRows = new HashSet<List<String>>();


	private Map<String, Map<String, String>> varValueMap;

	private String namePrefix;

	private Map<String, String> myKeyHash;


	private Set<String> mySuffixes=new HashSet<String>();


	private boolean showHistorical;


	public WF_Instance_List(String id, WF_Context ctx,List<List<String>> rows,String variatorColumn,boolean isVisible) {
		super(id, ctx,rows,isVisible);
		this.namePrefix = al.getFunctionalGroup(rows.get(0));
		myKeyHash = new HashMap<String,String>(gs.getCurrentKeyHash());
		myKeyHash.remove(variatorColumn);
		ctx.addEventListener(this, EventType.onSave);
		o = GlobalState.getInstance(ctx.getContext()).getLogger();
		this.variatorColumn=variatorColumn;
		Log.d("nils","INSTANCE LIST CREATED. ROWS: "+rows.size());
	}


	@Override 
	public Set<Variable> addVariableToEveryListEntry(String varSuffix,boolean displayOut,String format,boolean isVisible, boolean showHistorical,String initialValue) {
		this.showHistorical = showHistorical;	

		Set<String> vars = findAllVarsEndingWith(varSuffix,myRows);
		Log.d("nils","Addvartolistentry found "+vars.size()+" entries for varSuffix "+varSuffix);
		if (vars.size()!=0) {
			list.clear();
			myVars.addAll(vars);
			updateEntryFields();
		} else {
			Log.e("nils","Addvariable - did not find any new variables to add for "+varSuffix);
			o.addRow("");
			o.addRedText("Addvariable - did not find any new variables to add for "+varSuffix);
		}

		mySuffixes.add(varSuffix);
		return null;
	}

	Map<String,List<String>> suffixToVars=null;
	//Creates the entryfields iteratively for each new batch of variables.


	private Set<String> myVars=new HashSet<String>();

	private void updateEntryFields() {
		//clear existing entries.
		entryFields.clear();
		//Cache entryfields.
		//Map<String,WF_ClickableField_Selection> visitedEntryFields = new HashMap<String,WF_ClickableField_Selection>();	
		//Remove variator.
		myKeyHash.remove(variatorColumn);
		//fetch all variable instances of given namePrefix.
		Map<String, Map<String, String>> allInstances = gs.getDb().preFetchValues(myKeyHash, namePrefix, variatorColumn);		
		Log.d("nils","in update entry fields. AllInstances contain "+allInstances.size());
		if (allInstances !=null) {
			//For each instance, create new List entry if needed. Otherwise add.
			//Iterate over all variable instances.
			for (String varId: allInstances.keySet()) {
				Log.d("nils","Checking "+varId);
				if (myVars!=null && myVars.contains(varId)) {
					//get the variator to variable mapping.
					Map<String, String> indexToValue = allInstances.get(varId);
					//for each new index, create a new entryfield (on index) and chain.
					//if an entryfield with index already exist, add the variable.
					Set<String> indexes = indexToValue.keySet();
					Log.d("nils","indexes: "+indexes.toString());
					for (String index:indexes) {
						String value = indexToValue.get(index);					
						myKeyHash.put(variatorColumn, index);				
						Variable var = gs.getVariableConfiguration().getFixedVariableInstance(myKeyHash, varId,value);
						if (var!=null) {
							String entryInstanceLabel = al.getEntryLabel(var.getBackingDataSet())+" ["+index+"]";
							WF_ClickableField_Selection ef = entryFields.get(entryInstanceLabel);
							if (ef == null) {
								ef = new WF_ClickableField_Selection(entryInstanceLabel,al.getDescription(var.getBackingDataSet()),myContext,entryInstanceLabel,true);
								Log.d("nils","Added list entry for "+entryInstanceLabel);
								//cache
								entryFields.put(entryInstanceLabel, ef);	
								list.add(ef);
							}
							Set<Variable> vars = ef.getAssociatedVariables();
							//find missing variables for myvars. missing = var - suffix + other endings.				
							if (vars==null || !vars.contains(var)) {
								Log.d("nils","Adding variable "+var.getId()+" to EF: "+ef.getLabel());
								ef.addVariable(var, true, null, true,showHistorical);
							} else
								Log.e("nils","DID NOT ADD VARIABLE "+var.getId());


							//							visitedEntryFields.put(entryInstanceLabel,ef);
						} else
							Log.e("nils","Variable "+varId+" does not exist! (In WF_InstanceList, updateEntryFields)");
					}

				} else {
					Log.d("nils","is not member of myVars. Myvars has "+myVars.size()+" entries:");
					String mvS="";
					for (String m:myVars) 
						mvS+=m+",";
					Log.d("nils",mvS);						
				}
			}
		}

		//		entryFields = visitedEntryFields;
	}

	private Set<String> findAllVarsEndingWith(String suffix,
			List<List<String>> myRows) {
		String vId;		
		if (myRows==null)
			return null;
		Set<String> ret = new HashSet<String>();
		for (List<String> r:myRows) {
			vId = al.getVarName(r);
			if (vId.endsWith(suffix))
				ret.add(vId);
		}
		return ret;
	}


	@Override
	public void addFieldListEntry(String listEntryID,String label,String description) {		
		//not supported.
	}


	public Variable addVariableToListEntry(String varNameSuffix,boolean displayOut,String targetField,
			String format, boolean isVisible, boolean showHistorical,String initialValue) {
		return null;
		//not supported.
	}

	/*
	@Override
	public void addEntriesFromRows(List<List<String>> rows) 	{
		String format = null;
		if (rows!=null) {
			o.addRow("Adding "+rows.size()+" list entries (variables)");
			int index = 0;
			WF_ClickableField listRow=null;
			for(List<String> r:rows) {
				if (r==null) {
					o.addRow("found null value in config file row "+index);
				} else {
					if (al.getAction(r).equals("create")) {
						Log.d("nils","create...");
						//C_F_+index is the ID for the element.
						//TODO: ID is a bit hacked here..

						listRow = new WF_ClickableField_Selection(al.getEntryLabel(r),al.getDescription(r),myContext,"C_F_"+index,true);
						list.add(listRow);	
					} 
					if (!al.getAction(r).equals("add")&&!al.getAction(r).equals("create"))
						o.addRow("something is wrong...action is neither Create or Add: "+al.getAction(r));
					else {
						Log.d("nils","add...");
						if (listRow!=null) {
							Log.d("nils","var added "+al.getVarLabel(r));
							Variable v = al.getVariableInstance(al.getVarName(r));
							if (v!=null)
								listRow.addVariable(v,al.isDisplayInList(r),format,true);
						}
					}
				}
				index++;
			}
		}
	}
	 */

	@Override
	public void onEvent(Event e) {
		if (e.getProvider().equals(this))
			Log.d("nils","Throwing event that originated from me");
		else {
			Log.d("nils","GOT EVENT IN WF_INSTANCE!!");
			//Regenerate list.
			list.clear();
			updateEntryFields();
			draw();
		}
		myContext.registerEvent(new WF_Event_OnRedraw(this.getId()));
	}










}
