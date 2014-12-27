package com.teraim.vortex;

public interface FileLoadedCb {

	enum ErrorCode {
		newConfigVersionLoaded,
		newVarPatternVersionLoaded,
		HistoricalLoaded,
		bothFilesLoaded,
		notFound,
		parseError,
		ioError,
		sameold, 
		whatever, 
		configurationError, 
		Aborted, 
		LoadInBackground
	}
	
	
	public void onFileLoaded(ErrorCode errCode, String newVersion);
	
}
