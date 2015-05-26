package com.teraim.vortex.dynamic.types;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class CHash implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6300633169769731018L;

	
	public CHash(Map<String, String> keyHash, Map<String, Variable> rawHash) {
		this.keyHash = keyHash;
		this.rawHash = rawHash;
	}
	public CHash(String err) {
		this.err=err;
	}
	public String err = null;
	public Map<String, String> keyHash;
	public Map<String, Variable> rawHash;

	
}