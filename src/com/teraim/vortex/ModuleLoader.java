package com.teraim.vortex;

import java.io.IOException;
import java.util.List;

import android.util.Log;

import com.teraim.vortex.dynamic.types.ConfigurationModule;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.utils.LoadResult;
import com.teraim.vortex.utils.LoadResult.ErrorCode;
import com.teraim.vortex.utils.PersistenceHelper;

public class ModuleLoader implements FileLoadedCb{
	private List<ConfigurationModule> myModules;
	private LoggerI o;
	private PersistenceHelper globalPh;
	public ModuleLoader(List<ConfigurationModule> currentlyKnownModules, LoggerI o, PersistenceHelper glPh) {
		myModules = currentlyKnownModules;
		this.o=o;
		globalPh = glPh;
	}

	public void loadModulesIfRequired() {
		o.addRow("Loading Modules for "+globalPh.get(PersistenceHelper.BUNDLE_NAME));
		Log.d("vortex","Loading Modules for "+globalPh.get(PersistenceHelper.BUNDLE_NAME));

		for (ConfigurationModule module:myModules) {
			if (!module.isLoaded())
				module.load(this);
			else {
				o.addRow(module.fileName+": [");
				o.addYellowText(module.version);
				o.addText("] ");
				o.addGreenText("(OK)");			
			}
		}
	}



	@Override
	public void onUpdate() {
		o.ticky();
	}

	@Override
	public void onFileLoaded(LoadResult res) {
		Log.d("vortex","got back!!!");
	}

	@Override
	public void onFileLoaded(ErrorCode errCode, String version) {
		// TODO Auto-generated method stub

	}

}
