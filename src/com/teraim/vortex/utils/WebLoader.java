package com.teraim.vortex.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.os.Build;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.teraim.vortex.FileLoadedCb;
import com.teraim.vortex.dynamic.types.ConfigurationModule;
import com.teraim.vortex.utils.LoadResult.ErrorCode;


public class WebLoader extends Loader {

	private InputStream in;

	public WebLoader(ProgressBar pb, TextView tv, FileLoadedCb cb) {
		super(pb, tv, cb);
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
			String line = null;
			String header = reader.readLine();
			while ((line = reader.readLine()) != null)
			{
				sb.append(line + "\n");
				if(isCancelled()) {
					reader.close();
					return new LoadResult(module,ErrorCode.Aborted);
				}
				this.publishProgress(1);
			}
			
			//setresult runs a parser before returning. Parser is depending on module type.
			return module.setResult(sb.toString(),getVersion(header));

		} catch (MalformedURLException e) {
			e.printStackTrace();
			return new LoadResult(module,ErrorCode.Aborted);

		} catch (IOException e) {
			e.printStackTrace();
			return new LoadResult(module,ErrorCode.Aborted);
		}
		finally {
			try {if (in!=null)in.close();}catch (Exception e){};
		}
	}



}
