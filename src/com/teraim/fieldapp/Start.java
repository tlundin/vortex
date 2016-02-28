package com.teraim.fieldapp;

import java.lang.reflect.Field;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.ViewConfiguration;

import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.templates.LinjePortalTemplate;
import com.teraim.fieldapp.dynamic.types.CHash;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnActivityResult;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.log.Logger;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.synchronization.SyncEntry;
import com.teraim.fieldapp.synchronization.SyncStatus;
import com.teraim.fieldapp.synchronization.SyncStatusListener;
import com.teraim.fieldapp.synchronization.framework.SyncService;
import com.teraim.fieldapp.ui.DrawerMenu;
import com.teraim.fieldapp.ui.LoginConsoleFragment;
import com.teraim.fieldapp.ui.MenuActivity;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;



/**
 * @author Terje
 *
 */
public class Start extends MenuActivity {

	public static boolean alive = false;
	
	//	private Map<String,List<String>> menuStructure;

	//	private ArrayList<String> rutItems;
	//	private ArrayList<String> wfItems;
	private LoginConsoleFragment loginFragment;
	private AsyncTask<GlobalState, Integer, LoadResult> histT=null;
	public static Start singleton;
	private DrawerMenu mDrawerMenu;

	private ActionBarDrawerToggle mDrawerToggle;
	private Logger debugLogger;
	private boolean loading = false;

	// Constants
    // The authority for the sync adapter's content provider
    public static final String AUTHORITY = "com.teraim.fieldapp.provider";
    // An account type, in the form of a domain name
    public static final String ACCOUNT_TYPE = "teraim.com";
    // The account name
    public static final String ACCOUNT = "FieldApp";

	public static final long SYNC_INTERVAL = 60;
    // Instance fields
   // Account mAccount;

	private ContentResolver mResolver;

	
	

	/**
	 * Program entry point
	 * 
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		Log.d("nils","in START onCreate");
		singleton = this;
		//This is the frame for all pages, defining the Action bar and Navigation menu.
		setContentView(R.layout.naviframe);
		//This combats an issue on the target panasonic platform having to do with http reading.
		System.setProperty("http.keepAlive", "false"); 
		mDrawerMenu = new DrawerMenu(this);
		mDrawerToggle = mDrawerMenu.getDrawerToggle();
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
		
		  // Create a Sync account
       // mAccount = CreateSyncAccount(this);
	  	
		//Determine if program should start or first reload its configuration.
		if (!loading)
			checkStatics();
		
		 try {
		        ViewConfiguration config = ViewConfiguration.get(this);
		        Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
		        if(menuKeyField != null) {
		            menuKeyField.setAccessible(true);
		            menuKeyField.setBoolean(config, false);
		        }
		    } catch (Exception ex) {
		        // Ignore
		    }
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		Log.d("nils","In START onResume");
		//Check if program is already up.
		if (!loading)
			checkStatics();
		else 
			loading = false;
		 
		
		super.onResume();

	}

	
	
	
	/**
	 * 
	 */
	private void checkStatics() {
		if (GlobalState.getInstance()==null) {
			loading = true;
			Log.d("vortex","Globalstate null...need to reload");
			//Create a global logger.
			
			
			//Start the login fragment.
			android.app.FragmentManager fm = getFragmentManager();
			for(int i = 0; i < fm.getBackStackEntryCount(); ++i) {    
			    fm.popBackStack();
			}
			loginFragment = new LoginConsoleFragment();
			
			fm.beginTransaction()
				.replace(R.id.content_frame, loginFragment)
				.commit();
			
		} else {
			Log.d("vortex","Globalstate is not null!");
			
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.d("nils","In oncofigChanged");
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Pass event to ActionBarDrawerToggle, if it returns
		// true, then it has handled the app icon touch event
		if (mDrawerToggle.onOptionsItemSelected(item)) 
			return true;

		// Handle other action bar items

		return super.onOptionsItemSelected(item);
	}



	//




	@Override
	public void setTitle(CharSequence title) {

		getActionBar().setTitle(title);
	}

	//execute workflow.
	public void changePage(Workflow wf, String statusVar) {
		if (wf==null) {
			debugLogger.addRow("Workflow not defined for button. Check your project XML");
			Log.e("vortex","no wf in changepage");
			return;
		}
		GlobalState gs = GlobalState.getInstance();
		String label = wf.getLabel();
		String template = wf.getTemplate();
		
		//Set context.
		Log.e("vortex","change page called with wf "+wf.getName());
		CHash cHash = CHash.evaluate(wf.getContext());
		//if Ok err is null.
		if (cHash.isOk()) {
			debugLogger.addRow("Context now [");
			debugLogger.addGreenText(cHash.toString());
			debugLogger.addText("]");
			debugLogger.addText("wf context: "+wf.getContext());

			//gs.setRawHash(r.rawHash);
			//gs.setKeyHash(r.keyHash);
			//No template. This flow does not have a ui. Hand over to Executor.
			Fragment fragmentToExecute;
			Bundle args = new Bundle();
			args.putString("workflow_name", wf.getName());
			args.putString("status_variable", statusVar);
			
			if (template==null) {
				fragmentToExecute = wf.createFragment("EmptyTemplate");
				fragmentToExecute.setArguments(args);
				FragmentTransaction ft = getFragmentManager()
						.beginTransaction();
				//Log.i("vortex", "Adding fragment");
				//ft.add(R.id.lowerContainer, fragment, "AddedFragment");

				ft.add(fragmentToExecute,"EmptyTemplate");
				Log.i("vortex", "Committing Empty transaction");
				ft.commitAllowingStateLoss();
				Log.i("vortex", "Committed transaction");
			} else {				
				fragmentToExecute = wf.createFragment(template);
				fragmentToExecute.setArguments(args);
				changePage(fragmentToExecute,label);
			}
			//show error message.
		} else 
			showErrorMsg(cHash);
	}
	public void changePage(Fragment newPage, String label) {
		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager.beginTransaction()
		.replace(R.id.content_frame, newPage)
		.addToBackStack(null)
		.commit();
		setTitle(label);

		//mDrawerLayout.closeDrawer(mDrawerList);

	}


	/******************************
	 * Network?
	 */

	public boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager 
		= (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}






	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		GlobalState.getInstance().getCurrentContext().registerEvent(new WF_Event_OnActivityResult("Start",EventType.onActivityResult));
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
	}




