package com.teraim.vortex.bluetooth;


public class PingMessage extends SyncMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String partner,partnerLagId,bundleVersion,softwareVersion;
	
	public PingMessage(String partner, String partnerLagId, String bundleVersion, String softwareVersion) {
		this.partner = partner;
		this.partnerLagId = partnerLagId;
		this.bundleVersion=bundleVersion;
		this.softwareVersion=softwareVersion;
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
}
