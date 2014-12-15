package com.teraim.nils.non_generics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.teraim.nils.FileLoadedCb;
import com.teraim.nils.FileLoadedCb.ErrorCode;
import com.teraim.nils.GlobalState;
import com.teraim.nils.R;
import com.teraim.nils.dynamic.VariableConfiguration;
import com.teraim.nils.dynamic.templates.LinjePortalTemplate;
import com.teraim.nils.dynamic.templates.ProvytaTemplate;
import com.teraim.nils.dynamic.templates.SimpleRutaTemplate;
import com.teraim.nils.dynamic.types.Variable;
import com.teraim.nils.dynamic.types.Workflow;
import com.teraim.nils.log.Logger;
import com.teraim.nils.log.LoggerI;
import com.teraim.nils.ui.BackgroundFragment;
import com.teraim.nils.ui.DrawerMenu;
import com.teraim.nils.ui.DrawerMenuAdapter;
import com.teraim.nils.ui.DrawerMenuAdapter.RowType;
import com.teraim.nils.ui.DrawerMenuHeader;
import com.teraim.nils.ui.DrawerMenuItem;
import com.teraim.nils.ui.DrawerMenuSelectable;
import com.teraim.nils.ui.LoginConsoleFragment;
import com.teraim.nils.ui.MenuActivity;
import com.teraim.nils.utils.ConfigFileParser;
import com.teraim.nils.utils.HistoricalDataImport;
import com.teraim.nils.utils.LoadResult;
import com.teraim.nils.utils.PersistenceHelper;
import com.teraim.nils.utils.Tools;
import com.teraim.nils.utils.WorkflowParser;

public class Start extends MenuActivity {

	private final String VORTEX_VERSION = "Vortex_0_8";

	private GlobalState gs;
	private PersistenceHelper ph;
	//	private Map<String,List<String>> menuStructure;
	private SparseArray<String>mapItemsToName;
	//	private ArrayList<String> rutItems;
	//	private ArrayList<String> wfItems;
	private enum State {INITIAL, HISTORICAL_LOADED,WF_LOADED, CONF_LOADED, VALIDATE,POST_INIT};
	private LoginConsoleFragment loginFragment;
	private LoggerI loginConsole;
	private State myState=null;
	private String[] wfs;
	private AsyncTask<GlobalState, Integer, LoadResult> histT=null;
	public static Start singleton;
	private DrawerMenu mDrawerMenu;

	private ActionBarDrawerToggle mDrawerToggle;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("nils","in Menu Activity onCreate");
		singleton = this;
		//This is the frame for all pages, defining the Action bar and Navigation menu.
		setContentView(R.layout.naviframe);


		loginConsole = new Logger(this,"INITIAL");
		//loginConsole = new DummyLogger();
		//create subfolders. Copy assets..
		if (this.initIfFirstTime()) {		
			loginConsole.addRow("First time use...creating folders");
			loginConsole.addRow("");
			loginConsole.addYellowText("To change defaults, go to the config (wrench) menu");

		}
		//This combats an issue on the target panasonic platform having to do with http reading.
		System.setProperty("http.keepAlive", "false"); 

		//GlobalState
		if (gs==null)
			gs = GlobalState.getInstance(getApplicationContext());
		//gs.createLogger();
		ph = gs.getPersistence();

		//drawermenu
		mDrawerMenu = gs.getDrawerMenu();
		mDrawerToggle = mDrawerMenu.getDrawerToggle();

		//write down version..quickly! :)
		ph.put(PersistenceHelper.CURRENT_VERSION_OF_PROGRAM, VORTEX_VERSION);



		//Maps itemheaders to items.
		//		menuStructure = new HashMap<String,List<String>>();
		//		rutItems = new ArrayList<String>();
		//		wfItems = new ArrayList<String>();
		//Maps item numbers to Fragments.
		mapItemsToName = new SparseArray<String>();

