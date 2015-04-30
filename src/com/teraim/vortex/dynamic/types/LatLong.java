package com.teraim.vortex.dynamic.types;



public class LatLong implements Location {

	public final double	latitude;
	public final double longitude;

		
	public LatLong(double latitude, double longitude) {
		this.latitude=latitude;
		this.longitude=longitude;
	}


	@Override
	public double getX() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public double getY() {
		// TODO Auto-generated method stub
		return 0;
	}
}
