package com.teraim.nils.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.util.JsonWriter;
import android.util.Log;

import com.teraim.nils.GlobalState;
import com.teraim.nils.dynamic.VariableConfiguration;
import com.teraim.nils.utils.DbHelper.DBColumnPicker;
import com.teraim.nils.utils.DbHelper.StoredVariableData;

public class JSONExporter {

	GlobalState gs;
	DbHelper db;
	JsonWriter writer;
	PersistenceHelper ph;
	StringWriter sw;
	private VariableConfiguration al;

	public class Report {
		public String result;
		public int noOfVars = 0;
	}

	private static JSONExporter instance = null;

	public static JSONExporter getInstance(Context ctx) {
		if (instance == null) {
			instance = new JSONExporter(ctx);
		}
		return instance;
	}
	private JSONExporter(Context ctx) {
		this.gs=GlobalState.getInstance(ctx);
		al = gs.getArtLista();
		ph = gs.getPersistence();
		
	}



	public Report writeVariables(DBColumnPicker cp) {

		sw = new StringWriter();
		writer = new JsonWriter(sw);	

		try {
			if (cp.moveToFirst()) {
				writer.setIndent("  ");
				writer.beginObject();
				writeHeader();								
				Map<String,String> currentKeys = cp.getKeyColumnValues();
				Log.d("nils","Current keys: "+currentKeys.toString());
				writer.name("Elements");
				writer.beginArray();
				boolean more = true;
				do {
					Log.d("nils","Gets here once?");
					//writer.name("NewKey");
					writer.beginObject();
					writeSubHeader(currentKeys);
					writer.name("Variables");
					writer.beginArray();					
					while (true) {
						writeVariable(cp.getVariable());
						more = cp.next();
						if (!more)
							break;
						Map<String,String> newKeys = cp.getKeyColumnValues();
						if (!sameKeys(currentKeys,newKeys)) {
							currentKeys = newKeys;
							break;

						}
					}
					//Close variables array
					writer.endArray();
					//close NewKey.
					writer.endObject();


				} while (more);				
				//Close Elements
				writer.endArray();
				//Close header
				writer.endObject();
				writer.close();
				Log.d("nils","finished writing JSON");
				Log.d("nils", sw.toString());
				Report r = new Report();
				r.result=sw.toString();
				return r;
				
				


			} else {
				Log.e("nils","cursor empty in writeVariables.");
				writer.close();			
				return null;
			}





		} catch (IOException e) {
			e.printStackTrace();
			cp.close();
		} finally {
			cp.close();
		}

		return null;
	}





	private boolean sameKeys(Map<String, String> m1,
			Map<String, String> m2) {
		if (m1.size() != m2.size())
			return false;
		for (String key: m1.keySet()) {
			Log.d("nils","Key:"+key+" m1: "+(m1==null?"null":m1.toString())+" m2: "+(m2==null?"null":m2.toString()));
			if (m1.get(key)==null&&m2.get(key)==null)
				continue;
			if ((m1.get(key)==null || m2.get(key)==null)||!m1.get(key).equals(m2.get(key)))
				return false;
		}
		Log.d("nils","keys equal..no header");
		return true;
	}

	
	private void write(String name,String value) throws IOException {

		String val = (value==null||value.length()==0)?"NULL":value;
		writer.name(name).value(val);
	}

	private void writeVariable(StoredVariableData variable) throws IOException {
		//Type found from extended data
		String type;
		List<String> row = al.getCompleteVariableDefinition(variable.name);
		boolean isExported = true;
		if (row==null)
			type ="";
		else {
			type = al.getnumType(row).name();
			isExported = !al.isLocal(row);
		}
		if (isExported) {
		writer.beginObject();
		write("name",variable.name);
		write("value",variable.value);
		write("type",type);
		write("lag",variable.lagId);
		write("author",variable.creator);
		write("timestamp",variable.timeStamp);
		writer.endObject();
		} else 
			Log.d("nils","Didn't export "+variable.name);
		
			
	}
		

	private void writeSubHeader(Map<String,String> currentKeys) throws IOException {
		//subheader.
		Set<String> keys = currentKeys.keySet();
		for (String key:keys) 
			write(key,currentKeys.get(key));	
	}

	public void writeHeader() throws IOException {
		Date now = new Date();
		//File header.
		Log.d("nils","Exporting database");
		write("date",DateFormat.getInstance().format(now));
		write("time",DateFormat.getTimeInstance().format(now));
		write("programversion",ph.get(PersistenceHelper.CURRENT_VERSION_OF_PROGRAM));
		write("workflow bundle version",ph.get(PersistenceHelper.CURRENT_VERSION_OF_WF_BUNDLE));
		write("Artlista version",ph.get(PersistenceHelper.CURRENT_VERSION_OF_CONFIG_FILE));
		write("Variable Definition version",ph.get(PersistenceHelper.CURRENT_VERSION_OF_VARPATTERN_FILE));

		Log.d("nils",writer.toString());
		
	}


}