		//		menuStructure.put("Ruta och Provyta",rutItems);
		//		menuStructure.put("Delyta",wfItems);



		
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		loginFragment = new LoginConsoleFragment();
		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager.beginTransaction()
		.replace(R.id.content_frame, loginFragment)
		.commit();
		myState=null;
		//Executon continues in onStart() since we have to wait for fragment to load.

	}




	@Override
	protected void onStart() {
		super.onStart();
		this.invalidateOptionsMenu();

		if (myState ==null) {
			loginFragment.getVersionField().setText(VORTEX_VERSION	);
			loginConsole.setOutputView(loginFragment.getTextWindow());
			loginConsole.clear();
			loginConsole.addYellowText("["+VORTEX_VERSION+"]");
			loginConsole.addRow("");
			//loginConsole.addRedText("");
			if (ph.get(PersistenceHelper.LAG_ID_KEY).equals(PersistenceHelper.UNDEFINED)||
					ph.get(PersistenceHelper.USER_ID_KEY).equals(PersistenceHelper.UNDEFINED)) {
				loginConsole.addRedText("LagID eller Namn fattas. Lägg in det!");				
			}
			loginConsole.addRow("");
			loginConsole.addText("Ändringar:\n"					
					+ "* Vortex engine. \n"
					+ "* Dynamic menus.\n"
					);

			if (this.isNetworkAvailable()) {

				loginConsole.addRow("Loading configuration. Patience is a virtue.");
				loginConsole.addRow("Server URL: "+ph.get(PersistenceHelper.SERVER_URL));

				loader(State.INITIAL,null);
			} else {
				loginConsole.addRow("No network. Will use existing configuration. Please standby.");
				loader(State.VALIDATE,null);
			}

		}
	}

	private boolean doRefresh=false;

	private void loader(State state,ErrorCode errCode) {
		myState=state;
		loginConsole.draw();
		switch (state) {

		case INITIAL:

			loginConsole.addRow("Reading configuration files:\n"+ph.get(PersistenceHelper.BUNDLE_LOCATION)+": ");
			new WorkflowParser(ph, new FileLoadedCb() {

				@Override
				public void onFileLoaded(ErrorCode errCode,String ver) {	
					if (ver!=null)
						loginConsole.addText("["+ver+"]");
					loader(State.WF_LOADED,errCode);
				}

			}).execute(this);
			break;

		case HISTORICAL_LOADED:
		case WF_LOADED:
		case CONF_LOADED:
			Log.d("nils","STATE: "+state);
			switch(errCode) {
			case ioError:
				loginConsole.addRedText("[IO-ERROR]");
				loginConsole.addRow("Couldn't load "+(state==State.WF_LOADED?"workflow":"artlista and/or varpattern") +" config.\nWrong server url? :"+ph.get(PersistenceHelper.SERVER_URL)+
						"\nWrong filename? : "+(state==State.WF_LOADED?ph.get(PersistenceHelper.BUNDLE_LOCATION):("Artlista: "+ph.get(PersistenceHelper.CONFIG_LOCATION)
								+" Varpattern: "+ph.get("varpattern.csv"))));
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
			case HistoricalLoaded:
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
			if (state==State.WF_LOADED) {
				loginConsole.addRow(ph.get(PersistenceHelper.CONFIG_LOCATION)+": ");			
				new ConfigFileParser(ph, new FileLoadedCb() {
					@Override
					public void onFileLoaded(ErrorCode errCode,String ver) {
						Log.d("nils","Got "+errCode+" after configfileloaded");
						if (ver!=null)
							loginConsole.addText("["+ver+"]");
						loader(State.CONF_LOADED,errCode);
					}
				}
						).execute(this);
			} else if(state==State.CONF_LOADED)
				loader(State.VALIDATE,ErrorCode.whatever);
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
					//if a main workflow exist, fill the drawermenu.

					loginConsole.draw();

					//init current year.
					//TODO: REMOVE
					Variable v = gs.getArtLista().getVariableInstance(NamedVariables.CURRENT_YEAR);
					if (v.getValue()==null)
						v.setValue(Constants.CurrentYear);

					//Global counter for auto_increment variables.
					v = gs.getArtLista().getVariableInstance(NamedVariables.CURRENT_SAMPLE_INDEX);
					if (v.getValue()==null)
						v.setValue("0");

					v = gs.getArtLista().getVariableInstance("Current_Ruta");
					if (v.getValue()==null)
						v.setValue("1");
					/*
				v = gs.getArtLista().getVariableInstance("Current_Provyta");
				if (v.getValue()==null)
					v.setValue("2");
				v = gs.getArtLista().getVariableInstance("Current_Delyta");
				if (v.getValue()==null)
					v.setValue("100");
				v = gs.getArtLista().getVariableInstance("Current_Linje");
				if (v.getValue()==null)
					v.setValue("1");
				v = gs.getArtLista().getVariableInstance("Current_Meter");
				if (v.getValue()==null)
					v.setValue("1");

					 */
					//xx
					//createDrawerMenu(wfs);		
				} else
					loginConsole.addRedText("Found no workflows");
				gs.sendEvent(MenuActivity.INITDONE);
			}

			break;

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
			case file_not_found:
				loginConsole.addRedText("[Frozen object missing]");
				break;
			case workflows_not_found:
				loginConsole.addRedText("[Could not find any workflows]");
				break;
			case missing_required_column:
				loginConsole.addRedText("[A required column is missing from "+ph.get(PersistenceHelper.CONFIG_LOCATION)+"]");
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
							if (errCode == ErrorCode.HistoricalLoaded) {
								//The safe needs to be recreated.
								gs.createSafe();
								Log.d("nils","in onfileloaded for Historical. Returns version: "+histVer);
								gs.getSafe().setHistoricalFileVersion(histVer);						
							}
							loginConsole.addText("["+histVer+"]");
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
						}
					});
					dialog.show();
				} else
					loader(State.HISTORICAL_LOADED,ErrorCode.whatever);

				/*
				if (!success) {
					Log.e("nils","scandelningsdata failed");
					ec = GlobalState.ErrorCode.tagdata_not_found;
					loginConsole.addRedText("[Tågdata corrupt or not found]");
				} else 
					loginConsole.addGreenText("[OK]");
				 */
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


	public void changePage(Fragment newPage, String title) {
		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager.beginTransaction()
		.replace(R.id.content_frame, newPage)
		.addToBackStack(null)
		.commit();
		setTitle(title);
		//close menu if exists..
		mDrawerMenu.closeDrawer();
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

	/******************************
	 * First time? If so, create subfolders.
	 */
	private boolean initIfFirstTime() {
		//If testFile doesnt exist it will be created and found next time.
		PersistenceHelper ph = new PersistenceHelper(getSharedPreferences("nilsPrefs", Context.MODE_PRIVATE));
		Log.d("Strand","Checking if this is first time use...");
		boolean first = (ph.get(PersistenceHelper.FIRST_TIME_KEY).equals(PersistenceHelper.UNDEFINED));


		if (first) {
			ph.put(PersistenceHelper.FIRST_TIME_KEY,"NotEmpty");
			Log.d("Strand","Yes..executing  first time init");
			initialize(ph);   
			return true;
		}
		else {
			Log.d("Strand","..Not first time");
			return false;
		}

	}

	private void initialize(PersistenceHelper ph) {

		//create data folder. This will also create the ROOT folder for the Strand app.
		File folder = new File(Constants.CONFIG_FILES_DIR);
		if(!folder.mkdirs())
			Log.e("NILS","Failed to create config root folder");
		folder = new File(Constants.PIC_ROOT_DIR);
		if(!folder.mkdirs())
			Log.e("NILS","Failed to create pic root folder");
		folder = new File(Constants.OLD_PIC_ROOT_DIR);
		if(!folder.mkdirs())
			Log.e("NILS","Failed to create old pic root folder");
		folder = new File(Constants.EXPORT_FILES_DIR);
		if(!folder.mkdirs())
			Log.e("NILS","Failed to create export folder");


		ph.put(PersistenceHelper.CURRENT_VERSION_OF_CONFIG_FILE, PersistenceHelper.UNDEFINED);
		ph.put(PersistenceHelper.CURRENT_VERSION_OF_WF_BUNDLE, PersistenceHelper.UNDEFINED);
		//Set defaults if none.
		if (ph.get(PersistenceHelper.SERVER_URL).equals(PersistenceHelper.UNDEFINED))
			ph.put(PersistenceHelper.SERVER_URL, "www.teraim.com");
		if (ph.get(PersistenceHelper.BUNDLE_LOCATION).equals(PersistenceHelper.UNDEFINED))
			ph.put(PersistenceHelper.BUNDLE_LOCATION, "nilsbundle5.xml");
		if (ph.get(PersistenceHelper.CONFIG_LOCATION).equals(PersistenceHelper.UNDEFINED))
			ph.put(PersistenceHelper.CONFIG_LOCATION, "configv2.csv");
		if (ph.get(PersistenceHelper.DEVELOPER_SWITCH).equals(PersistenceHelper.UNDEFINED))
			ph.put(PersistenceHelper.DEVELOPER_SWITCH, false);
		if (ph.get(PersistenceHelper.VERSION_CONTROL_SWITCH_OFF).equals(PersistenceHelper.UNDEFINED))
			ph.put(PersistenceHelper.VERSION_CONTROL_SWITCH_OFF, false);

		//copy the configuration files into the root dir.
		//copyAssets();


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
		Log.d("nils","Saving Safe");
		Tools.witeObjectToFile(getApplication(), gs.getSafe(), Constants.CONFIG_FILES_DIR+"mysafe");
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


}
