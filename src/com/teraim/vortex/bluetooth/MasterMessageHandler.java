package com.teraim.vortex.bluetooth;

import java.io.IOException;

import android.util.Log;

import com.teraim.vortex.GlobalState.SyncStatus;
import com.teraim.vortex.utils.PersistenceHelper;

public class MasterMessageHandler extends MessageHandler {


	@Override
	public void handleSpecialized(Object message) {


		if (message instanceof SlavePing && gs.getSyncStatus()==SyncStatus.waiting_for_ping || gs.getSyncStatus()==SyncStatus.restarting) {
			SlavePing sp = (SlavePing)message;			
			//Check version. If not same, send warning.
			String myBundleVersion = gs.getPreferences().get(PersistenceHelper.CURRENT_VERSION_OF_WF_BUNDLE);
			String mySoftwareVersion = Float.toString(gs.getGlobalPreferences().getF(PersistenceHelper.CURRENT_VERSION_OF_PROGRAM));
			Log.d("vortex","myBundleV "+myBundleVersion+" msgVer "+sp.getbundleVersion()+" swVer: "+mySoftwareVersion+" msgVer: "+sp.getSoftwareVersion());
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


			Log.d("nils","Received Ping from Slave");
			o.addRow("[BT MESSAGE--->PING]");				
			gs.setMyPartner(sp.getPartner());
			if (sp.requestAll()) {
				gs.getPreferences().put(PersistenceHelper.TIME_OF_LAST_SYNC,PersistenceHelper.UNDEFINED);
				Log.d("vortex","Resetting DB Counter!");
				o.addRow("");
				o.addGreenText("Partner db empty. Resetting my DB Counter");
			}

			try {
				if (gs.triggerTransfer())
					gs.setSyncStatus(SyncStatus.waiting_for_ack);
				else {
					
					o.addRow("[SENDING_SYNC-->Empty package. No changes or error.]");
					gs.setSyncStatus(SyncStatus.sending);
					boolean success = BluetoothConnectionService.getSingleton().send(new NothingToSync());
					if (!success)
						bt.stop();
					else { 
						gs.setSyncStatus(SyncStatus.waiting_for_data);
						//No data ... if time of lastsync is underfined, set it to 0 to avoid endless loop of messages..
						if (gs.getPreferences().get(PersistenceHelper.TIME_OF_LAST_SYNC).equals(PersistenceHelper.UNDEFINED)) {
							Log.d("vortex","time of last sync set to 0");							
							gs.getPreferences().put(PersistenceHelper.TIME_OF_LAST_SYNC,"0");
						}
					}
				}
			} catch(IOException e) {
				Log.e("vortex","Exception when sending message...aborting");
				bt.stop();
			}

		}  else if (message instanceof MasterPing) {
			o.addRow("");
			o.addRedText("[BT MESSAGE -->Got Master Ping. Both devices configured as Master");
			gs.sendEvent(BluetoothConnectionService.SAME_SAME_SYNDROME);
		} else if (message instanceof SyncSuccesful && gs.getSyncStatus()==SyncStatus.waiting_for_ack ) {
			SyncSuccesful ssf = (SyncSuccesful)message;		
			Log.d("vortex","[BT MESSAGE -->Received SyncSuccesful message]");
			if (ssf!=null && ssf.getLastEntrySent()>0) {
				gs.getDb().syncDone(ssf.getLastEntrySent());
				o.addRow("");
				o.addGreenText("[BT MESSAGE -->Received SyncSuccesful message!]");
			} else 
				Log.d("nils","syncsuccessful null or -1");
			gs.setSyncStatus(SyncStatus.waiting_for_data);

		} else if (message instanceof NothingToSync && gs.getSyncStatus()==SyncStatus.waiting_for_data) {
			Log.d("vortex","Client had nothing to sync. I can close the connection");
			bt.success();
		} else if (message instanceof SyncEntry[] && gs.getSyncStatus()==SyncStatus.waiting_for_data) {
			SyncEntry[] ses = (SyncEntry[])message;
			Log.d("vortex","[BT MESSAGE -->Received Data message]");
			if (ses!=null && ses.length>0) {
				o.addRow("");
				o.addGreenText("[BT MESSAGE -->Recieved "+ses.length+" rows of data]");					
				gs.synchronise(ses,true);
				gs.sendEvent(BluetoothConnectionService.SYNK_DATA_RECEIVED);
				//Send back the timestamp i received in header.
				if (bt.send(new SyncSuccesful(((SyncEntryHeader)ses[0]).timeStampOfLastEntry)));
				
			} 
			else {
				Log.d("vortex","no data in sync. Closing connection");
				bt.success();
			}

		} else {
			Log.e("vortex","Discarded message..");	
			o.addRow("[BT MESSAGE --> Discarded message!]");
			Log.d("vortex","message is "+message.toString());
			
			bt.send(new SyncRestartRequest());
			bt.ping();
			gs.setSyncStatus(SyncStatus.restarting);
		}

	}



}
