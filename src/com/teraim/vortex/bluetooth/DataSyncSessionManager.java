package com.teraim.vortex.bluetooth;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.Executor;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.synchronization.ConnectionListener;
import com.teraim.vortex.synchronization.ConnectionProvider;
import com.teraim.vortex.ui.ConfirmCallBack;
import com.teraim.vortex.ui.MenuActivity;
import com.teraim.vortex.ui.MenuActivity.UIProvider;
import com.teraim.vortex.utils.DbHelper.SyncReport;
import com.teraim.vortex.utils.PersistenceHelper;

public class DataSyncSessionManager implements ConnectionListener {

	enum State {
		initial,
		waiting_for_confirmation,
		sending_ping,
		error,
		sending_data;
	}
	Context ctx;
	ConnectionProvider mConnection;
	private UIProvider ui;
	boolean lock = false;
	LoggerI o;
	State mState = State.initial;

	public DataSyncSessionManager(Context ctx, UIProvider ui) {
		this.ctx=ctx;
		//setup connection if missing. Asynch callback.
		o = GlobalState.getInstance().getLogger();

		this.ui = ui;

		ui.lock();
		ui.alert("Trying to establish connection");

		mConnection = GlobalState.getInstance().getConnectionManager().requestConnection(ctx);
		mConnection.registerConnectionListener(this);
		mConnection.openConnection(Constants.BLUETOOTH_NAME);


	}

	List<Object> messageCache = new ArrayList<Object>();

	@Override
	public void handleMessage(Object obj) {
		Log.d("vortex","Message arrived. .");
		//ui.update("Message arrived!");
		if (!lock) {
			handle(obj);
		} else {
			Log.d("vortex","Handle message delayed. lock");
			messageCache.add(obj);
		}
	}

	int pingAttempts = 5;

	@Override
	public void handleEvent(ConnectionEvent e) {

		switch(e) {
		case connectionAttemptFailed:
			ui.alert("Trying to establish connection. Attempts left: #"+mConnection.getTriesRemaining());
			break;
		case connectionGained:

			if (mState==State.initial && pingAttempts>0) {
				mState = State.sending_ping;
				//If no reply within 3 seconds, we should retry.

				Timer t = new Timer();
				t.schedule(new TimerTask() {
					public void run() { 

						if (mState == State.sending_ping) {
							mState = State.initial;
							ui.alert("No reply. Pinging again. ("+pingAttempts--+")");
							handleEvent(ConnectionEvent.connectionGained);
						} 
					}},5000);
				ping();
			} else
				Log.d("vortex","discarded duplicate message");
			break;
		case connectionBroken:
			ui.alert("Connection to other device broken.");

			break;
		case connectionClosedGracefully:
			ui.alert("Connection to other device closed.");
			break;
		case connectionFailed:
			ui.alert("Connection failed");
			break;
		case connectionFailedNoPartner:
			ui.alert("No bonded device.");
			break;
		case connectionFailedNamedPartnerMissing:
			ui.alert("Could not find a bounded device named '"+Constants.BLUETOOTH_NAME+"'"+". Please check your configuration under Bonded Devices in Bluetooth Settings. (This problem only applies if there are more than one bonded device, and none of them are named '"+Constants.BLUETOOTH_NAME+"')");
			break;
		case connectionStatus:
			break;
		case restartRequired:
			ui.alert("Data corrupted..see log for details");
			break;
		default:
			break;

		}
	}


	boolean pSyncDone,mSyncDone;
	SyncReport changesDone = null;

