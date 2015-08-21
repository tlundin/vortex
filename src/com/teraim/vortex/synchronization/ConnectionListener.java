package com.teraim.vortex.synchronization;

public interface ConnectionListener {

	public enum ConnectionEvent {
		connectionGained,
		connectionBroken,
		connectionFailed,
		connectionClosedGracefully,
		connectionStatus, 
		connectionAttemptFailed, 
		restartRequired, 
		connectionFailedNoPartner, 
		connectionFailedNamedPartnerMissing, connectionError
	}
	
	public void handleMessage(Object o);
	public void handleEvent(ConnectionEvent e);
	
	
}
