package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.Paint.Style;
import android.util.Log;

import com.teraim.vortex.dynamic.types.Location;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.PolyType;
import com.teraim.vortex.utils.Tools;


public abstract class GisPointObject extends GisObject {

	protected FullGisObjectConfiguration poc; 

	
	private int[] xy=null;
	
	public GisPointObject(FullGisObjectConfiguration poc,Map<String, String> keyChain,List<Location> myCoordinates, Variable statusVar) {
		super(poc,keyChain,myCoordinates,statusVar);
		this.poc=poc;
		
	}
	public abstract Location getLocation();
	public abstract boolean isDynamic();
	public abstract boolean isUser();
	
	public Bitmap getIcon() {
		return poc.getIcon();
	}
	public float getRadius() {
		return poc.getRadius();
	}

	




	@Override
	public boolean isTouchedByClick(Location mapLocationForClick,double pxr,double pyr) {
		Location myLocation = this.getLocation();
		if (myLocation==null) {
			Log.d("vortex","No location found for object "+this.getLabel());
			return false;
		}
		if (this.getWorkflow()==null)
			return false;
		Log.d("vortex","pxr pyr"+pxr+","+pyr);
		double xD = (mapLocationForClick.getX()-myLocation.getX())*pxr;
		double yD = (mapLocationForClick.getY()-myLocation.getY())*pyr;

		//double touchThresh = ClickThresholdInMeters;
		
		distanceToClick = Math.sqrt(xD*xD+yD*yD);
		/*
		if (isCircle())
			touchThresh = this.getRadius()/pxr;
		else {
			touchThresh = this.getRadius()/pyr;
		}
		 */
		//Log.d("vortex","I: D: "+this.getLabel()+","+distanceToClick);
		if (distanceToClick<ClickThresholdInMeters) {
			Log.d("vortex","found friend!");			
			return true;
		}
		//Log.d("vortex", "Dist x  y  tresh: "+xD+","+yD+","+touchThresh);
		return false;
	}
	public Style getStyle() {
		return poc.getStyle();
	}
	
	
	private Map<GisFilter,Boolean> filterCache = new HashMap<GisFilter,Boolean>();
	
	public boolean hasCachedFilterResult(GisFilter filter) {
		return filterCache.get(filter)!=null;
	}
	public void setCachedFilterResult(GisFilter filter, boolean b) {
		filterCache.put(filter, b);
		
	}
	public boolean getCachedFilterResult(GisFilter filter) {
		return filterCache.get(filter);
	}

	public int[] getTranslatedLocation() {
		return xy;
	}
	public void setTranslatedLocation(int[] xy) {
		this.xy=xy;
	}



}









