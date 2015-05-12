package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.util.List;
import java.util.Map;

import android.graphics.Bitmap;

import com.teraim.vortex.dynamic.types.Location;
import com.teraim.vortex.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.PolyType;
import com.teraim.vortex.utils.Tools;


public abstract class GisPointObject extends GisObject {

	protected FullGisObjectConfiguration poc;
	
	public GisPointObject(FullGisObjectConfiguration poc,Map<String, String> keyChain,List<Location> myCoordinates) {
		super(keyChain,myCoordinates);
		this.poc=poc;
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










}









