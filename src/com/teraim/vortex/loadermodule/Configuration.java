package com.teraim.vortex.loadermodule;

import java.util.List;


public class Configuration {
	
	private List<ConfigurationModule> mModules;
	private int current=0;
	
	public Configuration(List<ConfigurationModule> modules) {
		mModules=modules;
	}

	public ConfigurationModule getCurrent() {
		if (current<mModules.size())
			return mModules.get(current);
		return null;
	}
	
	public boolean next() {
		current++;
		return current<mModules.size();
	}
	
	public ConfigurationModule getModule(String moduleName) {
		for(ConfigurationModule m:mModules) 
			if (m.getName().equals(moduleName))
				return m;
		return null;
	}
	
}
