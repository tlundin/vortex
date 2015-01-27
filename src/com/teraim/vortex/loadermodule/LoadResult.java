package com.teraim.vortex.loadermodule;

import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;

public class LoadResult {
	
	public enum ErrorCode {
		ok,
		newVarPatternVersionLoaded,
		loaded,
		bothFilesLoaded,
		notFound,
		parseError,
		ioError,
		sameold, 
		whatever, 
		configurationError, 
		Aborted, 
		LoadInBackground, newConfigVersionLoaded, badURL,frozen, parsed, noData, frozenWithoutVersion
	}
	
	
	public ErrorCode errCode;
	public ConfigurationModule module;
	public String errorMessage;


	public LoadResult(ConfigurationModule module,ErrorCode errC,String errM) {
		errCode = errC;
		this.module=module;
		errorMessage=errM;
	}

	public LoadResult(ConfigurationModule module, ErrorCode errC) {
		errCode = errC;
		this.module = module;
	}

	public LoadResult(ErrorCode configurationerror, Object object) {
		// TODO Auto-generated constructor stub
	}
}
