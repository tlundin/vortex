package com.teraim.fieldapp.ui;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import android.accounts.Account;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.types.SpinnerDefinition;
import com.teraim.fieldapp.dynamic.types.Table;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.loadermodule.Configuration;
import com.teraim.fieldapp.loadermodule.ConfigurationModule;
import com.teraim.fieldapp.loadermodule.ModuleLoader;
import com.teraim.fieldapp.loadermodule.ModuleLoader.ModuleLoaderListener;
import com.teraim.fieldapp.loadermodule.configurations.SpinnerConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.VariablesConfiguration;
import com.teraim.fieldapp.log.Logger;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.PersistenceHelper;


public class LoginConsoleFragment extends Fragment implements ModuleLoaderListener {

	TextView log;
	private final String License = "This sw uses 3rd party components that are under Apache 2.0 license.";
	private LoggerI loginConsole,debugConsole;
	private PersistenceHelper globalPh,ph;
	private ModuleLoader myLoader=null,myDBLoader=null;
	private String bundleName;
	private Configuration myModules;
	private DbHelper myDb;
	private TextView appTxt;
	private float oldV = -1;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_login_console,
				container, false);
		TextView versionTxt,licenseTxt;

		log = (TextView)view.findViewById(R.id.logger);
		versionTxt = (TextView)view.findViewById(R.id.versionTxt);
		licenseTxt = (TextView)view.findViewById(R.id.licenseTxt);
		ImageView logo = (ImageView)view.findViewById(R.id.logo);
		appTxt = (TextView)view.findViewById(R.id.appTxt);
		Typeface type=Typeface.createFromAsset(getActivity().getAssets(),
				"clacon.ttf");
		log.setTypeface(type);
		log.setMovementMethod(new ScrollingMovementMethod());
		versionTxt.setText("Vortex engine ver. "+Constants.VORTEX_VERSION);
		licenseTxt.setText(License);
		//Create global state


		globalPh = new PersistenceHelper(this.getActivity().getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_MULTI_PROCESS));
		
		bundleName = globalPh.get(PersistenceHelper.BUNDLE_NAME);
		if (bundleName == null || bundleName.length()==0)
			bundleName = "Vortex";
		appTxt.setText("Running application "+bundleName);
		ph	 = new PersistenceHelper(this.getActivity().getSharedPreferences(globalPh.get(PersistenceHelper.BUNDLE_NAME), Context.MODE_MULTI_PROCESS));
		oldV= ph.getF(PersistenceHelper.CURRENT_VERSION_OF_APP);
		//appTxt.setText("Running application "+bundleName+" ["+oldV+"]");

		Log.d("imgul",  server()+bundleName+"logo.png");
		new DownloadImageTask((ImageView) view.findViewById(R.id.logo))
		.execute(server()+bundleName.toLowerCase()+"/logo.png");



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
		}else 
			Log.d("vortex","This application has been executed before.");

		//TODO: Move this code into above in next release.
		File folder = new File(Constants.VORTEX_ROOT_DIR+bundleName);
		if(!folder.mkdirs())
			Log.e("NILS","Failed to create App root folder");
		folder = new File(Constants.VORTEX_ROOT_DIR+bundleName+"/config");
		if(!folder.mkdirs())
			Log.e("NILS","Failed to create config folder");
		folder = new File(Constants.VORTEX_ROOT_DIR+bundleName+"/cache");
		if(!folder.mkdirs())
			Log.e("NILS","Failed to create cache folder");



		//write down version..quickly! :)
		globalPh.put(PersistenceHelper.CURRENT_VERSION_OF_PROGRAM, Constants.VORTEX_VERSION);

		//create module descriptors for all known configuration files.
		Log.d("vortex","Creating Configuration and ModuleLoader");
		myModules = new Configuration(Constants.getCurrentlyKnownModules(globalPh,ph,server(),bundleName,debugConsole));
		String loaderId = "moduleLoader";
		boolean allFrozen = ph.getB(PersistenceHelper.ALL_MODULES_FROZEN+loaderId);
		myLoader = new ModuleLoader(loaderId,myModules,loginConsole,globalPh,allFrozen,debugConsole,this,this.getActivity());

		if (Constants.FreeVersion && expired())
			showErrorMsg("The license has expired. The App still works, but you will not be able to export any data.");;

			return view;
	}





	private boolean expired() {
		long takenIntoUseTime = globalPh.getL(PersistenceHelper.TIME_OF_FIRST_USE);
		long currentTime = System.currentTimeMillis();
		long diff = currentTime - takenIntoUseTime;
		return (diff > Constants.MS_MONTH);

	}





	@Override
	public void onResume() {
		super.onResume();
		if (GlobalState.getInstance() == null) {
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
				loginConsole.addRow("Loading In Memory Modules");
				loginConsole.addRow("Version control: "+globalPh.get(PersistenceHelper.VERSION_CONTROL));
				Log.d("vortex","Loading In Memory Modules");
				myLoader.loadModules(true);
				loginConsole.draw();
				Log.d("vortex","loginConsole object "+loginConsole);
			}
		}
	}



	@Override
	public void onStop() {
		if (myLoader!=null)
			myLoader.stop();
		if (myDBLoader!=null)
			myDBLoader.stop();
		Log.e("vortex","stop called on loaders!");
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
		Log.d("vortex","Checking if this is first time use of Vortex...");
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
		if (globalPh.get(PersistenceHelper.VERSION_CONTROL).equals(PersistenceHelper.UNDEFINED))
			globalPh.put(PersistenceHelper.VERSION_CONTROL, "Major");
		if (globalPh.get(PersistenceHelper.SYNC_METHOD).equals(PersistenceHelper.UNDEFINED))
			globalPh.put(PersistenceHelper.SYNC_METHOD, "Bluetooth");
		

		folder = new File(Constants.VORTEX_ROOT_DIR+globalPh.get(PersistenceHelper.BUNDLE_NAME)+"/"+Constants.CACHE_ROOT_DIR);
		if(!folder.mkdirs())
			Log.e("NILS","Failed to create gis image folder");		

		globalPh.put(PersistenceHelper.FIRST_TIME_KEY,"Initialized");
		long millis = System.currentTimeMillis();
		//date = Constants.getTimeStamp();
		globalPh.put(PersistenceHelper.TIME_OF_FIRST_USE,millis);
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
	public void loadSuccess(String loaderId, final boolean majorVersionChange) {
		Log.d("vortex","Arrives to loadsucc with ID: "+loaderId);
		ph.put(PersistenceHelper.ALL_MODULES_FROZEN+loaderId,true);
		if (this.getActivity()==null) {
			Log.e("vortex","No activity!");
		}
		//If load successful, create database and import data into it. 
		if (loaderId.equals("moduleLoader")) {
			loginConsole.addRow("");
			//			loginConsole.addGreenText("All modules loaded...preparing database");
			loginConsole.draw();

			//Create or update database from Table object.
			ConfigurationModule m = myModules.getModule(VariablesConfiguration.NAME);

			if (m!=null) {
				Log.d("vortex","name "+m.getFileName());
				Table t = (Table)m.getEssence();
				myDb = new DbHelper(this.getActivity().getApplicationContext(),t, globalPh,ph,bundleName);
				//Load configuration files asynchronously.
				Constants.getDBImportModules(globalPh, ph, server(), bundleName, debugConsole, myDb,t, new AsyncLoadDoneCb() {
					public void onLoadSuccesful(List<ConfigurationModule> modules) {
						Configuration dbModules = new Configuration(modules);
						if (modules!=null) {
							String loaderId = "dbLoader";
							boolean allFrozen = ph.getB(PersistenceHelper.ALL_MODULES_FROZEN+loaderId);
							myDBLoader = new ModuleLoader(loaderId,dbModules,loginConsole,globalPh,allFrozen,debugConsole,LoginConsoleFragment.this,LoginConsoleFragment.this.getActivity());
							loginConsole.addRow("Defaults & GIS modules");			
							myDBLoader.loadModules(majorVersionChange);
						} else 
							Log.e("vortex","null returned from getDBImportModules");
					}
				});
				//Configuration dbModules = new Configuration(Constants.getDBImportModules(globalPh, ph, server(), bundleName, debugConsole, myDb,t));
				//Import historical data to database. 


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
			if (this.getActivity()!=null) {
				GlobalState gs = 
						GlobalState.createInstance(this.getActivity().getApplicationContext(),globalPh,ph,debugConsole,myDb, workflows, t,sd);
				Start.alive=true;
				//Update app version if new
				//if (majorVersionChange) {
				float loadedAppVersion = ph.getF(PersistenceHelper.NEW_APP_VERSION);
				Log.d("vortex","updating App version to "+loadedAppVersion);
				ph.put(PersistenceHelper.CURRENT_VERSION_OF_APP,loadedAppVersion);
				//				}
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
					float newV = ph.getF(PersistenceHelper.CURRENT_VERSION_OF_APP);
					if (newV==-1)
						appTxt.setText("Running application "+bundleName+" [no version]"); 
					else {
						if (newV>oldV)
							appTxt.setText("Running application "+bundleName+" --New Version! ["+newV+"]");
						else
							appTxt.setText("Running application "+bundleName+" ["+newV+"]");
					}
					Start.singleton.changePage(wf,null);
					Log.d("vortex","executing workflow main!");
					gs.setModules(myModules);

					GlobalState.getInstance().onStart();
				} else {
					loginConsole.addRow("");
					loginConsole.addRedText("Found no workflow 'Main'. Exiting..");
				}
			} else {
				Log.e("vortex","No activity.");
			}
		}





	}



	private static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
		ImageView bmImage;

		public DownloadImageTask(ImageView bmImage) {
			this.bmImage = bmImage;
		}

		protected Bitmap doInBackground(String... urls) {
			String urldisplay = urls[0];
			Bitmap mIcon11 = null;
			try {
				InputStream in = new java.net.URL(urldisplay).openStream();
				mIcon11 = BitmapFactory.decodeStream(in);
			} catch (Exception e) {

			}
			return mIcon11;
		}

		protected void onPostExecute(Bitmap result) {
			if (result!=null)
				bmImage.setImageBitmap(result);
		}
	}



	@Override
	public void loadFail(String loaderId) {
		Log.d("vortex","sending broadcast");
		getActivity().sendBroadcast(new Intent(MenuActivity.INITFAILED));
	}




}
