package com.teraim.vortex.loadermodule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONException;
import org.xmlpull.v1.XmlPullParserException;

import android.os.Build;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.teraim.vortex.FileLoadedCb;
import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;


public class WebLoader extends Loader {

	private InputStream in;

	public WebLoader(ProgressBar pb, TextView tv, FileLoadedCb cb, boolean versionControl) {
		super(pb, tv, cb,versionControl);
	}

	@Override
	protected LoadResult doInBackground(ConfigurationModule... params) {
		ConfigurationModule module = params[0];
		URL url;
		try {

			url = new URL(module.fullPath);

			URLConnection ucon = url.openConnection();
			if (Build.VERSION.SDK != null && Build.VERSION.SDK_INT > 13) { ucon.setRequestProperty("Connection", "close"); }
			in = ucon.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			StringBuilder sb = new StringBuilder();
			
			String header = reader.readLine();
			//If the file is not readable or reachable, header is null.
			if (header==null) {
				Log.e("vortex","cannot read data..exiting");
				return new LoadResult(module,ErrorCode.IOError);
			}
			//from this point, equal code independent of source. Implemented in parent.
			ErrorCode ec = read(module,getVersion(header),reader,sb);
			
			
			//setresult runs a parser before returning. Parser is depending on module type.
			LoadResult loadResult;
			if (ec==ErrorCode.loaded) {
				loadResult = parse(module);
				if (loadResult.errCode==ErrorCode.parsed)
					return freeze(module);
				else
					return loadResult;
			}
			else
				return new LoadResult(module,ec);

		} catch (MalformedURLException e) {
			e.printStackTrace();
			return new LoadResult(module,ErrorCode.BadURL);

		} catch (IOException e) {
			return new LoadResult(module,ErrorCode.IOError);
		} catch (XmlPullParserException e) {
			return new LoadResult(module,ErrorCode.ParseError);
		} catch (JSONException e) {
			e.printStackTrace();
			return new LoadResult(module,ErrorCode.ParseError);
		}
		finally {
			try {if (in!=null)in.close();}catch (Exception e){};
		}
	}





}
