package com.teraim.vortex.synchronization;


public class PingMessage extends SyncMessage {

	private static final long serialVersionUID = 1595706891719878226L;
	/**
	 * 
	 */

	private String partnerApp,partner,partnerTeam;
	float appVersion,softwareVersion;
	private boolean requestAll; 
		
	public PingMessage(String name, String app, String team,
			float appVersion, float softwareVersion, boolean requestAll) {
		this.partner = name;
		this.partnerApp = app;
		this.partnerTeam = team;
		this.appVersion=appVersion;
		this.softwareVersion=softwareVersion;
		this.requestAll=requestAll;
		
	}

	public String getPartner() {
		return partner;
	}
	
	
	public String getPartnerTeam() {
		return partnerTeam;
	}
	
	public float getSoftwareVersion() {
		return softwareVersion;
	}
	
	public float getAppVersion() {
		return appVersion;
	}
	
	public boolean requestAll() {
		return requestAll;
	}

	public String getPartnerAppName() {
		return partnerApp;
	}
}
