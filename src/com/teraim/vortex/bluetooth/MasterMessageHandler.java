package com.teraim.vortex.bluetooth;

import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.PersistenceHelper;

public class MasterMessageHandler extends MessageHandler {


	

	public MasterMessageHandler(GlobalState gs) {
		super(gs);
	}

	@Override
	public void handleSpecialized(Object message) {
		
		if (!initializationDone) {
		if (message instanceof SlavePing) {
			SlavePing sp = (SlavePing)message;
			
			//Check version. If not same, send warning.
			String myBundleVersion = gs.getPreferences().get(PersistenceHelper.CURRENT_VERSION_OF_WF_BUNDLE);
			String mySoftwareVersion = Float.toString(gs.getGlobalPreferences().getF(PersistenceHelper.CURRENT_VERSION_OF_PROGRAM));
			if (!myBundleVersion.equals(sp.getbundleVersion()) ||
					!mySoftwareVersion.equals(sp.getSoftwareVersion())) {
				gs.sendEvent(BluetoothConnectionService.VERSION_MISMATCH);
				o.addRow("");
				o.addRedText("[BT MESSAGE -->PING. VERSION MISMATCH!");
				o.addRow("My vortex version: "+mySoftwareVersion);
				o.addRow("Others vortex version: "+sp.getSoftwareVersion());
				o.addRow("My bundle version: "+myBundleVersion);
				o.addRow("Others bundle version: "+sp.getbundleVersion());

			} else {
				o.addRow("");
				o.addGreenText("[BT MESSAGE -->PING. VERSIONS OK");
			}
			
			if (pingC++==0) {
			Log.d("nils","Received Ping from Slave");
			o.addRow("[BT MESSAGE--->PING]");				
			gs.setMyPartner(sp.getPartner());
			initializationDone=true;
			gs.triggerTransfer();
			} else 
				Log.e("nils","received extra slave ping");
			
		}  else if (message instanceof MasterPing) {
			o.addRow("");
			o.addRedText("[BT MESSAGE -->Got Master Ping. Both devices configured as Master");
			gs.sendEvent(BluetoothConnectionService.SAME_SAME_SYNDROME);
		} else {
			o.addRow("");
			o.addRedText("[BT MESSAGE -->Initialization not done! Discarding message");
		}
		} else {
		if (message instanceof SyncEntry[]) {
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

		} else {
			Log.e("vortex","Discarded message..");	
			o.addRow("[BT MESSAGE --> Discarded message!]");
		}

	}
	}


}
