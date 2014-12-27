package com.teraim.vortex.utils;

import com.teraim.vortex.FileLoadedCb.ErrorCode;

public class LoadResult {
	ErrorCode errCode;
	String version;
	
	public LoadResult(ErrorCode errC, String ver) {
		errCode = errC;
		version = ver;
	}
}
