package com.teraim.vortex.bluetooth;

public class MasterPing extends PingMessage  {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7192503893294648764L;

	public MasterPing(String partner, String lagId, String bundleVersion, String softwareVersion,boolean requestAll) {
		super (partner,lagId,bundleVersion,softwareVersion,requestAll);		
	}

	
}

