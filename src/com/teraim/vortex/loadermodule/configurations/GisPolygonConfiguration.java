package com.teraim.vortex.loadermodule.configurations;

import java.io.IOException;

import com.teraim.vortex.loadermodule.CSVConfigurationModule;
import com.teraim.vortex.loadermodule.LoadResult;
import com.teraim.vortex.loadermodule.ConfigurationModule.Source;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.PersistenceHelper;

public class GisPolygonConfiguration extends JSONConfigurationModule {
	
	private LoggerI o;

	public GisPolygonConfiguration(PersistenceHelper globalPh,PersistenceHelper ph, String server, String bundle, LoggerI debugConsole) {
		super(globalPh,ph, Source.file,Constants.GIS_DATA_DIR, "blockdef", "Gis Blocks            ");
		this.o = debugConsole;

	}

	@Override
	public LoadResult parse(String row, Integer currentRow) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	@Override
	public String getFrozenVersion() {
		return (ph.get(PersistenceHelper.CURRENT_VERSION_OF_GIS_BLOCKS));
		return null;
	}

	@Override
	protected void setFrozenVersion(String version) {
		ph.put(PersistenceHelper.CURRENT_VERSION_OF_GIS_BLOCKS,version);

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

	@Override
	public void setEssence() {
		// TODO Auto-generated method stub
		
	}


}
