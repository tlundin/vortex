package com.teraim.vortex;

import com.teraim.vortex.loadermodule.LoadResult;
import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;

public interface FileLoadedCb {

	public void onFileLoaded(LoadResult res);

	public void onFileLoaded(ErrorCode errCode, String version);

	void onUpdate(Integer... args);
	
}
