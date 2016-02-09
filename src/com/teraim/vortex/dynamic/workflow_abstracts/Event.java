package com.teraim.vortex.dynamic.workflow_abstracts;

public abstract class Event {

	public enum EventType {
		onSave,
		onClick,
		onRedraw,
		onBluetoothMessageReceived,
		onAttach,onActivityResult, onContextChange
	}
	
	private  String generatorId;
	private  EventType myType;
	public final static String EXTERNAL_SOURCE = "ext";

	public Event (String fromId, EventType et) {
		this.generatorId = fromId;
		myType = et;
	}
	public EventType getType() {
		return myType;
	}
	
	public String getProvider() {
		return generatorId;
	}
	
	
}
