package com.teraim.vortex.loadermodule;

import java.util.List;

import android.util.Log;

import com.teraim.vortex.FileLoadedCb;
import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.utils.PersistenceHelper;

public class ModuleLoader implements FileLoadedCb{
	private Configuration myModules;
	private LoggerI o,debug;
	private PersistenceHelper globalPh;
	private ModuleLoaderListener caller;
	private String loaderId;

	public interface ModuleLoaderListener {
		
		void loadSuccess(String loaderId);
	}
	
	
	
	public ModuleLoader(String loaderId, Configuration currentlyKnownModules, LoggerI o, PersistenceHelper glPh, LoggerI debugConsole,ModuleLoaderListener caller) {
		myModules = currentlyKnownModules;
		this.o=o;
		this.debug=debugConsole;
		globalPh = glPh;
		this.caller = caller;
		this.loaderId=loaderId;
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
				if (module.isRequired()) {
					o.addRedText(" !");o.addText(res.errCode.name());o.addRedText("!");
				}
				if (res.errCode==ErrorCode.Unsupported) {
					o.addRow("The file contains instructions that this Vortex version is not able to run. Please upgrade.");
				}
				if (module.frozenFileExists()) {
					if (thawModule(module)) {
						o.addRow("");o.addYellowText("Current version will be used.");
					} else {
						if (module.isRequired()) {
							o.addRow("Upstart aborted..could neither load file from Net or Disk");
							o.draw();
							return;
						}
					}
				} else 
					if (module.isRequired()) {
						o.addRow("Upstart aborted..could neither load file from Net or Disk");
						o.draw();
						return;
					} else {
						o.addText(" Not loaded");
						module.setNotFound();
					}
				break;
			default:
				o.addText("?: "+res.errCode.name());
				break;
			}
				
			o.draw();
			loadModules();
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
