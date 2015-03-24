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

import com.teraim.vortex.GlobalState.CHash;
import com.teraim.vortex.dynamic.templates.LinjePortalTemplate;
import com.teraim.vortex.dynamic.types.Workflow;
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
	private enum State {INITIAL, HISTORICAL_LOADED,WF_LOADED, CONF_LOADED, VALIDATE,POST_INIT, GIS_LOADED};
	private LoginConsoleFragment loginFragment;
	private AsyncTask<GlobalState, Integer, LoadResult> histT=null;
	public static Start singleton;
	private DrawerMenu mDrawerMenu;

	private ActionBarDrawerToggle mDrawerToggle;
	private Logger debugLogger;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("nils","in Menu Activity onCreate");
		singleton = this;
		//This is the frame for all pages, defining the Action bar and Navigation menu.
		setContentView(R.layout.naviframe);

		//This combats an issue on the target panasonic platform having to do with http reading.
		System.setProperty("http.keepAlive", "false"); 

		//drawermenu

		mDrawerMenu = new DrawerMenu(this);
		mDrawerToggle = mDrawerMenu.getDrawerToggle();
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
		
		//Create a global logger.
		debugLogger = new Logger(this,"DEBUG");
		
		//Start the login fragment.
		if (loginFragment == null) {
			loginFragment = new LoginConsoleFragment();
			FragmentManager fragmentManager = getFragmentManager();
			fragmentManager.beginTransaction()
			.replace(R.id.content_frame, loginFragment)
			.commit();
		}
		
		//Execution continues in loginFragment

	}




	@Override
	protected void onStart() {
		super.onStart();
	//	this.invalidateOptionsMenu();


	}

