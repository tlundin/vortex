package com.teraim.vortex.dynamic.workflow_abstracts;


public interface Filterable {
	public String getId();
	public void removeFilter(Filter f);
	public void addFilter(Filter f);
}
