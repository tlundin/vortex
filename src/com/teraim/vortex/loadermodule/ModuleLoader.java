package com.teraim.vortex.loadermodule;

import android.util.Log;

import com.teraim.vortex.FileLoadedCb;
import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.utils.PersistenceHelper;

public class ModuleLoader implements FileLoadedCb{
	private Configuration myModules;
	private LoggerI o;
	private PersistenceHelper globalPh;
	public ModuleLoader(Configuration currentlyKnownModules, LoggerI o, PersistenceHelper glPh) {
		myModules = currentlyKnownModules;
		this.o=o;
		globalPh = glPh;
	}

	public void loadModulesIfRequired() {

		ConfigurationModule module;
		do {
			module = myModules.getCurrent();
			if (module!=null) {
			o.addRow(module.getName()+" :");
			if (!module.isLoaded()) {
				//This asynchronous call will return through callback
				module.load(this);
				break;
			}
			o.addRow(module.getName()+" :");
			o.addText(": [");
			o.addYellowText(module.version==null?"?":module.version);
			o.addText("] ");
			o.addGreenText("(OK)");
			o.draw();
			}
		} while (myModules.next()); 
	}




	@Override
	public void onUpdate(Integer ...args) {
		if (args.length==1)
			o.writeTicky(args[0].toString());
		else
			o.writeTicky(args[0].toString()+"/"+args[1].toString());
		
	}

	@Override
	public void onFileLoaded(LoadResult res) {
		o.removeTicky();
		Log.d("vortex","got back!!!");
		ConfigurationModule module = res.module;
		if (module != null) { 
			o.addText(" [");
			if (res.errCode==ErrorCode.frozen || res.errCode == ErrorCode.sameold || res.errCode == ErrorCode.frozenWithoutVersion) {
				//Now we can safely update the version of the module and set loaded. 
				module.setLoaded();
				if (module.version==null)
					o.addYellowText("?");
				else
					o.addGreenText(module.version);
				o.addText("] ");				
				switch (res.errCode) {
				case frozen:
					o.addGreenText("(New!)");
					break;
				case sameold:
					o.addGreenText("(OK)");
					break;
				case frozenWithoutVersion:
					o.addYellowText("(Ok)");
					break;
				}
				
				o.draw();
				if (myModules.next())
					loadModulesIfRequired();
			} else {
				//Not frozen..something happened..
				
				//frozen in earlier times?
				if (module.frozenFileExists()) {
					o.addYellowText(module.getFrozenVersion());
					o.addText("] ");
					o.addYellowText("(load failed but found frozen)");
					o.draw();
					module.setLoadedFromFrozenCopy();
					//Any modules left to load?
					if (myModules.next())
						loadModulesIfRequired();
					//not frozen and error?
				} else {
					//Not ok if required!
					if (module.isRequired()) {
						o.addRedText("ERROR");
						o.addText("] ");
						o.addRedText("(load failed)");
						o.addRow("Application load failed due to missing required module");
						o.addRow("Error: "+res.errCode.name());
						o.draw();
						if (res.errorMessage!=null)
							o.addRow("Detail: "+res.errorMessage);

					} else {
						o.addYellowText("Not Found");
						o.addText("] ");
						o.addGreenText("(OK)");
						o.draw();
						module.setLoadedFromFrozenCopy();
						//Any modules left to load?
						if (myModules.next())
							loadModulesIfRequired();

					}
				}

			} 
				
		}
	}
	//TODO: REMOVE
	public void onFileLoaded(ErrorCode errCode, String version) {};
}