//	private boolean doRefresh=false;
/*
	private void loader(State state,ErrorCode errCode) {
		myState=state;
		loginConsole.draw();
		switch (state) {

		case INITIAL:
			//quick check that the bundle name is not null, empty or ends with xml.
			if (checkBundleName()) {
				loginConsole.addRow("Reading configuration files:\n"+ph.get(PersistenceHelper.BUNDLE_NAME)+".xml: ");
				new WorkflowParser(gs, new FileLoadedCb() {

					@Override
					public void onFileLoaded(ErrorCode errCode,String ver) {	
						if (ver!=null)
							loginConsole.addText("["+ver+"]");
						loader(State.WF_LOADED,errCode);
					}

				}).execute(this);
			} else
				loginConsole.addRedText("Missing application name! Select application to load under 'wrench' menu and restart the App!");
			break;

		case HISTORICAL_LOADED:
		case GIS_LOADED:
		case WF_LOADED:
		case CONF_LOADED:
			
			Log.d("nils","STATE: "+state);
			
			switch(errCode) {
			case ioError:
				loginConsole.addRedText("[IO-ERROR]");
				loginConsole.addRow("Couldn't load configuration\nWrong server url? :"+ph.get(PersistenceHelper.SERVER_URL)+
						"\nOr did you forget to create a folder named "+ph.get(PersistenceHelper.BUNDLE_NAME)+" ?");

				break;
			case sameold:
				loginConsole.addText("(No change)");
				break;
			case parseError:
				loginConsole.addRedText("[Parse Error]");
				break;
			case newConfigVersionLoaded:
				loginConsole.addGreenText("(New)");
				doRefresh=true;
				break;
			case newVarPatternVersionLoaded:
				loginConsole.addGreenText("(New)");
				doRefresh=true;
				break;
			case bothFilesLoaded:
				loginConsole.addGreenText("(New])");
				doRefresh=true;
				break;
			case notFound:
				loginConsole.addRedText("[Not found]");
				loginConsole.addRow("(Existing configuration will be used if found)");
				break;
			case configurationError:
				loginConsole.addRedText("[Configuration error]");
				break;
			case Loaded:
				loginConsole.addGreenText("[OK]");				
				break;
			case Aborted:
				loginConsole.addRedText("[ABORTED BY USER]");
				break;
			case LoadInBackground:
				loginConsole.addYellowText("[Loading in background]");								
			case whatever:
				break;

			}	
			//WORKFLOWS LOADED
			if (state==State.WF_LOADED) {
				loginConsole.addRow(Constants.TypesFileName+": ");			
				new ConfigFileParser(gs, new FileLoadedCb() {
					@Override
					public void onFileLoaded(ErrorCode errCode,String ver) {
						Log.d("nils","Got "+errCode+" after configfileloaded");
						if (ver!=null)
							loginConsole.addText("["+ver+"]");
						loader(State.CONF_LOADED,errCode);
					}
				}
						).execute(this);
			} 
			//CONFIGURATION LOADED
			else if(state==State.CONF_LOADED)
				loader(State.VALIDATE,ErrorCode.whatever);
			
			//HISTORICAL LOADED			
			else if(state==State.HISTORICAL_LOADED) {
				wfs = gs.getWorkflowLabels();
				//make sure workflows exist. if so, get appname & version.
				if (wfs!=null) {
					//appname and version saved in each workflow. pick first.
					Workflow first = gs.getWorkflowFromLabel(wfs[0]);
					first.getApplication();
					first.getApplicationVersion();
					loginConsole.addRow("Loaded App "+first.getApplication());
					loginConsole.addRow("App Bundle Version "+first.getApplicationVersion());
					loginConsole.addRow("Contains "+wfs.length+" workflows.");
					gs.getLogger().addRow("Initialization done");
					gs.getLogger().addRow("************************************************");

					//We know the workflows. We can create the menu.
					myState = State.POST_INIT;
					loginConsole.draw();

					//init current year.
					//TODO: REMOVE
					Variable v = gs.getVariableConfiguration().getVariableInstance(NamedVariables.CURRENT_YEAR);
					if (v.getValue()==null)
						v.setValue(Constants.getYear());

					//Global counter for auto_increment variables.
					v = gs.getVariableConfiguration().getVariableInstance(NamedVariables.CURRENT_SAMPLE_INDEX);
					if (v!=null&&v.getValue()==null)
						v.setValue("0");
					
					//TODO: REMOVE - TEST CODE
					v = gs.getVariableConfiguration().getVariableInstance("Temp_Ruta");
					if (v!=null&&v.getValue()==null)
						v.setValue("1");
					
					v = gs.getArtLista().getVariableInstance("Current_Ruta");
					if (v!=null&&v.getValue()==null)
						v.setValue("1");
					v = gs.getArtLista().getVariableInstance("Current_Provyta");
					if (v!=null&&v.getValue()==null)
						v.setValue("1");
					v = gs.getArtLista().getVariableInstance("Current_Delyta");
					if (v!=null&&v.getValue()==null)
						v.setValue("1");
					 
					//execute main workflow if it exists.
					Workflow wf = gs.getWorkflow("Main");
					if (wf == null) {
						String[] x = gs.getWorkflowNames();
						Log.d("vortex","workflow main not found. These are available:");
						for (String n:x) 
							Log.d("vortex",n);
					}
					if (wf!=null) {
						this.changePage(wf,null);
						Log.d("vortex","executing workflow main!");

					} else {
						loginConsole.addRow("");
						loginConsole.addRedText("Found no workflow 'Main'. Exiting..");
					}
				} else
					loginConsole.addRedText("Found no workflows");
				gs.sendEvent(MenuActivity.INITDONE);
			}

			break;
			
			//GIS_LOADED:
			

		case VALIDATE:
			//If a new version has been loaded and frozen, refresh global state.
			if (doRefresh) {
				loginConsole.addRow("Refreshing frozen objects with new configuration: ");
				loginConsole.addGreenText("[OK]");
				gs.refresh();
			}
			loginConsole.addRow("Validating frozen objects: ");
			GlobalState.ErrorCode ec = gs.validateFrozenObjects();
			switch (ec) {
			case spinners_not_found:
				loginConsole.addRedText("[Spinner definitions missing!]");
				break;
			case workflows_not_found:
				loginConsole.addRedText("[Workflow definitions missing!]");
				break;
			case config_not_found:
				loginConsole.addRedText("[types or variable definitions missing!]");
				break;
			case missing_required_column:
				loginConsole.addRedText("[A required column is missing from "+Constants.TypesFileName+"]");
				break;
			case ok:
				loginConsole.addGreenText("[OK]");	
				loginConsole.addRow("Historical loaded: ");
				loginConsole.draw();


				//skapa nytt safe objekt.
				if (this.isNetworkAvailable()) {
					final Dialog dialog = new Dialog(this);
					dialog.setContentView(R.layout.tag_load);			
					ProgressBar pb = (ProgressBar)dialog.findViewById(R.id.tag_progress_bar);
					TextView tv = (TextView)dialog.findViewById(R.id.tag_progress_text);
					Button backgroundB = (Button)dialog.findViewById(R.id.backgroundB);
					histT = (new HistoricalDataImport(pb,tv,loginConsole,new FileLoadedCb() {


						@Override
						public void onFileLoaded(ErrorCode errCode, String histVer) {		
							dialog.setOnDismissListener(null);
							dialog.dismiss();
							if (errCode == ErrorCode.Loaded) {
								//The safe needs to be recreated.								
								Log.d("nils","in onfileloaded for Historical. Returns version: "+histVer);
								gs.getPreferences().put(PersistenceHelper.CURRENT_VERSION_OF_HISTORY_FILE,histVer);						
								loginConsole.addText("["+histVer+"]");
								
							}
							if (errCode == ErrorCode.notFound) {
								loginConsole.addYellowText("[Importdata.json not found]");
								errCode = ErrorCode.Loaded;
							}
							//If user has loaded in background, discard this one.
							if (myState == State.VALIDATE)
								loader(State.HISTORICAL_LOADED,errCode);
						}
					})).execute(gs);
					dialog.setTitle("Laddar GammData");
					dialog.setCanceledOnTouchOutside(false);
					dialog.setOnDismissListener(new OnDismissListener() {					
						@Override
						public void onDismiss(DialogInterface dialog) {
							Log.d("nils","DIALOG DISMISS!!!!");						
							loader(State.HISTORICAL_LOADED,ErrorCode.LoadInBackground);						
						}
					});
					dialog.setCancelable(true);

					backgroundB.setOnClickListener(new OnClickListener() {				
						@Override
						public void onClick(View v) {						
							dialog.dismiss();
							loader(State.HISTORICAL_LOADED,ErrorCode.whatever);
						}
					});
					dialog.show();
				} else
					loader(State.HISTORICAL_LOADED,ErrorCode.whatever);

				
				if (!success) {
					Log.e("nils","scandelningsdata failed");
					ec = GlobalState.ErrorCode.tagdata_not_found;
					loginConsole.addRedText("[Tågdata corrupt or not found]");
				} else 
					loginConsole.addGreenText("[OK]");
				 
				break;

			default:
				loginConsole.addRedText("[Error in config: "+ec+"]");
				break;
			}
			//Get workflows
			if (ec != GlobalState.ErrorCode.ok) {
				loginConsole.addRow("");
				loginConsole.addRedText("Program cannot start because of previous errors. Please correct your configuration");					

			}
			break;	
		} 



		loginConsole.draw();


	}
	*/



	private boolean checkBundleName() {
		String appName = ph.get(PersistenceHelper.BUNDLE_NAME);
		if (appName == null || appName.length()==0)
			return false;
		if (appName.endsWith(".xml")) {
			appName = appName.substring(0, appName.length()-4);
			ph.put(PersistenceHelper.BUNDLE_NAME, appName);
		}
		return true;
	}




	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		Log.d("nils","Gets to onResume");


		super.onResume();

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
		GlobalState gs = GlobalState.getInstance(this);
		String label = wf.getLabel();
		String template = wf.getTemplate();
		//Set context.
		Log.d("noob","Context ["+wf.getContext()+"]");
		debugLogger.addRow("Context ["+wf.getContext()+"]");
		CHash r = gs.evaluateContext(wf.getContext());
		//if Ok err is null.
		if (r.err==null) {
			debugLogger.addRow("Context [");
			debugLogger.addGreenText("OK");
			debugLogger.addText("]");

			gs.setRawHash(r.rawHash);
			if (r.keyHash==null)
				Log.e("noob","Keyhas still null even though it is OK");
			gs.setKeyHash(r.keyHash);
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
			showErrorMsg(r.err, wf);
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
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
	}




	@Override
	public void onDestroy() {
		if (histT!=null) {
			Log.d("nils","Trying to cancel histT");
			histT.cancel(true);
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
		return debugLogger;
	}
	


}
