package com.teraim.vortex;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.teraim.vortex.bluetooth.ConnectionManager;
import com.teraim.vortex.bluetooth.SyncMessage;
import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.dynamic.types.CHash;
import com.teraim.vortex.dynamic.types.SpinnerDefinition;
import com.teraim.vortex.dynamic.types.Table;
import com.teraim.vortex.dynamic.types.VarCache;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.types.Workflow;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;
import com.teraim.vortex.expr.Aritmetic;
import com.teraim.vortex.expr.Parser;
import com.teraim.vortex.gis.Tracker;
import com.teraim.vortex.loadermodule.Configuration;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.non_generics.StatusHandler;
import com.teraim.vortex.ui.DrawerMenu;
import com.teraim.vortex.utils.DbHelper;
import com.teraim.vortex.utils.PersistenceHelper;
import com.teraim.vortex.utils.RuleExecutor;
import com.teraim.vortex.utils.RuleExecutor.SubstiResult;
import com.teraim.vortex.utils.RuleExecutor.TokenType;
import com.teraim.vortex.utils.RuleExecutor.TokenizedItem;
import com.teraim.vortex.utils.Tools;


/**
 * 
 * @author Terje
 *
 * Classes defining datatypes for ruta, provyta, delyta and t�g.
 * There are two Scan() functions reading data from two input files (found under the /raw project folder).
 */
public class GlobalState  {

	//access only through getSingleton(Context).
	//This is because of the Activity lifecycle. This object might need to be re-instantiated any time.
	private static GlobalState singleton;


	private Context myC;
	private LoggerI log;
	private PersistenceHelper ph = null;	
	private DbHelper db = null;
	private Parser parser=null;
	private VariableConfiguration artLista=null;
	//Map workflows into a hash with name as key.
	private Map<String,Workflow> myWfs; 
	//Spinner definitions
	private SpinnerDefinition mySpinnerDef;
	private DrawerMenu myDrawerMenu;
	//Global state for sync.
	private SyncStatus syncStatus;	
	public String TEXT_LARGE;
	private WF_Context currentContext;
	private String myPartner="?";
	private VarCache myVarCache;
	private PersistenceHelper globalPh=null;
	private Tracker myTracker;
	private ConnectionManager myConnectionManager; 
	
	
	public static GlobalState getInstance() {
		if (singleton == null) {			
			//singleton = new GlobalState(c.getApplicationContext());
			Log.e("vortex","Global state was lost...ajabaja");

		}
		return singleton;
	}

	public static GlobalState createInstance(Context applicationContext, PersistenceHelper globalPh,
			PersistenceHelper ph, LoggerI debugConsole, DbHelper myDb,
			List<Workflow> workflows,Table t,SpinnerDefinition sd) {
		singleton = null;
		return new GlobalState(applicationContext,  globalPh,
				ph, debugConsole,  myDb,
				workflows, t, sd);

	}

	//private GlobalState(Context ctx)  {
	private GlobalState(Context applicationContext, PersistenceHelper globalPh,
			PersistenceHelper ph, LoggerI debugConsole, DbHelper myDb,
			List<Workflow> workflows,Table t,SpinnerDefinition sd) {

		myC = applicationContext;
		this.globalPh=globalPh;		
		this.ph=ph;		
		this.db=myDb;		
		this.log = debugConsole;
		//Parser for rules
		parser = new Parser(this);
		//Artlista
		myVarCache = new VarCache(this);
		artLista = new VariableConfiguration(this,myVarCache,t);			
		myWfs = mapWorkflowsToNames(workflows);		
		//Event Handler on the Bluetooth interface.
		//myHandler = getHandler();
		//Handles status for 
		myStatusHandler = new StatusHandler(this);

		mySpinnerDef = sd;

		singleton =this;

		//GPS listener service
		myTracker = new Tracker();

		myExecutor = new RuleExecutor(this);

		myConnectionManager = new ConnectionManager(this);
	

	}




	/*Validation
	 * 
	 */

