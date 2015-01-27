package com.teraim.vortex.loadermodule;

import java.io.IOException;

import org.json.JSONException;

import com.teraim.vortex.utils.PersistenceHelper;

public abstract class JSONConfigurationModule extends ConfigurationModule {

	public JSONConfigurationModule(PersistenceHelper gPh,
			Source source, String urlOrPath, String fileName, String moduleName) {
		super(gPh, Type.json, source, urlOrPath, fileName, moduleName);
	}

	protected abstract LoadResult prepare(String json) throws IOException, JSONException;
	public abstract LoadResult parse(String row, Integer currentRow) throws IOException, JSONException;
	
}
