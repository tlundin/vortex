package com.teraim.vortex.dynamic.workflow_realizations.gis;

import android.graphics.Bitmap;
import android.graphics.Paint;

import com.teraim.vortex.dynamic.types.CHash;


//Subclass with interfaces that restricts access to all.

public interface FullGisObjectConfiguration extends GisObjectBaseAttributes {

	public enum GisObjectType {
		Point,
		Multipoint,
		Polygon, Linestring
	}
	
	public enum PolyType {
		circle,
		rect
	}
	
	public float getRadius();
	public String getColor();
	public GisObjectType getGisPolyType();
	public Bitmap getIcon();
	public Paint.Style getStyle();
	public PolyType getShape();
	public String getClickFlow();
	public CHash getObjectKeyHash();
	public String getStatusVariable();
	public boolean isUser();
	public String getName();
	
	
}