	/*
	public ErrorCode validateFrozenObjects() {

		if (myWfs == null)
			return ErrorCode.workflows_not_found;
		if (artLista == null)
			return ErrorCode.config_not_found;
			//return ErrorCode.spinners_not_found;
		else {
			ErrorCode artL = artLista.validateAndInit();
			if (artL != ErrorCode.ok)
				return artL;
			else
				if(myWfs.size()==0)
					return ErrorCode.workflows_not_found;
				else
					return ErrorCode.ok;
		}
	}
	 */

	/*Singletons available for all classes
	 * 
	 */
	public SpinnerDefinition getSpinnerDefinitions() {
		return mySpinnerDef;
	}
	/*
	public void setSpinnerDefinitions(SpinnerDefinition sd) {
		if (sd!=null)
			Log.d("nils","SetSpinnerDef called with "+sd.size()+" spinners");
		else 
			Log.e("nils","Spinnerdef null!!!");
		mySpinnerDef=sd;
	}
	 */
	//Persistance for app specific variables.
	public PersistenceHelper getPreferences() {
		return ph;
	}

	//Persistence for global, non app specific variables
	public PersistenceHelper getGlobalPreferences() {
		return globalPh;
	}


	public DbHelper getDb() {
		return db;
	}

	public Parser getParser() {
		return parser;
	}

	public Context getContext() {
		return myC;
	}

	public RuleExecutor getRuleExecutor() {
		return myExecutor;
	}

	public VariableConfiguration getVariableConfiguration() {
		return artLista;
	}





	/**************************************************
	 * 
	 * Mapping workflow to workflow name.
	 */

	public Map<String,Workflow> mapWorkflowsToNames(List<Workflow> l) {
		Map<String,Workflow> ret=null;
		if (l==null) 
			Log.e("NILS","Parse Error: Workflowlist is null in SetWorkFlows");
		else {

			for (Workflow wf:l)
				if (wf!=null) {
					if (wf.getName()!=null) {
						Log.d("NILS","Adding wf with name "+wf.getName()+" and length "+wf.getName().length());
						if (ret== null)
							ret = new TreeMap<String,Workflow>(String.CASE_INSENSITIVE_ORDER);

						ret.put(wf.getName(), wf);
					} else
						Log.d("NILS","Workflow name was null in setWorkflows");
				} else
					Log.d("NILS","Workflow was null in setWorkflows");
		}
		return ret;
	}

	/*
	public Table thawTable() { 	
		return ((Table)Tools.readObjectFromFile(myC,Constants.CONFIG_FILES_DIR+Constants.CONFIG_FROZEN_FILE_ID));		
	}
	 */

	public Workflow getWorkflow(String id) {
		if (id == null || id.isEmpty())
			return null;
		return myWfs.get(id);
	}

	public Workflow getWorkflowFromLabel(String label) {
		if (label==null)
			return null;
		for (Workflow wf:myWfs.values()) 
			if (wf.getLabel()!=null && wf.getLabel().equals(label))
				return wf;
		Log.e("nils","flow not found: "+label);
		return null;
	}	


	public String[] getWorkflowNames() {
		if (myWfs==null)
			return null;
		String[] array = new String[myWfs.keySet().size()];
		myWfs.keySet().toArray(array);
		return array;

	}

	public String[] getWorkflowLabels() {
		if (myWfs==null)
			return null;
		String[] array = new String[myWfs.keySet().size()];
		int i=0;String label;
		for (Workflow wf:myWfs.values()) {
			label = wf.getLabel();
			if (label!=null)
				array[i++] = label;
		}
		return array;

	}














	/**************************************
	 * Getter/Setter for sync status and Globally accessible method for sending data asynchronously to twin device.
	 */

	public enum SyncStatus {
		stopped,
		searching,
		waiting_for_ping,
		waiting_for_data,
		reading_data_from_db,
		writing_data,
		waiting_for_ack,		
		ack_received, 
		sending, 
		waiting_for_connection_to_close, started, restarting, insert_update_message, 

	}
	public SyncStatus getSyncStatus() {
		return syncStatus;
	}

	



