package com.teraim.vortex.loadermodule.configurations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONException;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.dynamic.types.Location;
import com.teraim.vortex.dynamic.types.SweLocation;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisConstants;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisObject;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisPolygonObject;
import com.teraim.vortex.loadermodule.JSONConfigurationModule;
import com.teraim.vortex.loadermodule.LoadResult;
import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.utils.DbHelper;
import com.teraim.vortex.utils.PersistenceHelper;

public class GisObjectConfiguration extends JSONConfigurationModule {

	private LoggerI o;
	private DbHelper myDb;
	private static GisObjectConfiguration singleton;
	private List<GisObject> myGisObjects = new ArrayList<GisObject>();
	private String myType;

	public GisObjectConfiguration(PersistenceHelper globalPh,PersistenceHelper ph,String fileLocation, String fileName,LoggerI debugConsole,DbHelper myDb) {
		super(globalPh,ph, Source.file,fileLocation, fileName, fixedLength(fileName));
		this.o = debugConsole;
		this.myDb = myDb;
		//isDatabaseModule=true;
		this.singleton=this;
		this.hasSimpleVersion=true;
		this.isDatabaseModule=true;
		this.myType = fileName;
	}

	private static String fixedLength(String fileName) {
		if ((22-fileName.length())<=0)
			return fileName;
		String space22 = new String(new char[20-fileName.length()]).replace('\0', ' ');

		return ("[" + fileName + "]"+space22);
	}

	@Override
	public String getFrozenVersion() {
		return (ph.get(PersistenceHelper.CURRENT_VERSION_OF_GIS_OBJECT_BLOCKS+fileName));

	}

	@Override
	protected void setFrozenVersion(String version) {
		ph.put(PersistenceHelper.CURRENT_VERSION_OF_GIS_OBJECT_BLOCKS+fileName,version);

	}

	@Override
	public boolean isRequired() {
		return false;
	}


