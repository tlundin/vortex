package com.teraim.vortex.bluetooth;



public class SlavePing extends PingMessage {

	
	private static final long serialVersionUID = -8117606619445790130L;

	public SlavePing(String name, String lagId) {
		super(name,lagId);
	}
}