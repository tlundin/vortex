package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.teraim.vortex.dynamic.types.Location;

public class GisObject {

	public enum CoordinateType {
		sweref,
		latlong
	}
	
	protected String myLabel;
	protected CoordinateType coordinateType = CoordinateType.sweref;
	protected List<Location> myCoordinates = new ArrayList<Location>();
	protected Map<String, String> keyChain = new HashMap<String,String>();
	public Map<String, String> getKeyHash() {
		return keyChain;
	}
	
	public String coordsToString() {
		StringBuilder sb = new StringBuilder();
		Log.d("vortex","Size of myC: "+myCoordinates.size());
		for (Location l:myCoordinates) {
			
			sb.append(l.toString());
			sb.append(",");
		}
		if (sb.length()>0)
			return sb.substring(0, sb.length()-1);
		else
			return null;
	}

	public List<Location> getCoordinates() {
		return myCoordinates;
	}

}
