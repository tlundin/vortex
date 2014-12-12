package com.teraim.nils.utils;

import com.teraim.nils.FileLoadedCb.ErrorCode;

public class LoadResult {
	ErrorCode errCode;
	String version;
	
	public LoadResult(ErrorCode errC, String ver) {
		errCode = errC;
		version = ver;
	}
}
