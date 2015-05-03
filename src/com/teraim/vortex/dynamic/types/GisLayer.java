package com.teraim.vortex.dynamic.types;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.util.Log;

import com.teraim.vortex.dynamic.workflow_realizations.gis.GisObject;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisPointObject;

public class GisLayer {

	String name, label;
	boolean isVisible, hasWidget;
	Map<String,Set<GisObject>> myObjects;
	
	public GisLayer(String name, String label, boolean isVisible,
			boolean hasWidget) {
		super();
		this.name = name;
		this.label = label;
		this.isVisible = isVisible;
		this.hasWidget = hasWidget;
		myObjects = new HashMap<String,Set<GisObject>>();
	}

	public void addObjectBag(String key, Set<GisObject> myGisObjects) {
		myObjects.put(key, myGisObjects);
		Log.d("vortex","added "+myGisObjects.size()+" gisObjects to Layer: "+name+" of type "+key);
	}

	public Map<String,Set<GisObject>> getGisBags() {
		return myObjects;
	}

}
