package com.teraim.vortex.loadermodule;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;
import com.teraim.vortex.utils.PersistenceHelper;

public abstract class XMLConfigurationModule extends ConfigurationModule {

	
	public XMLConfigurationModule(PersistenceHelper gPh,PersistenceHelper ph,
			Type type,Source source, String urlOrPath, String fileName, String moduleName) {
		super(gPh,ph, type, source, urlOrPath, fileName, moduleName);
	}
	protected abstract LoadResult prepare(XmlPullParser parser) throws XmlPullParserException, IOException;
	protected abstract LoadResult parse(XmlPullParser parser) throws XmlPullParserException, IOException;
	
		
}
