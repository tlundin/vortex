package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.util.List;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.Paint.Style;
import android.util.Log;

import com.teraim.vortex.dynamic.types.Location;
import com.teraim.vortex.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.PolyType;
import com.teraim.vortex.utils.Tools;


public abstract class GisPointObject extends GisObject {

	protected FullGisObjectConfiguration poc;
	private String uniqueKey,uniqueValue;
	
	public GisPointObject(FullGisObjectConfiguration poc,Map<String, String> keyChain,List<Location> myCoordinates) {
		super(keyChain,myCoordinates);
		this.poc=poc;
		if (poc.getCommonHash()!=null)
			uniqueKey = Tools.findKeyDifference(keyChain, poc.getCommonHash().keyHash);
		if (uniqueKey !=null)
			uniqueValue = keyChain.get(uniqueKey);
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
	


	@Override
	public boolean isTouchedByClick(Location mapLocationForClick,double pxr,double pyr) {
		Log.d("vortex","In touchclick");
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
		Log.d("vortex", "Dist x  y  tresh: "+xD+","+yD+","+touchThresh);
		return false;
	}
	public Style getStyle() {
		return poc.getFillType();
	}
	
	public String getLabel() {
		return poc.getLabel()+(uniqueValue!=null?" "+uniqueValue:"");
	}





}









