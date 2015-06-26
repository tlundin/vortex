package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.util.List;
import java.util.Map;

import com.teraim.vortex.dynamic.types.Location;
import com.teraim.vortex.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.GisObjectType;

public class GisMultiPointObject extends GisObject {

	private FullGisObjectConfiguration poc;
	
	public GisMultiPointObject(FullGisObjectConfiguration conf,Map<String, String> keyChain,List<Location> myCoordinates) {
		//TODO: Add statusvariable.
		super(conf,keyChain,myCoordinates,null);
		poc = conf;
	}
	
	public boolean isLineString() {
		return poc.getGisPolyType()==GisObjectType.Linestring;
	}

	

	
}
