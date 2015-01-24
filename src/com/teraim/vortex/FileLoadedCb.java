package com.teraim.vortex;

import com.teraim.vortex.dynamic.types.ConfigurationModule;
import com.teraim.vortex.utils.LoadResult;
import com.teraim.vortex.utils.LoadResult.ErrorCode;

public interface FileLoadedCb {

	
	
	
	public void onUpdate();

	public void onFileLoaded(LoadResult res);

	public void onFileLoaded(ErrorCode errCode, String version);
	
}
