package com.teraim.vortex.dynamic.types;

public class GisLayer {

	String name, label;
	boolean isVisible, hasWidget;
	public GisLayer(String name, String label, boolean isVisible,
			boolean hasWidget) {
		super();
		this.name = name;
		this.label = label;
		this.isVisible = isVisible;
		this.hasWidget = hasWidget;
	}

}
