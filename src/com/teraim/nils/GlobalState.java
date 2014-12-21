package com.teraim.nils;

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

import com.teraim.nils.bluetooth.BluetoothConnectionService;
import com.teraim.nils.bluetooth.MasterMessageHandler;
import com.teraim.nils.bluetooth.MessageHandler;
import com.teraim.nils.bluetooth.SlaveMessageHandler;
import com.teraim.nils.bluetooth.SyncEntry;
import com.teraim.nils.bluetooth.SyncEntryHeader;
import com.teraim.nils.dynamic.VariableConfiguration;
import com.teraim.nils.dynamic.types.SpinnerDefinition;
import com.teraim.nils.dynamic.types.Table;
import com.teraim.nils.dynamic.types.VarCache;
import com.teraim.nils.dynamic.types.Variable;
import com.teraim.nils.dynamic.types.Workflow;
import com.teraim.nils.dynamic.workflow_realizations.WF_Context;
import com.teraim.nils.expr.Aritmetic;
import com.teraim.nils.expr.Parser;
import com.teraim.nils.log.DummyLogger;
import com.teraim.nils.log.FastLogger;
import com.teraim.nils.log.LoggerI;
import com.teraim.nils.non_generics.Constants;
import com.teraim.nils.ui.DrawerMenu;
import com.teraim.nils.non_generics.StatusHandler;
import com.teraim.nils.ui.MenuActivity;
import com.teraim.nils.utils.DbHelper;
import com.teraim.nils.utils.DbHelper.Selection;
import com.teraim.nils.utils.PersistenceHelper;
import com.teraim.nils.utils.Tools;


/**
 * 
 * @author Terje
 *
 * Classes defining datatypes for ruta, provyta, delyta and tåg.
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
			log = new FastLogger(this.getContext(),"RUNTIME");
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
		if (artLista == null||myWfs==null||mySpinnerDef==null)
			return ErrorCode.file_not_found;
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

	public VariableConfiguration getArtLista() {
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
		Map<String,Workflow> ret = new TreeMap<String,Workflow>(String.CASE_INSENSITIVE_ORDER);
		List<Workflow> l = ((ArrayList<Workflow>)Tools.readObjectFromFile(myC,Constants.CONFIG_FILES_DIR+Constants.WF_FROZEN_FILE_ID));		
		if (l==null) 
			Log.e("NILS","Parse Error: Workflowlist is null in SetWorkFlows");
		else {
			for (Workflow wf:l)
				if (wf!=null) {
					if (wf.getName()!=null) {
						Log.d("NILS","Adding wf with id "+wf.getName()+" and length "+wf.getName().length());
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
			return "SÖKER";
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

	Map<String,String> myKeyHash;


	private Map<String, Variable> myRawHash;


	public Map<String,String> getCurrentKeyHash() {
		return myKeyHash;
	}


	public void  setKeyHash(Map<String,String> h) { 	
		artLista.destroyCache();
		myKeyHash=h;
	}

	public void setRawHash(Map<String,Variable> h) {
		myRawHash = h;
	}

	public Map<String, String> refreshKeyHash() {
		if (myRawHash == null) {
			Log.d("nils","Failed to renew hash - no raw hash available");

		} else {
			for (String s:myRawHash.keySet()) {
				Variable v= myRawHash.get(s);
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
		missing_lag_id,
		missing_user_id,
		current_ruta_not_set,
		current_provyta_not_set,
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
		else if (isMaster()&&getArtLista().getVariableValue(null, "Current_Ruta")==null)
			return ErrorCode.current_ruta_not_set;
		else if (isMaster()&&getArtLista().getVariableValue(null, "Current_Provyta")==null)
			return ErrorCode.current_provyta_not_set;
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

	Object message;


	private StatusHandler myStatusHandler;



	public void setMsg(Object message) {
		this.message=message;
	}

	public Object getOriginalMessage() {
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





}




