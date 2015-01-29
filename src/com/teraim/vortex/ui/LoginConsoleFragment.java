package com.teraim.vortex.ui;

import java.io.File;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.R;
import com.teraim.vortex.Start;
import com.teraim.vortex.dynamic.types.SpinnerDefinition;
import com.teraim.vortex.dynamic.types.Table;
import com.teraim.vortex.dynamic.types.Workflow;
import com.teraim.vortex.loadermodule.Configuration;
import com.teraim.vortex.loadermodule.ConfigurationModule;
import com.teraim.vortex.loadermodule.ModuleLoader;
import com.teraim.vortex.loadermodule.ModuleLoader.ModuleLoaderListener;
import com.teraim.vortex.loadermodule.configurations.SpinnerConfiguration;
import com.teraim.vortex.loadermodule.configurations.VariablesConfiguration;
import com.teraim.vortex.log.Logger;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.DbHelper;
import com.teraim.vortex.utils.PersistenceHelper;




public class LoginConsoleFragment extends Fragment implements ModuleLoaderListener {

	TextView log;
	private TextView versionTxt;
	private TextView licenseTxt;
	private final String License = "This sw uses 3rd party components that are under Apache 2.0 license.";
	private LoggerI loginConsole,debugConsole;
	private PersistenceHelper globalPh,ph;
	private ModuleLoader myLoader;
	private String bundleName;
	private Configuration myModules;
	private DbHelper myDb;
	private boolean running=false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_login_console,
				container, false);

		log = (TextView)view.findViewById(R.id.logger);
		versionTxt = (TextView)view.findViewById(R.id.versionTxt);
		licenseTxt = (TextView)view.findViewById(R.id.licenseTxt);
		Typeface type=Typeface.createFromAsset(getActivity().getAssets(),
				"clacon.ttf");
		log.setTypeface(type);
		log.setMovementMethod(new ScrollingMovementMethod());
		versionTxt.setText("Vortex, ver. "+Constants.VORTEX_VERSION);
		licenseTxt.setText(License);

		//Create global state


		globalPh = new PersistenceHelper(this.getActivity().getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_PRIVATE));

		bundleName = globalPh.get(PersistenceHelper.BUNDLE_NAME);
		if (bundleName == null || bundleName.length()==0)
			bundleName = "Vortex";


		ph	 = new PersistenceHelper(this.getActivity().getSharedPreferences(globalPh.get(PersistenceHelper.BUNDLE_NAME), Context.MODE_PRIVATE));

		loginConsole = new Logger(this.getActivity(),"INITIAL");
		loginConsole.setOutputView(log);
		debugConsole = Start.singleton.getLogger();

		//Send a signal that init starts


		//First time vortex runs? Then create global folders.
		if (this.initIfFirstTime()) {
			if (!Start.singleton.isNetworkAvailable()) {
				showErrorMsg("You need a working network connection first time you start the program to load the configuration files.");
				//TODO EXIT HERE.
			} else {
				this.initialize();
				debugConsole.addRow("First time use...creating folders");
				debugConsole.addRow("");
				debugConsole.addYellowText("To change defaults, go to the config (wrench) menu");
			}
		}
		//First time this application runs? Then create config folder.
		if (!new File(Constants.VORTEX_ROOT_DIR+bundleName).isDirectory()) {
			Log.d("vortex","First time execution!");
			debugConsole.addPurpleText("First time execution of App "+bundleName);

			File folder = new File(Constants.VORTEX_ROOT_DIR+bundleName);
			if(!folder.mkdirs())
				Log.e("NILS","Failed to create App root folder");
			folder = new File(Constants.VORTEX_ROOT_DIR+bundleName+"/config");
			if(!folder.mkdirs())
				Log.e("NILS","Failed to create config root folder");

		} else 
			Log.d("vortex","This application has been executed before.");



		//write down version..quickly! :)
		globalPh.put(PersistenceHelper.CURRENT_VERSION_OF_PROGRAM, Constants.VORTEX_VERSION);

		//create module descriptors for all known configuration files.
		myModules = new Configuration(Constants.getCurrentlyKnownModules(globalPh,ph,server(),bundleName,debugConsole));
		myLoader = new ModuleLoader("moduleLoader",myModules,loginConsole,globalPh,debugConsole,this);


		running = false;
		return view;
	}





	@Override
	public void onResume() {
		super.onResume();
		if (!globalPh.get(PersistenceHelper.BUNDLE_NAME).equals(bundleName)) {
			Fragment frg = null;
			frg = getFragmentManager().findFragmentById(R.id.content_frame);
			final FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.detach(frg);
			ft.attach(frg);
			ft.commit();
		} else {

			Intent intent = new Intent();
			intent.setAction("INITSTARTS");
			this.getActivity().sendBroadcast(intent);
			loginConsole.addRow("Loading Modules for ");
			loginConsole.addYellowText(bundleName);
			Log.d("vortex","Loading Modules for "+bundleName);
			myLoader.loadModules();
		}
	}





	@Override
	public void onStart() {
		/*
			if (Start.singleton.isNetworkAvailable()) {

				loginConsole.addRow("Loading configuration. Patience is a virtue.");
				loginConsole.addRow("Server URL: "+ph.get(PersistenceHelper.SERVER_URL));
				load();
				;
			} else {
				loginConsole.addRow("No network. Will use existing configuration. Please standby.");
				validate();
			}
		 */

		super.onStart();
	}





	private void writeVortexLog() {

		debugConsole.addYellowText("["+Constants.VORTEX_VERSION+"]");
		debugConsole.addRow("");
		//debugConsole.addRedText("");
		if (globalPh.get(PersistenceHelper.LAG_ID_KEY).equals(PersistenceHelper.UNDEFINED)||
				globalPh.get(PersistenceHelper.USER_ID_KEY).equals(PersistenceHelper.UNDEFINED)) {
			debugConsole.addYellowText("LagID och/eller Namn fattas.");				
		}
		debugConsole.addRow("");
		debugConsole.addText("Changes:\n"					
				+ "* Dynamic menus.\n"
				+ "* Colored log.\n"
				+ "* Colored menus.\n"
				+ "* New time functions: getyear(), gethour(), getsecond() etc"
				+ "* Improved startup cycle.\n"
				+ "* Sync feature can be hidden from menu\n"
				+ "* Wfs with no PagedefineBlock has no UI\n"
				+ "* new generic Envelope for bluetooth messaging\n"
				+ "* Stricter handling of context for workflows\n"
				+ "* Automatic restart of App when bundle is changed\n"
				+ "* First GIS prototype\n"
				+ "* Chart Engine proto\n"
				+ "* Backup support\n"
				+ "* TEXT concatenation for integers and texts\n"
				+ "* Safe object removed (Nils specific)\n"
				+ "* Export to CSV or JSON based on context\n"
				+ "* Export Button block\n"
				+ "* Separate Persistence for different Bundles\n"
				+ "* New more Efficient Rule Executor with Tokenizer\n"
				);
	}

	/******************************
	 * First time? If so, create subfolders.
	 */
	private boolean initIfFirstTime() {
		//If testFile doesnt exist it will be created and found next time.
		Log.d("Strand","Checking if this is first time use...");
		boolean first = (globalPh.get(PersistenceHelper.FIRST_TIME_KEY).equals(PersistenceHelper.UNDEFINED));


		if (first) {
			Log.d("vortex","Yes..executing  first time init");
			return true;
		}
		else {
			Log.d("vortex","..Not first time");
			return false;
		}

	}

	private void initialize() {

		//create data folder. This will also create the ROOT folder for the Strand app.
		File folder = new File(Constants.PIC_ROOT_DIR);
		if(!folder.mkdirs())
			Log.e("NILS","Failed to create pic root folder");
		folder = new File(Constants.OLD_PIC_ROOT_DIR);
		if(!folder.mkdirs())
			Log.e("NILS","Failed to create old pic root folder");
		folder = new File(Constants.EXPORT_FILES_DIR);
		if(!folder.mkdirs())
			Log.e("NILS","Failed to create export folder");


		//Set defaults if none.
		if (globalPh.get(PersistenceHelper.SERVER_URL).equals(PersistenceHelper.UNDEFINED))
			globalPh.put(PersistenceHelper.SERVER_URL, "www.teraim.com");
		if (globalPh.get(PersistenceHelper.BUNDLE_NAME).equals(PersistenceHelper.UNDEFINED))
			globalPh.put(PersistenceHelper.BUNDLE_NAME, "Vortex");
		if (globalPh.get(PersistenceHelper.DEVELOPER_SWITCH).equals(PersistenceHelper.UNDEFINED))
			globalPh.put(PersistenceHelper.DEVELOPER_SWITCH, false);
		if (globalPh.get(PersistenceHelper.VERSION_CONTROL_SWITCH_OFF).equals(PersistenceHelper.UNDEFINED))
			globalPh.put(PersistenceHelper.VERSION_CONTROL_SWITCH_OFF, false);

		globalPh.put(PersistenceHelper.FIRST_TIME_KEY,"Initialized");

	}


	private void showErrorMsg(String error) {
		new AlertDialog.Builder(this.getActivity())
		.setTitle("Error message")
		.setMessage(error) 
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

	public String server() {
		String serverUrl = globalPh.get(PersistenceHelper.SERVER_URL);
		if (!serverUrl.endsWith("/"))
			serverUrl+="/";
		if (!serverUrl.startsWith("http://")) 
			serverUrl = "http://"+serverUrl;
		return serverUrl;
	}





	@Override
	public void loadSuccess(String loaderId) {
		Log.d("vortex","Arrives to loadsucc with ID: "+loaderId);
		//If load successful, create database and import data into it. 
		if (loaderId.equals("moduleLoader")) {
			Log.d("vortex","getzzz: "+loaderId);
			loginConsole.addRow("");
			//			loginConsole.addGreenText("All modules loaded...preparing database");
			loginConsole.draw();
			//Create or update database from Table object.
			ConfigurationModule m = myModules.getModule(VariablesConfiguration.NAME);

			if (m!=null) {
				Log.d("vortex","name "+m.getFileName());
				Table t = (Table)m.getEssence();
				myDb = new DbHelper(this.getActivity().getApplicationContext(),t, globalPh,ph,bundleName);
				//Import historical data to database. 
				ModuleLoader myDBLoader = new ModuleLoader("dbloader",new Configuration(Constants.getDBImportModule(globalPh,ph,server(),bundleName,debugConsole,myDb)
						),loginConsole,globalPh,debugConsole,this);
				myDBLoader.loadModules();

			} 
		} else {
			//Program is ready to run.
			//Create the global state from all module objects. 
			//Context applicationContext, PersistenceHelper globalPh,
			//PersistenceHelper ph, LoggerI debugConsole, DbHelper myDb,
			//Map<String, Workflow> workflows,Table t,SpinnerDefinition sd

			List<Workflow> workflows = (List<Workflow>)(myModules.getModule(bundleName).getEssence());
			Table t = (Table)(myModules.getModule(VariablesConfiguration.NAME).getEssence());
			SpinnerDefinition sd = (SpinnerDefinition)(myModules.getModule(SpinnerConfiguration.NAME).getEssence());

			GlobalState gs = new GlobalState(this.getActivity().getApplicationContext(),
					globalPh,ph,debugConsole,myDb, workflows, t,sd);

			//drawermenu
			gs.setDrawerMenu(Start.singleton.getDrawerMenu());

			//Change to main.
			//execute main workflow if it exists.
			Workflow wf = gs.getWorkflow("Main");
			if (wf == null) {
				String[] x = gs.getWorkflowNames();
				debugConsole.addRow("");
				debugConsole.addRedText("workflow main not found. These are available:");
				for (String n:x) 
					debugConsole.addRow(n);
			}
			if (wf!=null) {
				Start.singleton.getDrawerMenu().closeDrawer();
				Start.singleton.getDrawerMenu().clear();
				gs.sendEvent(MenuActivity.INITDONE);
				running = true;
				Start.singleton.changePage(wf,null);
				Log.d("vortex","executing workflow main!");
				gs.setModules(myModules);

			} else {
				loginConsole.addRow("");
				loginConsole.addRedText("Found no workflow 'Main'. Exiting..");
			}
		}





	}








}
