package com.teraim.vortex.loadermodule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.teraim.vortex.FileLoadedCb;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.PersistenceHelper;
import com.teraim.vortex.utils.Tools;


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
	public String rawData,version,fileName,fullPath,name,frozenPath;
	protected PersistenceHelper globalPh;
	protected boolean IamLoaded=false,versionControl;

	private Integer linesOfRawData;
	private Configuration mModules;
	
	
	public ConfigurationModule(PersistenceHelper gPh, Type type, Source source, String urlOrPath,String fileName,String moduleName) {
		this.source=source;
		this.type=type;
		this.urlOrPath=urlOrPath;
		this.fileName=fileName;
		this.globalPh=gPh;
		this.name=moduleName;
		fullPath = urlOrPath+fileName+"."+type.name();
		frozenPath = Constants.CONFIG_FILES_DIR+fileName;
		Log.d("vortex","full path "+fullPath);
		this.versionControl=!globalPh.getB(PersistenceHelper.VERSION_CONTROL_SWITCH_OFF);
	}
	

	public boolean frozenFileExists() {
		return new File(frozenPath).isFile();
	}
	
	public abstract String getFrozenVersion();
	

	//Stores version number. Can be different from frozen version during load.
	public void setVersion(String version) {
		this.version=version;
	}

	//Freeze version number when load succesful
	public void setLoaded() {
		IamLoaded=true;
		setFrozenVersion(version);
	}
	
	protected abstract void setFrozenVersion(String version);

	public abstract boolean isRequired();

	public boolean isLoaded() {
		// :)
		return IamLoaded;
	}
	
	public void load(FileLoadedCb moduleLoader) {
		Loader mLoader;
		if (source == Source.internet) 
			mLoader = new WebLoader(null, null, moduleLoader,versionControl);
		else 
			mLoader = new FileLoader(null, null, moduleLoader,versionControl);
		mLoader.execute(this);
	}


	
	public void setRawData(String data, Integer tot) {
		rawData = data;
		this.linesOfRawData = tot;
	}
	
	protected Integer getNumberOfLinesInRawData() {
		// TODO Auto-generated method stub
		return linesOfRawData;
	}
	

	public void setLoadedFromFrozenCopy() {
		//load the data from frozen
		IamLoaded=true;
	}

	
	public String getName() {
		return name;
	}
	
	//Freeze this configuration.
	public void freeze(Object object) throws IOException {
		Tools.witeObjectToFile(object, this.frozenPath);
		
	}


	public abstract Object getEssence();




}
