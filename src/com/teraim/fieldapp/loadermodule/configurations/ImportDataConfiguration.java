package com.teraim.fieldapp.loadermodule.configurations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.Table;
import com.teraim.fieldapp.dynamic.types.ValuePair;
import com.teraim.fieldapp.loadermodule.JSONConfigurationModule;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.PersistenceHelper;

public class ImportDataConfiguration extends JSONConfigurationModule {

	private LoggerI o;
	private DbHelper myDb;
	private ArrayList<ValuePair> vars;
	private Map<String,String> meta,keyz;
	private Table varTable;

	public ImportDataConfiguration(PersistenceHelper globalPh,PersistenceHelper ph, String server, String bundle, LoggerI debugConsole,
			DbHelper myDb, Table t) {
		super(globalPh,ph, Source.internet, server+bundle.toLowerCase()+"/", "Importdata","Historical data module");	 
		this.o = debugConsole;
		this.myDb = myDb;
		isDatabaseModule=true;
		varTable = t;

	}

	@Override
	public float getFrozenVersion() {
		return (ph.getF(PersistenceHelper.CURRENT_VERSION_OF_HISTORY_FILE));
	}

	@Override
	protected void setFrozenVersion(float version) {
		ph.put(PersistenceHelper.CURRENT_VERSION_OF_HISTORY_FILE,version);

	}

	@Override
	public boolean isRequired() {
		return false;
	}


	@Override
	protected LoadResult prepare(JsonReader reader) throws IOException, JSONException {

		try {
			reader.beginObject();
			meta = new HashMap<String,String>();
			while (reader.hasNext()) {
				String name = reader.nextName();
				if (name.equals("date")) {
					meta.put("date",getAttribute(reader));
				} else if (name.equals("time")) {
					meta.put("time",getAttribute(reader));
				} else if (name.equals("version")) {
					meta.put("version",getAttribute(reader));
				} else if (name.equals("source")) {
					//start of array. Return ok.
					reader.beginArray();
					break;
				}
				else {
					reader.skipValue();
				}
			}
			state = State.readingKeys;
			Log.d("vortex","found date time version "+meta.get("date")+","+meta.get("time")+","+meta.get("version"));
			o.addRow("Import file date time version: ["+meta.get("date")+"],["+meta.get("time")+"],["+meta.get("version")+"]");
			//jArray = jObject.getJSONArray("source");
			//Erase old history
			o.addRow("");
			o.addYellowText("Deleting existing historical data..");
			if (!myDb.deleteHistory())
				return new LoadResult(this,ErrorCode.Aborted,"Database is missing column '�r', cannot continue");
			return null;
		} catch (IllegalStateException e) {
			return new LoadResult(this,ErrorCode.ParseError,"Could not read header in ImportDataConfig.json");
		}
	}


	private enum State {
		readingKeys,
		readingVariables,
	}
	State state = null;

	@Override
	public LoadResult parse(JsonReader reader) throws IOException, JSONException {
		LoadResult lr=null;
		if (state == State.readingKeys) {
			//Log.d("vortex","In parse ImportData");
			reader.beginObject(); 
			if (reader.nextName().equals("contentType"))
				lr = readKeys(reader);
			if (lr!=null)
				return lr;
			//Variables should follow now.
			reader.beginArray();
			state = State.readingVariables;
			return null;
		} else {
			//Log.d("vortex","reading variables");
			lr = readVariables(reader);
			if (lr!=null)
				return lr;
			else {
				state = State.readingKeys;
				return null;
			}

		}


	}
	private class Entry {

		Map<String,String> keys;
		List<ValuePair> variables;
		public Entry(Map<String,String> keys, List<ValuePair> variables) {
			super();
			this.keys = keys;
			this.variables = variables;
		}

	}
	private List<Entry> entries = new ArrayList<Entry>();

