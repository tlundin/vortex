package com.teraim.vortex.loadermodule.configurations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import com.teraim.vortex.dynamic.types.ValuePair;
import com.teraim.vortex.loadermodule.JSONConfigurationModule;
import com.teraim.vortex.loadermodule.LoadResult;
import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.utils.DbHelper;
import com.teraim.vortex.utils.PersistenceHelper;

public class ImportDataConfiguration extends JSONConfigurationModule {

	private LoggerI o;
	private DbHelper myDb;
	private ArrayList<ValuePair> vars;
	private Map<String,String> meta,keys;

	public ImportDataConfiguration(PersistenceHelper globalPh,PersistenceHelper ph, String server, String bundle, LoggerI debugConsole,
			DbHelper myDb) {
		super(globalPh,ph, Source.internet, server+bundle.toLowerCase()+"/", "Importdata","Historical data module");	 
		this.o = debugConsole;
		this.myDb = myDb;
		isDatabaseModule=true;

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
			//jArray = jObject.getJSONArray("source");
			//Erase old history
			o.addRow("");
			o.addYellowText("HISTORIA RENSAS!!");
			if (myDb.deleteNilsHistory())
				myDb.fastPrep();
			else 
				return new LoadResult(this,ErrorCode.Aborted,"Database is not a NILS database. Missing column 'år'");
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
			Log.d("vortex","In parse ImportData");
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
			Log.d("vortex","reading variables");
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
				Log.d("vortex","name: "+varName);
			} else
				return new LoadResult(this,ErrorCode.ParseError);
			name = reader.nextName();
			if (name.equals("value")) {
				value = getAttribute(reader);
				Log.d("vortex","value: "+value);
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
			Log.d("vortex","found "+c+" variable values");
			if (reader.peek() == JsonToken.END_OBJECT) {
				//wow! found end of current content type!
				reader.endObject();
				Entry entry = new Entry(keys,vars);
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

		Log.d("vortex","Reading content type "+getAttribute(reader));
		keys = new HashMap<String,String>();
		while (reader.hasNext()) {
			String name = reader.nextName();
//			Log.d("vortex","Name: "+name);
			if (name.equals("ar")) {
				ar = getAttribute(reader);
			} else if (name.equals("ruta")) {
				keys.put("ruta",getAttribute(reader));
			} else if (name.equals("provyta")) {
				keys.put("provyta",getAttribute(reader));
			} else if (name.equals("delyta")) {
				keys.put("delyta",getAttribute(reader));
			} else if (name.equals("smaprovyta")) {
				keys.put("smaprovyta",getAttribute(reader));
			} else if (name.equals("linje")) {
				keys.put("linje",getAttribute(reader));
			}else if (name.equals("abo")) {
				keys.put("abo",getAttribute(reader));
			} else if (name.equals("Vars")) {
				//found variables array. Time to return and report progress.
				return null;
			} else {
				reader.skipValue();
			}
		}		

		return new LoadResult(this,ErrorCode.ParseError,"Error in keys object in importdata");
	}




	

	@Override
	public void setEssence() {
		essence=entries;
	}

	@Override
	public boolean freeze(int counter) throws IOException {
		if (entries == null)
			return false;

		//Insert variables into database
		Entry e = entries.get(counter);
		{
			for(ValuePair v:e.variables) {
			myDb.fastHistoricalInsert(e.keys.get("ruta"),
					e.keys.get("provyta"),
					e.keys.get("delyta"),
					e.keys.get("smaprovyta"),
				v.mkey,v.mval);
		}
		}
		return true;
	}



}





