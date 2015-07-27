package com.teraim.vortex.bluetooth;

import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.log.LoggerI;

public abstract class MessageHandler {


	protected LoggerI o;
	protected boolean initializationDone=false;
	protected int pingC = 0;
	protected GlobalState gs;

	public MessageHandler(GlobalState gs) {
		this.gs=gs;
	}

	public void resetPingCounter() {
		pingC=0;
	}

	public void handleMessage(Object message) {
		gs = GlobalState.getInstance();
		o = gs.getLogger();

		if (initializationDone) {
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
		} else
			handleSpecialized(message);
	}



	public abstract void handleSpecialized(Object message);





}
