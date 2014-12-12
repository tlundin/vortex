package com.teraim.nils.bluetooth;

public class SyncSuccesful extends SyncMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5372243649670548590L;

	private long timeStamp =-1;
	public SyncSuccesful(long lastEntrySent) {
		this.timeStamp=lastEntrySent;
	}
	
	public long getLastEntrySent() {
		return timeStamp;
	}
}
