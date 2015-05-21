package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.teraim.vortex.dynamic.types.LatLong;
import com.teraim.vortex.dynamic.types.Location;
import com.teraim.vortex.dynamic.types.SweLocation;
import com.teraim.vortex.utils.Tools;

public class GisObject {

	public enum CoordinateType {
		sweref,
		latlong
	}
	
	public GisObject(Map<String, String> keyChain,List<Location> myCoordinates) {
		this.keyChain=keyChain;this.myCoordinates=myCoordinates;
		
	}
	
	public GisObject(Map<String, String> keyChain,
			List<Location> myCoordinates, Map<String, String> attributes) {
		this.keyChain=keyChain;this.myCoordinates=myCoordinates;this.attributes=attributes;
	}

	protected CoordinateType coordinateType = CoordinateType.sweref;
	protected List<Location> myCoordinates = new ArrayList<Location>();
	protected Map<String, String> keyChain = new HashMap<String,String>();
	protected Map<String, String> attributes;
	


	public List<Location> getCoordinates() {
		return myCoordinates;
	}
	
	public Map<String, String> getKeyHash() {
		return keyChain;
	}
	
	public Map<String, String> getAttributes() {
		return attributes;
	}
	
	public static List<Location> createListOfLocations(String value, String coordType) {
		if (value==null) {
			return null;
		}
		String[] coords = value.split(",");
		boolean x=true;
		String gX=null;
		List<Location> ret = new ArrayList<Location>();
		for (String coord:coords) {
			if (x) {
				x=false;
				gX=coord;
			} else {
				x=true;
				if(coordType.equals(GisConstants.SWEREF))
					ret.add(new SweLocation(gX,coord));
				else
					ret.add(new LatLong(gX,coord));
			}
		}
		return ret;
	}
	
	public String coordsToString() {
		StringBuilder sb = new StringBuilder();
		for (Location l:myCoordinates) {
			
			sb.append(l.toString());
			sb.append(",");
		}
		if (sb.length()>0)
			return sb.substring(0, sb.length()-1);
		else
			return null;
	}

	//Only implemented by Point object

	public boolean isTouchedByClick(Location mapLocationForClick, double pxr,
			double pyr) {
		// TODO Auto-generated method stub
		return false;
	}





}