	@Override
	protected LoadResult prepare(JsonReader reader) throws IOException, JSONException {
		//first should be an array.
		if (!myDb.deleteHistoryEntries(GisConstants.TYPE_COLUMN,myType))
			return new LoadResult(this,ErrorCode.Aborted,"Database is missing column '�r', cannot continue");
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			Log.d("vortex","found "+name);
			if (name.equals("features")) {
				Log.d("vortex","Found beginning of data");
				reader.beginArray();
				return null;
			} else
				reader.skipValue();
		}
		return new LoadResult(this,ErrorCode.IOError);
	}

	//Parses one row of data, then updates status.
	@Override
	public LoadResult parse(JsonReader reader) throws IOException {
		Location myLocation;

		JsonToken tag = reader.peek();
		if (tag.equals(JsonToken.END_ARRAY)) {
			//end array means we are done.
			this.setEssence();
			reader.close();
			o.addRow("");
			o.addText("Found "+myGisObjects.size()+" objekt");
			freezeSteps = myGisObjects.size();
			Log.d("vortex","Found "+myGisObjects.size()+" objekt");
			//freezeSteps=myBlocks.size();
			return new LoadResult(this,ErrorCode.parsed);
		}
		else if (tag.equals(JsonToken.BEGIN_OBJECT)) {
			reader.beginObject();
			//type
			reader.nextName();
			//Feature
			this.getAttribute(reader);	
			//Geometry
			reader.nextName();
			reader.beginObject();
			reader.nextName();
			String type = this.getAttribute(reader);
			//Log.d("vortex","This is a "+type);
			if (type==null) {
				o.addRow("");
				o.addRedText("Type field expected (point, polygon..., but got null");
				Log.e("vortex","type null!");
				return new LoadResult(this,ErrorCode.ParseError);
			}
			reader.nextName();
			reader.beginArray();
			double x,y,z;
			Map<String,String>keyChain = new HashMap<String,String>();
			Map<String,String> attributes = new HashMap<String,String>();
			String mType = type.trim().toLowerCase();
			if (mType.equals(GisConstants.POINT)) {
				//Log.d("vortex","parsing point object.");
				//coordinates
				//Log.d("vortex","reading coords");
				x = reader.nextDouble();
				y = reader.nextDouble();
				myLocation = new SweLocation(x, y);
				myGisObjects.add(new GisObject(keyChain,Arrays.asList(new Location[] {myLocation}),attributes));
			} else if (mType.equals(GisConstants.MULTI_POINT)||(mType.equals(GisConstants.LINE_STRING))){
				List<Location> myCoordinates = new ArrayList<Location>();
				while (!reader.peek().equals(JsonToken.END_ARRAY)) {					
					reader.beginArray();
					x = reader.nextDouble();
					y = reader.nextDouble();
					myCoordinates.add(new SweLocation(x, y));
					reader.endArray();
				}
				myGisObjects.add(new GisObject(keyChain,myCoordinates,attributes));
			}  else if (mType.equals(GisConstants.POLYGON)){
				Map<String,List<Location>> polygons = null;
				List<Location> myCoordinates=null; 
				int proxyId = 0;
				polygons = new HashMap<String,List<Location>>();
				while (!reader.peek().equals(JsonToken.END_ARRAY)) {	
					reader.beginArray();
					myCoordinates = new ArrayList<Location>();
					while (!reader.peek().equals(JsonToken.END_ARRAY)) {
						reader.beginArray();
						x = reader.nextDouble();
						y = reader.nextDouble();
						if (!reader.peek().equals(JsonToken.END_ARRAY)) 
							z = reader.nextDouble();
						myCoordinates.add(new SweLocation(x, y));
						reader.endArray();
					}					
					polygons.put((proxyId)+"" , myCoordinates);
					proxyId++;
					reader.endArray();
				}

				if (polygons!=null&&!polygons.isEmpty())
					myGisObjects.add(new GisPolygonObject(keyChain,polygons,attributes));

			} else {
				o.addRow("");
				o.addRedText("Unsupported Geo Type in parser: "+type);
				Log.e("vortex","type not supported! "+type);
				return new LoadResult(this,ErrorCode.ParseError);
			}
			while(reader.hasNext()) {
				Log.d("vortex","skipping: "+this.getAttribute(reader));
			}
			reader.endArray();
			reader.endObject();
			//Properties
			reader.nextName();
			reader.beginObject();
			while(reader.hasNext()) {
				attributes.put(reader.nextName(),this.getAttribute(reader));
			}
			Log.d("vortex",attributes.toString());
			//end attributes
			reader.endObject();
			//end row
			reader.endObject();
			String uuid = attributes.remove(GisConstants.GlobalID);
			String rutaId = attributes.remove(GisConstants.RutaID);
			if (rutaId==null) {
				Log.e("vortex","ingen ruta ID!!!!");
				rutaId = "666";
			}
			
				
			if (uuid!=null)
				keyChain.put("uid",uuid);
			else
				keyChain.put("uid",UUID.randomUUID().toString());
			keyChain.put("�r", VariableConfiguration.HISTORICAL_MARKER);
			keyChain.put("ruta", rutaId);
			keyChain.put(GisConstants.TYPE_COLUMN, myType);



			//Log.d("vortex","added new gis object");
			return null;
		} else {
			o.addRow("");
			o.addRedText("Parse error, expected object");
			return new LoadResult(this,ErrorCode.ParseError);
		}
	}




	@Override
	public void setEssence() {
		essence = null;
	}



	boolean firstCall = true;
	@Override
	public boolean freeze(int counter) throws IOException {
		if (firstCall) {
			myDb.beginTransaction();
			Log.d("vortex","Transaction begins");
			firstCall = false;
			Log.d("vortex","keyhash for first: "+myGisObjects.get(0).getKeyHash().toString());
		}
	
		//Insert GIS variables into database
		GisObject go = myGisObjects.get(counter);
		if (!myDb.fastHistoricalInsert(go.getKeyHash(),
				GisConstants.Location,go.coordsToString())) {
			o.addRow("");
			o.addRedText("Row: "+counter+". Insert failed for "+GisConstants.Location+". Hash: "+go.getKeyHash().toString());
		}
		Map<String, String> attr = go.getAttributes();

		for (String key:attr.keySet()) {
			String val = attr.get(key);
			if (!myDb.fastHistoricalInsert(go.getKeyHash(),key,val)) {
				o.addRow("");
				o.addRedText("Row: "+counter+". Insert failed for "+key+". Hash: "+go.getKeyHash().toString());;
			}
		}
		
		if (this.freezeSteps==(counter+1)) {
			Log.d("vortex","Transaction ends");
			myDb.endTransaction();
		}
		
		return true;

	}



	public static GisObjectConfiguration getSingleton() {
		return singleton;
	}

}