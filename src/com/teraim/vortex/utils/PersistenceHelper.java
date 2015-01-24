package com.teraim.vortex.utils;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import com.teraim.vortex.non_generics.Constants;

public class PersistenceHelper {
	public static final String UNDEFINED = "";
	//	public static final String CURRENT_RUTA_ID_KEY = "ruta_id";
	//	public static final String CURRENT_PROVYTA_ID_KEY = "provyta_id";
	//	public static final String CURRENT_DELYTA_ID_KEY = "delyta_id";
	//	public static final String CURRENT_YEAR_ID_KEY = "current_year_id";
	public static final String USER_ID_KEY = "user_id";
	public static final String LAG_ID_KEY = "lag_id";
	public static final String MITTPUNKT_KEY = "mittpunkt";
	public static final String DEVICE_COLOR_KEY = "device_type";
	public static final String CONFIG_LOCATION = "config_name";
	public static final String BUNDLE_NAME = "bundle_name";
	public static final String SERVER_URL = "server_location";
	public static final String CURRENT_VERSION_OF_WF_BUNDLE = "current_version_wf";
	public static final String CURRENT_VERSION_OF_CONFIG_FILE = "current_version_config";
	public static final String CURRENT_VERSION_OF_PROGRAM = "prog_version";
	public static final String CURRENT_VERSION_OF_HISTORY_FILE = "current_version_hist";
	public static final String CURRENT_VERSION_OF_SPINNERS = "current_version_spinners";
	public static final String FIRST_TIME_KEY = "firzzt";
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

	public boolean getB(String key) {
		return sp.getBoolean(key, false);
	}

	public int getI(String key) {
		return sp.getInt(key, -1);
	}

	/* Checks if external storage is available for read and write */
	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	public File getDbBackupStorageDir(String albumName) {
		// Get the directory for the user's public pictures directory.
		String path = "/mnt/external_sdcard/"+Constants.EXT_BACKUP_DIR;

		File backupFolder = new File(path);
		if (!backupFolder.exists()) {
			System.out.println("creating directory: " + Constants.EXT_BACKUP_DIR);
			boolean result = false;

			try{
				backupFolder.mkdir();
				result = true;
			} catch(SecurityException se){
				//handle it
			}        
			if(result) {    
				System.out.println("DIR created");  
			}
		}


		return null;
	}
	public boolean backup(Context ctx) {

		Log.d("vortex","DB path: "+ctx.getDatabasePath(DbHelper.DATABASE_NAME));
		try {
			File sd = Environment.getExternalStorageDirectory();
			File data = Environment.getDataDirectory();
			Log.d("vortex","data directory is "+data.getAbsolutePath());
			/*
        if (sd.canWrite()) {
            String currentDBPath = "//data//{package name}//databases//{database name}";
            String backupDBPath = "{database name}";
            File currentDB = new File(data, currentDBPath);
            File backupDB = new File(sd, backupDBPath);

            if (currentDB.exists()) {
                FileChannel src = new FileInputStream(currentDB).getChannel();
                FileChannel dst = new FileOutputStream(backupDB).getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();
            }
        }
			 */
		} catch (Exception e) {
			Log.e("vortex","Error backing up database\n");e.printStackTrace();
			return false;
		}
		return true;
	}


}
