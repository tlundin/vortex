package com.teraim.vortex.bluetooth;

import java.io.IOException;

import android.content.Intent;
import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.GlobalState.SyncStatus;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.non_generics.NamedVariables;
import com.teraim.vortex.utils.PersistenceHelper;

public class SlaveMessageHandler extends MessageHandler {

	@Override
	public void handleSpecialized(Object message) {

		if (message instanceof MasterPing ) {
			//TODO: CHECK BUNDLE NAME!!
			//String myBundleName = gs.getPreferences().get(PersistenceHelper.BUNDLE_NAME);
			MasterPing mp = (MasterPing)message;		
			//Check version. If not same, send warning.
			String myBundleVersion = gs.getPreferences().get(PersistenceHelper.CURRENT_VERSION_OF_WF_BUNDLE);
			String mySoftwareVersion = Float.toString(gs.getGlobalPreferences().getF(PersistenceHelper.CURRENT_VERSION_OF_PROGRAM));
			Log.d("vortex","myBundleV "+myBundleVersion+" msgVer "+mp.getbundleVersion()+" swVer: "+mySoftwareVersion+" msgVer: "+mp.getSoftwareVersion());
			if (!myBundleVersion.equals(mp.getbundleVersion()) ||
					!mySoftwareVersion.equals(mp.getSoftwareVersion())) {
				gs.sendEvent(BluetoothConnectionService.VERSION_MISMATCH);
				o.addRow("");
				o.addRedText("[BT MESSAGE -->PING. VERSION MISMATCH!");
				o.addRow("My vortex version: "+mySoftwareVersion);
				o.addRow("Others vortex version: "+mp.getSoftwareVersion());
				o.addRow("My bundle version: "+myBundleVersion);
				o.addRow("Others bundle version: "+mp.getbundleVersion());

			} else {
				o.addRow("");
				o.addGreenText("[BT MESSAGE -->PING. VERSIONS OK");
			}

			//Invalidate Cache.
			//gs.getVariableCache().invalidateAll();


			String masterLag = mp.getPartnerLagId();
			String clientLag = gs.getGlobalPreferences().get(PersistenceHelper.LAG_ID_KEY);

			if (masterLag!=null && !masterLag.equals(clientLag)) {
				gs.getGlobalPreferences().put(PersistenceHelper.LAG_ID_KEY, masterLag);
			}
			gs.setMyPartner(mp.getPartner());

			Log.d("nils","Got MasterPing..waiting for sync data.");	
			o.addRow("[BT MESSAGE --> Got MasterPing. Now expecting sync data]");

			if (mp.requestAll()) {
				gs.getPreferences().put(PersistenceHelper.TIME_OF_LAST_SYNC,PersistenceHelper.UNDEFINED);
				Log.d("vortex","Resetting DB Counter!");
				o.addRow("");
				o.addGreenText("Partner db empty. Resetting my DB Counter");
			}
			gs.setSyncStatus(SyncStatus.waiting_for_data);
		}
		else if (message instanceof NothingToSync ) {
			o.addRow("[BT MESSAGE -->Recieved nothing to Sync message]");				
			Log.d("vortex","[BT MESSAGE -->Recieved nothing to Sync message]");
			try {
				if (gs.triggerTransfer())			
					gs.setSyncStatus(SyncStatus.waiting_for_ack);
			else {
				Log.d("vortex","Received nothing to sync. I have nothing either...so closing connection");
	
				bt.send(new NothingToSync());
				gs.setSyncStatus(SyncStatus.waiting_for_connection_to_close);
				if (gs.getPreferences().get(PersistenceHelper.TIME_OF_LAST_SYNC).equals(PersistenceHelper.UNDEFINED)) {
					Log.d("vortex","time of last sync set to 0");							
					gs.getPreferences().put(PersistenceHelper.TIME_OF_LAST_SYNC,"0");
				}
			}
			} catch(IOException e) {
				Log.e("vortex","Exception when sending message...aborting");
				bt.stop();
			}
		}
		else if (message instanceof SyncEntry[] ) {			
			SyncEntry[] ses = (SyncEntry[])message;
			if (ses!=null && ses.length!=0) {
				o.addRow("[BT MESSAGE -->Recieved "+ses.length+" rows of data]");				
				Log.d("nils","[BT MESSAGE -->Recieved "+ses.length+" rows of data]");
				//Send the number of blocks to read to the ui.

				gs.synchronise(ses,false); 
				gs.sendEvent(BluetoothConnectionService.SYNK_DATA_RECEIVED);
				boolean success = bt.send(new SyncSuccesful(((SyncEntryHeader)ses[0]).timeStampOfLastEntry));
				if (!success)
					BluetoothConnectionService.getSingleton().stop();
				else {
					
					o.addRow("Trying to send my data to Master.");
					try {
					if (gs.triggerTransfer())
						gs.setSyncStatus(SyncStatus.waiting_for_ack);
					else {
						Log.d("vortex","Nothing to sync. I will tell Master and expect him to close the connection");
						bt.send(new NothingToSync());
						gs.setSyncStatus(SyncStatus.waiting_for_connection_to_close);
						
						if (gs.getPreferences().get(PersistenceHelper.TIME_OF_LAST_SYNC).equals(PersistenceHelper.UNDEFINED)) {
							Log.d("vortex","time of last sync set to 0");							
							gs.getPreferences().put(PersistenceHelper.TIME_OF_LAST_SYNC,"0");
						}
					}
					} catch(IOException e) {
						Log.e("vortex","Exception when sending message...aborting");
						bt.stop();
					}
				}
			} else {
				Log.d("vortex","no data in sync. Something wrong. Closing connection");
				bt.stop();
			}			


		} else if (message instanceof SyncSuccesful && gs.getSyncStatus()==SyncStatus.waiting_for_ack ) {
			SyncSuccesful ssf = (SyncSuccesful)message;			
			if (ssf!=null && ssf.getLastEntrySent()>0) {
				gs.getDb().syncDone(ssf.getLastEntrySent());
				
				o.addRow("");
				o.addGreenText("[BT MESSAGE -->Received SyncSuccesful message!]");
				bt.success();
			} else 
				Log.d("nils","syncsuccessful null or -1");

		} else if (message instanceof SlavePing) {
			o.addRow("");
			o.addRedText("Got Slave Ping. Both devices configured as slaves");
			gs.sendEvent(BluetoothConnectionService.SAME_SAME_SYNDROME);
		} 
		else {
		
			Log.e("vortex","message out of order.. "+message.toString());	
			o.addRow("[BT MESSAGE --> Restart of Sync...message lost]");
			
			bt.send(new SyncRestartRequest());
			bt.ping();
			gs.setSyncStatus(SyncStatus.restarting);
		}
	}
}