	private LoadResult readVariables(JsonReader reader) throws IOException {
		String varName = null, value = null;
		vars = new ArrayList<ValuePair>();
		int c=0;
		while (reader.hasNext()) {
			reader.beginObject();
			String name = reader.nextName();
			if (name.equals("name")) {
				varName = getAttribute(reader);
				//Log.d("vortex","name: "+varName);
			} else
				return new LoadResult(this,ErrorCode.ParseError);
			name = reader.nextName();
			if (name.equals("value")) {
				value = getAttribute(reader);
				//Log.d("vortex","value: "+value);
				vars.add(new ValuePair(varName,value));

			}
			else
				return new LoadResult(this,ErrorCode.ParseError);
			c++;
			reader.endObject();
		}
		if (reader.peek() == JsonToken.END_ARRAY) {
			//found end of array...consume and return
			reader.endArray();

			if (reader.peek() == JsonToken.END_OBJECT) {
				//wow! found end of current content type!
				reader.endObject();
				Entry entry = new Entry(keyz,vars);
				entries.add(entry);
				//double powwow! End of the whole import!!
				if (reader.peek() == JsonToken.END_ARRAY) {
					reader.close();
					this.setEssence();
					//set number of steps for counter
					freezeSteps = entries.size();

					return new LoadResult(this,ErrorCode.parsed);
				} else
					return null;
			} else {
				Log.e("vortex","Found a place where contenttype object is not ended");
				return new LoadResult(this,ErrorCode.ParseError);
			}


		}
		Log.e("vortex","unexpected end of import file");
		return new LoadResult(this,ErrorCode.ParseError);

	}

	String ar,ruta,provyta,delyta,smaprovyta,linje,abo;


	private LoadResult readKeys(JsonReader reader) throws IOException {

		//Log.d("vortex","Reading content type "+);
		getAttribute(reader);
		keyz = new HashMap<String,String>();
		while (reader.hasNext()) {
			String name = reader.nextName();
			//			Log.d("vortex","Name: "+name);
			if (name.equals("ar")) {
				ar = getAttribute(reader);
			} else if (name.equals("ruta")) {
				keyz.put("ruta",getAttribute(reader));
			} else if (name.equals("provyta")) {
				keyz.put("provyta",getAttribute(reader));
			} else if (name.equals("delyta")) {
				keyz.put("delyta",getAttribute(reader));
			} else if (name.equals("smaprovyta")) {
				keyz.put("smaprovyta",getAttribute(reader));
			} else if (name.equals("linje")) {
				keyz.put("linje",getAttribute(reader));
			}else if (name.equals("abo")) {
				keyz.put("abo",getAttribute(reader));
			} else if (name.equals("Vars")) {
				//found variables array. Time to return and report progress.
				for (String k:keyz.keySet())
					allKeys.add(k);
				return null;
			} else {
				skipped.add(name);
				reader.skipValue();
			}
		}		

		return new LoadResult(this,ErrorCode.ParseError,"Error in keys object in importdata");
	}

	Set<String> skipped = new HashSet<String>();
	Set<String> allKeys = new HashSet<String>();
	Set<String> missingVariables = new HashSet<String>();


	@Override
	public void setEssence() {
		//No essence...all goes into db.
		essence = null;
		Log.e("vortex","SKIPPED KEYS:\n"+skipped.toString());
		Log.e("vortex","Keys Found:\n"+allKeys.toString());
		o.addRow("");
		if (skipped.isEmpty())
			o.addGreenText("No unknown keys..");
		else
			o.addRow("Unknown keys: "+skipped.toString());
		o.addRow("");
		o.addGreenText("Keys Found:\n"+allKeys.toString());


	}
	boolean firstCall = true;

	@Override
	public boolean freeze(int counter) throws IOException {
		if (entries == null)
			return false;

		if (firstCall) {
			myDb.beginTransaction();
			Log.d("vortex","Transaction begins");
			firstCall = false;
		}
		if (this.freezeSteps==(counter+1)) {
			Log.d("vortex","Transaction ends");
			myDb.endTransactionSuccess();
			if (missingVariables !=null && !missingVariables.isEmpty()) {
				o.addRow("");
				o.addRedText("Variables not found:");
				for (String var:missingVariables) {
					o.addRow("");
					o.addRedText(var);				
				}
				Log.e("vortex","Variables not found:\n"+missingVariables.toString());
			}
		}
		//Insert variables into database
		Entry e = entries.get(counter);
		{

			for(ValuePair v:e.variables) {
				if(varTable.getRowFromKey(v.mkey)==null)
					missingVariables.add(v.mkey);
				//Still insert even if variable is missing.
				boolean success = myDb.fastHistoricalInsert(e.keys,
						v.mkey,v.mval);
				if (!success) {
					o.addRow("");
					o.addRedText("Row: "+counter+". Insert failed. Variable: "+v.mkey);
				}

			}
		}

		return true;
	}



}