	public synchronized Aritmetic makeAritmetic(String name, String label) {
		/*Variable result = myVars.get(name);
		if (result == null) {
		    myVars.put(name, result = new Aritmetic(name,label));
		    return (Aritmetic)result;
		}
		else {
			return (Aritmetic)result;
		}
		 */
		return new Aritmetic(name,label);
	}




	public VarCache getVariableCache() {
		return myVarCache;
	}

	public LoggerI getLogger() {
		return log;
	}

	public void setCurrentContext(WF_Context myContext) {
		currentContext = myContext;
	}

	public WF_Context getCurrentContext() {
		return currentContext;
	}

	int logID=1;

	private Map<String,String> myKeyHash;


	private Map<String, Variable> myRawHash;


	private RuleExecutor myExecutor;


	public Map<String,String> getCurrentKeyHash() {

		//Log.d("vortex",this.toString()+" getCurrentKeyHash returned "+myKeyHash);
		return myKeyHash==null?null:myKeyHash;
	}


	public void  setKeyHash(Map<String,String> h) { 	
		//artLista.destroyCache();
		myExecutor.destroyCache();
		myKeyHash=h;
		Log.d("vortex","SetKeyHash was called with "+h+" on this "+this.toString());
	}

	public void setRawHash(Map<String,Variable> h) {
		myRawHash = h;
	}

	//TODO:This is not working since keyhash is changed separately from rawhash.
	public Map<String, String> refreshKeyHash() {
		if (myRawHash == null) {
			Log.d("nils","Failed to renew hash - no raw hash available");

		} else {
			for (String s:myRawHash.keySet()) {
				Variable v= myRawHash.get(s);
				Log.d("vortex"," RH Var "+v.getLabel()+" value "+v.getValue());
				String value = v.getValue();
				myKeyHash.put(s, value);
			}
		}
		Log.d("nils","Keyhash refreshed");
		return getCurrentKeyHash();
	}



	public boolean isMaster() {
		String m;
		if ((m = globalPh.get(PersistenceHelper.DEVICE_COLOR_KEY)).equals(PersistenceHelper.UNDEFINED)) {
			globalPh.put(PersistenceHelper.DEVICE_COLOR_KEY, "Master");
			return true;
		}
		else
			return m.equals("Master");

	}

	public boolean isSolo() {
		return globalPh.get(PersistenceHelper.DEVICE_COLOR_KEY).equals("Solo");
	}

	public boolean isSlave() {
		return globalPh.get(PersistenceHelper.DEVICE_COLOR_KEY).equals("Client");
	}
	/*
	public MessageHandler getHandler() {
		if (myHandler==null)
			myHandler = getNewMessageHandler(isMaster());
		return myHandler;
	}

	public void resetHandler() {
		myHandler = getNewMessageHandler(isMaster());
		getHandler();
	}

	private MessageHandler getNewMessageHandler(boolean master) {
		if (master)
			return new MasterMessageHandler();
		else
			return new SlaveMessageHandler();
	}
	 */
	public enum ErrorCode {
		ok,
		missing_required_column,
		file_not_found, workflows_not_found,
		tagdata_not_found,parse_error,
		config_not_found,spinners_not_found,
		missing_lag_id,
		missing_user_id,

	}





	public ErrorCode checkSyncPreconditions() {
		if (this.isMaster()&&globalPh.get(PersistenceHelper.LAG_ID_KEY).equals(PersistenceHelper.UNDEFINED))
			return ErrorCode.missing_lag_id;
		else if (globalPh.get(PersistenceHelper.USER_ID_KEY).equals(PersistenceHelper.UNDEFINED))
			return ErrorCode.missing_user_id;

		else 
			return ErrorCode.ok;
	}


