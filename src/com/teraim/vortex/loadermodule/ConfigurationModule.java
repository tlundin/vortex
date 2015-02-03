package com.teraim.vortex.loadermodule;

import java.io.File;
import java.io.IOException;

import android.provider.MediaStore.Files;
import android.util.Log;

import com.teraim.vortex.FileLoadedCb;
import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;
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
	public String rawData,version,fileName,fullPath,printedLabel,frozenPath;
	protected PersistenceHelper globalPh,ph;
	protected boolean IamLoaded=false,versionControl;

	private Integer linesOfRawData;
	protected Object essence;
	private boolean notFound=false;
	//freezeSteps contains the number of steps required to freeze the object. Should be -1 if not set specifically by specialized classes.
	protected int freezeSteps=-1;
	//tells if this module is stored on disk or db.
	protected boolean isDatabaseModule = false,hasSimpleVersion=true;
	
	public ConfigurationModule(PersistenceHelper gPh,PersistenceHelper ph, Type type, Source source, String urlOrPath,String fileName,String moduleName) {
		this.source=source;
		this.type=type;
		this.urlOrPath=urlOrPath;
		this.fileName=fileName;
		this.globalPh=gPh;
		this.ph=ph;
		this.printedLabel=moduleName;
		fullPath = urlOrPath+fileName+"."+type.name();
		frozenPath = Constants.VORTEX_ROOT_DIR+gPh.get(PersistenceHelper.BUNDLE_NAME)+"/config/"+fileName;
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
	}
	
	public void setNotFound() {
		notFound=true;
	}
	
	protected abstract void setFrozenVersion(String version);

	public abstract boolean isRequired();

	public boolean isLoaded() {
		// :)
		return IamLoaded||notFound;
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

	
	public String getFileName() {
		return fileName;
	}
	
	public String getLabel() {
		return printedLabel;
	}
	
	//Freeze this configuration. counter is used by some dependants.
	public boolean freeze(int counter) throws IOException {
		this.setEssence();
		if (essence!=null) {
			Tools.witeObjectToFile(essence, this.frozenPath);
			return true;
		}
		else
			return false;
	}
	
	public LoadResult thaw(){
		//A database module is by default saved already.
		if (isDatabaseModule)
			return new LoadResult(this,ErrorCode.thawed);
		try {
			essence = Tools.readObjectFromFile(this.frozenPath);
			if (essence==null)
				Log.e("vortex","nä men va faaan!!");
		} catch (IOException e) {
			return new LoadResult(this,ErrorCode.IOError,"Failed to load frozen object");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return new LoadResult(this,ErrorCode.ClassNotFound,"Failed to load frozen object");
		}
		return new LoadResult(this,ErrorCode.thawed);
		
	}

	public Object getEssence() {
		return essence;
	}
	
	//Must set essence before freeze.
	public abstract void setEssence();


	public boolean deleteFrozen() {
		return new File(this.frozenPath).delete();
	}
	


	




}
