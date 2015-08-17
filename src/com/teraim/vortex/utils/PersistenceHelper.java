package com.teraim.vortex.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import com.teraim.vortex.non_generics.Constants;

public class PersistenceHelper {
	public static final String UNDEFINED = "";
	public static final String USER_ID_KEY = "user_id";
	public static final String LAG_ID_KEY = "lag_id";
	public static final String MITTPUNKT_KEY = "mittpunkt";
	public static final String DEVICE_COLOR_KEY = "device_type";
	public static final String CONFIG_LOCATION = "config_name";
	public static final String BUNDLE_NAME = "bundle_name";
	public static final String BACKUP_LOCATION = "backup_location";	
	public static final String SHOW_CONTEXT = "show_context";	
	public static final String SERVER_URL = "server_location";
	public static final String CURRENT_VERSION_OF_WF_BUNDLE = "current_version_wf";
	public static final String CURRENT_VERSION_OF_CONFIG_FILE = "current_version_config";
	public static final String CURRENT_VERSION_OF_PROGRAM = "prog_version";
	public static final String CURRENT_VERSION_OF_HISTORY_FILE = "current_version_hist";
	public static final String CURRENT_VERSION_OF_SPINNERS = "current_version_spinners";
	public static final String CURRENT_VERSION_OF_GIS_BLOCKS = "current_version_gis_blocks";
	public static final String CURRENT_VERSION_OF_GIS_OBJECT_BLOCKS = "current_version_gis_object_blocks";
	public static final String FIRST_TIME_KEY = "firzzt";
	public static final String GIS_CREATE_FIRST_TIME_KEY = "fzzzt_gis";
	public static final String DEVELOPER_SWITCH = "dev_switch";
	public static final String VERSION_CONTROL_SWITCH_OFF = "no_version_control";
	public static final String SYNC_FEATURE = "sync_feature";
	public static final String CURRENT_VERSION_OF_VARPATTERN_FILE = "current_version_varpattern";
	public static final String TIME_OF_LAST_SYNC = "kakkadua";
	public static final String HistoricalRutorList = "historical_rutor";
	public static final String AVSTAND_IS_PRESSED = "avstands_matning_pressed";
	public static final String NO_OF_PROVYTOR = "antalprovytor";
	public static final String NO_OF_RUTOR = "antalrutor";
	public static final String CHANGE_BUNDLE = "change_bundle";
	public static final String HIST_LOAD_COUNTER = "hist_counter";
	public static final String AVSTAND_WARNING_SHOWN = "Avstand_warning_was_shown";
	public static final String GLOBAL_AUTO_INC_COUNTER = "auto_increment_counter";

	


	SharedPreferences sp;

	ArrayList<String> delta = new ArrayList<String>();

	public PersistenceHelper(SharedPreferences sp) {
		this.sp = sp;
	}


	public String get(String key) {
		return sp.getString(key,UNDEFINED);
	}

	public void put(String key, String value) {
		sp.edit().putString(key,value).commit();
	}
	public void put(String key, boolean value) {
		sp.edit().putBoolean(key,value).commit();
	}
	public void put(String key, int value) {
		sp.edit().putInt(key,value).commit();
	}
	public void put(String key, float value) {
		sp.edit().putFloat(key,value).commit();
	}

	public boolean getB(String key) {
		return sp.getBoolean(key, false);
	}

	public int getI(String key) {
		return sp.getInt(key, -1);
	}
	
	public float getF(String key) {
		return sp.getFloat(key, -1);
	}

	/* Checks if external storage is available for read and write */
	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	public File getBackupStorageDir() {
		// Get the directory for the user's public pictures directory.
		String path = this.get(BACKUP_LOCATION);
		if (path==null||path.length()==0) {
			//use default if not set.
			path = Constants.DEFAULT_EXT_BACKUP_DIR;
		}

		File backupFolder = new File(path);
		if (!backupFolder.exists()) {
			System.out.println("creating directory: " + Constants.DEFAULT_EXT_BACKUP_DIR);
			boolean result = false;

			try{
				backupFolder.mkdir();
				result = true;
			} catch(SecurityException se){
				//handle it
			}        
			if(result) {    
				System.out.println("DIR created");  
				return backupFolder;
			}
		}


		return null;
	}
	
	public boolean backup(String exportFileName, String data) {
		
		String state = Environment.getExternalStorageState();
		Log.d("vortex","ext state: "+state);
		Log.d("backup stordir: ", Environment.getExternalStorageDirectory().toString());
		if (exportFileName==null) {
			Log.e("vortex","no filename!!");
			return false;
		}
		if (data==null) {
			Log.e("vortex","no data sent to backup!");
			return false;
		}
		//File sdCard = Environment.getExternalStorageDirectory();
		//File dir = new File (sdCard.getAbsolutePath() + "/vortex");
		String backupFolder = get(BACKUP_LOCATION);
		if (backupFolder==null || backupFolder.isEmpty()) {
			Log.e("vortex","no backup folder configured...reverting to default");
			backupFolder = Constants.DEFAULT_EXT_BACKUP_DIR;
		}
		File dir = new File (backupFolder);
		
		dir.mkdirs();
		File file = new File(dir, exportFileName);
		BufferedWriter outWriter=null;
		try { 
			FileOutputStream f = new FileOutputStream(file);
			outWriter = new BufferedWriter(new OutputStreamWriter(f),1024);
            // write the whole string
            outWriter.write(data);
            outWriter.close();
	    }catch(Exception e){
	        System.out.println("Could not write backup file! Filename: "+exportFileName);
	        e.printStackTrace();
	        try {
				if (outWriter!=null)
					outWriter.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	        return false;
	    }
		Log.d("vortex","file succesfully written to backup: "+exportFileName);
		
		return true;
		
		
	}


	
		
		
	


}
