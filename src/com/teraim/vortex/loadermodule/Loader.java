package com.teraim.vortex.loadermodule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.json.JSONException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;
import android.util.Xml;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.teraim.vortex.FileLoadedCb;
import com.teraim.vortex.loadermodule.ConfigurationModule.Type;
import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.utils.Tools;

//Loader might have progressbar.

public abstract class Loader extends AsyncTask<ConfigurationModule ,Integer,LoadResult> {


	ProgressBar pb;
	TextView tv;
	FileLoadedCb cb;
	LoggerI myLog;
	String vNo ="";
	LoadResult loadR=null;
	boolean versionControl;

	public Loader(ProgressBar pb, TextView tv,FileLoadedCb cb, boolean versionControl) {
		this.pb=pb;
		this.tv=tv;
		this.cb=cb;
		this.versionControl=versionControl;
	}


	public static String getVersion(String h) {

		if (h==null)
			return null;
		String[] header = h.split(",");

		if (header!=null&&header.length>=2) {
			String potVer = header[1].trim(); 
			if (Tools.isVersionNumber(potVer))
				return potVer;
		}
		Log.d("vortex","No version found for simple lookup.");
		Log.d("vortex","Header: "+h);
		return null;

	}

	/* Reads input files with historical data and inserts into database. */
	@Override
	protected void onPostExecute(LoadResult res) {
		cb.onFileLoaded(res);
	}


	@Override
	protected void onProgressUpdate(Integer... values) {
		//tv.setText("Loaded: "+values[0]+"/"+values[1]);
		//pb.setProgress(values[0]);
		cb.onUpdate(values);
	}

	int rowC = 1;
	protected ErrorCode read(ConfigurationModule m, String newVersion, BufferedReader reader,StringBuilder sb) throws IOException {
		String line = null;
		if (newVersion != null) {
			m.setVersion(newVersion);
			if (versionControl) {
				String version = m.getFrozenVersion();
				if (version!=null && version.equals(newVersion)) {
					//We can set the version number safely.
					return ErrorCode.sameold;
				}
			}
		}
		while ((line = reader.readLine()) != null)
		{
			sb.append(line + "\n");
			if(isCancelled()) {
				reader.close();
				return ErrorCode.Aborted;
			}
			if (rowC++%10==0)
				this.publishProgress(0,rowC);
		}
		m.setRawData(sb.toString(), rowC);
		return ErrorCode.loaded;
	}

	protected LoadResult parse(ConfigurationModule m) throws IOException, XmlPullParserException, JSONException {
		if (m.type==Type.csv)
			return parseCSV((CSVConfigurationModule)m);
		else if (m.type==Type.json)
			return parseJSON((JSONConfigurationModule)m);
		else
			return parseXML((XMLConfigurationModule)m);
	}

	protected LoadResult parseCSV(CSVConfigurationModule m) throws IOException {
		String[] myRows=null;
		Integer noOfRows=rowC;
		LoadResult res = null;
		rowC=1;
		LoadResult lr = m.prepare();
		//exit if prepare returns.
		if (lr!=null)
			return lr;

		if (m.rawData!=null) 
			myRows = m.rawData.split("\\n");
		else
			return new LoadResult(m,ErrorCode.noData);
		for (String row:myRows) {
			loadR = m.parse(row,rowC);
			if (rowC++%20==0)
				this.publishProgress(rowC,noOfRows);
			if (loadR!=null) {
				res = loadR;
				break;
			}
		}
		if (res==null)
			res = new LoadResult(m,ErrorCode.parsed);
		this.publishProgress(rowC,noOfRows);
		return res;
	}


	protected LoadResult parseXML(XMLConfigurationModule m) throws XmlPullParserException, IOException {
		XmlPullParser parser = Xml.newPullParser();
		parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
		parser.setInput(new StringReader(m.rawData));
		LoadResult lr = m.prepare(parser);
		//exit if prepare returns.
		if (lr!=null)
			return lr;
		rowC=0;
		while((lr=m.parse(parser))==null)
			this.publishProgress(rowC++);
		return lr;
	}
	
	
	protected LoadResult parseJSON(JSONConfigurationModule m) throws IOException, JSONException {
		JsonReader parser = new JsonReader(new StringReader(m.rawData));
		LoadResult lr = m.prepare(parser);
		if (lr!=null)
			return lr;
		rowC=0;
		while((lr=m.parse(parser))==null)
			this.publishProgress(rowC++);
		return lr;
	}
	

	protected LoadResult freeze(ConfigurationModule m) throws IOException {
		
		//Multiple steps or only one to freeze?
		if (m.freezeSteps>0) {
			rowC=0;
			while (rowC<m.freezeSteps) {
			m.freeze(rowC++);
			if (rowC%10==0)
				this.publishProgress(rowC,m.freezeSteps);

			}
		} else 
			m.freeze(-1);
			
		
		if (m.version!=null)
			m.setFrozenVersion(m.version);
		if (m.getEssence()!=null)
			return new LoadResult(m,ErrorCode.frozen);
		else {
			Log.d("vortex","in freez: Essence is "+m.getEssence());
			return new LoadResult(m,ErrorCode.noData);
		}
			
	}

}
