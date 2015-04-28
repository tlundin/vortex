package com.teraim.vortex.dynamic.types;

public class SweLocation implements Location {

	public final double north;
	public final double east;
	
	public SweLocation(double north,double east) {
		this.north=north;
		this.east=east;
	}

}
