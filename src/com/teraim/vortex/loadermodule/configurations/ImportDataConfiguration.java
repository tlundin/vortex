package com.teraim.vortex.loadermodule.configurations;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import com.teraim.vortex.loadermodule.JSONConfigurationModule;
import com.teraim.vortex.loadermodule.LoadResult;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.utils.DbHelper;
import com.teraim.vortex.utils.PersistenceHelper;

public class ImportDataConfiguration extends JSONConfigurationModule {
	
	private LoggerI o;
	private DbHelper myDb;

	public ImportDataConfiguration(PersistenceHelper globalPh, String server, String bundle, LoggerI debugConsole,
			Context ctx) {
		super(globalPh, Source.internet, server+bundle.toLowerCase()+"/", "Importdata","Historical data module");	 
		this.o = debugConsole;
		myDb = new DbHelper(ctx,bundle);
	}

	@Override
	public String getFrozenVersion() {
		return (globalPh.get(PersistenceHelper.CURRENT_VERSION_OF_HISTORY_FILE));
	}

	@Override
	protected void setFrozenVersion(String version) {
		globalPh.put(PersistenceHelper.CURRENT_VERSION_OF_HISTORY_FILE,version);
		
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
		myDb.deleteHistory();
		myDb.fastPrep();
		
		return null;
	}
	
	
	
	@Override
	public LoadResult parse(String row, Integer currentRow) throws IOException, JSONException {

			JSONObject keyObj = jArray.getJSONObject(currentRow);
			// Pulling items from the array
			JSONArray varArray = keyObj.getJSONArray("Vars");
			JSONObject varObj;
			String varId,value;
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
	public Object getEssence() {
		return null;
	}

	
		
		
	}

	

	

