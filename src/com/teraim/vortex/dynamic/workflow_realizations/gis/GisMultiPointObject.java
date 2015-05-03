package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.util.List;
import java.util.Map;

import com.teraim.vortex.dynamic.types.Location;

public class GisMultiPointObject extends GisObject {

	public GisMultiPointObject(Map<String, String> keyChain, List<Location> myCoordinates) {
		this.myCoordinates=myCoordinates;
		this.keyChain=keyChain;
	}

	
}