	Handler mHandler= new Handler(Looper.getMainLooper()) {
	      
        @Override
		public void handleMessage(Message msg) {
        	Intent intent =null;

        	if(msg.obj instanceof String) {
        	Log.d("vortex","IN HANDLE MESSAGE WITH MSG: "+msg.toString());
        	String s = (String)msg.obj;
        	intent = new Intent();
    		intent.setAction(s);
        	} else
        		if (msg.obj instanceof Intent)
        			intent = (Intent)msg.obj;
    		if (intent!=null)
    			getContext().sendBroadcast(intent);
    		else
    			Log.e("vortex","Intent was null in handleMessage");
        	
		}

		
	};

	
	public void sendSyncEvent(Intent intent) {
		Message m = Message.obtain(mHandler);
		m.obj=intent;
		m.sendToTarget();
	}

	public void sendEvent(String action) {
		Log.d("vortex","IN SEND EVENT WITH ACTION "+action);
		if (mHandler!=null) {
			Message m = Message.obtain(mHandler);
			m.obj=action;
			m.sendToTarget();
		} else
			Log.e("vortex","NO MESSAGE NO HANDLER!!");
	}

	SyncMessage message;


	private StatusHandler myStatusHandler;



	public void setSyncMessage(SyncMessage message) {
		this.message=message;
	}

	public SyncMessage getOriginalMessage() {
		return message;
	}


	public void setMyPartner(String partner) {
		myPartner = partner;
	}

	public String getMyPartner() {
		return myPartner;
	}

	public StatusHandler getStatusHandler() {
		return myStatusHandler;
	}


	private Configuration myModules;





/*
	public void synchronise(SyncEntry[] ses, boolean isMaster) {	
		Log.e("nils,","SYNCHRONIZE. MESSAGES: ");
		setSyncStatus(SyncStatus.writing_data);
		for(SyncEntry se:ses) {
			Log.e("nils","Action:"+se.getAction());
			Log.e("nils","Target: "+se.getTarget());
			Log.e("nils","Keys: "+se.getKeys());
			Log.e("nils","Values:"+se.getValues());
			Log.e("nils","Change: "+se.getChange());

		}
		db.synchronise(ses, myVarCache,this);
		
	}
*/
	public DrawerMenu getDrawerMenu() {
		// TODO Auto-generated method stub
		return myDrawerMenu;
	}

	public void setDrawerMenu(DrawerMenu mDrawerMenu) {
		myDrawerMenu = mDrawerMenu;
	}


	public Map<String, Workflow> getWfs() {
		return myWfs;
	}


	//Change current context (side effect) to the context given in the workflow startblock.
	//If no context can be built (missing variable values), return error. Otherwise, return null.


