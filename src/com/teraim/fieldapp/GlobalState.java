package com.teraim.fieldapp;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.CHash;
import com.teraim.fieldapp.dynamic.types.SpinnerDefinition;
import com.teraim.fieldapp.dynamic.types.Table;
import com.teraim.fieldapp.dynamic.types.VarCache;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.expr.Aritmetic;
import com.teraim.fieldapp.expr.Parser;
import com.teraim.fieldapp.gis.Tracker;
import com.teraim.fieldapp.loadermodule.Configuration;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.non_generics.StatusHandler;
import com.teraim.fieldapp.synchronization.ConnectionManager;
import com.teraim.fieldapp.synchronization.SyncMessage;
import com.teraim.fieldapp.ui.DrawerMenu;
import com.teraim.fieldapp.ui.MenuActivity;
import com.teraim.fieldapp.utils.BackupManager;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;


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


	private static Context myC=null;
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
	
	public String TEXT_LARGE;
	private WF_Context currentContext;
	private String myPartner="?";
	private VarCache myVarCache;
	private PersistenceHelper globalPh=null;
	private Tracker myTracker;
	private ConnectionManager myConnectionManager; 
	private BackupManager myBackupManager;
	private static Account mAccount;
	
	public static GlobalState getInstance() {

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
		artLista = new VariableConfiguration(this,t);			
		myWfs = mapWorkflowsToNames(workflows);		
		//Event Handler on the Bluetooth interface.
		//myHandler = getHandler();
		//Handles status for 
		myStatusHandler = new StatusHandler(this);

		mySpinnerDef = sd;

		singleton =this;

		//GPS listener service
		myTracker = new Tracker();

		//myExecutor = new RuleExecutor(this);

		myConnectionManager = new ConnectionManager(this);
	
		myBackupManager = new BackupManager(this);
		
		myBackupManager.startBackupIfTimeAndNeed();
		
		
		
		
		sendEvent(MenuActivity.REDRAW);
	}


	public static Account getmAccount(Context ctx) {
		if (mAccount==null )
			mAccount = CreateSyncAccount(ctx);
		return mAccount;
	}

	/*Validation
	 * 
	 */

	

	/*Singletons available for all classes
	 * 
	 */
	public SpinnerDefinition getSpinnerDefinitions() {
		return mySpinnerDef;
	}
	
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

	//public RuleExecutor getRuleExecutor() {
	//	return myExecutor;
	//}

	public VariableConfiguration getVariableConfiguration() {
		return artLista;
	}

	public BackupManager getBackupManager() {
		return myBackupManager;
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

	private CHash myKeyHash;

	//private RuleExecutor myExecutor;

	public CHash getCurrentKeyHash() {
		if (myKeyHash == null)
			return new CHash(null,null);
		else 
			return myKeyHash;
	}
	public Map<String,String> getCurrentKeyMap() {

		//Log.d("vortex",this.toString()+" getCurrentKeyHash returned "+myKeyHash);
		if (myKeyHash == null)
			return null;
		else
			return myKeyHash.getContext();
	}


	public void  setKeyHash(CHash context) { 	
		myKeyHash=context;
		Log.d("vortex","SetKeyHash was called with "+context+" on this "+this.toString());
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
		if (mHandler!=null) {
		Message m = Message.obtain(mHandler);
		m.obj=intent;
		m.sendToTarget();
		} else
			Log.e("vortex","NO MESSAGE NO HANDLER!!");
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

	

	  /**
     * Create a new dummy account for the sync adapter
     *
     * @param context The application context
     */
    private static Account CreateSyncAccount(Context context) {
        // Create the account type and default account
        Account newAccount = new Account(
                Start.ACCOUNT, Start.ACCOUNT_TYPE);
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(
                		Start.ACCOUNT_SERVICE);
        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call context.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */
        	Log.d("vortex","Created account: "+newAccount.name);
        	
        } else {
        	/*
        	Account[] aa = accountManager.getAccounts();
        	Log.d("vortex","Accounts found: ");
        	for (Account a:aa) {
        		Log.d("vortex",a.name);
        		if (a.equals(newAccount)) {
        			Log.d("vortex","failed...exists..");
        			break;
        		}
        	}
        	*/
            /*
             * The account exists or some other error occurred. Log this, report it,
             * or handle it internally.
             */
        	Log.d("vortex","add  sync account failed for some reason");
        }
        return newAccount;
    }

	public void onStart() {
		//check synk
		//db.processSyncEntriesIfAny();
			
	}

	




}




