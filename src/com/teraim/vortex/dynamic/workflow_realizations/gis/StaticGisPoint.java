package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.util.Map;

import com.teraim.vortex.dynamic.types.Location;

public class StaticGisPoint extends GisPointObject {
	
	public StaticGisPoint(String myLabel, Location myLocation) {
		super();
		this.myLabel = myLabel;
		myCoordinates.add(myLocation);
		dynamic = false;
	}

	public StaticGisPoint(String myLabel,Map<String, String> keyChain, Location myLocation) {
		super();
		this.keyChain = keyChain;
		myCoordinates.add(myLocation);
		dynamic = false;
	}

	@Override
	public Location getLocation() {
		if (myCoordinates.isEmpty())
			return null;
		return myCoordinates.get(0);

	}
	
	@Override
	public String toString() {
		String res="";
		res+=" \nDynamic: no";
		res+="\nLabel: "+myLabel;
		return res;
	}

}
