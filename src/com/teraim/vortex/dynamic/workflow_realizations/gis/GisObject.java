package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Path;
import android.util.Log;

import com.teraim.vortex.dynamic.types.LatLong;
import com.teraim.vortex.dynamic.types.Location;
import com.teraim.vortex.dynamic.types.SweLocation;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.GisObjectType;
import com.teraim.vortex.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.PolyType;
import com.teraim.vortex.utils.Tools;

public class GisObject {

	
	protected static final double ClickThresholdInMeters = 50;
	protected double distanceToClick=-1;
	private String label=null;
	private Variable statusVar=null;
	
	
	public enum CoordinateType {
		sweref,
		latlong
	}

	private FullGisObjectConfiguration foc;
	
	public GisObject(Map<String, String> keyChain,List<Location> myCoordinates) {
		this.keyChain=keyChain;this.myCoordinates=myCoordinates;
		
	}
	
	public GisObject(Map<String, String> keyChain,
			List<Location> myCoordinates, Map<String, String> attributes) {
		this.keyChain=keyChain;this.myCoordinates=myCoordinates;this.attributes=attributes;
	}

	public GisObject(FullGisObjectConfiguration conf,
			Map<String, String> keyChain,List<Location> myCoordinates,Variable statusVar) {
		this.keyChain=keyChain;
		this.foc = conf;
		this.myCoordinates=myCoordinates;
		this.statusVar=statusVar;
		
	}
	
	

	protected CoordinateType coordinateType = CoordinateType.sweref;
	protected List<Location> myCoordinates = new ArrayList<Location>();
	protected Map<String, String> keyChain = new HashMap<String,String>();
	protected Map<String, String> attributes;
	private boolean isUseful;
	

	//This default behavior is overridden for objects with more than one point or dynamic value. See subclasses for implementation.
	public Location getLocation() {
		if (myCoordinates==null || myCoordinates.isEmpty())
			return null;
		return myCoordinates.get(0);		
	};


	public List<Location> getCoordinates() {
		return myCoordinates;
	}
	
	public Map<String, String> getKeyHash() {
		return keyChain;
	}
	
	public Map<String, String> getAttributes() {
		return attributes;
	}
	
	public String getWorkflow() {
		return foc.getClickFlow();
	}
	
	public String getLabel() {
		if (label!=null)
			return label;
		label = foc.getLabel();
		//@notation for id
		if (label!=null && label.startsWith("@")) {
			String key = label.substring(1, label.length());
			if (key.length()>0) 
				label = keyChain.get(key);
			
		} else 
			label = Tools.parseString(label,keyChain);
		if (label==null)
			label = "";
		
		return label;
	}
	
	public String getId() {
		return foc.getName();
	}
	
	public  GisObjectType getGisPolyType() {
		return foc.getGisPolyType();
	}
	
	public PolyType getShape() {
		return foc.getShape();
	}
	
	public Variable getStatusVariable() {
		return statusVar;
	}
	
	public String getColor() {
		return foc.getColor();
	}
	
	public double getDistanceToClick() {
		return distanceToClick;
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
		//Log.d("vortex","createlistlocations returning: "+ret.toString());

		return ret;
	}
	
	public String coordsToString() {
		if (myCoordinates == null)
			return null;
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

	//Should be overridden by subclasses.

	public boolean isTouchedByClick(Location mapLocationForClick, double pxr,
			double pyr) {
		Log.e("vortex","Should never be here");
		return false;
	}


	//Should be overridden. 
	
	public void clearCache() {Log.e("vortex","Getszzzzz");}

	public void markAsUseful() {
		this.isUseful = true;
		
	}
	
	public void unmark() {
		this.isUseful = false;
		
	}

	public boolean isUseful() {
		return isUseful;
	};

	

}
