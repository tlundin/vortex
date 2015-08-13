package com.teraim.vortex.bluetooth;

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
		
		if (message instanceof MasterPing && gs.getSyncStatus()==SyncStatus.waiting_for_ping) {
			//TODO: CHECK BUNDLE NAME!!
			//String myBundleName = gs.getPreferences().get(PersistenceHelper.BUNDLE_NAME);
			MasterPing mp = (MasterPing)message;		
			//Check version. If not same, send warning.
			String myBundleVersion = gs.getPreferences().get(PersistenceHelper.CURRENT_VERSION_OF_WF_BUNDLE);
			String mySoftwareVersion = Float.toString(gs.getGlobalPreferences().getF(PersistenceHelper.CURRENT_VERSION_OF_PROGRAM));
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
			gs.getVariableCache().invalidateAll();


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
		else if (message instanceof NothingToSync && gs.getSyncStatus()==SyncStatus.waiting_for_data) {
			o.addRow("[BT MESSAGE -->Recieved nothing to Sync message]");				
			Log.d("vortex","[BT MESSAGE -->Recieved nothing to Sync message]");
			if (gs.triggerTransfer())
				gs.setSyncStatus(SyncStatus.waiting_for_ack);
			else {
				Log.d("vortex","Received nothing to sync. I have nothing either...so closing connection");
				bt.stop();
			}
		}
		else if (message instanceof SyncEntry[] && gs.getSyncStatus()==SyncStatus.waiting_for_data) {
			
			SyncEntry[] ses = (SyncEntry[])message;
			
			o.addRow("[BT MESSAGE -->Recieved "+ses.length+" rows of data]");				
			Log.d("nils","[BT MESSAGE -->Recieved "+ses.length+" rows of data]");	
			
			if (ses!=null && ses.length!=0) {
				gs.synchronise(ses,false); 
				bt.send(new SyncSuccesful(((SyncEntryHeader)ses[0]).timeStampOfLastEntry));
			} else {
				Log.d("vortex","no data in sync. Closing connection");
				bt.stop();
			}
				
			//Always send syncsuccesful for now. back the timestamp identifying this sync.
			o.addRow("Trying to send sync succesful message to Master");
			gs.sendEvent(BluetoothConnectionService.SYNK_DATA_RECEIVED);
			o.addRow("Trying to send my data to Master.");
			if (gs.triggerTransfer())
				gs.setSyncStatus(SyncStatus.waiting_for_ack);
			else {
				Log.d("vortex","Nothing to sync. I will tell Master and expect him to close the connection");
				gs.setSyncStatus(SyncStatus.waiting_for_connection_to_close);
				bt.send(new NothingToSync());
			}

		 } else if (message instanceof SyncSuccesful && gs.getSyncStatus()==SyncStatus.waiting_for_ack ) {
				SyncSuccesful ssf = (SyncSuccesful)message;			
				if (ssf!=null && ssf.getLastEntrySent()>0) {
					gs.getDb().syncDone(ssf.getLastEntrySent());
					gs.sendEvent(BluetoothConnectionService.SYNK_DATA_TRANSFER_DONE);
					o.addRow("");
					o.addGreenText("[BT MESSAGE -->Received SyncSuccesful message!]");
					bt.stop();
				} else 
					Log.d("nils","syncsuccessful null or -1");

			} else if (message instanceof SlavePing) {
			o.addRow("");
			o.addRedText("Got Slave Ping. Both devices configured as slaves");
			gs.sendEvent(BluetoothConnectionService.SAME_SAME_SYNDROME);
		} else {
			Log.e("vortex","Discarded message.. "+message.toString());	
			o.addRow("[BT MESSAGE --> Discarded message!]");
		}
	}
}
	/*	
		
	
		
		
		if (!initializationDone) {
			if (message instanceof MasterPing) {
				//Might be several master pings. Only use first.
				if (pingC++==0) {
					//TODO: CHECK BUNDLE NAMES. ADD TO MASTER_PING
					String myBundleName = gs.getPreferences().get(PersistenceHelper.BUNDLE_NAME);
					MasterPing mp = (MasterPing)message;		
					//Check version. If not same, send warning.
					String myBundleVersion = gs.getPreferences().get(PersistenceHelper.CURRENT_VERSION_OF_WF_BUNDLE);
					String mySoftwareVersion = Float.toString(gs.getGlobalPreferences().getF(PersistenceHelper.CURRENT_VERSION_OF_PROGRAM));
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
					gs.getVariableCache().invalidateAll();


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
					initializationDone=true;
				} else 
					Log.e("nils","received extra master ping");
			} else if (message instanceof SlavePing) {
					o.addRow("");
					o.addRedText("Got Slave Ping. Both devices configured as slaves");
					gs.sendEvent(BluetoothConnectionService.SAME_SAME_SYNDROME);
				}
			else {
				Log.e("vortex","Discarded message..not a ping (init not done)");	
				o.addRow("[BT MESSAGE --> Discarded message (no ping). Init not done!]");
			} 
			
		
		} else 	{ 
			if (message instanceof SyncEntry[]) {
		
				SyncEntry[] ses = (SyncEntry[])message;
				o.addRow("[BT MESSAGE -->Recieved "+ses.length+" rows of data]");				
				Log.d("nils","[BT MESSAGE -->Recieved "+ses.length+" rows of data]");	
				if (ses!=null && ses.length>1) {
					gs.synchronise(ses,false); 
				//Send back the timestamp identifying this sync.
				gs.sendMessage(new SyncSuccesful(((SyncEntryHeader)ses[0]).timeStampOfLastEntry));
				}
				o.addRow("Trying to send sync succesful message to Master");
				gs.sendEvent(BluetoothConnectionService.SYNK_DATA_RECEIVED);
				o.addRow("Trying to send my data to Master.");
				gs.triggerTransfer();

			} else {
				Log.e("vortex","Discarded message..");	
				o.addRow("[BT MESSAGE --> Discarded message!]");
			}

		}



		

	}
}
*/
