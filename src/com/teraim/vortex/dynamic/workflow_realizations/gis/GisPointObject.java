package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.util.Map;

import android.graphics.Bitmap;
import android.util.Log;

import com.teraim.vortex.dynamic.types.Location;
import com.teraim.vortex.dynamic.types.SweLocation;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.utils.Tools;


public abstract class GisPointObject extends GisObject {
	Bitmap icon;
	boolean dynamic = false;
	float radius=-1;
	


	public abstract Location getLocation();
	
	public Bitmap getIcon() {
		return icon;
	}

	public void setIcon(Bitmap mb) {
		this.icon=mb;
	}

	public float getRadius() {
		return radius;
	}

	public void setRadius(String radius) {
		if (!Tools.isNumeric(radius))
			return;
		this.radius = Float.parseFloat(radius);
	}



}









