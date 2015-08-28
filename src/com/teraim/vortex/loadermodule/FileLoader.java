package com.teraim.vortex.loadermodule;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.json.JSONException;
import org.xmlpull.v1.XmlPullParserException;

import android.widget.ProgressBar;
import android.widget.TextView;

import com.teraim.vortex.FileLoadedCb;
import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;

public class FileLoader extends Loader {

	public FileLoader(ProgressBar pb, TextView tv, FileLoadedCb cb,
			boolean versionControl) {
		super(pb, tv, cb, versionControl);
	}

	@Override
	protected LoadResult doInBackground(ConfigurationModule... params) {

		ConfigurationModule module = params[0];
		
		BufferedReader reader=null;
		try {
			reader = new BufferedReader(new FileReader(module.fullPath));
		String version=null;
		StringBuilder sb = new StringBuilder();		
		if (module.hasSimpleVersion) {
			String header = reader.readLine();
			version = versionControl?getVersion(header,null):null;
		}
		ErrorCode ec = read(module,version,reader,sb);
		LoadResult lr = new LoadResult(module,ec);
		//setresult runs a parser before returning. Parser is depending on module type.
		if (ec==ErrorCode.loaded) {
			lr = parse(module);
			if (lr.errCode == ErrorCode.parsed) {
				lr = freeze(module);
			}
		}
		return lr;

		}  catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new LoadResult(module,ErrorCode.IOError);
		} catch (XmlPullParserException e) {
			e.printStackTrace();
			return new LoadResult(module,ErrorCode.ParseError);
		} catch (JSONException e) {
			e.printStackTrace();
			return new LoadResult(module,ErrorCode.ParseError);
		}
		finally {
			try {if (reader!=null)reader.close();}catch (Exception e){};
		}
	}

}
