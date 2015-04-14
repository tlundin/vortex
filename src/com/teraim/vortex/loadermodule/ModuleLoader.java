package com.teraim.vortex.loadermodule;

import java.util.List;

import android.content.Context;
import android.util.Log;

import com.teraim.vortex.FileLoadedCb;
import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.utils.PersistenceHelper;
import com.teraim.vortex.utils.Tools;

public class ModuleLoader implements FileLoadedCb{
	private Configuration myModules;
	private LoggerI o,debug;
	private PersistenceHelper globalPh;
	private ModuleLoaderListener caller;
	private String loaderId;
	private Context ctx;

	public interface ModuleLoaderListener {
		
		void loadSuccess(String loaderId);
	}
	
	
	
	public ModuleLoader(String loaderId, Configuration currentlyKnownModules, LoggerI o, PersistenceHelper glPh, LoggerI debugConsole,ModuleLoaderListener caller,Context ctx) {
		myModules = currentlyKnownModules;
		this.o=o;
		this.debug=debugConsole;
		globalPh = glPh;
		this.caller = caller;
		this.loaderId=loaderId;
		this.ctx=ctx;
	}

	public void loadModules() {

		ConfigurationModule module;
		//check if any module needs to load.
		if (myModules.hasNext()) {
			module = myModules.next();
			o.addRow(module.getLabel()+" :");
			Log.d("vortex",module.getLabel()+" :");
			module.load(this);
		} else 
			caller.loadSuccess(loaderId);
			
	}




	@Override
	public void onUpdate(Integer ...args) {
		if (args.length==1)
			o.writeTicky(" "+args[0].toString());
		else
			o.writeTicky(" "+args[0].toString()+"/"+args[1].toString());

	}

	@Override
	public void onFileLoaded(LoadResult res) {
		o.removeTicky();
		ConfigurationModule module = res.module;
		if (module != null) { 
			debug.addRow("Module "+res.module.fileName+" loaded. Returns code "+res.errCode.name()+(res.errorMessage!=null?" and errorMessage: "+res.errorMessage:""));
			Log.d("vortex","Module "+res.module.fileName+" loaded. Returns code "+res.errCode.name()+(res.errorMessage!=null?" and errorMessage: "+res.errorMessage:""));
			o.addText(" [");
			if (module.version!=null)
				o.addText(module.version);
			else
				o.addText("?");
			o.addText("]");

			switch (res.errCode) {
			
			case sameold:
				if (thawModule(module)) {
					module.setLoaded();
					o.addText(" No update");
				} else { 
					o.addYellowText(" fail..retrying..");
					module.setFrozenVersion(null);
					module.load(this);
					o.draw();
					return;
					//failing thaw will lead to retry.
				}
				break;
			case frozen:
			case nothingToFreeze:
				module.setLoaded();
				o.addGreenText(" New!");
				break;
			case Unsupported:
			case IOError:
			case BadURL:
			case ParseError:
			case Aborted:
			case noData:
				if (module.isRequired()&&!module.frozenFileExists()) {
					o.addRedText(" !");o.addText(res.errCode.name());o.addRedText("!");
					o.addRow("Upstart aborted..Unable to load mandatory file.");
					printError(res.errCode);
					o.draw();
					return;
				} else {
					if (res.errCode!=ErrorCode.IOError)
						o.addYellowText(" "+res.errCode.name());
				}
				printError(res.errCode);
				if (module.frozenFileExists()) {
					if (thawModule(module)) {
						o.addRow("");o.addYellowText("Current version will be used.");
					} 
				} else 
					module.setNotFound();
					
				break;
			default:
				o.addText("?: "+res.errCode.name());
				break;
			}				
			o.draw();
			loadModules();
		}

	}

	private void printError(ErrorCode errCode) {
		if (errCode==ErrorCode.IOError) {
			if (!Tools.isNetworkAvailable(ctx)) 
				o.addRow("No network");
			else
				o.addRow("File not found");
		} else if (errCode==ErrorCode.Unsupported) {
			o.addRow("The file contains instructions that this Vortex version is not able to run. Please upgrade.");
		} else if (errCode==ErrorCode.ParseError) {
			o.addRow("");
			o.addPurpleText("The file contains an error. Please check log for details");
		}

		
	}

	private boolean thawModule(ConfigurationModule module) {
		LoadResult res = module.thaw();
		if (res.errCode!=ErrorCode.thawed) {
			debug.addRow("");
			debug.addRedText("Failed to thaw module "+module.fileName+" due to "+res.errCode.name());
			Log.e("vortex","Failed to thaw module "+module.fileName+" due to "+res.errCode.name());
			//Remove the corrupt file and remove version.
			if (module.deleteFrozen())
				debug.addYellowText("Corrupt file has been deleted");
			//retry loading the file - don't set it loaded.
				return false;
		}
		debug.addRow("Module "+module.getFileName()+" was thawed.");
		return true;
	}

	//TODO: REMOVE
	public void onFileLoaded(ErrorCode errCode, String version) {}

	public void stop() {
		Log.e("vortex","In stop for "+loaderId);
		List<ConfigurationModule> list = myModules.getAll();
		for (ConfigurationModule module:list) {
			Log.e("vortex","Cancelling loader process for "+module.getFileName());
			module.cancelLoader();
		}
	}

	
}
