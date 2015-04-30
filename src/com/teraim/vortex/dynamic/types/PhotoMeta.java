package com.teraim.vortex.dynamic.types;

import java.io.Serializable;

import android.util.Log;

public class PhotoMeta implements Serializable {
	private static final long serialVersionUID = -3400543797668108398L;
	public double top=0,left=0,bottom=0,right=0;


	public PhotoMeta(String topN,String topE,String bottomN,String bottomE) {
		try {
			top = Double.parseDouble(topN);
			left = Double.parseDouble(topE);
			bottom = Double.parseDouble(bottomN);
			right = Double.parseDouble(bottomE);
		} catch (NumberFormatException e) { Log.e("vortex","non number in gis bg coordinates"); };


	}
	public PhotoMeta(double topN,double topE,double bottomN,double bottomE) {
		this.top=topN;
		this.left=topE;
		this.bottom=bottomN;
		this.right=bottomE;
	}
	
	public double getWidth()  {
		return right-left;
	}
	public double getHeight() {
		return top-bottom;
	}
}