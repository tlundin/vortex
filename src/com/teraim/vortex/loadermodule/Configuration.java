package com.teraim.vortex.loadermodule;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;


public class Configuration {
	
	private List<ConfigurationModule> mModules;
	private int current=-1;
	
	public Configuration(List<ConfigurationModule> modules) {
		mModules=modules;
	}
	public Configuration(ConfigurationModule module) {
		mModules=new ArrayList<ConfigurationModule>();
		mModules.add(module);
	}
		
	
	public ConfigurationModule next() {
		current++;
		return mModules.get(current);
	}
	
	public ConfigurationModule getModule(String moduleName) {
		for(ConfigurationModule m:mModules) 
			if (m.getFileName().equals(moduleName))
				return m;
		return null;
	}
	public List<ConfigurationModule> getAll() {
		return mModules;
	}
	public boolean hasNext() {
		return current<mModules.size()-1;
	}
	public void flush() {
		Log.d("vortex","Calling flush!");
		current = -1;
	}
	
}
