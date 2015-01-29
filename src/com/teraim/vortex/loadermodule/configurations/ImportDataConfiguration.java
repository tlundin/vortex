package com.teraim.vortex.loadermodule.configurations;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.loadermodule.JSONConfigurationModule;
import com.teraim.vortex.loadermodule.LoadResult;
import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.utils.DbHelper;
import com.teraim.vortex.utils.PersistenceHelper;

public class ImportDataConfiguration extends JSONConfigurationModule {
	
	private LoggerI o;
	private DbHelper myDb;

	public ImportDataConfiguration(PersistenceHelper globalPh,PersistenceHelper ph, String server, String bundle, LoggerI debugConsole,
			DbHelper myDb) {
		super(globalPh,ph, Source.internet, server+bundle.toLowerCase()+"/", "Importdata","Historical data module");	 
		this.o = debugConsole;
		this.myDb = myDb;
		
	}

	@Override
	public String getFrozenVersion() {
		return (ph.get(PersistenceHelper.CURRENT_VERSION_OF_HISTORY_FILE));
	}

	@Override
	protected void setFrozenVersion(String version) {
		ph.put(PersistenceHelper.CURRENT_VERSION_OF_HISTORY_FILE,version);
		
	}

	@Override
	public boolean isRequired() {
		return false;
	}
	JSONArray jArray;
	
	@Override
	protected LoadResult prepare(String json) throws IOException, JSONException {
		JSONObject jObject = new JSONObject(json);
		jArray = jObject.getJSONArray("source");
		//Erase old history
		o.addRow("");
		o.addYellowText("HISTORIA RENSAS!!");
		if (myDb.deleteNilsHistory())
			myDb.fastPrep();
		else 
			return new LoadResult(this,ErrorCode.Aborted,"Database is not a NILS database. Missing column 'år'");
		return null;
	}
	
	
	

	@Override
	public LoadResult parse(String row, Integer currentRow) throws IOException, JSONException {
		Log.d("nils","getzzzz");
			JSONObject keyObj = jArray.getJSONObject(currentRow);
			// Pulling items from the array
			JSONArray varArray = keyObj.getJSONArray("Vars");
			JSONObject varObj;
			String varId,value;
			Log.d("vortex","Vararray has "+varArray.length()+" members");
			for (int a=0; a < varArray.length(); a++) {
				varObj = varArray.getJSONObject(a);
				varId = varObj.getString("name");
				value = varObj.getString("value");
				myDb.fastHistoricalInsert(keyObj.getString("ruta"),
						keyObj.getString("provyta"),
						keyObj.getString("delyta"),
						keyObj.getString("smaprovyta"),
						varId,value);										
			}
			return null;
		}
 
	
	
	@Override
	public void setEssence() {
		essence=null;
	}

	
		
		
	}

	

	

