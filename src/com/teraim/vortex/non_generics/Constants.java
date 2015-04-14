package com.teraim.vortex.non_generics;

/**
 * @author Terje 
 * 
 * This is the Common Vars class giving access to global state stored in the Persisted memory.
 * For now, persistence implemented via SharedPreferences only.
 */

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import android.os.Environment;

import com.teraim.vortex.loadermodule.ConfigurationModule;
import com.teraim.vortex.loadermodule.configurations.GisPolygonConfiguration;
import com.teraim.vortex.loadermodule.configurations.GroupsConfiguration;
import com.teraim.vortex.loadermodule.configurations.ImportDataConfiguration;
import com.teraim.vortex.loadermodule.configurations.SpinnerConfiguration;
import com.teraim.vortex.loadermodule.configurations.VariablesConfiguration;
import com.teraim.vortex.loadermodule.configurations.WorkFlowBundleConfiguration;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.utils.DbHelper;
import com.teraim.vortex.utils.PersistenceHelper;

public class Constants {

	public final static float VORTEX_VERSION = 1.069f;

	
	//String constants
	//The root folder for the SD card is in the global Environment.
	private final static String path = Environment.getExternalStorageDirectory().getPath();
		//Remember to always add system root path before any app specific path!

	//Root for NILS
	
	public final static String HISTORICAL_TOKEN = "*HISTORICAL*";
	public final static String VORTEX_ROOT_DIR = path+"/vortex/";
	public static final String EXPORT_FILES_DIR = VORTEX_ROOT_DIR + "export/";
	public static final String PIC_ROOT_DIR = VORTEX_ROOT_DIR + "pics/";
	public static final String OLD_PIC_ROOT_DIR = VORTEX_ROOT_DIR + "old_pics/";
	
	//Folders for backup on SD card.
	public static final String EXT_BACKUP_DIR = VORTEX_ROOT_DIR + "Backup";
	
	//public static String NILS_BASE_DIR = "/nils";
	public static String UNDEFINED = "undefined";

	public static String Color_Pressed="#4682B4";


	public static String[] defaultGroupHeader=new String[] {"Label,Description,Group Name,Variable Name,Internet link,P_Familj,P_Class"};


	public static final String SYNC_ID = "SYNX";

	//NILS uid
	public static final UUID RED_UID = UUID.fromString("58500d27-6fd9-47c9-bf6b-d0969ce78bb3");
	public static final UUID BLUE_UID = UUID.fromString("ce8ec829-30e3-469b-886e-6cf8f1168e98");
		
	
	//Static methods
	public static String compassToPicName(int compass) {
		return (compass==0?"vast":(compass==1?"norr":(compass==2?"syd":(compass==3?"ost":null))));
	}


	public static final String TRUE = "true";
	public static final String FALSE= "false";

	//Static constants


	public static final int KEY_LENGTH = 10;
	public static final String SLU_URL = "https://arbetsplats.slu.se/sites/srh/Landskapsanalys/Faltportal/default.aspx";
	public static final String STATUS_INITIAL = "0";
	public static final String STATUS_STARTAD_MEN_INTE_KLAR = "1";
	public static final String STATUS_STARTAD_MED_FEL = "2";
	public static final String STATUS_AVSLUTAD_OK = "3";
	public static final String STATUS_DISABLED = "4";
	public static final int MAX_NILS_LINJER = 12;
	public static final String NORR = "NORR";
	public static final String SYD = "SYD";
	public static final String OST = "OST";
	public static final String VAST = "VAST";

	public static final String NULL_VALUE = "NULL";

	public static final String NO_DEFAULT_VALUE = "*NULL*";



	
	public static UUID getmyUUID() {
		/*
		String myC = getDeviceColor();
		if (myC.equals(nocolor()))
			return null;
		else if (myC.equals(red()))
			return RED_UID;
		else
		*/
			return BLUE_UID;
	}


//TODO: REMOVE
	public static final int MAX_NILS = 16;

	public static final int MIN_ABO = 50;

	public static final int MAX_ABO = 99;
	
	//ruta size in meters.
	public static final float RUTA_SIZE = 3000;

	public static final String CONFIG_FROZEN_FILE_ID = null;

	public static final String TypesFileName = null;

	public static boolean isAbo(int pyID) {
		return pyID>=Constants.MIN_ABO && pyID<=Constants.MAX_ABO;
	}

 	//Static Time providers.


	public static String getTimeStamp() {
		 return getYear()+getMonth()+getDayOfMonth()+"_"+getHour()+"_"+getMinute();
	}
	
	public static String getYear() {
		 return Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
	}

	public static String getWeekNumber() {
		 return Integer.toString(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
	}

	public static String getMonth() {
		 return Integer.toString(Calendar.getInstance().get(Calendar.MONTH));
	}

	public static String getDayOfMonth() {
		 return Integer.toString(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
	}
		
	public static String getHour() {
		 return Integer.toString(Calendar.getInstance().get(Calendar.HOUR));
	}
	
	public static String getMinute() {
		 return Integer.toString(Calendar.getInstance().get(Calendar.MINUTE));
	}
	public static String getSecond() {
		 return Integer.toString(Calendar.getInstance().get(Calendar.SECOND));
	}
	
	public static final String GroupFileName = "Groups.csv";
	public static final String VariablesFileName = "Variables.csv";
	public static final String SpinnersFileName = "Spinners.csv";
	public static final String PY_HISTORICAL_FILE_NAME = "Importdata.json";

	public static final String WF_FROZEN_SPINNER_ID = null;

	public static final String WF_FROZEN_FILE_ID = null;

	public static final String GLOBAL_PREFS = "GlobalPrefs";

	public final static int VAR_PATTERN_ROW_LENGTH = 11;


	public static final String VariableSeparator = ":";


	public static List<ConfigurationModule> getCurrentlyKnownModules(PersistenceHelper globalPh,PersistenceHelper ph,String server, String bundle, LoggerI debugConsole) {
		List<ConfigurationModule> ret = new ArrayList<ConfigurationModule>();
		//Workflow xml. Named same as bundle.
		
		ret.add(new SpinnerConfiguration(globalPh,ph,server,bundle,debugConsole));
		ret.add(new WorkFlowBundleConfiguration(globalPh,ph,server,bundle,debugConsole));
		ret.add(new GroupsConfiguration(globalPh,ph,server,bundle,debugConsole));		
		//VariableConfiguration depends on the Groups Configuration.
		ret.add(new VariablesConfiguration(globalPh,ph,server,bundle,debugConsole));
		
		return ret;
	}

	public static List<ConfigurationModule> getDBImportModules(
			PersistenceHelper globalPh, PersistenceHelper ph, String server,
			String bundle, LoggerI debugConsole,DbHelper db) {
		List<ConfigurationModule> ret = new ArrayList<ConfigurationModule>();
		//Workflow xml. Named same as bundle.		
		ret.add(new GisPolygonConfiguration(globalPh,ph,Constants.VORTEX_ROOT_DIR+bundle+"/flygdata/",debugConsole,db));
		ret.add(new ImportDataConfiguration(globalPh,ph,server,bundle,debugConsole,db));
		return ret;
	}

	
	//Historical picture year is currently five years minus current year.
	public static int getHistoricalPictureYear() {
		return Calendar.getInstance().get(Calendar.YEAR)-5;
	}
	





	

	


}
