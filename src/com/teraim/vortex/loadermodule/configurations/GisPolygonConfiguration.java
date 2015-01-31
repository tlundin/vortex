package com.teraim.vortex.loadermodule.configurations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import com.teraim.vortex.loadermodule.JSONConfigurationModule;
import com.teraim.vortex.loadermodule.LoadResult;
import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.DbHelper;
import com.teraim.vortex.utils.PersistenceHelper;

public class GisPolygonConfiguration extends JSONConfigurationModule {
	
	private LoggerI o;
	private DbHelper myDb;
	

	public GisPolygonConfiguration(PersistenceHelper globalPh,PersistenceHelper ph,String fileLocation, LoggerI debugConsole,DbHelper myDb) {
		super(globalPh,ph, Source.file,fileLocation, "blockdef", "Gis Blocks            ");
		this.o = debugConsole;
		this.myDb = myDb;
		isDatabaseModule=true;
	
	}

	@Override
	public String getFrozenVersion() {
		return (ph.get(PersistenceHelper.CURRENT_VERSION_OF_GIS_BLOCKS));

	}

	@Override
	protected void setFrozenVersion(String version) {
		ph.put(PersistenceHelper.CURRENT_VERSION_OF_GIS_BLOCKS,version);

	}

	@Override
	public boolean isRequired() {
		return false;
	}

	
	@Override
	protected LoadResult prepare(JsonReader reader) throws IOException, JSONException {
		//first should be an array.
		reader.beginArray();
		return null;
	}

	private class SweRefCoordinate {
		public String N,E,S;
		
	}
	private class Block {
		public String area, blockId,
		markslag,rutaId;
		
		public List<List<SweRefCoordinate>> polygons;
	}
	
	List<Block> myBlocks = new ArrayList<Block>();
	
	@Override
	public LoadResult parse(JsonReader reader) throws IOException {
		Block b = new Block();
		JsonToken tag = reader.peek();
		if (tag.equals(JsonToken.END_ARRAY)) {
			//end array means we are done.
			this.setEssence();
			reader.close();
			freezeSteps=myBlocks.size();
			return new LoadResult(this,ErrorCode.parsed);
		}
		else if (tag.equals(JsonToken.BEGIN_OBJECT)) {
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("Shape_area")) {
				b.area = this.getAttribute(reader);
			} else if (name.equals("BLOCKID")) {
				b.blockId=this.getAttribute(reader);
			}else if (name.equals("MARKSLAG")) {
				b.markslag=this.getAttribute(reader);
			}else if (name.equals("RUTA_ID")) {
				b.rutaId=this.getAttribute(reader);
			}else if (name.equals("json_geometry")) {
				b.polygons=createCoordinates(reader);
			} else
				reader.skipValue();
		}
		reader.endObject();
		myBlocks.add(b);
		return null;
		} else {
			Log.e("vortex","Bad token in gis parser: "+tag.name());
			return new LoadResult(this,ErrorCode.ParseError);
		}
	}
	
	
	private List<List<SweRefCoordinate>> createCoordinates(JsonReader reader) throws IOException {
		List<List<SweRefCoordinate>> ret = new ArrayList<List<SweRefCoordinate>>();
		List<SweRefCoordinate> poly; 
		reader.beginObject();
		//type
		reader.nextName();
		String type = getAttribute(reader);
		if (!type.equals("Polygon"))
			Log.e("vortex"," Not a polygon in GIS IMPORT: "+type);
		String c = reader.nextName();
		if (c.equals("coordinates")) {
			reader.beginArray();
			SweRefCoordinate sec;
			poly = new ArrayList<SweRefCoordinate>();
			while (reader.peek() == JsonToken.BEGIN_ARRAY) {
				//Log.d("vortex","new poly!");
				reader.beginArray();
			while (reader.hasNext()) {
				sec = new SweRefCoordinate();
				reader.beginArray();
				sec.N = reader.nextString();
				sec.E = reader.nextString();
				sec.S = reader.nextString();
				//Log.d("vortex","N E S "+sec.N+","+sec.E+","+sec.S);
				reader.endArray();
				poly.add(sec);
			}
			ret.add(poly);
			reader.endArray();
			}
			reader.endArray();
		}
			
		reader.endObject();
		return ret;
	}

	@Override
	public void setEssence() {
		essence = myBlocks;
	}
	

	@Override
	public boolean freeze(int counter) throws IOException {
		if (myBlocks == null)
			return false;
		//Insert variables into database
		Block b = myBlocks.get(counter);
		/*
		{
			for(ValuePair v:e.variables) {
			myDb.fastGISInsert(b.rutaId,
					b.
				v.mkey,v.mval);
		}
		}
		*/
		Log.d("vortex","writing "+counter+" block");
		return true;
	}

}
