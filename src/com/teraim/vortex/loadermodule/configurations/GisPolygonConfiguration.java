package com.teraim.vortex.loadermodule.configurations;

import java.io.IOException;

import com.teraim.vortex.loadermodule.CSVConfigurationModule;
import com.teraim.vortex.loadermodule.LoadResult;
import com.teraim.vortex.loadermodule.ConfigurationModule.Source;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.PersistenceHelper;

public class GisPolygonConfiguration extends CSVConfigurationModule {

	public GisPolygonConfiguration(PersistenceHelper globalPh, String server, String bundle, LoggerI debugConsole) {
		super(globalPh, Source.file,Constants.GIS_DATA_DIR, "polydef", "Gis Polygons          ");
		
	}

	@Override
	public LoadResult parse(String row, Integer currentRow) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getFrozenVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void setFrozenVersion(String version) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isRequired() {
		return false;
	}

	@Override
	public Object getEssence() {
		return null;
	}

	@Override
	protected LoadResult prepare() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