	public CHash evaluateContext(String cContext) {
		boolean contextError=false;
		String err = null;
		Map<String, String> keyHash = null;
		Map<String, Variable> rawHash = null;
		LoggerI o = getLogger();
		Log.d("noob","In evaluate Context!!");
		if (cContext==null||cContext.isEmpty()) {
			Log.d("nils","No context!!");
			keyHash = this.getCurrentKeyHash();
			rawHash = this.myRawHash;
		} else {
			keyHash = new HashMap<String, String>();
			rawHash = new HashMap<String, Variable>();
			Log.d("nils","Found context!!");
			String[] pairs = cContext.split(",");
			if (pairs==null||pairs.length==0) {
				o.addRow("Could not split context on comma (,)");
				err = "Could not split context on comma (,). ";
				contextError = true;			
			} else {

				for (String pair:pairs) {
					Log.d("nils","found pair: "+pair);
					if (pair!=null&&!pair.isEmpty()) {
						String[] kv = pair.split("=");
						if (kv==null||kv.length<2) {
							o.addRow("");
							o.addRedText("Could not split context on equal sign (=).");
							contextError=true;
							err = "Could not split context on equal sign (=).";
						} else {
							//Calculate value of context variables, if any.
							//is it a variable or a value?
							String arg = kv[0].trim();
							String val = kv[1].trim();
							Log.d("nils","Keypair: "+arg+","+val);

							if (val.isEmpty()||arg.isEmpty()) {
								o.addRow("");
								o.addRedText("Empty variable or argument in definition");
								err = "Empty variable or argument in definition";
								contextError=true;
								break;
							} else {

								if (Character.isDigit(val.charAt(0))) {
									//constant
									keyHash.put(arg, val);
									Log.d("nils","Added "+arg+","+val+" to current context");
								} 
								else {
									//TODO: Get rid of historical token...
									if (val.equals(VariableConfiguration.HISTORICAL_MARKER)) {
										Log.d("vortex","Historical!");
										keyHash.put(arg,val);
									} else if (val.equals("*")) {
										Log.d("vortex","WILDCARD!!");
										keyHash.put(arg,val);
									}
									else {
										//Variable or function. need to evaluate first..
										Variable v=null;// = getVariableConfiguration().getVariableInstance(val);
										String varVal=null;
										//if (v==null) {
										//Parse value..either constant, function or variable.

										List<TokenizedItem> tokens = myExecutor.findTokens(val, null);
										if (tokens!=null && !tokens.isEmpty()) {										
											TokenizedItem firstToken = tokens.get(0);
											if (firstToken.getType()==TokenType.variable) {
												Log.d("vortex","Found variable!");
												v = firstToken.getVariable();
												varVal = v.getValue();
												if (varVal==null) {
													o.addRow("");
													o.addRedText("One of the variables used in current context("+v.getId()+") has no value in database");
													Log.e("nils","var was null or empty: "+v.getId());
													err = "One of the variables used in current context("+v.getId()+") has no value in database";
													contextError=true;
												}
											} else if (firstToken.getType().getParent()==TokenType.function) {
												Log.d("vortex","Found function!");
												SubstiResult subsRes = myExecutor.substituteForValue(tokens, val, false);
												if (subsRes!=null) {
													varVal = subsRes.result;
												} else {
													Log.e("vortex","subsresult was null for function"+val+" in evalContext");
													contextError=true;
													err = "subsresult was null for function"+val+" in evalContext";
												}
											} else if (firstToken.getType()==TokenType.literal) {
												Log.d("vortex","Found literal!");
												varVal = val;
											} else {
												Log.e("vortex","Could not find "+firstToken.getType().name());
												contextError=true;
												err = "Could not find "+firstToken.getType().name();
											}

										} else {
											o.addRow("");
											o.addRedText("Could not evaluate expression "+val+" in context");
											Log.e("vortex","Could not evaluate expression "+val+" in context");
											contextError=true;
											err="Could not evaluate expression "+val+" in context";
										}


										/*
											contextError=true;
											o.addRow("");
											o.addRedText("One of the variables missing: "+val);
											err = "Context missing (at least) variable: "+val;
											Log.d("nils","Couldn't find variable "+val);
											break;
										 */

										//} else 
										//	varVal = v.getValue();

										if(!contextError) {
											keyHash.put(arg, varVal);
											rawHash.put(arg,v);
											Log.d("nils","Added "+arg+","+varVal+" to current context");
											if (v!=null)
												v.setKeyChainVariable(arg);
											//update status menu

										}

									}
								}
							}
						}
					} else {
						Log.d("nils","Found empty or null pair");
						contextError=true;
						err="Found empty or null pair";
					}

				} 

			}
		}
		if (keyHash!=null && !contextError && !keyHash.isEmpty()) {
			o.addRow("");
			o.addYellowText("Context now: "+keyHash.toString());
			return new CHash(keyHash,rawHash);
		}
		else
			return new CHash(err);

	}

	public void setModules(Configuration myModules) {
		this.myModules = myModules;
	}



	public static void destroy() {
		singleton=null;
	}

	public Tracker getTracker() {
		return myTracker;
	}

	public File getCachedFileFromUrl(String fileName) {
		return Tools.getCachedFile(fileName, Constants.VORTEX_ROOT_DIR+globalPh.get(PersistenceHelper.BUNDLE_NAME)+"/cache/");
	}

	
	public ConnectionManager getConnectionManager() {
		return myConnectionManager;
	}

	//Get a string resource and print it. convenience function.
	public CharSequence getString(int identifier) {
		return getContext().getResources().getString(identifier);
	}
	




}




