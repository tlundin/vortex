package com.teraim.vortex.dynamic.types;

import java.io.Serializable;

public class PhotoMeta implements Serializable {
	private static final long serialVersionUID = -3400543797668108398L;
	public float top,left,bottom,right;
	
	public float getWidth()  {
		return right-left;
	}
	public float getHeight() {
		return bottom-top;
	}
}