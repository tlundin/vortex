package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.util.Map;

import com.teraim.vortex.dynamic.types.Location;

public class GisObjectType {

	Map<String,String> myKeyHash;

	public enum GeoType {
		Point
		
	}
	
	public GisObjectType(String id, Map<String, String> myKeyHash, String SweNVariable, String SweEVariable) {
		super();
		this.myKeyHash = myKeyHash;
		
	}
	
	
	
}
