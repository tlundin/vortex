package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.util.List;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.Paint.Style;
import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.types.Location;
import com.teraim.vortex.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.PolyType;
import com.teraim.vortex.utils.RuleExecutor;
import com.teraim.vortex.utils.RuleExecutor.SubstiResult;
import com.teraim.vortex.utils.RuleExecutor.TokenizedItem;
import com.teraim.vortex.utils.Tools;


public abstract class GisPointObject extends GisObject {

	protected FullGisObjectConfiguration poc; 
	private Map<String, String> uniqueKeys;
	
	public GisPointObject(FullGisObjectConfiguration poc,Map<String, String> keyChain,List<Location> myCoordinates) {
		super(keyChain,myCoordinates);
		this.poc=poc;
		if (poc.getCommonHash()!=null)
			uniqueKeys = Tools.findKeyDifferences(keyChain, poc.getCommonHash().keyHash);
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





}









