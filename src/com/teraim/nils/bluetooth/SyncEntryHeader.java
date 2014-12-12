package com.teraim.nils.bluetooth;

public class SyncEntryHeader extends SyncEntry {
	
	private static final long serialVersionUID = -6519809807746304800L;
	
	public long timeStampOfLastEntry=-1;
	
	public SyncEntryHeader(long maxStamp) {
		this.timeStampOfLastEntry=maxStamp;
	}
	
	
}
