package com.teraim.vortex.gis;

public interface TrackerListener {

	public enum GPS_State {
		disabled,
		enabled,
		newValueReceived,
		ping
	}
	public void gpsStateChanged(GPS_State newState);
}