	@Override
	public void onDestroy() {
		if (histT!=null) {
			Log.d("nils","Trying to cancel histT");
			histT.cancel(true);
		}
		if (GlobalState.getInstance()!=null) {
			
			GlobalState.getInstance().getDb().closeDatabaseBeforeExit();
			
			GlobalState.destroy();
		}

		super.onDestroy();
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			Log.d("vortex","gets here key back");

			GlobalState gs = GlobalState.getInstance();
			if (gs!=null) {
			final WF_Context wfCtx = gs.getCurrentContext();
			boolean map=false;
			if (wfCtx!=null) {
				if (wfCtx.getCurrentGis()!=null) {
					map=true;
				}
				Workflow wf = wfCtx.getWorkflow();
				Log.d("vortex","gets here wf is "+wf);
				if (wf!=null) {
					if (!wf.isBackAllowed()) {
						new AlertDialog.Builder(this).setTitle("Warning!")
						.setMessage("This will exit the page.")
						.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {

								wfCtx.mapLayer--;
								Log.d("vortex","mapLayer is now "+wfCtx.mapLayer);
								getFragmentManager().popBackStackImmediate();
								GlobalState.getInstance().setCurrentContext(null);
							}})
							.setNegativeButton(R.string.cancel,new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) { 

								}})
								.setCancelable(false)
								.setIcon(android.R.drawable.ic_dialog_alert)
								.show();					
					} else {
						if (map)
							wfCtx.mapLayer--;
						Log.d("vortex","back was allowed");
					}
				}
			}
			
			if (getFragmentManager().findFragmentById(R.id.content_frame) instanceof LinjePortalTemplate) {
				final LinjePortalTemplate lp = (LinjePortalTemplate)getFragmentManager().findFragmentById(R.id.content_frame);
				if (lp.isRunning()) {
					new AlertDialog.Builder(this).setTitle("Linjem�tning p�g�r!")
					.setMessage("Vill du verkligen g� ut fr�n sidan?")
					.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							getFragmentManager().popBackStackImmediate();
						}})
						.setNegativeButton(R.string.no,new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) { 

							}})
							.setCancelable(false)
							.setIcon(android.R.drawable.ic_dialog_alert)
							.show();								
				}
			}
			setTitle("");
			}
		}

		return super.onKeyDown(keyCode, event);
	}

	private void showErrorMsg(CHash context) {

			String dialogText = "Faulty or incomplete context\nError: "+context.toString();
			new AlertDialog.Builder(this)
			.setTitle("Context problem")
			.setMessage(dialogText) 
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setCancelable(false)
			.setNeutralButton("Ok",new Dialog.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					

				}
			} )
			.show();
		
	}




	public DrawerMenu getDrawerMenu() {
		return mDrawerMenu;
	}




	public LoggerI getLogger() {
		if (debugLogger==null)
			debugLogger = new Logger(this,"DEBUG");
		return debugLogger;
	}

	
	
 
    
    


    /*
    public void sayHello() {
        if (!mBound) return;
        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, SyncService.MSG_SAY_HELLO, 0, 0);
        
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    */
    
  


}