	private void handle(Object message) {
		GlobalState gs = GlobalState.getInstance();

		if (message instanceof SyncEntry[]) {
			SyncEntry[] ses = (SyncEntry[])message;
			ui.alert("Writing into Database");
			o.addRow("");
			o.addGreenText("[BT MESSAGE -->Received SYNCENTRIES: "+ses.length+"]");
			changesDone = gs.getDb().synchronise(ses, ui,o);	
			o.addRow("");
			o.addGreenText("[BT MESSAGE -->Sending SyncSuccesful");
			send(new SyncSuccesful(((SyncEntryHeader)ses[0]).timeStampOfLastEntry));
			
			pSyncDone=true;

		} else if (message instanceof SyncSuccesful) {
			SyncSuccesful ssf = (SyncSuccesful)message;
			Log.d("vortex","[BT MESSAGE -->Received SyncSuccesful message]");
			if (ssf!=null && ssf.getLastEntrySent()>0) {
				GlobalState.getInstance().getDb().syncDone(ssf.getLastEntrySent());
				o.addRow("");
				o.addGreenText("[BT MESSAGE -->Received SyncSuccesful message!]");
				GlobalState.getInstance().sendSyncEvent(new Intent(MenuActivity.REDRAW));
				mSyncDone=true;
			} else 
				ui.alert("Sync failed on checksum");
		} else if (message instanceof NothingToSync) {
			Log.d("vortex","[BT MESSAGE -->Received Nothing to SYNC message]");
			pSyncDone=true;
		} else if (message instanceof PingMessage) {
			mState = State.error;
			if (message instanceof MasterPing && gs.isMaster()) 
				ui.alert("Both devices configured as Master. Please change under Configuration.");
			else if (message instanceof SlavePing && !gs.isMaster()) 
				ui.alert("Both devices configured as Client. Please change under Configuration.");
			else {
				PingMessage sp = (PingMessage)message;
				String myBundleVersion = gs.getPreferences().get(PersistenceHelper.CURRENT_VERSION_OF_WF_BUNDLE);
				String mySoftwareVersion = Float.toString(gs.getGlobalPreferences().getF(PersistenceHelper.CURRENT_VERSION_OF_PROGRAM));
				Log.d("vortex","myBundleV "+myBundleVersion+" msgVer "+sp.getbundleVersion()+" swVer: "+mySoftwareVersion+" msgVer: "+sp.getSoftwareVersion());
				String versionText = "My vortex version: "+mySoftwareVersion+
						"\nOthers vortex version: "+sp.getSoftwareVersion()+
						"\nMy bundle version: "+myBundleVersion+
						"\nOthers bundle version: "+sp.getbundleVersion();
				gs.setMyPartner(sp.getPartner());
				if (!myBundleVersion.equals(sp.getbundleVersion()) ||
						!mySoftwareVersion.equals(sp.getSoftwareVersion())) {
					versionText = "Version mismatch:\n"+versionText+"\n\n Please confirm that you wish to proceed";

					o.addRow("");
					o.addRedText("[BT MESSAGE -->PING. VERSION MISMATCH!");
					o.addRow(versionText);
				} else {
					versionText = versionText+"\n\nPlease confirm that you wish to proceed";				
					o.addRow("");
					o.addGreenText("[BT MESSAGE -->PING. VERSIONS OK");

				}
				//Confirm!
				ui.confirm(versionText,new ConfirmCallBack() {

					@Override
					public void confirm() {
						startDataTransfer();
					}});
				mState = State.waiting_for_confirmation;
				lock=true;
			}
		}
		if (pSyncDone && mSyncDone) {
			if (changesDone!=null) {
				ui.alert("Synchronization succesful. Report: ");
				ui.setCounter(
						"Deletes    : "+changesDone.deletes+
						"\nFaults     : "+changesDone.faults+
						"\nInserts    : "+changesDone.inserts+
						"\nRejected   : "+changesDone.refused);
			}
			else
				ui.alert("Synchronization failed. Could not insert the data.");
		}
			
	}

	private void startDataTransfer() {
		Thread thread = new Thread() {
			@Override
			public void run() {

				SyncEntry[] entries = createEntries();
				mState = DataSyncSessionManager.State.sending_data;

				if (entries!=null) {
					o.addRow("");
					o.addGreenText("[BT MESSAGE -->SENDING "+entries.length+" ENTRIES");
					send(entries);
				}
				else {
					o.addRow("");
					o.addGreenText("[BT MESSAGE -->SENDING NOTHING TO SYNC");
					send(new NothingToSync());
					mSyncDone=true;
				}

				lock = false;
				if (!messageCache.isEmpty()) {
					Log.d("sync","found cached messages..will call handle");
					for (Object obj:messageCache) {
						o.addRow("");
						o.addGreenText("Handling cached message: "+obj.toString());
						handle(obj);
					}
				}
				//If sync not done, we still expect more messages..
				if (!pSyncDone || !mSyncDone) {
					o.addRow("");
					o.addYellowText("Still not done..");
				}
			}
		};


		thread.start();
	}




	private void send(Object entries) {
		Log.d("vortex", "in send entries");
		if (mConnection!=null && mConnection.isOpen()) {
			ui.alert("Sending to partner device..");
			mConnection.write(entries);
			o.addRow("");
			o.addGreenText("[BT MESSAGE -->Send succesful!");
			ui.alert("Waiting for reply..");
		} else {
			o.addRow("");
			o.addRedText("[BT MESSAGE -->Send not possible! Connection was closed!");
			ui.alert("Cannot send data, since the connection is not open");
		}
	}



	/**
	 * Collect changes done from database. 
	 */
	private SyncEntry[] createEntries() {

		ui.alert("Connection established...reading changes");
		return GlobalState.getInstance().getDb().getChanges(ui);

	}

	/**Close and destroy this session handler
	 * 
	 */
	public void destroy() {
		//Update the world if changes occured.
		if (changesDone!=null&&changesDone.hasChanges())
			GlobalState.getInstance().sendSyncEvent(new Intent(Executor.REDRAW_PAGE));
		GlobalState.getInstance().getConnectionManager().releaseConnection(mConnection);
		mConnection.closeConnection();

	}

	private void ping() {
		GlobalState gs = GlobalState.getInstance();
		//Send a ping to see if we can connect straight away.
		Log.d("NILS","Sending ping");
		String myName, myLag;
		String bundleVersion,softwareVersion;
		myName = gs.getGlobalPreferences().get(PersistenceHelper.USER_ID_KEY);
		myLag = gs.getGlobalPreferences().get(PersistenceHelper.LAG_ID_KEY);
		bundleVersion = gs.getPreferences().get(PersistenceHelper.CURRENT_VERSION_OF_WF_BUNDLE);
		softwareVersion = Float.toString(gs.getGlobalPreferences().getF(PersistenceHelper.CURRENT_VERSION_OF_PROGRAM));
		boolean requestAll = gs.getPreferences().get(PersistenceHelper.TIME_OF_LAST_SYNC).equals(PersistenceHelper.UNDEFINED);

		//send(new PingMessage(myName,myLag,bundleVersion,softwareVersion,requestAll));
		o.addRow("");
		o.addGreenText("[BT MESSAGE -->SENDING PING");
		send(gs.isMaster()?new MasterPing(myName,myLag,bundleVersion,softwareVersion,requestAll):new SlavePing(myName,myLag,bundleVersion,softwareVersion,requestAll));

	}


}
