package com.teraim.vortex.utils;

import com.teraim.vortex.dynamic.types.ConfigurationModule;
import com.teraim.vortex.utils.LoadResult.ErrorCode;

public class LoadResult {
	
	public enum ErrorCode {
		ok,
		newVarPatternVersionLoaded,
		Loaded,
		bothFilesLoaded,
		notFound,
		parseError,
		ioError,
		sameold, 
		whatever, 
		configurationError, 
		Aborted, 
		LoadInBackground, newConfigVersionLoaded
	}
	
	
	ErrorCode errCode;
	String version;
	ConfigurationModule module;

	
	public LoadResult(ConfigurationModule module,ErrorCode errC) {
		errCode = errC;
		this.module=module;
	}


	public LoadResult(ErrorCode parseerror, Object errC) {
		// TODO Auto-generated constructor stub
	}
}
