package com.teraim.vortex.dynamic.workflow_realizations.gis;

import com.teraim.vortex.dynamic.types.Location;


public class GisObject {

	private String myLabel;
	private Location myLocation;
	
	public GisObject(String myLabel, Location myLocation) {
		super();
		this.myLabel = myLabel;
		this.myLocation = myLocation;
	}

	public Location getLocation() {
		return myLocation;
	}

}

	
	 
	
	
	
	
	

