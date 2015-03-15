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

import com.teraim.vortex.loadermodule.JSONConfigurationModule;
import com.teraim.vortex.loadermodule.LoadResult;
import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.utils.DbHelper;
import com.teraim.vortex.utils.PersistenceHelper;

public class GisPolygonConfiguration extends JSONConfigurationModule {
	
	private LoggerI o;
	private DbHelper myDb;
	private static GisPolygonConfiguration singleton;

	public GisPolygonConfiguration(PersistenceHelper globalPh,PersistenceHelper ph,String fileLocation, LoggerI debugConsole,DbHelper myDb) {
		super(globalPh,ph, Source.file,fileLocation, "blockdef", "Gis Blocks            ");
		this.o = debugConsole;
		this.myDb = myDb;
		//isDatabaseModule=true;
		this.singleton=this;
		this.hasSimpleVersion=false;
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

	


	public class SweRefCoordinate {
		public float N,E;
		
	}
	public class GisBlock {
		public String area, blockId,
		markslag,rutaId;
		
		public List<List<SweRefCoordinate>> polygons;
	}
	
	//Map between rutor and block
	Map<String,List<GisBlock>> myBlocks = new HashMap<String,List<GisBlock>>();
	
	
	@Override
	protected LoadResult prepare(JsonReader reader) throws IOException, JSONException {
		//first should be an array.
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("features")) {
				Log.d("vortex","Found beginning of data in GIS DATA!");
				reader.beginArray();
				return null;
			} else
				reader.skipValue();
		}
		return new LoadResult(this,ErrorCode.IOError);
	}
	
	
	@Override
	public LoadResult parse(JsonReader reader) throws IOException {
		GisBlock b = new GisBlock();
		JsonToken tag = reader.peek();
		if (tag.equals(JsonToken.END_ARRAY)) {
			//end array means we are done.
			this.setEssence();
			reader.close();
			o.addRow("");
			o.addText("GIS Blocks parsed succesfully");
			//freezeSteps=myBlocks.size();
			return new LoadResult(this,ErrorCode.parsed);
		}
		else if (tag.equals(JsonToken.BEGIN_OBJECT)) {
		reader.beginObject();
		String attr = reader.nextName();
		Log.d("vortex","Attr "+attr);
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
			} else
				reader.skipValue();
		}
		//End post attributes
		reader.endObject();
		//geometry..
		reader.nextName();
		
		b.polygons=createCoordinates(reader);
		
		List<GisBlock> lb = myBlocks.get(b.rutaId);
		if (lb==null) {
			lb = new ArrayList<GisBlock>();
			myBlocks.put(b.rutaId,lb);
		}
		lb.add(b);
		//End pre attributes
		reader.endObject();
		return null;
		} else {
			Log.e("vortex","Bad token in gis parser: "+tag.name());
			return new LoadResult(this,ErrorCode.ParseError);
		}
	}
	
	
	private List<List<SweRefCoordinate>> createCoordinates(JsonReader reader) throws IOException {
		List<List<SweRefCoordinate>> ret = new ArrayList<List<SweRefCoordinate>>();
		List<SweRefCoordinate> poly;
		//begin geometry
		reader.beginObject();
		//rings
		String type = reader.nextName();
		if (!type.equals("rings"))
			Log.e("vortex"," Not a ring in GIS IMPORT: "+type);
		
		else {		
			//Begin post ring
			reader.beginArray();
			SweRefCoordinate sec;
			poly = new ArrayList<SweRefCoordinate>();
			while (reader.peek() == JsonToken.BEGIN_ARRAY) {
				Log.d("vortex","new poly!");
				reader.beginArray();
				
			while (reader.hasNext()) {
				sec = new SweRefCoordinate();
				reader.beginArray();
				sec.E = Float.parseFloat(reader.nextString());
//				Log.d("vortex","E "+sec.E);
				sec.N = Float.parseFloat(reader.nextString());
//				Log.d("vortex","N "+sec.N);
				reader.endArray();
				poly.add(sec);
			}
			ret.add(poly);
			reader.endArray();
			}
			//End post ring
			reader.endArray();
		}
		//end geometry
		reader.endObject();
		return ret;
	}

	@Override
	public void setEssence() {
		essence = myBlocks;
	}
	
	public List<GisBlock> getBlocks(String rutaId) {
		return myBlocks.get(rutaId);
	}
	

	@Override
	public boolean freeze(int counter) throws IOException {
		if (myBlocks == null)
			return false;
		//Insert variables into database
		/*
		{
			for(ValuePair v:e.variables) {
			myDb.fastGISInsert(b.rutaId,
					b.
				v.mkey,v.mval);
		}
		}
		*/
		return true;
	}
	
	public static GisPolygonConfiguration getSingleton() {
		return singleton;
	}

}
