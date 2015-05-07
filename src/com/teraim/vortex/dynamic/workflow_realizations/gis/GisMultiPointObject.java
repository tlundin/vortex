package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.util.List;
import java.util.Map;

import com.teraim.vortex.dynamic.types.Location;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisMultiPointObject.Type;

public class GisMultiPointObject extends GisObject {

	public enum Type {
		MULTIPOINT,
		LINESTRING, POLYGON
	}
	private Type myType=null;
	
	public GisMultiPointObject(Map<String, String> keyChain, List<Location> myCoordinates, Type type) {
		this.myCoordinates=myCoordinates;
		this.keyChain=keyChain;
		myType = type;
	}
	
	public boolean isLineString() {
		return myType.equals(Type.LINESTRING);
	}

	

	
}
