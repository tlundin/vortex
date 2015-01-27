package com.teraim.vortex.ui;

import java.io.File;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.R;
import com.teraim.vortex.Start;
import com.teraim.vortex.loadermodule.Configuration;
import com.teraim.vortex.loadermodule.ModuleLoader;
import com.teraim.vortex.log.Logger;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.PersistenceHelper;




public class LoginConsoleFragment extends Fragment {

	TextView log;
	private TextView versionTxt;
	private TextView licenseTxt;
	private final String VORTEX_VERSION = "Vortex 0_9_9";
	private final String License = "This sw uses 3rd party components that are under Apache 2.0 license.";
	private LoggerI loginConsole,debugConsole;
	private PersistenceHelper globalPh;
	private boolean configurationLoaded=false;
	private ModuleLoader myLoader;

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
		versionTxt.setText(VORTEX_VERSION);
		licenseTxt.setText(License);

		//Create global state
		
		
		globalPh = new PersistenceHelper(this.getActivity().getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_PRIVATE));

		loginConsole = new Logger(this.getActivity(),"INITIAL");
		loginConsole.setOutputView(log);
		debugConsole = new Logger(this.getActivity(),"DEBUG");
		
		

		if (this.initIfFirstTime()) {
			if (!Start.singleton.isNetworkAvailable()) {
				showErrorMsg("You need a working network connection first time you start the program to load the configuration files.");
				//TODO EXIT HERE.
			} else {
				this.initialize();
				loginConsole.addRow("First time use...creating folders");
				loginConsole.addRow("");
				loginConsole.addYellowText("To change defaults, go to the config (wrench) menu");
			}
		}
		//write down version..quickly! :)
		globalPh.put(PersistenceHelper.CURRENT_VERSION_OF_PROGRAM, VORTEX_VERSION);

		//create module descriptors for all known configuration files.
		myLoader = new ModuleLoader(new Configuration(Constants.getCurrentlyKnownModules(globalPh,server(),globalPh.get(PersistenceHelper.BUNDLE_NAME),this.getActivity(),debugConsole)),
				loginConsole,globalPh);

		return view;
	}





	@Override
	public void onResume() {
		super.onResume();
		
		loginConsole.addRow("Loading Modules for "+globalPh.get(PersistenceHelper.BUNDLE_NAME));
		Log.d("vortex","Loading Modules for "+globalPh.get(PersistenceHelper.BUNDLE_NAME));
	
		myLoader.loadModulesIfRequired();
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










	private void run() {
		Log.d("vortex","Starting!");
		writeVortexLog();


	}





	private void writeVortexLog() {
		loginConsole.clear();
		loginConsole.addYellowText("["+VORTEX_VERSION+"]");
		loginConsole.addRow("");
		//loginConsole.addRedText("");
		if (globalPh.get(PersistenceHelper.LAG_ID_KEY).equals(PersistenceHelper.UNDEFINED)||
				globalPh.get(PersistenceHelper.USER_ID_KEY).equals(PersistenceHelper.UNDEFINED)) {
			loginConsole.addYellowText("LagID och/eller Namn fattas.");				
		}
		loginConsole.addRow("");
		loginConsole.addText("Changes:\n"					
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
	
}
