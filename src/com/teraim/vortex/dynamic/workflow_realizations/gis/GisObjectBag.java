package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.util.List;
import java.util.Set;

public class GisObjectBag   {


	private String myLayer,myId, myIconUrl;
	private Set<GisObject> myGisObjects;
	

	
	public GisObjectBag(String myLayer, String myId, String myIconUrl,
			Set<GisObject> myGisObjects) {
		super();
		this.myLayer = myLayer;
		this.myId = myId;
		this.myIconUrl = myIconUrl;
		this.myGisObjects = myGisObjects;
	}



	
	
	
	

	
	
}
