package com.teraim.vortex.gis;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.types.SweLocation;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.non_generics.NamedVariables;
import com.teraim.vortex.utils.Geomatte;


public class Tracker extends Service implements LocationListener {

	

	// flag for GPS status
	boolean isGPSEnabled = false;

	// flag for network status
	boolean isNetworkEnabled = false;

	boolean canGetLocation = false;

	Location location; // location
	double latitude; // latitude
	double longitude; // longitude

	// The minimum distance to change Updates in meters
	private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

	// The minimum time between updates in milliseconds
	private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

	private static final double Threshold = 3;

	// Declaring a Location Manager
	protected LocationManager locationManager;

	private final Variable myX,myY;

	public Tracker() {
		
		myX = GlobalState.getInstance().getVariableCache().getVariable(NamedVariables.MY_GPS_LAT);
		myY = GlobalState.getInstance().getVariableCache().getVariable(NamedVariables.MY_GPS_LONG);

	}

	public enum ErrorCode {
		GPS_VARS_MISSING,
		GPS_NOT_ON,
		UNSTABLE,
		GPS_NOT_ENABLED,
		GPS_OK
	}

	public ErrorCode startScan(Context ctx) {
		//do we have variables?	
				if(myX==null||myY==null)
					return ErrorCode.GPS_VARS_MISSING;
				//does Globalstate exist?
				GlobalState gs = GlobalState.getInstance();
				if (gs==null)
					return ErrorCode.UNSTABLE;

				try {
					locationManager = (LocationManager) ctx
							.getSystemService(LOCATION_SERVICE);

					// getting GPS status
					isGPSEnabled = locationManager
							.isProviderEnabled(LocationManager.GPS_PROVIDER);

					// getting network status
					isNetworkEnabled = locationManager
							.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

					if (!isGPSEnabled && !isNetworkEnabled) {
						return ErrorCode.GPS_NOT_ENABLED;
					} else {
						this.canGetLocation = true;
						// First get location from Network Provider
						if (isNetworkEnabled) {
							locationManager.requestLocationUpdates(
									LocationManager.NETWORK_PROVIDER,
									MIN_TIME_BW_UPDATES,
									MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
							Log.d("Network", "Network");
							if (locationManager != null) {
								location = locationManager
										.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
								if (location != null) {
									latitude = location.getLatitude();
									longitude = location.getLongitude();
								}
							}
						}
						// if GPS Enabled get lat/long using GPS Services
						if (isGPSEnabled) {
							if (location == null) {
								locationManager.requestLocationUpdates(
										LocationManager.GPS_PROVIDER,
										MIN_TIME_BW_UPDATES,
										MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
								Log.d("GPS Enabled", "GPS Enabled");
								/*	                        if (locationManager != null) {
	                            location = locationManager
	                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
	                            if (location != null) {
	                                latitude = location.getLatitude();
	                                longitude = location.getLongitude();
	                            }
	                        } */
							}
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

				return ErrorCode.GPS_OK;
	}
	@Override
	public void onLocationChanged(Location location) {
		Log.d("vortex","got new coords: "+location.getLatitude()+","+location.getLongitude());
		if (myX!=null) {
			Log.d("vortex","setting sweref location");
			SweLocation myL = Geomatte.convertToSweRef(location.getLatitude(),location.getLongitude());
			
			String oldX = myX.getValue();
			String oldY = myY.getValue();
			if (oldX!=null&&oldY!=null) {
				double oldXd = Double.parseDouble(oldX);
				double oldYd = Double.parseDouble(oldY);
				double distx = Math.abs(oldXd-myL.getX());
				double disty = Math.abs(oldYd-myL.getY());
				if (distx>Threshold || disty>Threshold) {
					Log.d("vortex","Measured distance in Tracker: (x,y) "+distx+","+disty);
					myX.setValue(myL.getX()+"");
					myY.setValue(myL.getY()+"");					
				} else 
					Log.d("vortex","no change...diff was only (x,y)"+distx+","+disty);
			} else {
				myX.setValue(myL.getX()+"");
				myY.setValue(myL.getY()+"");
			}
			
		}
	}



	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.d("vortex","Provider enabled in gps listener");
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.d("vortex","Provider disabled in gps listener");

	}
	public void stopUsingGPS(){
		if(locationManager != null){
			locationManager.removeUpdates(Tracker.this);
		}       
	}

}
