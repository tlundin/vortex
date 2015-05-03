package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.types.Location;


public class GisPointObject extends GisObject {
	Bitmap icon;
	
	public GisPointObject(String myLabel, Location myLocation) {
		super();
		this.myLabel = myLabel;
		myCoordinates.add(myLocation);

	}

	public GisPointObject(Map<String, String> keyChain, Location myLocation) {
		this.keyChain = keyChain;
		myCoordinates.add(myLocation);
	}
	public Location getLocation() {
		if (myCoordinates.isEmpty())
			return null;
		return myCoordinates.get(0);
	}
	
	public Bitmap getIcon() {
		return icon;
	}
	
	public void setIcon(Bitmap mb) {
		this.icon=mb;
	}



}

	
	 
	
	
	
	
	

