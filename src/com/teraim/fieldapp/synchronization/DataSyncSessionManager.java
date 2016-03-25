package com.teraim.fieldapp.synchronization;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.ui.ConfirmCallBack;
import com.teraim.fieldapp.ui.MenuActivity;
import com.teraim.fieldapp.ui.MenuActivity.UIProvider;
import com.teraim.fieldapp.utils.PersistenceHelper;

/**
 * 
 * @author Terje
 *
 * Class that keeps track of a connection.
 */
public class DataSyncSessionManager implements ConnectionListener,SyncStatusListener {

	enum State {
		initial,
		waiting_for_confirmation,
		sending_ping,
		error,
		sending_data,
		closed;
	}
	private static DataSyncSessionManager singleton=null;
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

		//Lock the ui while connection is established.

		ui.lock();


		ui.alert("Trying to establish connection");

		//Ask for a connection from the connection manager.

		mConnection = GlobalState.getInstance().getConnectionManager().requestConnection(ctx);
		mConnection.registerConnectionListener(this);

		//Open connection to named partner.
		mConnection.openConnection(Constants.BLUETOOTH_NAME);


	}

	List<Object> messageCache = new ArrayList<Object>();

	@Override
	public void handleMessage(Object obj) {
		Log.d("vortex","Message arrived.");
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
		Log.d("vortex","In HandleEvent "+this.toString());
		switch(e) {
		case connectionAttemptFailed:
			ui.alert("Trying to establish connection. Attempts left: #"+mConnection.getTriesRemaining());
			break;
		case connectionGained:
			mState = State.initial;
			/*if (mState==State.initial && pingAttempts>0) {
				mState = State.sending_ping;
				//If no reply within 3 seconds, we should retry.

				Timer t = new Timer();
				t.schedule(new TimerTask() {
					public void run() { 

						if (mState == State.sending_ping) {
							mState = State.initial;							
							handleEvent(ConnectionEvent.connectionGained);
						} 
					}},5000);
			*/
			Log.d("vortex","Connection gained. sending ping");
				ping();
			//} else
			//	Log.d("vortex","discarded duplicate message");
			break;
		case connectionBroken:
			if (mState!=State.closed)
				ui.alert("Connection to other device broken.");
			mState=State.closed;
			break;
		case connectionClosedGracefully:
			if (mState!=State.closed)
				ui.alert("Connection to other device closed.");
			mState=State.closed;
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
	SyncReport syncReport = null;
	private SyncReport partnerSyncReport;
	boolean nameErr=false;
	
	private void handle(Object message) {
		GlobalState gs = GlobalState.getInstance();

		if (message instanceof SyncEntry[]) {
			SyncEntry[] ses = (SyncEntry[])message;
			ui.alert("Inserting data");
			o.addRow("");
			o.addGreenText("[BT MESSAGE -->Received SYNCENTRIES: "+ses.length+"]");
			syncReport = gs.getDb().synchronise(ses, ui,o,this);	
			o.addRow("");
			o.addGreenText("[BT MESSAGE -->Sending SyncSuccesful");
			send(new SyncSuccesful(((SyncEntryHeader)ses[0]).timeStampOfLastEntry,syncReport));

			pSyncDone=true;

		} else if (message instanceof SyncSuccesful) {
			SyncSuccesful ssf = (SyncSuccesful)message;
			Log.d("vortex","[BT MESSAGE -->Received SyncSuccesful message]");
			if (ssf!=null && ssf.getLastEntrySent()>0) {
				GlobalState.getInstance().getDb().syncDone(ssf.getLastEntrySent());
				o.addRow("");
				o.addGreenText("[BT MESSAGE -->Received SyncSuccesful message!]");
				gs.sendEvent(MenuActivity.REDRAW);
				partnerSyncReport=ssf.getChangesDone();
				mSyncDone=true;
			} else 
				ui.alert("Sync failed on checksum");
		} else if (message instanceof SyncStatus) {
			SyncStatus ss = (SyncStatus)message;
			Log.d("vortex","Received sync status!");
			ui.alert("Transferred :"+ss.getStatus());
		} else if (message instanceof NothingToSync) {
			Log.d("vortex","[BT MESSAGE -->Received Nothing to SYNC message]");
			pSyncDone=true;
		} else if (message instanceof PingMessage) {
			if (mState !=State.initial) {
				Log.e("vortex","Double kiss...discard");
				return;
			}
			mState = State.error;
			if (message instanceof MasterPing && gs.isMaster()) 
				ui.alert("Both devices configured as Master. Please change under Configuration.");
			else if (message instanceof SlavePing && !gs.isMaster()) 
				ui.alert("Both devices configured as Client. Please change under Configuration.");
			else {
				final PingMessage sp = (PingMessage)message;
				String myAppName = gs.getGlobalPreferences().get(PersistenceHelper.BUNDLE_NAME);

				String partnerAppName = sp.getPartnerAppName();

				if (partnerAppName == null || !partnerAppName.equals(myAppName))
					ui.alert("Different applications running"+
							"\nYour app: "+myAppName+
							"\nPartner app: "+partnerAppName+
							"\nSynchronization requires that both use same");
				else {
					//Here we know it is ok.
					mState = State.waiting_for_confirmation;
					lock=true;
					
					float myAppVersion = gs.getPreferences().getF(PersistenceHelper.CURRENT_VERSION_OF_APP);
					float mySoftwareVersion = gs.getGlobalPreferences().getF(PersistenceHelper.CURRENT_VERSION_OF_PROGRAM);

					Log.d("vortex","myBundleV "+myAppVersion+" msgVer "+sp.getAppVersion()+" swVer: "+mySoftwareVersion+" msgVer: "+sp.getSoftwareVersion());
					StringBuilder versionText = new StringBuilder();
					versionText.append("My vortex version: "+mySoftwareVersion+
							"\nOthers vortex version: "+sp.getSoftwareVersion()+
							"\nMy App version: "+myAppVersion+
							"\nOthers App version: "+sp.getAppVersion()+
							"\nMy Time: "+Constants.getSweDate()+
							"\nOthers Time: "+sp.getTime()+
							"\nPerson: "+sp.getPartner());
							
					
					String mYear = Constants.getYear();
					String pYear = sp.getTime().substring(0,4);
					String mPartner = gs.getPreferences().get(PersistenceHelper.PARTNER_NAME);
					if (mPartner.isEmpty()) {
						Log.d("vortex","no previous value. setting partner");
						gs.getPreferences().put(PersistenceHelper.PARTNER_NAME, sp.getPartner());
						mPartner = sp.getPartner();
					}
					boolean err=false;
					if (myAppVersion!=sp.getAppVersion() ||
							mySoftwareVersion!=sp.getSoftwareVersion()) {
						err=true;
						versionText.insert(0,"*WARNING*: Version mismatch:\n");
						o.addRow("");
						o.addRedText("[BT MESSAGE -->PING. VERSION MISMATCH!");
						o.addRow(versionText.toString());

					} else if(!mYear.equals(pYear)) {
						versionText.insert(0,"*WARNING*: Year mismatch:\n");
						o.addRow("");
						o.addRedText("[BT MESSAGE -->PING. TIME MISMATCH!");
						o.addRow(versionText.toString());
						err=true;
					
					} else if (!mPartner.equals(sp.getPartner())) {
						err=true;
						nameErr=true;
						versionText.insert(0,"*WARNING*: Partner name mismatch.\nExisting: "+mPartner+". New: "+sp.getPartner());
						o.addRow("");
						o.addRedText("[BT MESSAGE -->PING. NAME MISMATCH!");
						o.addRow("Existing: "+mPartner+". New: "+sp.getPartner());
					}
					
					if (!err){
						versionText.append("\n\nAll Ok!\nConfirm to start sync");				
						o.addRow("");
						o.addGreenText("[BT MESSAGE -->PING. VERSIONS OK");

					} else {
						versionText.append("\n\n Please confirm that you wish to proceed");

					}
					//Confirm!
					ui.confirm(versionText.toString(),new ConfirmCallBack() {

						@Override
						public void confirm() {
							if (nameErr) {
								//Change partner name permanently to avoid repeat error
								GlobalState.getInstance().getPreferences().put(PersistenceHelper.PARTNER_NAME, sp.getPartner());	
								nameErr=false;
							}
							startDataTransfer();
						}});
					
				}
			}
		}
		if (pSyncDone && mSyncDone) {
			if (syncReport!=null) {
				ui.alert("Synchronization succesful. Report: ");
				 
				ui.setInfo(
						"Deletes    : "+syncReport.deletes+
						"\nFaults     : "+syncReport.faults+
						"\nInserts    : "+syncReport.inserts+
						"\nRejected   : "+syncReport.refused+
						(partnerSyncReport!=null?(
						"\nPartner Deletes    : "+partnerSyncReport.deletes+
						"\nPartner Faults     : "+partnerSyncReport.faults+
						"\nPartner Inserts    : "+partnerSyncReport.inserts+
						"\nPartner Rejected   : "+partnerSyncReport.refused
						):
							"\nNo changes in partner device")
						
						);
			}
			else 
					ui.alert("Synchronization done. No changes.");
				
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
					ui.alert("Sending "+entries.length+" entries (this can take a long time and the operation locks the ui on some devices. Please be patient and wait)");
					send(entries);
					ui.alert("Entries sent. Waiting for data.");
				}
				else {
					o.addRow("");
					o.addGreenText("[BT MESSAGE -->SENDING NOTHING TO SYNC");
					ui.alert("Nothing to sync. Waiting for data.");
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




	public void send(Object entries) {
		Log.d("vortex", "in send entries");
		if (mConnection!=null && mConnection.isOpen()) {
			mConnection.write(entries);
			o.addRow("");
			o.addGreenText("[BT MESSAGE -->Send succesful!");
		} else {
			o.addRow("");
			o.addRedText("[BT MESSAGE -->Send not possible! Connection was closed!");
			ui.alert("Error: Cannot send data, connection is closed");
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
		if (syncReport!=null&&syncReport.hasChanges())
			GlobalState.getInstance().sendEvent(Executor.REDRAW_PAGE);
		GlobalState.getInstance().getConnectionManager().releaseConnection(mConnection);
		mConnection.closeConnection();
		mConnection.unRegisterConnectionListener(this);

	}

	private void ping() {
		GlobalState gs = GlobalState.getInstance();
		//Send a ping to see if we can connect straight away.
		Log.d("NILS","Sending ping");
		String myName, myTeam, myApp, myTime;
		float appVersion,softwareVersion;
		myName = gs.getGlobalPreferences().get(PersistenceHelper.USER_ID_KEY);
		myTeam = gs.getGlobalPreferences().get(PersistenceHelper.LAG_ID_KEY);
		myApp = gs.getGlobalPreferences().get(PersistenceHelper.BUNDLE_NAME);
		appVersion = gs.getPreferences().getF(PersistenceHelper.CURRENT_VERSION_OF_APP);
		softwareVersion = gs.getGlobalPreferences().getF(PersistenceHelper.CURRENT_VERSION_OF_PROGRAM);
		myTime = Constants.getSweDate();
		boolean requestAll = gs.getPreferences().get(PersistenceHelper.TIME_OF_LAST_SYNC).equals(PersistenceHelper.UNDEFINED);

		//send(new PingMessage(myName,myLag,bundleVersion,softwareVersion,requestAll));
		o.addRow("");
		o.addGreenText("[BT MESSAGE -->SENDING PING");
		send(gs.isMaster()?new MasterPing(myName,myApp,myTeam,appVersion,softwareVersion,requestAll,myTime):new SlavePing(myName,myApp,myTeam,appVersion,softwareVersion,requestAll,myTime));

	}

	public static void start(MenuActivity menuActivity, UIProvider uiProvider) {
		if (singleton==null)
			singleton = new DataSyncSessionManager(menuActivity,uiProvider);
	}

	public static void stop() {
		if (singleton != null)
			singleton.destroy();
		singleton = null;
	}


}
