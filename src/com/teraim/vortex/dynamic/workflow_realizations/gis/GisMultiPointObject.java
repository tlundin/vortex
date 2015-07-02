package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.util.List;
import java.util.Map;

import android.util.Log;

import com.teraim.vortex.dynamic.types.Location;
import com.teraim.vortex.dynamic.types.SweLocation;
import com.teraim.vortex.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.GisObjectType;
import com.teraim.vortex.utils.Geomatte;

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

	
	//return the geometrical centroid of the first polygon. 
	@Override
	public Location getLocation() {
		if (myCoordinates == null||myCoordinates.isEmpty()) {
			Log.e("Vortex","Cannot calculate centroid for multipoint. No points found or obj contains no points!!");
			return null;
		}
		double centroidX=0,centroidY=0;
		for (Location l:myCoordinates) {
			centroidX+=l.getX();
			centroidY+=l.getY();
			
		}
		//TODO: Add LATLONG!!
		return new SweLocation(centroidX/myCoordinates.size(),centroidY/myCoordinates.size());
	}
	
	final static Location origo = new SweLocation(0,0);
	
	@Override
	public boolean isTouchedByClick(Location mapLocationForClick,double pxr,double pyr) {
		//Only linestrings can be touched.
		Log.d("vortex", "in istouch for linestr");
		if (!isLineString()) {
			Log.d("vortex", "this is no linestring...exiting.");
			return false;
		}
		if (myCoordinates == null||myCoordinates.isEmpty()) {
			Log.d("vortex", "found no coordinates...exiting");
			return false;
		}
		for (int i=0;i<myCoordinates.size()-1;i++) {

			Location A = Geomatte.subtract(myCoordinates.get(i),mapLocationForClick);
			Location B = Geomatte.subtract(myCoordinates.get(i+1),mapLocationForClick);
			double dist = Geomatte.pointToLineDistance(A, B, origo);
			double dist2 = Geomatte.pointToLineDistance3(A, B, origo);
			Log.d("vortex","Distance O: "+dist+" Distance N: "+dist2);
			if (dist2<(ClickThresholdInMeters)) {
				Log.d("vortex","found!!!");
				return true;
			}
		}
		return false;
	}
	
	
}
