package com.teraim.nils.bluetooth;


public class PingMessage extends SyncMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String partner,partnerLagId;
	
	public PingMessage(String partner, String partnerLagId) {
		this.partner = partner;
		this.partnerLagId = partnerLagId;
	}
	
	public String getPartner() {
		return partner;
	}
	
	public String getPartnerLagId() {
		return partnerLagId;
	}
}
