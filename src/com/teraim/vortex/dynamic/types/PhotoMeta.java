package com.teraim.vortex.dynamic.types;

import java.io.Serializable;

import android.util.Log;

public class PhotoMeta implements Serializable {
	private static final long serialVersionUID = -3400543797668108398L;
	public float top=0,left=0,bottom=0,right=0;


	public PhotoMeta(String topN,String topE,String bottomN,String bottomE) {
		try {
			top = Float.parseFloat(topN);
			left = Float.parseFloat(topE);
			bottom = Float.parseFloat(bottomN);
			right = Float.parseFloat(bottomE);
		} catch (NumberFormatException e) { Log.e("vortex","non number in gis bg coordinates"); };


	}
	public PhotoMeta(float topN,float topE,float bottomN,float bottomE) {
		this.top=topN;
		this.left=topE;
		this.bottom=bottomN;
		this.right=bottomE;
	}
	
	public float getWidth()  {
		return right-left;
	}
	public float getHeight() {
		return top-bottom;
	}
}