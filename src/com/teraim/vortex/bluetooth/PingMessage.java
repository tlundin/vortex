package com.teraim.vortex.bluetooth;


public class PingMessage extends SyncMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String partner,partnerLagId,bundleVersion,softwareVersion;
	private boolean requestAll; 
	public PingMessage(String partner, String partnerLagId, String bundleVersion, String softwareVersion,boolean requestAll) {
		this.partner = partner;
		this.partnerLagId = partnerLagId;
		this.bundleVersion=bundleVersion;
		this.softwareVersion=softwareVersion;
		this.requestAll=requestAll;
	}
	
	public String getPartner() {
		return partner;
	}
	
	public String getPartnerLagId() {
		return partnerLagId;
	}
	
	public String getSoftwareVersion() {
		return softwareVersion;
	}
	
	public String getbundleVersion() {
		return bundleVersion;
	}
	
	public boolean requestAll() {
		return requestAll;
	}
}
