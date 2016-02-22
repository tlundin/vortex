package com.teraim.fieldapp.dynamic.types;



public class LatLong implements Location {

	public final double	latitude;
	public final double longitude;

		
	public LatLong(double latitude, double longitude) {
		this.latitude=latitude;
		this.longitude=longitude;
	}
	public LatLong(String latitude, String longitude) {
		this.latitude=Double.parseDouble(latitude);
		this.longitude=Double.parseDouble(longitude);
	}

	@Override
	public double getX() {
		
		return latitude;
	}


	@Override
	public double getY() {
		
		return longitude;
	}
	
	@Override
	public String toString() {
		return getX()+","+getY();
	}
}
