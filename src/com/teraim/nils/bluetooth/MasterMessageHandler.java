package com.teraim.nils.bluetooth;

import android.util.Log;

import com.teraim.nils.GlobalState;
import com.teraim.nils.non_generics.Constants;
import com.teraim.nils.utils.PersistenceHelper;

public class MasterMessageHandler extends MessageHandler {


	public MasterMessageHandler(GlobalState gs) {
		super(gs);
	}

	@Override
	public void handleSpecialized(Object message) {
		if (message instanceof SlavePing) {
			SlavePing sp = (SlavePing)message;
			if (pingC++==0) {
			Log.d("nils","Received Ping from Slave");
			o.addRow("[BT MESSAGE--->PING]");				
			gs.setMyPartner(sp.getPartner());
			if (gs.getPersistence().get(Constants.MOJO).equals(PersistenceHelper.UNDEFINED)) {
				Log.d("nils","Resetting timestamp of sync to 0 in master");
				gs.getPersistence().put(PersistenceHelper.TIME_OF_LAST_SYNC, Long.toString(0));
				gs.getPersistence().put(Constants.MOJO, "Lifeasweknowit");
			}				
			gs.triggerTransfer();
			} else 
				Log.e("nils","received extra slave ping");
			
		}  else if (message instanceof MasterPing) {
			o.addRow("");
			o.addRedText("[BT MESSAGE -->Got Master Ping. Both devices configured as Master");
			gs.sendEvent(BluetoothConnectionService.SAME_SAME_SYNDROME);
		}
		else if (message instanceof SyncEntry[]) {
			SyncEntry[] ses = (SyncEntry[])message;
			if (ses!=null && ses.length>0) {
				o.addRow("");
				o.addGreenText("[BT MESSAGE -->Recieved "+ses.length+" rows of data]");	
				gs.synchronise(ses,true);
				o.addRow("Trying to send SyncSuccesful Message to Slave");
				//Send back the timestamp i received in header.
				gs.sendMessage(new SyncSuccesful(((SyncEntryHeader)ses[0]).timeStampOfLastEntry));
				gs.sendEvent(BluetoothConnectionService.SYNK_DATA_RECEIVED);
			}

		}

	}


}
