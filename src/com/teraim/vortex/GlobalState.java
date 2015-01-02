package com.teraim.vortex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.teraim.vortex.bluetooth.BluetoothConnectionService;
import com.teraim.vortex.bluetooth.MasterMessageHandler;
import com.teraim.vortex.bluetooth.MessageHandler;
import com.teraim.vortex.bluetooth.SlaveMessageHandler;
import com.teraim.vortex.bluetooth.SyncEntry;
import com.teraim.vortex.bluetooth.SyncEntryHeader;
import com.teraim.vortex.bluetooth.SyncMessage;
import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.dynamic.blocks.StartBlock;
import com.teraim.vortex.dynamic.types.SpinnerDefinition;
import com.teraim.vortex.dynamic.types.Table;
import com.teraim.vortex.dynamic.types.VarCache;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.types.Workflow;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;
import com.teraim.vortex.expr.Aritmetic;
import com.teraim.vortex.expr.Parser;
import com.teraim.vortex.log.DummyLogger;
import com.teraim.vortex.log.FastLogger;
import com.teraim.vortex.log.Logger;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.non_generics.StatusHandler;
import com.teraim.vortex.ui.DrawerMenu;
import com.teraim.vortex.ui.MenuActivity;
import com.teraim.vortex.utils.DbHelper;
import com.teraim.vortex.utils.PersistenceHelper;
import com.teraim.vortex.utils.Tools;
import com.teraim.vortex.utils.DbHelper.Selection;


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
	private MessageHandler myHandler;

	private DrawerMenu myDrawerMenu;

	//Global state for sync.
	private int syncStatus=BluetoothConnectionService.SYNK_STOPPED;	



	public String TEXT_LARGE;


	private WF_Context currentContext;


	private ParameterSafe mySafe;

	private String myPartner="?";


	private VarCache myVarCache;

	public static GlobalState getInstance(Context c) {
		if (singleton == null) {			
			singleton = new GlobalState(c.getApplicationContext());
		}
		return singleton;

	}

	private GlobalState(Context ctx)  {

		myC = ctx;
		//Shared PreferenceHelper 
		//SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(ctx);

		ph = new PersistenceHelper(myC.getSharedPreferences("nilsPrefs", Context.MODE_PRIVATE));
		//Logger. Note that logger must be initialized with a TextView when used! 
		if (ph.getB(PersistenceHelper.DEVELOPER_SWITCH))
			log = new Logger(this.getContext(),"RUNTIME");
		//removeLogger();
		else {
			Log.d("nils","LOGGER WAS REMOVED");
			removeLogger();
		}
		//Parser for rules
		parser = new Parser(this);
		//Artlista

		myVarCache = new VarCache(this);

		artLista = new VariableConfiguration(this,myVarCache);	

		//Database Helper
		db = new DbHelper(ctx,artLista.getTable(),ph);

		myWfs = thawWorkflows();		

		//Spinners
		mySpinnerDef = Tools.thawSpinners(ctx);

		//Event Handler on the Bluetooth interface.
		myHandler = getHandler();

		//Get ParameterSafe.
		mySafe = getSafe();

		//Handles status for 
		myStatusHandler = new StatusHandler(this);


	}


	/*Validation
	 * 
	 */
	public ErrorCode validateFrozenObjects() {

		if (myWfs == null)
			return ErrorCode.workflows_not_found;
		if (artLista == null)
			return ErrorCode.config_not_found;
		if (mySpinnerDef==null)
			return ErrorCode.spinners_not_found;
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


	/*Singletons available for all classes
	 * 
	 */
	public SpinnerDefinition getSpinnerDefinitions() {
		return mySpinnerDef;
	}

	public void setSpinnerDefinitions(SpinnerDefinition sd) {
		if (sd!=null)
			Log.d("nils","SetSpinnerDef called with "+sd.size()+" spinners");
		else 
			Log.e("nils","Spinnerdef null!!!");
		mySpinnerDef=sd;
	}

	public PersistenceHelper getPersistence() {
		return ph;
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

	public VariableConfiguration getVariableConfiguration() {
		return artLista;
	}

	public ParameterSafe getSafe() {
		//if null, try read from file.
		if (mySafe==null)
			mySafe = (ParameterSafe) Tools.readObjectFromFile(myC, Constants.CONFIG_FILES_DIR+"mysafe");
		//if fail, create new.
		if (mySafe == null)
			createSafe();
		return mySafe;
	}

	public void createSafe() {
		Log.d("nils","CREATING NEW SAFE OBJECT!!!!");
		mySafe = new ParameterSafe();
		ArrayList<Integer> rutor = new ArrayList<Integer>();
		HashSet<Integer> temp = new HashSet<Integer>();
		List<String[]> values=null;
		try {
			values = getDb().getValues(new String[] {getDb().getColumnName("ruta")}, new Selection());
		} catch (SQLiteException e) {
			Log.e("nils","Error calling database");
			e.printStackTrace();
		}
		if (values!=null) {
			for (String[] val:values)
				temp.add(Integer.parseInt(val[0]));
			rutor.addAll(temp);
			Collections.sort(rutor);
			mySafe.setRutor(rutor);						
		}
		Tools.witeObjectToFile(myC, mySafe, Constants.CONFIG_FILES_DIR+"mysafe");

	}

	/**************************************************
	 * 
	 * Thawing of files to objects.
	 */

	public Map<String,Workflow> thawWorkflows() {
		Map<String,Workflow> ret=null;
		List<Workflow> l = ((ArrayList<Workflow>)Tools.readObjectFromFile(myC,Constants.CONFIG_FILES_DIR+Constants.WF_FROZEN_FILE_ID));		
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

	public Table thawTable() { 	
		return ((Table)Tools.readObjectFromFile(myC,Constants.CONFIG_FILES_DIR+Constants.CONFIG_FROZEN_FILE_ID));		
	}

	public Workflow getWorkflow(String id) {
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

	public int getSyncStatus() {
		return syncStatus;
	}

	public String getSyncStatusS() {
		switch (syncStatus) {
		case BluetoothConnectionService.SYNK_STOPPED:
			return "AV";
		case BluetoothConnectionService.SYNK_SEARCHING:
			return "S�KER";
		case BluetoothConnectionService.SYNC_READY_TO_ROCK:
			return "REDO";
		case BluetoothConnectionService.BUSY:
			return "AKTIV";
		default:
			return "?";
		}
	}

	public void setSyncStatus(int status) {
		syncStatus = status;
		sendEvent(MenuActivity.REDRAW);
	}

	public synchronized boolean sendMessage(Object message) {
		if (syncIsAllowed()){
			setSyncStatus(BluetoothConnectionService.BUSY);
			Log.d("nils","Message is being sent now..");
			BluetoothConnectionService.getSingleton().send(message);
			setSyncStatus(BluetoothConnectionService.SYNC_READY_TO_ROCK);
			return true;
		} else
			return false;
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

	/*************************************
	 * 
	 * Variable Generators.
	 * 
	 */
	/*
	public synchronized Bool makeBoolean(String name, String label) {
		Variable result = myVars.get(name);
		if (result == null) {
		    myVars.put(name, result = new Bool(name,label));
		    return (Bool)result;
		}
		else {
			return (Bool)result;
		}
	}

	public synchronized Numeric makeNumeric(String name, String label) {
		Variable result = myVars.get(name);
		if (result == null) {
		    myVars.put(name, result = new Numeric(name,label));
		    return (Numeric)result;
		}
		else {
			return (Numeric)result;
		}
	}

	public synchronized Literal makeLiteral(String name, String label) {
		Variable result = myVars.get(name);
		if (result == null) {
		    myVars.put(name, result = new Literal(name,label));
		    return (Literal)result;
		}
		else {
			return (Literal)result;
		}
	}	

	public Variable getVariable(String name) {
		return myVars.get(name);
	}
	 */




	//IF new configuration files have been loaded, replace existing instances.
	public void refresh() {
		artLista = new VariableConfiguration(this,myVarCache);	
		myWfs = thawWorkflows();	
		if (artLista.getTable()!=null) 
			db.init(artLista.getTable().getKeyParts());			
		else {
			log.addRow("");
			log.addRedText("Refresh failed - Table is missing. This is likely due to previous errors on startup");
		}
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
	public void createLogger() {
		log = new FastLogger(this.getContext(),"CREATED "+logID++);
		//log = new Logger(this.getContext(),"CREATED "+logID++);
	}

	public void removeLogger() {
		log = new DummyLogger();
	}

	private Map<String,String> myKeyHash;


	private Map<String, Variable> myRawHash;


	public Map<String,String> getCurrentKeyHash() {
		Log.d("vortex","getCurrentKeyHash returned "+myKeyHash);
		return myKeyHash;
	}


	public void  setKeyHash(Map<String,String> h) { 	
		artLista.destroyCache();
		myKeyHash=h;
		Log.d("vortex","SetKeyHash was called with "+h.toString());
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
		if ((m = ph.get(PersistenceHelper.DEVICE_COLOR_KEY)).equals(PersistenceHelper.UNDEFINED)) {
			ph.put(PersistenceHelper.DEVICE_COLOR_KEY, "Master");
			return true;
		}
		else
			return m.equals("Master");

	}

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
			return new MasterMessageHandler(this);
		else
			return new SlaveMessageHandler(this);
	}

	public enum ErrorCode {
		ok,
		missing_required_column,
		file_not_found, workflows_not_found,
		tagdata_not_found,parse_error,
		config_not_found,spinners_not_found,
		missing_lag_id,
		missing_user_id,
		no_handler_available

	}

	public boolean syncIsAllowed() {
		return (syncStatus == BluetoothConnectionService.SYNC_READY_TO_ROCK);
	}



	public ErrorCode checkSyncPreconditions() {
		if (this.isMaster()&&ph.get(PersistenceHelper.LAG_ID_KEY).equals(PersistenceHelper.UNDEFINED))
			return ErrorCode.missing_lag_id;
		else if (ph.get(PersistenceHelper.USER_ID_KEY).equals(PersistenceHelper.UNDEFINED))
			return ErrorCode.missing_user_id;
		else if (myHandler ==null)
			return ErrorCode.no_handler_available;
		else 
			return ErrorCode.ok;
	}

	public void triggerTransfer() {
		Log.d("nils","Doing da sync..");	
		if (syncIsAllowed()) {
			setSyncStatus(BluetoothConnectionService.BUSY);
			sendEvent(BluetoothConnectionService.SYNK_INITIATE);
			SyncEntry[] changes = db.getChanges();

			Log.d("nils","Syncrequest received. Sending "+(changes==null?"no changes":changes.toString()));
			if (changes==null) {
				log.addRow("[SENDING_SYNC-->Empty package. No changes.]");
				Log.d("nils","SENDING SYNC EMPTY");
				SyncEntryHeader seh = new SyncEntryHeader(-1);
				changes = new SyncEntry[]{seh};
			}
			else{ 
				log.addRow("[SENDING_SYNC-->"+changes.length+" rows]");	
				Log.d("nils","[SENDING_SYNC-->"+changes.length+" rows]");			
				//sendmessage will change state to not busy..if not, change it ourselves.
			}
			setSyncStatus(BluetoothConnectionService.SYNC_READY_TO_ROCK);
			if(!sendMessage(changes))
				setSyncStatus(BluetoothConnectionService.SYNC_READY_TO_ROCK);

		} else 
			Log.d("nils","Sync was not allowed.");
	}

	public void sendEvent(String action) {
		Intent intent = new Intent();
		intent.setAction(action);
		getContext().sendBroadcast(intent);
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

	boolean syncDone=false;
	public void synchronise(SyncEntry[] ses, boolean isMaster) {	
		Log.e("nils,","SYNCHRONIZE. MESSAGES: ");
		setSyncStatus(BluetoothConnectionService.BUSY);
		for(SyncEntry se:ses) {
			Log.e("nils","Action:"+se.getAction());
			Log.e("nils","Target: "+se.getTarget());
			Log.e("nils","Keys: "+se.getKeys());
			Log.e("nils","Values:"+se.getValues());
			Log.e("nils","Change: "+se.getChange());

		}
		syncDone = false;	
		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {
				if (!syncDone)
					sendEvent(BluetoothConnectionService.SYNK_BLOCK_UI);
			}}, 2000);

		db.synchronise(ses, isMaster, myVarCache,this);
		syncDone = true;
		sendEvent(BluetoothConnectionService.SYNK_UNBLOCK_UI);
		setSyncStatus(BluetoothConnectionService.SYNC_READY_TO_ROCK);

	}

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
	public String evaluateContextFromWorkflow(Workflow wf) {
		boolean contextError=false;
		String err = "undefined error";
		LoggerI o = getLogger();
		String cContext = wf.getContext();
		o.addRow("");
		o.addRow("Creating context...for workflow "+wf.getName());
		if (cContext==null||cContext.isEmpty()) {
			Log.d("nils","No context!!");
			o.addRow("");
			o.addRow("Empty or missing context. This is a potential error");
			err=null;
		} else {
			Log.d("nils","Found context!!");
			String[] pairs = cContext.split(",");
			if (pairs==null||pairs.length==0) {
				o.addRow("Could not split context on comma (,)");
				err = "Could not split context on comma (,). ";
				contextError = true;			
			} else {

				Map<String, String> keyHash = new HashMap<String, String>();
				Map<String, Variable> rawHash = new HashMap<String, Variable>();
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
									//Variable. need to evaluate first..
									Variable v = getVariableConfiguration().getVariableInstance(val);
									if (v==null) {
										contextError=true;
										o.addRow("");
										o.addRedText("One of the variables missing: "+val);
										err = "Context missing (at least) variable: "+val;
										Log.d("nils","Couldn't find variable "+val);
										break;
									} else {
										String varVal = v.getValue();
										if(varVal==null||varVal.isEmpty()) {
											contextError=true;
											err = "Context missing value for (at least): "+val;
											o.addRow("");
											o.addRedText("One of the variables used in current context("+v.getId()+") has no value in database");
											Log.e("nils","var was null or empty: "+v.getId());
											break;
										} else {

											keyHash.put(arg, varVal);
											rawHash.put(arg,v);
											Log.d("nils","Added "+arg+","+varVal+" to current context");
											v.setKeyChainVariable(arg);
											//update status menu
											sendEvent(MenuActivity.REDRAW);
										}

									}

								}
							}
						}
					} else
						Log.d("nils","Found empty or null pair");
				} 
				if (!contextError && !keyHash.isEmpty()) {
					Log.d("nils","added keyhash to gs");
					//This destroys the cache. Need to add back the keychain variables.
					setKeyHash(keyHash);
					//for (Variable v:rawHash.values()) 
					//	getVariableConfiguration().addToCache(v);
					//Keep this if the hash values change.
					setRawHash(rawHash);
				} 

			}
		}
	

	if(contextError)
		return err;
	else 
		return null;
}


}



