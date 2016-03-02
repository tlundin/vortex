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
		String app=null, user=null, group=null;

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
					group = gh.getString(PersistenceHelper.LAG_ID_KEY, null);
					Log.d("vortex","APP: "+app+" user: "+user+" Group: "+group);
					if (user == null || user.length()==0 ||
							group == null || group.length()==0 || 
							app==null || app.length() == 0) {
						Log.e("vortex","user id or group name or app name is null or zero length in Sync Adapter. Cannot sync: "+user+","+group+","+app);
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

		final Uri CONTENT_URI = Uri.parse("content://"
				+ SyncContentProvider.AUTHORITY + "/synk");

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
			Bundle bundle = new Bundle();
			Log.d("vortex","SYNCING --> ["+targets+"]");
			if (sendAndReceive(group,user,app,sa,bundle,maxStamp)) {
				//mContentResolver.insert(CONTENT_URI, cv);
				Message msg = Message.obtain(null, SyncService.MSG_SYNC_DATA);
				msg.setData(bundle);
				try {
					mClient.send(msg);
				} catch (RemoteException e) {

					e.printStackTrace();
				}
			} else {
				Log.e("vortex","Send failed!");
				Message msg = Message.obtain(null, SyncService.MSG_SYNC_FAIL);
				try {
					mClient.send(msg);
				} catch (RemoteException e) {

					e.printStackTrace();
				}
			}


		}


	}

	private boolean sendAndReceive(String group,String user, String app, SyncEntry[] sa, Bundle bundle, long maxStamp) {
		URL url ;
		URLConnection conn=null;
		assert(sa!=null);
		Log.d("vortex","In Send");

		try { 
			url = new URL(Constants.SynkServerURI);
			conn = url.openConnection();
			conn.setConnectTimeout(10*1000);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setUseCaches(false);

			// send object
			Log.d("vortex","creating outstream...");
			ObjectOutputStream objOut = new ObjectOutputStream(conn.getOutputStream());
			//First syncgroup
			Log.d("vortex","writing group..."+group);
			objOut.writeObject(group);
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
			objOut.flush();


			Object reply=null;
			Log.d("vortex","...now waiting for data back");
			ObjectInputStream objIn = new ObjectInputStream(conn.getInputStream());

			reply = objIn.readObject();
			Log.d("vortex","After read object. reply is "+(reply==null?"not":"")+" null");

			if (reply instanceof String)
				Log.d("vortex","got back "+reply.toString());

			if (reply instanceof String && reply.equals("OK")) {
				Log.d("vortex","ok!! "+reply.toString());
				bundle.putLong("timestamp",maxStamp);
				Log.d("vortex","Timestamp for last sync entry from me is "+maxStamp);
				boolean notDone = true;
				int i=0;
				while (notDone) {
					reply = objIn.readObject();
					if (reply instanceof EndOfStream) {
						Log.d("vortex","Got END OF STREAM!!");
						return true;
					}
					else if (reply instanceof SyncFailed) {
						Log.e("vortex","SYNC FAILED. REASON: "+((SyncFailed)reply).getReason());
						notDone=false;
					}
					else if (reply instanceof byte[]) {
						bundle.putByteArray(i+"", (byte[])reply);
						Log.d("vortex","inserted byte array no "+i+" into bundle");
						i++;
					}
					else {
						Log.e("vortex","Got back alien object!!");
						notDone=false;
					}
				}
				objIn.close();
				objOut.close();

			} else {
				Log.e("vortex","OK not returned. instead "+reply.getClass().getCanonicalName());

				return false;

			}
		} catch (StreamCorruptedException e) {
			Log.e("vortex","Stream corrupted. Reason: "+e.getMessage()+" Timestamp: "+System.currentTimeMillis());
			return false;
		}



		catch (Exception ex) {
			ex.printStackTrace();
			Log.d("vortex","In Sending false back");
			return false;
		}

		Log.d("vortex","In Sending false back");
		return false;
	}
}