package com.teraim.vortex.bluetooth;

import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.utils.PersistenceHelper;

public abstract class MessageHandler {

	private static final String WhatEver = null;
	private static final String SYNC_SOURCE = "SYNC_SOURCE";
	protected GlobalState gs;
	protected LoggerI o;
	
	protected int pingC = 0;

	public MessageHandler(GlobalState gs) {
		this.gs=gs;
	}
	
	public void resetPingCounter() {
		pingC=0;
	}
	
	public void handleMessage(Object message) {
		o = gs.getLogger();
		if (message instanceof SyncSuccesful) {
			SyncSuccesful ssf = (SyncSuccesful)message;			
			if (ssf!=null && ssf.getLastEntrySent()>0) {
				gs.getDb().syncDone(ssf.getLastEntrySent());
				gs.sendEvent(BluetoothConnectionService.SYNK_DATA_TRANSFER_DONE);
				o.addRow("");
				o.addGreenText("[BT MESSAGE -->Received SyncSuccesful message!]");
			} else 
				Log.d("nils","syncsuccessful null or -1");
			
		}
		else if (message instanceof EnvelopedMessage) {
			//Open envelope, and save in globalstate.
			gs.setSyncMessage(((EnvelopedMessage)message).getMessage());
			o.addRow("");
			o.addGreenText("[BT MESSAGE -->Received message: "+gs.getOriginalMessage().toString()+"]");
			gs.sendEvent(BluetoothConnectionService.BLUETOOTH_MESSAGE_RECEIVED);
		}
		
		else
			handleSpecialized(message);
	}
	
	

	public abstract void handleSpecialized(Object message);
	
	
	
	
	
}