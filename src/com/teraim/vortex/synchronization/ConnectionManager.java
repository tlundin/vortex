package com.teraim.vortex.synchronization;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.teraim.vortex.GlobalState;

/**
 * 
 * @author Terje
 * 
 * ConnectionManager handles connections and provides one on request.
 * 
 */
public class ConnectionManager {

	private final GlobalState gs;
	private final List<ConnectionProvider> activeConnections = new ArrayList<ConnectionProvider>();
	
	//This is a singleton owned by GlobalState.
	public ConnectionManager(GlobalState gs) {
		this.gs=gs;
		
	}
	
	
	/**
	 * 
	 * @Connection Listener l
	 * 
	 * A request for a connection that does not specify type. If one is available, return first. Otherwise, create.
	 * Currently, only bluetooth is supported.
	 */
	public ConnectionProvider requestConnection(Context ctx) {
		if (activeConnections.isEmpty()) {
			activeConnections.add(new BluetoothConnectionProvider(ctx));
			
		} 
		return activeConnections.get(0);
	}


	public void releaseConnection(ConnectionProvider mConnection) {
		if (activeConnections!=null) {
			activeConnections.remove(mConnection);
			
		}
	}
}
