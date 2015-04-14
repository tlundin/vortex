package com.teraim.vortex.dynamic.types;


import java.io.Serializable;
import java.util.HashMap;

public class CHash implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6300633169769731018L;

	
	public CHash(HashMap<String, String> keyHash, HashMap<String, Variable> rawHash) {
		this.keyHash = keyHash;
		this.rawHash = rawHash;
	}
	public CHash(String err) {
		this.err=err;
	}
	public String err = null;
	public HashMap<String, String> keyHash;
	public HashMap<String, Variable> rawHash;

	
}