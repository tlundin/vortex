package com.teraim.vortex.gis;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.types.SweLocation;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.gis.TrackerListener.GPS_State;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.non_generics.NamedVariables;
import com.teraim.vortex.utils.Geomatte;


public class Tracker extends Service implements LocationListener {

	List<TrackerListener> mListeners = null; 

	// flag for GPS status
	boolean isGPSEnabled = false;

	// flag for network status
	boolean isNetworkEnabled = false;

	boolean canGetLocation = false;

	Location location; // location
	double latitude; // latitude
	double longitude; // longitude

	// The minimum distance to change Updates in meters
	private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 1 meters

	// The minimum time between updates in milliseconds
	private static final long MIN_TIME_BW_UPDATES = 1000 * 5; // 1 minute


	// Declaring a Location Manager
	protected LocationManager locationManager;

	private final Variable myX,myY;

	private final Map<String,String>YearKeyHash = new HashMap<String,String>();
	
	public Tracker() {
		YearKeyHash.put("�r", Constants.getYear());
		myX = GlobalState.getInstance().getVariableConfiguration().getVariableUsingKey(YearKeyHash, NamedVariables.MY_GPS_LAT);
		myY = GlobalState.getInstance().getVariableConfiguration().getVariableUsingKey(YearKeyHash, NamedVariables.MY_GPS_LONG);

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
					isNetworkEnabled = false; 
					//locationManager
					//		.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

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
		try {
		//Log.d("vortex","got new coords: "+location.getLatitude()+","+location.getLongitude());
		if (myX!=null) {
			//Log.d("vortex","setting sweref location");
			SweLocation myL = Geomatte.convertToSweRef(location.getLatitude(),location.getLongitude());
			/*
			String oldX = myX.getValue();
			String oldY = myY.getValue();
			if (oldX!=null&&oldY!=null) {
				double oldXd = Double.parseDouble(oldX);
				double oldYd = Double.parseDouble(oldY);
				double distx = Math.abs(oldXd-myL.getX());
				double disty = Math.abs(oldYd-myL.getY());
				
				Log.d("vortex","Distance between mesaurements in Tracker: (x,y) "+distx+","+disty);
				myX.setValue(myL.getX()+"");
				myY.setValue(myL.getY()+"");								
			} else {
				myX.setValue(myL.getX()+"");
				myY.setValue(myL.getY()+"");
			}
			*/
			myX.setValue(myL.getX()+"");
			myY.setValue(myL.getY()+"");
			sendMessage(GPS_State.newValueReceived);
			
		}
		}catch(Exception e) {
			LoggerI o = GlobalState.getInstance().getLogger();
			o.addRow("");
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);		
			o.addRedText(sw.toString());
			e.printStackTrace();
		}
	}



	private void sendMessage(GPS_State newState) {
		if (mListeners!=null) {
			for (TrackerListener gl:mListeners) 
				gl.gpsStateChanged(newState);
		}
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		switch(status) 
        {
            case GpsStatus.GPS_EVENT_STARTED:
                //System.out.println("TAG - GPS searching: ");                        
                break;
            case GpsStatus.GPS_EVENT_STOPPED:    
               // System.out.println("TAG - GPS Stopped");
                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
            	 System.out.println("FIX!");
                /*
                 * GPS_EVENT_FIRST_FIX Event is called when GPS is locked            
                 */
                    Location gpslocation = locationManager
                            .getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    if(gpslocation != null)
                    {       
                    System.out.println("GPS Info:"+gpslocation.getLatitude()+":"+gpslocation.getLongitude());

                    /*
                     * Removing the GPS status listener once GPS is locked  
                     */
                        //locationManager.removeGpsStatusListener(mGPSStatusListener);                
                    }               

                break;
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
 //                 System.out.println("TAG - GPS_EVENT_SATELLITE_STATUS");
                break;                  
       }
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.d("vortex","Provider enabled in gps listener");
		sendMessage(GPS_State.enabled);
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.d("vortex","Provider disabled in gps listener");
		sendMessage(GPS_State.disabled);
	}
	public void stopUsingGPS(){
		if(locationManager != null){
			locationManager.removeUpdates(Tracker.this);
		}       
	}
	
	public void registerListener(TrackerListener tl) {
		if (mListeners==null)
			mListeners = new ArrayList<TrackerListener>();
		mListeners.add(tl);
	}

}
