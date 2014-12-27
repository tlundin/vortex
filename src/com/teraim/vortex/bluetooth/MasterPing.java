package com.teraim.vortex.bluetooth;

public class MasterPing extends PingMessage  {
	
	private String ruta,provyta;
	
	public MasterPing(String ruta, String provyta,String partner, String lagId) {
		super (partner,lagId);
		this.ruta=ruta;
		this.provyta=provyta;
	}

	/**
	 * @return the ruta
	 */
	public String getRuta() {
		return ruta;
	}

	/**
	 * @return the provyta
	 */
	public String getProvyta() {
		return provyta;
	}
	
}

