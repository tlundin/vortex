package com.teraim.vortex.non_generics;

/**
 * @author Terje 
 * 
 * This is the Common Vars class giving access to global state stored in the Persisted memory.
 * For now, persistence implemented via SharedPreferences only.
 */

import java.util.UUID;

import android.os.Environment;

public class Constants {

	
	
	//String constants
	//The root folder for the SD card is in the global Environment.
	private final static String path = Environment.getExternalStorageDirectory().getPath();
		//Remember to always add system root path before any app specific path!

	//Root for NILS
	
	public final static String HISTORICAL_TOKEN = "*HISTORICAL*";
	public final static String VORTEX_ROOT_DIR = path+"/vortex/";
	public final static String CONFIG_FILES_DIR = VORTEX_ROOT_DIR + "config/";
	public static final String EXPORT_FILES_DIR = VORTEX_ROOT_DIR + "export/";
	public static final String PIC_ROOT_DIR = VORTEX_ROOT_DIR + "pics/";
	public static final String OLD_PIC_ROOT_DIR = VORTEX_ROOT_DIR + "old_pics/";
	
	//public static String NILS_BASE_DIR = "/nils";
	public static String UNDEFINED = "undefined";

	public static String Color_Pressed="#4682B4";


	public static final String SYNC_ID = "SYNX";

	public static final String CurrentYear = "2014";
	
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

	public static final String WF_FROZEN_FILE_ID = "workflows";
	public static final String CONFIG_FROZEN_FILE_ID = "artlista";

	public static final int KEY_LENGTH = 10;

	public static final String WF_FROZEN_SPINNER_ID = "spinnerdefinitions";

	public static final String SLU_URL = "https://arbetsplats.slu.se/sites/srh/Landskapsanalys/Faltportal/default.aspx";


	
	public static final String STATUS_INITIAL = "0";
	public static final String STATUS_STARTAD_MEN_INTE_KLAR = "1";
	public static final String STATUS_STARTAD_MED_FEL = "2";
	public static final String STATUS_AVSLUTAD_OK = "3";
	public static final String STATUS_DISABLED = "4";
	public static final int MAX_NILS_LINJER = 12;
	public static final String NORR = "SYD";
	public static final String SYD = "NORR";
	public static final String OST = "VAST";
	public static final String VAST = "OST";

	public static final String NULL_VALUE = "NULL";

	public static final String NO_DEFAULT_VALUE = "*NULL*";

	public static final String TypesFileName = "Types.csv";

	public static final String VariablesFileName = "Variables.csv";
	
	public static final String SpinnersFileName = "Spinners.csv";

	public static final String PY_HISTORICAL_FILE_NAME = "Importdata.json";


	
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

	public static boolean isAbo(int pyID) {
		return pyID>=Constants.MIN_ABO && pyID<=Constants.MAX_ABO;
	}

 	//Persisted variables.




	
	
	

	





	

	


}
