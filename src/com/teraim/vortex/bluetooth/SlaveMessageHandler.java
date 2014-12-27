package com.teraim.vortex.bluetooth;

import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.non_generics.NamedVariables;
import com.teraim.vortex.utils.PersistenceHelper;

public class SlaveMessageHandler extends MessageHandler {


	public SlaveMessageHandler(GlobalState gs) {
		super(gs);
	}

	@Override
	public void handleSpecialized(Object message) {
		boolean changedConfig = false;
		if (message instanceof MasterPing) {
			if (pingC++==0) {
				MasterPing p = (MasterPing)message;		
				Variable ruta = gs.getArtLista().getVariableInstance(NamedVariables.CURRENT_RUTA);
				Variable provyta = gs.getArtLista().getVariableInstance(NamedVariables.CURRENT_PROVYTA);
				if (!p.getRuta().equals(ruta.getValue())||
						!p.getProvyta().equals(provyta.getValue())) {
					ruta.setValue(p.getRuta());
					provyta.setValue(p.getProvyta());
					//TODO: REMOVE
					//Invalidate Cache.
					gs.getVariableCache().invalidateAll();
					Variable stratum = gs.getArtLista().getVariableUsingKey(gs.getArtLista().createRutaKeyMap(),NamedVariables.STRATUM);
					Variable hStratum = gs.getArtLista().getVariableUsingKey(gs.getArtLista().createRutaKeyMap(),NamedVariables.STRATUM_HISTORICAL);
					hStratum.setValue(stratum.getHistoricalValue());
					changedConfig=true;
				}
				String masterLag = p.getPartnerLagId();
				String clientLag = gs.getPersistence().get(PersistenceHelper.LAG_ID_KEY);
				if (masterLag!=null && !masterLag.equals(clientLag)) {
					changedConfig = true;
					gs.getPersistence().put(PersistenceHelper.LAG_ID_KEY, masterLag);
				}
				gs.setMyPartner(p.getPartner());

				if (changedConfig)
					gs.sendEvent(BluetoothConnectionService.MASTER_CHANGED_MY_CONFIG);
				Log.d("nils","Got MasterPing..waiting for sync data.");	
				o.addRow("[BT MESSAGE --> Got MasterPing. Now expecting sync data]");
			} else 
				Log.e("nils","received extra master ping");

		} 
		else if (message instanceof SyncEntry[]) {
			SyncEntry[] ses = (SyncEntry[])message;
			o.addRow("[BT MESSAGE -->Recieved "+ses.length+" rows of data]");				
			Log.d("nils","[BT MESSAGE -->Recieved "+ses.length+" rows of data]");	
			if (ses!=null && ses.length>1)
				gs.synchronise(ses,false); 
			//Send back the timestamp identifying this sync.
			gs.sendMessage(new SyncSuccesful(((SyncEntryHeader)ses[0]).timeStampOfLastEntry));
			o.addRow("Trying to send sync succesful message to Master");
			gs.sendEvent(BluetoothConnectionService.SYNK_DATA_RECEIVED);
			o.addRow("Trying to send my data to Master.");
			if (gs.getPersistence().get(Constants.MOJO).equals(PersistenceHelper.UNDEFINED)) {
				Log.d("nils","Resetting timestamp of sync to 0 in slave");
				gs.getPersistence().put(PersistenceHelper.TIME_OF_LAST_SYNC, Long.toString(0));
				gs.getPersistence().put(Constants.MOJO, "Lifeasweknowit");
			}				
			gs.triggerTransfer();

		}

		else if (message instanceof SlavePing) {
			o.addRow("");
			o.addRedText("Got Slave Ping. Both devices configured as slaves");
			gs.sendEvent(BluetoothConnectionService.SAME_SAME_SYNDROME);
		}

		


	}

}
