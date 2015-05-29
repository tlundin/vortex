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
	private Variable statusVar=null;
	
	public GisPointObject(FullGisObjectConfiguration poc,Map<String, String> keyChain,List<Location> myCoordinates, Variable statusVar) {
		super(keyChain,myCoordinates);
		this.poc=poc;
		this.statusVar=statusVar;
	}
	public abstract Location getLocation();
	public Bitmap getIcon() {
		return poc.getIcon();
	}
	public float getRadius() {
		return poc.getRadius();
	}
	public String getColor() {
		return poc.getColor();
	}
	public boolean isCircle() {
		return poc.getShape()== PolyType.circle;
	}
	public boolean isRect() {
		return poc.getShape()== PolyType.rect;
	}
	
	public String getWorkflow() {
		return poc.getClickFlow();
	}
	
	public Variable getStatusVariable() {
		return statusVar;
	}
	


	@Override
	public boolean isTouchedByClick(Location mapLocationForClick,double pxr,double pyr) {
		Location myLocation = this.getLocation();
		if (myLocation==null) {
			Log.d("vortex","No location found for object "+this.getLabel());
			return false;
		}
		double xD = Math.abs(mapLocationForClick.getX()-myLocation.getX());
		double yD = Math.abs(mapLocationForClick.getY()-myLocation.getY());

		double touchThresh;
		
		if (isCircle())
			touchThresh = this.getRadius()/pxr;
		else {
			touchThresh = this.getRadius()/pyr;
		}

		if (xD<touchThresh&&yD<touchThresh) {
			Log.d("vortex","found friend!");
			return true;
		}
		//Log.d("vortex", "Dist x  y  tresh: "+xD+","+yD+","+touchThresh);
		return false;
	}
	public Style getStyle() {
		return poc.getFillType();
	}
	
	public String getLabel() {
		String label = poc.getLabel();
		if (label==null)
			return "";
		if (label.startsWith("@")) {
			String key = label.substring(1, label.length());
			if (key.length()>0) {
				return key+" "+keyChain.get(key);
			}
		}
		return poc.getLabel();
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





}









