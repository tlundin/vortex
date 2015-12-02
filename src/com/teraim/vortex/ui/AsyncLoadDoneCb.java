package com.teraim.vortex.ui;

import java.util.List;

import com.teraim.vortex.loadermodule.ConfigurationModule;

public interface AsyncLoadDoneCb {
	public void onLoadSuccesful(List<ConfigurationModule> modules);
	
}
