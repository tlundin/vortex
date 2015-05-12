package com.teraim.vortex.dynamic.workflow_realizations.gis;

import android.graphics.Bitmap;
import android.graphics.Paint;


//Subclass with interfaces that restricts access to all.

public interface FullGisObjectConfiguration extends GisObjectBaseAttributes {

	public enum GisObjectType {
		point,
		multipoint,
		polygon, linestring
	}
	
	public enum PolyType {
		circle,
		rect
	}
	
	public float getRadius();
	public String getColor();
	public GisObjectType getGisPolyType();
	public Bitmap getIcon();
	public Paint.Style getFillType();
	public PolyType getShape();
	
}
