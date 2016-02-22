package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Paint.Style;
import android.util.Log;

import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.types.SweLocation;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.utils.Geomatte;

public class GisPolygonObject extends GisPathObject {

	Map<String, List<Location>> polygons;
	private FullGisObjectConfiguration conf;
	//String representation of polygon.
	private String polyString;
	

	//Called when XML has defined the object with a full configuration
	public GisPolygonObject(FullGisObjectConfiguration conf, Map<String, String> keyChain,
			String polygons,String coordType,Variable statusVar) {
		//TODO: Add statusvariable
		super(conf,keyChain,null,statusVar);
		this.polygons=null;
		polyString = polygons;
		this.conf=conf;
	}

	//Called when the object is imported using only a partial configuration. 
	public GisPolygonObject(Map<String, String> keyChain,
			Map<String, List<Location>> polygons,
			Map<String, String> attributes) {
		super(keyChain,null,attributes);
		this.polygons=polygons;	
	}

	public static Map<String, List<Location>> buildMap(String polygons,String coordType) {
		int i=1;
		if (polygons==null) 
			return null;
		String[] polys = polygons.split("\\|");
		Map<String, List<Location>> ret = new HashMap<String, List<Location>>();
		if (polys == null || polys.length==0)
			ret.put("Poly 1", new ArrayList<Location>());
		else {
		for (String poly:polys) {
			//Log.d("vortex","in poly with poly: ["+poly+"]");
			ret.put("Poly "+i, GisObject.createListOfLocations(poly, coordType));
			
			i++;
		}
		}
		
		return ret;
	}

	//returns the first main polygon. The others are apparaently only holes.
	@Override
	public List<Location> getCoordinates() {
		//If string representation, parse it now and cache
		if (polygons==null && polyString!=null)
			polygons = buildMap(polyString,GisConstants.SWEREF);
		//still null?
		if (polygons==null)
			return null;
		
		myCoordinates = polygons.get("Poly 1");
		
		if (myCoordinates==null)
				Log.e("Vortex","No poly 1 found");
		return myCoordinates;
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

	//return the geometrical centroid of the first polygon. 
	@Override
	public Location getLocation() {
		if (polygons == null) return null;
		List<Location> p1 = getCoordinates();
		if (p1==null||p1.isEmpty()) {
			return null;
		}
		double centroidX=0,centroidY=0;
		for (Location l:p1) {
			centroidX+=l.getX();
			centroidY+=l.getY();
			
		}
		//TODO: Add LATLONG!!
		return new SweLocation(centroidX/p1.size(),centroidY/p1.size());
	}

	public Map<String, List<Location>> getPolygons() {
		//If string representation, parse it now.
		if (polygons==null && polyString!=null)
			polygons = buildMap(polyString,GisConstants.SWEREF);
		return polygons;
	}

	public String getColor() {
		return conf.getColor();
	}

	public Style getStyle() {
		return conf.getStyle();
	}
	
	@Override
	public boolean isTouchedByClick(Location mapLocationForClick,double pxr,double pyr) {
		
		if (this.getWorkflow()==null)
			return false;
		myCoordinates =  getCoordinates() ;
		if (myCoordinates == null||myCoordinates.isEmpty()) {
			Log.d("vortex", "found no coordinates...exiting");
			return false;
		}
		distanceToClick=ClickThresholdInMeters;
		for (int i=0;i<myCoordinates.size()-1;i++) {

			Location A = myCoordinates.get(i);
			Location B = myCoordinates.get(i+1);
			double dist = Geomatte.pointToLineDistance3(A, B, mapLocationForClick,pxr,pyr);
			//Log.d("vortex","dist to "+this.getId()+" is "+dist+ "Thresh was Divided by pxr: "+ClickThresholdInMeters/pxr);
			if (dist<distanceToClick) 
				distanceToClick=dist;
		}
		if (distanceToClick<ClickThresholdInMeters)
			return true;
		return false;
	}
	

}
