package com.teraim.vortex.dynamic.types;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.util.Log;

import com.teraim.vortex.dynamic.workflow_realizations.gis.GisObject;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisPointObject;

public class GisLayer {

	String name, label;
	private boolean isVisible, hasWidget,hasDynamic=false;
	Map<String,Set<GisObject>> myObjects;
	private boolean showLabels;
	
	public GisLayer(String name, String label, boolean isVisible,
			boolean hasWidget, boolean showLabels) {
		super();
		this.name = name;
		this.label = label;
		this.isVisible = isVisible;
		this.hasWidget = hasWidget;
		this.showLabels=showLabels;
		myObjects = new HashMap<String,Set<GisObject>>();
	}

	public void addObjectBag(String key, Set<GisObject> myGisObjects, boolean dynamic) {
		myObjects.put(key, myGisObjects);
		Log.d("vortex","added "+myGisObjects.size()+" gisObjects to Layer: "+name+" of type "+key);
		if (dynamic)
			this.hasDynamic = true;
		
	}

	public Map<String,Set<GisObject>> getGisBags() {
		return myObjects;
	}

	public void setVisible(boolean isVisible) {
		this.isVisible=isVisible;
	}
	
	public void setShowLabels(boolean show) {
		showLabels=show;
	}
	
	public boolean hasDynamic() {
		return hasDynamic;
	}
	
	public boolean isVisible() {
		return isVisible;
	}

	public boolean showLabels() {
		return showLabels;
	}

}
