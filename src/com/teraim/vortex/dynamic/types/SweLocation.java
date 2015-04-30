package com.teraim.vortex.dynamic.types;

import android.util.Log;

public class SweLocation implements Location {

	public final double north;
	public final double east;
	
	public SweLocation(double north,double east) {
		this.north=north;
		this.east=east;
	}

	//Y=N-S, X=E-W
	public SweLocation(String Y, String X) {
		if (Y==null||X==null) {
			north=-1;
			east=-1;
			Log.e("vortex","null value in sweloc constructor! "+X+" "+Y);
			return;
		}
		this.north=Double.parseDouble(Y);
		this.east=Double.parseDouble(X);
	}
	
	public double getX() {
		return east;
	}
	public double getY() {
		return north;
	}
}
