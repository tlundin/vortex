package com.teraim.vortex.utils;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.teraim.vortex.FileLoadedCb;
import com.teraim.vortex.dynamic.types.ConfigurationModule;
import com.teraim.vortex.log.LoggerI;

//Loader might have progressbar.

public abstract class Loader extends AsyncTask<ConfigurationModule ,Integer,LoadResult> {


		ProgressBar pb;
		TextView tv;
		FileLoadedCb cb;
		LoggerI myLog;
		String vNo ="";
		
		public Loader(ProgressBar pb, TextView tv,FileLoadedCb cb) {
			this.pb=pb;
			this.tv=tv;
			this.cb=cb;
		}


		public static String getVersion(String h) {
			
			if (h==null)
				return null;
			String[] header = h.split(",");
		
			if (header==null||header.length!=2) {
				
				Log.d("vortex","No version found...likely config syntax error");
				Log.d("vortex","Header: "+h);
				return null;
			} else 
				return header[1].trim();
		}

		/* Reads input files with historical data and inserts into database. */
		@Override
		protected void onPostExecute(LoadResult res) {
			cb.onFileLoaded(res);
		}

		protected abstract LoadResult doInBackground(ConfigurationModule... params);
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			//tv.setText("Loaded: "+values[0]+"/"+values[1]);
			//pb.setProgress(values[0]);
			cb.onUpdate();
		}
}
