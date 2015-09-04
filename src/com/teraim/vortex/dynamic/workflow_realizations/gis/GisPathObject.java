package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.util.List;
import java.util.Map;

import android.graphics.Path;

import com.teraim.vortex.dynamic.types.Location;
import com.teraim.vortex.dynamic.types.Variable;

public abstract class GisPathObject extends GisObject {

	private Path myPath = null;
	public GisPathObject(FullGisObjectConfiguration conf,
			Map<String, String> keyChain, List<Location> myCoordinates,
			Variable statusVar) {
		super(conf, keyChain, myCoordinates, statusVar);
		
	}

	
	public GisPathObject(Map<String, String> keyChain,
			Map<String, List<Location>> polygons,
			Map<String, String> attributes) {
		super(keyChain,null,attributes);
	}


	public Path getPath() {
		return myPath;
	}
	public void setPath(Path p) {
		myPath = p;
	}

	@Override
	public void clearCache() {
		myPath=null;
	}
}
