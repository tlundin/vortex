package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Paint.Style;

import com.teraim.vortex.dynamic.blocks.AddGisPointObjects;
import com.teraim.vortex.dynamic.types.Location;

public class GisPolygonObject extends GisObject {

	Map<String, List<Location>> polygons;
	private FullGisObjectConfiguration conf;
	
	public GisPolygonObject(Map<String, String> keyChain,
			Map<String, List<Location>> polygons) {
		super(keyChain,null);
		this.polygons=polygons;
	}
	
	public GisPolygonObject(FullGisObjectConfiguration conf, Map<String, String> keyChain,
			String polygons,String coordType) {
		super(keyChain,null);
		this.polygons=buildMap(polygons,coordType);	
		this.conf=conf;
	}


	public GisPolygonObject(Map<String, String> keyChain,
			Map<String, List<Location>> polygons,
			Map<String, String> attributes) {
		super(keyChain,null,attributes);
		this.polygons=polygons;	
	}

	private Map<String, List<Location>> buildMap(String polygons,String coordType) {
		int i=1;
		if (polygons==null) 
			return null;
		String[] polys = polygons.split("\\|");
		Map<String, List<Location>> ret = new HashMap<String, List<Location>>();
		for (String poly:polys) {
			ret.put("Poly "+i, GisObject.createListOfLocations(poly, coordType));
		}
		return ret;
	}

	@Override
	public List<Location> getCoordinates() {
		return null;
	}


	@Override
	public String coordsToString() {
		if (polygons==null)
			return null;
		String ret="";
		for(List<Location> l:polygons.values()) {
			myCoordinates = l;
			ret+= super.coordsToString()+"|";
		}
		if (!ret.isEmpty())
			ret = ret.substring(0,ret.length()-1);
		return ret;
	}

	public Map<String, List<Location>> getPolygons() {
		return polygons;
	}

	public String getColor() {
		return conf.getColor();
	}

	public Style getStyle() {
		return conf.getFillType();
	}
	
	

}
