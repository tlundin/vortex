package com.teraim.vortex.dynamic.types;

import android.content.Context;

import com.teraim.vortex.FileLoadedCb;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.utils.LoadResult;
import com.teraim.vortex.utils.PersistenceHelper;
import com.teraim.vortex.utils.LoadResult.ErrorCode;

public class SpinnerConfiguration extends ConfigurationModule {

	public SpinnerConfiguration(PersistenceHelper globalPh, String server, String bundle,Context ctx) {
		super(globalPh, Type.csv, Source.internet, server+bundle, "Spinners");
		
	}

	@Override
	public String getFrozenVersion() {
		return (globalPh.get(PersistenceHelper.CURRENT_VERSION_OF_SPINNERS));
	}

	@Override
	protected void setFrozenVersion(String version) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isRequired() {
		return false;
	}

	@Override
	protected LoadResult parse() {
		return new LoadResult(this,ErrorCode.ok);
	}

		

}
