package com.teraim.vortex.loadermodule.configurations;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;

import com.teraim.vortex.dynamic.types.PhotoMeta;
import com.teraim.vortex.loadermodule.LoadResult;
import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;
import com.teraim.vortex.loadermodule.XMLConfigurationModule;
import com.teraim.vortex.utils.PersistenceHelper;

public class AirPhotoMetaData extends XMLConfigurationModule {



	
	public AirPhotoMetaData(PersistenceHelper gPh, PersistenceHelper ph,
			 Source source, String urlOrPath, String metaDataFileName,
			String moduleName) {
		super(gPh, ph, source, urlOrPath, metaDataFileName, moduleName);
		hasSimpleVersion=false;
	}

	@Override
	protected LoadResult prepare(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		return null;
	}

	@Override
	protected LoadResult parse(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		Log.d("vortex","Parsing metadata");
		   int eventType = parser.getEventType();
	      while (eventType != XmlPullParser.END_DOCUMENT) {

	            if (eventType == XmlPullParser.START_TAG) {
	                if (parser.getName().equalsIgnoreCase("nativeExtBox")) {

	                   essence = readCorners(parser);
	                   return new LoadResult(this,ErrorCode.parsed);
	                   }
	            }

	            eventType = parser.next();
	        }
          Log.d("found","Did not find the meta data for image GPS coordinates!");
	      return new LoadResult(this,ErrorCode.ParseError);
	}

	private PhotoMeta readCorners(XmlPullParser parser) throws XmlPullParserException, IOException {
		float topE=0,bottomE=0,topN=0,bottomN=0;
		Log.d("vortex","calling readCordners");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}

			Log.d("vortex","reading corners!");
			String name= parser.getName();
			if (name.equals("westBL")) {
				topE = getCorner(parser);
			} else if (name.equals("eastBL")) {
				bottomE = getCorner(parser);
			} else if (name.equals("northBL")) {
				topN = getCorner(parser);
			} else if (name.equals("southBL")) {
				bottomN = getCorner(parser);
			} else
				skip(name,parser);
		}
		PhotoMeta pm = new PhotoMeta(topN,topE,bottomN,bottomE);
		return pm;
	}
	
	private float getCorner(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.next();
		float f =Float.parseFloat(parser.getText());
		parser.nextTag();
		return f;

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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setEssence() {
		
	}

}
