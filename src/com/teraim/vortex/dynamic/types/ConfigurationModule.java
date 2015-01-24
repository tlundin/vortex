package com.teraim.vortex.dynamic.types;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import android.os.Build;
import android.util.Log;

import com.teraim.vortex.FileLoadedCb;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.LoadResult;
import com.teraim.vortex.utils.Loader;
import com.teraim.vortex.utils.PersistenceHelper;
import com.teraim.vortex.utils.WebLoader;


//Class that describes the specific load behaviour for a certain type of input data.
public abstract class ConfigurationModule {

	public enum Type {
		json,
		xml,
		csv
	}
	
	public enum Source {
		file,
		internet
	}
	public Source source;
	public Type type;
	public  String urlOrPath;
	public String rawData,version,fileName,fullPath;
	protected PersistenceHelper globalPh;
	private boolean IamLoaded=false;
	
	
	public ConfigurationModule(PersistenceHelper gPh, Type type, Source source, String urlOrPath,String fileName) {
		this.source=source;
		this.type=type;
		this.urlOrPath=urlOrPath;
		this.fileName=fileName;
		this.globalPh=gPh;
		fullPath = urlOrPath+fileName+"."+type.name();
		Log.d("vortex","full path "+fullPath);
	}
	
	public void setLoaded(String data, String version) {
		rawData = data;
		//setVersion(version);
	}
	
	//Read first line of file. Get file version. Check with frozen if compares.
	//If file does not exist, it is up to specialized class to determine what to do!
	public String getVersionFromSource() throws IOException {
		String version = "";
			if (source == Source.file) 
				version = getVersionFromFile();
			else
				version = getVersionFromURL();
			 
		//parse row..return.
		return Loader.getVersion(version);
	}
	
	public boolean isFrozen() {
		return new File(Constants.CONFIG_FILES_DIR+fileName).isFile();
	}
	
	private String getVersionFromFile() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(fullPath));
		String line;
		line = br.readLine();
		br.close();
		return line;
	}
	
	private String getVersionFromURL() throws IOException {
		URL url = new URL(fullPath);
		URLConnection ucon = url.openConnection();
		if (Build.VERSION.SDK != null && Build.VERSION.SDK_INT > 13) { ucon.setRequestProperty("Connection", "close"); }
		BufferedReader br = new BufferedReader(new InputStreamReader(ucon.getInputStream(), "UTF-8"));
		String line;
		line = br.readLine();
		br.close();
		return line;
	}
	
	public abstract String getFrozenVersion();
	protected abstract void setFrozenVersion(String version);

	public abstract boolean isRequired();

	public boolean isLoaded() {
		// :)
		return IamLoaded;
	}

	public void load(FileLoadedCb moduleLoader) {
		if (source == Source.internet) {
			(new WebLoader(null, null, moduleLoader)).execute(this);
		}
	}
		
	/*
	 * 			boolean isFrozen = module.isFrozen();
				String frozenVersion = null,version=null;
				if (isFrozen)
					frozenVersion = module.getFrozenVersion();
				try {
					version = module.getVersionFromSource();
				} catch (IOException e) {
					if (isFrozen) {
						o.addRow(module.fileName+": [");
						o.addYellowText(frozenVersion);
						o.addText("] ");
						o.addYellowText("(load failed but found frozen)");
						module.setLoadedFromFrozenCopy();
						continue;
					} else {
						if (module.isRequired()) {
							o.addRow(module.fileName+": [");
							o.addRedText("ERROR");
							o.addText("] ");
							o.addRedText("(load failed)");
							o.addRow("Application load failed due to missing required module");
							break;
						}
					}
				}
				//Module has another version than frozen? Reload and update. Otherwise, next.
				if (frozenVersion==null||!frozenVersion.equals(version)) {
					Log.d("vortex","Calling module load");
					module.load(this);
					break;
	 */

	//Specialized parser classes for different modules.
	public LoadResult setResult(String data, String version) {
		rawData = data;
		this.version = version;
		//If parser, run it.
		return parse();
	}
	
	protected abstract LoadResult parse();

	public void setLoadedFromFrozenCopy() {
		//load the data from frozen
		IamLoaded=true;
	}
}
