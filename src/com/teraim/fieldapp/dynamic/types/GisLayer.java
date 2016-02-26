package com.teraim.fieldapp.dynamic.types;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.os.Bundle;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisFilter;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisObject;
import com.teraim.fieldapp.utils.PersistenceHelper;
/**
 * 
 * A Layer holds the GIS Objects drawn in GisImageView, created by block_add_gis_layer.
 * Each GIS Layer may hold reference to any GIS Object type. 
 * Each GIS Layer may be visible or hidden, controlled by user.
 * A GIS Layer may or may not have a Widget, controlled by XML Tag. 
 * Please see the XML Block definition for block_add_gis_layer
 */

public class GisLayer {

	private String name, label;
	private boolean hasWidget,hasDynamic=false;
	private Map<String,Set<GisObject>> myObjects;
	private boolean showLabels;
	private Map<String, Set<GisFilter>> myFilters;
	private boolean defaultVisible;
	
	
	public GisLayer(String name, String label, boolean isVisible,
			boolean hasWidget, boolean showLabels) {
		super();
		this.name = name;
		this.label = label;
		this.hasWidget = hasWidget;
		this.showLabels=showLabels;
		//If no user set value exist, use default.
		//Else, set isvisible if stored value is 1.
		defaultVisible = isVisible;
		
	}

	//TODO: Potentially split incoming objects into two bags. one for static and one for changeable. 
	//This would speed up CRUD for map objects.
	
	public void addObjectBag(String key, Set<GisObject> myGisObjects, boolean dynamic) {
		if (myObjects==null) {
			myObjects = new HashMap<String,Set<GisObject>>();
			
		} 
		
		if (myGisObjects == null) {
			myGisObjects = new HashSet<GisObject>();
			Log.d("vortex","Added empty set");
		}
		myObjects.put(key, myGisObjects);
		Log.d("vortex","added "+myGisObjects.size()+" objects to layer: "+name+" of type "+key);
		if (dynamic)
			this.hasDynamic = true;
		
	}
	
	public void addObjectFilter(String key, GisFilter f) {
		Set<GisFilter> setOfFilters = myFilters.get(key);
		if (setOfFilters==null) 
			setOfFilters = new HashSet<GisFilter>();
		
		setOfFilters.add(f);
		Log.d("vortex","added filter "+name+" of type "+key);
		myFilters.put(key, setOfFilters);
		
	}

	public Map<String,Set<GisObject>> getGisBags() {
		return myObjects;
	}
	public Set<GisObject> getBagOfType(String type) {
		if (myObjects !=  null )
			return myObjects.get(type);
		return null;
	}
	public Map<String,Set<GisFilter>> getFilters() {
		if (myFilters !=  null )
			return myFilters;
		return null;
	}

	public void setVisible(boolean isVisible) {
	
		Log.d("vortex","SetVisible called with "+isVisible+" on "+this.getId()+" Obj: "+this.toString());
		GlobalState.getInstance().getPreferences().put(PersistenceHelper.LAYER_VISIBILITY+name, isVisible?1:0);
		
	}
	
	/** Search for GisObject in all bags. 
	 * @param go   -- the object to look for.
	 * @return -- the first instance of the object if found. 
	 * */
	public Set<GisObject> getBagContainingGo(GisObject go) {
		if (myObjects == null)
			return null;
		for (String k:myObjects.keySet()) {
			Set<GisObject> gos = myObjects.get(k);
			for (GisObject g:gos) 
				if (go.equals(g))
					return gos;
			
		}
		return null;
	}
	
	public void setShowLabels(boolean show) {
		showLabels=show;
	}
	
	public boolean hasDynamic() {
		return hasDynamic;
	}
	
	public boolean isVisible() {
		int v = GlobalState.getInstance().getPreferences().getI(PersistenceHelper.LAYER_VISIBILITY+name);
		//Log.d("vortex","Layer "+name+" has visibility set to "+v+" default is "+defaultVisible);
		if (v==-1)
			return defaultVisible;
		else
			return v==1;

	}

	public boolean showLabels() {
		return showLabels;
	}
	
	public String getLabel() {
		return label;
	}
	
	public String getId() {
		return name;
	}

	public void clearCaches() {
		if (myObjects==null)
			return;
		for (String key:myObjects.keySet()) {
			Set<GisObject> bag = myObjects.get(key);
			for (GisObject go:bag) {
				go.clearCache();
				go.unmark();
			}
		}
	}


	public boolean hasWidget() {
		return hasWidget;
	}




	



}
