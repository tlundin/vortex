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
			module = myModules.next();
		if (module!=null) {
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
			

			switch (res.errCode) {
			case existingVersionIsMoreCurrent:
			case sameold:
				if (thawModule(module)) {
					module.setLoaded(true);
					if (res.errCode==ErrorCode.existingVersionIsMoreCurrent) {
						o.addRow("");
						o.addText(" Remote version older: [");
						o.addRedText(res.errorMessage);
						o.addText("]");
					}
					else 
						o.addText(" No update");
					o.addRow("");
					o.addYellowText("Current version will be used: "+module.getFrozenVersion());
				} else { 
					Log.d("vortex","Retrying.");
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
				module.setLoaded(true);
				o.addGreenText(" New!");
				o.addText(" [");
				if (module.version!=null)
					o.addText(module.version);
				else
					o.addText("?");
				o.addText("]");
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
					printError(res);
					o.draw();
					return;
				} else {
					if (res.errCode!=ErrorCode.IOError)
						o.addYellowText(" "+res.errCode.name());
				}
				printError(res);
				if (module.frozenFileExists()) {
					if (thawModule(module)) {
						o.addRow("");o.addYellowText("Current version will be used: "+module.getFrozenVersion());
						module.setLoaded(true);
					} 
				} else 
					module.setNotFound();
					
				break;
			case reloadDependant:
				Log.d("vortex","Dependant ["+res.errorMessage+"] needs to be reloaded.");
				ConfigurationModule reloadModule = myModules.getModule(res.errorMessage);
				if (reloadModule!=null) {
					reloadModule.setLoaded(false);
					reloadModule.setFrozenVersion(null);
					module.setFrozenVersion(null);
					module.setLoaded(false);
					Log.d("vortex","Now retry load of modules");
					o.addRow("");
					o.addRedText("Reload required for dependant "+res.errorMessage);
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

	private void printError(LoadResult res) {
		ErrorCode errCode = res.errCode;
		if (errCode==ErrorCode.IOError) {
			if (ctx!=null && !Tools.isNetworkAvailable(ctx)) 
				o.addRow("No network");
			else
				o.addRow("File not found");
		} else if (errCode==ErrorCode.Unsupported) {
			o.addRow("Remote file requires Vortex version ["+res.errorMessage+"]. Please upgrade.");
		} else if (errCode==ErrorCode.ParseError) {
			o.addRow("");
			o.addRedText("The file contains an error. Please check log for details");
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
