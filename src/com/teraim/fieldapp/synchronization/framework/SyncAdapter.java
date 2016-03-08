package com.teraim.fieldapp.synchronization.framework;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.URL;
import java.net.URLConnection;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.synchronization.EndOfStream;
import com.teraim.fieldapp.synchronization.SyncEntry;
import com.teraim.fieldapp.synchronization.SyncFailed;
import com.teraim.fieldapp.utils.PersistenceHelper;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

	// Global variables
	// Define a variable to contain a content resolver instance
	ContentResolver mContentResolver;
	SharedPreferences gh;
	private Messenger mClient;
	
	private final Uri CONTENT_URI = Uri.parse("content://"
			+ SyncContentProvider.AUTHORITY + "/synk");


	/**
	 * Set up the sync adapter
	 * @param globalState 
	 */
	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		/*
		 * If your app uses a content resolver, get an instance of it
		 * from the incoming Context
		 */
		mContentResolver = context.getContentResolver();
		gh = context.getSharedPreferences(Constants.GLOBAL_PREFS,Context.MODE_MULTI_PROCESS);
		if (gh!=null) {

			System.err.println("JAJAJA");

		} else
			System.err.println("NEJNEJNEJ");
	}

	/**
	 * Set up the sync adapter. This form of the
	 * constructor maintains compatibility with Android 3.0
	 * and later platform versions
	 */
	public SyncAdapter(
			Context context,
			boolean autoInitialize,
			boolean allowParallelSyncs) {
		super(context, autoInitialize, allowParallelSyncs);

		/*
		 * If your app uses a content resolver, get an instance of it
		 * from the incoming Context
		 */
		mContentResolver = context.getContentResolver();


	}

	public void setClient(Messenger client) {
		Log.d("vortex","mClient SET! ");
		mClient = client;
	}

	int i=0;
	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {
		boolean fail = false;
		String app=null, user=null, teamName=null;
		return;
		/*
		Log.d("vortex","onPerformSync for ("+gh.getString(PersistenceHelper.USER_ID_KEY, ""));
		
		if(mClient==null) {
			Log.d("vortex","Not ready so discarding call");
			return;
		} else {

			if (gh!=null) {
				String syncMethod = gh.getString(PersistenceHelper.SYNC_METHOD,null);

				if (syncMethod == null || !syncMethod.equals("Internet")) {
					Log.e("vortex","Sync method is not Internet...but sync is on. Will turn sync off and exit..");
					ContentResolver.setSyncAutomatically(account, authority, false);
					fail = true;
				} else {

					app = gh.getString(PersistenceHelper.BUNDLE_NAME, null);
					user = gh.getString(PersistenceHelper.USER_ID_KEY, null);
					teamName = gh.getString(PersistenceHelper.LAG_ID_KEY, null);
					Log.d("vortex","APP: "+app+" user: "+user+" Group: "+teamName);
					if (user == null || user.length()==0 ||
							teamName == null || teamName.length()==0 || 
							app==null || app.length() == 0) {
						Log.e("vortex","user id or group name or app name is null or zero length in Sync Adapter. Cannot sync: "+user+","+teamName+","+app);
						fail = true;
					}
				}
			} else {
				Log.e("vortex","GLOBAL_PREFS null in Sync Adapter. Cannot sync!");
				fail = true;
			}
		}
		//Send a fail msg back to listener. This will turn sync symbol red.
		if (fail) {
			Message msg = Message.obtain(null, SyncService.MSG_SYNC_FAIL);
			try {
				mClient.send(msg);
			} catch (RemoteException e) {

				e.printStackTrace();
			}
			return;
		}


		//Never sync more than 100 at a time.
		final int MaxSyncable = 100;

		//Check values for application, group and user.


		Cursor c = mContentResolver.query(CONTENT_URI, null, null, null, MaxSyncable+"");
		SyncEntry[] sa =null;
		StringBuilder targets=new StringBuilder("");
		long maxStamp = -1;
		if (c!=null) {
			if (c.getCount()==0) {
				Log.d("vortex","Nothing to sync from me!");

			} else {
				Log.d("vortex","cursor had "+c.getCount()+" elems");

				String entryStamp,action,changes,target;
				int cn=0;
				int maxToSync = Math.min(c.getCount(),MaxSyncable);

				sa = new SyncEntry[maxToSync];

				while (c.moveToNext()&&cn<maxToSync) {
					action 		=	c.getString(c.getColumnIndex("action"));
					changes 	=	c.getString(c.getColumnIndex("changes"));
					entryStamp	=	c.getString(c.getColumnIndex("timestamp"));
					target 		= 	c.getString(c.getColumnIndex("target"));

					long es = Long.parseLong(entryStamp);

					if (es>maxStamp) 
						maxStamp=es;
					sa[cn++] = new SyncEntry(action,changes,entryStamp,target);

					targets.append(target);
					targets.append(",");
				}
			}
			c.close();
			//If succesful, update the counter.
			
			Log.d("vortex","SYNCING --> ["+targets+"]");
			Log.d("vortex","Maxstamp--> ["+maxStamp+"]");

			sendAndReceive(teamName,user,app,sa,maxStamp);
			Message msg = Message.obtain(null, SyncService.MSG_SYNC_ENDED);
			try {
				mClient.send(msg);
				} catch (RemoteException e) {

					e.printStackTrace();
				}
			} else 
				Log.e("vortex","DATABASE CURSOR NULL IN SYNCADAPTER AFTER Q CALL");


		}


	
	

	private void sendAndReceive(String teamName,String user, String app, SyncEntry[] sa, long maxStamp) {
		URL url ;
		URLConnection conn=null;
		assert(sa!=null);
		Log.d("vortex","In Send");

		try { 
			url = new URL(Constants.SynkServerURI);
			conn = url.openConnection();

			conn.setConnectTimeout(30*1000);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setUseCaches(false);

			// send object
			Log.d("vortex","creating outstream...");
			ObjectOutputStream objOut = new ObjectOutputStream(conn.getOutputStream());
			//First syncgroup
			Log.d("vortex","writing group..."+teamName);
			objOut.writeObject(teamName);
			//Then user
			Log.d("vortex","writing user..."+user);
			objOut.writeObject(user);
			//Then app name
			Log.d("vortex","writing app..."+app);
			objOut.writeObject(app);
			Long trId= System.currentTimeMillis();
			Log.d("vortex","writing transID..."+trId);
			objOut.writeObject(trId);
			if (sa!=null && sa.length>0) {
				Log.d("vortex","written object...");
				objOut.writeObject(sa);				
			}
			else
				Log.d("vortex","did not write any data...sa empty");
			objOut.writeObject(new EndOfStream());
			//objOut.flush();
			objOut.close();
			ObjectInputStream objIn = new ObjectInputStream(conn.getInputStream());
			
			boolean moreData = true;
			int cc=0;
			//loop while the server has more data to offer.
			while (moreData) {
				Object reply=null;
				Log.d("vortex","Waiting for Data ["+cc++ +"]");

				reply = objIn.readObject();
				Log.d("vortex","After read object. reply is "+(reply==null?"not":"")+" null");

				if (reply instanceof String ) {
					Log.d("vortex","Number of Rows that will arrive: "+reply);
					//We can advance the sync counter for this device. 
					if (maxStamp!=-1) {
						Log.d("vortex","Inserting timestamp for last sync internet: "+maxStamp);
						gh.edit().putString(PersistenceHelper.TIME_OF_LAST_SYNC_INTERNET+teamName,maxStamp+"").apply();
					} else
						Log.d("vortex","Timestamp for Time Of Last Sync not changed for Internet sync.");

					
					boolean notDone = true;
					int i=0;
					ContentValues cv = new ContentValues();
					while (notDone) {
						reply = objIn.readObject();
						if (reply instanceof EndOfStream) {
							Log.d("vortex","Got END OF STREAM!!");
							//TRANSMISSION SUCCESFUL. ACK
							if (i>0) {
								Log.d("vortex","has objects. writing trans ack...");
								//If data has been received, acknowledge.
								//TODO: Consider if ack should wait until firmly written into database.
								//objOut.writeObject(Constants.TRANS_ACK);
								//objOut.flush();
								notDone = false;
								moreData = true;
							} else {
								Log.d("vortex","has no objects, no trans ack");
								//We are done here.
								notDone = false;
								moreData = false;
							}
						}
						else if (reply instanceof SyncFailed) {
							Log.e("vortex","SYNC FAILED. REASON: "+((SyncFailed)reply).getReason());
							notDone=false;
							moreData = false;
							Message msg = Message.obtain(null, SyncService.MSG_SYNC_FAIL);
							try {
								mClient.send(msg);
							} catch (RemoteException e) {
								e.printStackTrace();
							}
						}
						else if (reply instanceof byte[]) {							
							cv.put("data", (byte[])reply);
							mContentResolver.insert(CONTENT_URI, cv);
							Log.d("vortex","sent byte array no "+i+" to database");
							Message msg = Message.obtain(null, SyncService.MSG_SYNC_DATA_INSERTED);
							try {
								mClient.send(msg);
							} catch (RemoteException e) {
								e.printStackTrace();
							}
							
							i++;
						}
						else {
							Log.e("vortex","Got back alien object!!");
							notDone=false;
							moreData = false;
						}
					}

					

				} else {
					Log.e("vortex","OK not returned. instead "+reply.getClass().getCanonicalName());
					moreData = false;

				}
			}
			objIn.close();
			objOut.close();
		} catch (StreamCorruptedException e) {
			Log.e("vortex","Stream corrupted. Reason: "+e.getMessage()+" Timestamp: "+System.currentTimeMillis());
			
		}

		catch (Exception ex) {
			ex.printStackTrace();
			
		}

	*/
	}
}