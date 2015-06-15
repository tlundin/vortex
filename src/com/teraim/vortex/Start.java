package com.teraim.vortex;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;

import com.teraim.vortex.dynamic.templates.LinjePortalTemplate;
import com.teraim.vortex.dynamic.types.CHash;
import com.teraim.vortex.dynamic.types.Workflow;
import com.teraim.vortex.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Event_OnActivityResult;
import com.teraim.vortex.loadermodule.LoadResult;
import com.teraim.vortex.log.Logger;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.ui.DrawerMenu;
import com.teraim.vortex.ui.LoginConsoleFragment;
import com.teraim.vortex.ui.MenuActivity;
import com.teraim.vortex.utils.PersistenceHelper;

public class Start extends MenuActivity {

	private PersistenceHelper ph;
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
		if (!loading)
			checkStatics();
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
		String context = wf.getContext();
		//Set context.
		Log.e("vortex","change page called with wf "+wf.getName());
		Log.d("noob","Context ["+context+"]");
		debugLogger.addRow("Context ["+context+"]");
		CHash cHash = gs.evaluateContext(context);
		//if Ok err is null.
		if (cHash.err==null) {
			debugLogger.addRow("Context [");
			debugLogger.addGreenText("OK");
			debugLogger.addText("]");

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
			showErrorMsg(cHash.err, wf);
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
		Log.d("nils","GETZZZZ");
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

			if (getFragmentManager().findFragmentById(R.id.content_frame) instanceof LinjePortalTemplate) {
				final LinjePortalTemplate lp = (LinjePortalTemplate)getFragmentManager().findFragmentById(R.id.content_frame);
				if (lp.isRunning()) {
					new AlertDialog.Builder(this).setTitle("Linjemätning pågår!")
					.setMessage("Vill du verkligen gå ut från sidan?")
					.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							getFragmentManager().popBackStackImmediate();
						}})
						.setNegativeButton("Nej",new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) { 

							}})
							.setCancelable(false)
							.setIcon(android.R.drawable.ic_dialog_alert)
							.show();								
				}
			}
			setTitle("");
		}

		return super.onKeyDown(keyCode, event);
	}

	private void showErrorMsg(String error, Workflow wf) {
		if (wf!=null) {
			String dialogText = "Faulty or incomplete context prevents execution of "+wf.getName()+".\n Context: "+wf.getContext()+"\nError: "+error;
			new AlertDialog.Builder(this)
			.setTitle("Context problem")
			.setMessage(dialogText) 
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setCancelable(false)
			.setNeutralButton("Ok",new Dialog.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub

				}
			} )
			.show();
		}
	}




	public DrawerMenu getDrawerMenu() {
		return mDrawerMenu;
	}




	public LoggerI getLogger() {
		if (debugLogger==null)
			debugLogger = new Logger(this,"DEBUG");
		return debugLogger;
	}
	


}
