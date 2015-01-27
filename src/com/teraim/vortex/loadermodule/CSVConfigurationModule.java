package com.teraim.vortex.loadermodule;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.teraim.vortex.utils.PersistenceHelper;

public abstract class CSVConfigurationModule extends ConfigurationModule {

	public CSVConfigurationModule(PersistenceHelper gPh,
			Source source, String urlOrPath, String fileName, String moduleName) {
		super(gPh, Type.csv, source, urlOrPath, fileName, moduleName);
	}

	protected abstract LoadResult prepare() throws IOException;
	public abstract LoadResult parse(String row, Integer currentRow) throws IOException;
	
}
