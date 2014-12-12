package com.teraim.nils.bluetooth;

import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.teraim.nils.GlobalState;
import com.teraim.nils.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.nils.log.LoggerI;
import com.teraim.nils.utils.PersistenceHelper;

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
		else if (message instanceof LinjeStarted) {
			//Toast.makeText(gs.getContext(), "Andra dosan startade linje", Toast.LENGTH_SHORT).show();
			gs.setMsg(message);
			o.addRow("");
			o.addGreenText("[BT MESSAGE -->Received LinjeStarted message!]");
			gs.sendEvent(BluetoothConnectionService.LINJE_STARTED);
		}
		else if (message instanceof LinjeDone) {
			gs.setMsg(message);
			o.addRow("");
			o.addGreenText("[BT MESSAGE -->Received Linje_Done message!]");
			gs.sendEvent(BluetoothConnectionService.LINJE_DONE);
		}
		else
			handleSpecialized(message);
	}
	
	

	public abstract void handleSpecialized(Object message);
	
	
	
	
	
}
