package com.teraim.fieldapp.synchronization.framework;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.URL;
import java.net.URLConnection;

import com.teraim.fieldapp.non_generics.Constants;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
   
    // Global variables
    // Define a variable to contain a content resolver instance
    ContentResolver mContentResolver;
    /**
     * Set up the sync adapter
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
       // mContentResolver = context.getContentResolver();
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
       // mContentResolver = context.getContentResolver();
        
    }

    int i=0;
	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {
		Log.d("vortex","I am performing sync here!");
		 URL url ;
		 URLConnection conn=null;
			
			try {
				Log.d("vortex","In webservice");
				url = new URL(Constants.SynkServerURI);
				 conn = url.openConnection();
			        conn.setDoInput(true);
			        conn.setDoOutput(true);
			        conn.setUseCaches(false);
			        
			        // send object
			        ObjectOutputStream objOut = new ObjectOutputStream(conn.getOutputStream());
			        objOut.writeObject(new Integer(i++));
			        objOut.flush();
			        objOut.close();
				
			
			
			Object reply=null;

		        ObjectInputStream objIn = new ObjectInputStream(conn.getInputStream());
		        reply = objIn.readObject();
		        if (reply instanceof String)
					Log.d("vortex","got back "+((String)reply));
		        objIn.close();
		        
		    } catch (StreamCorruptedException e) {
		    	Log.e("vortex","Stream corrupted. Reason: "+e.getMessage()+" Timestamp: "+System.currentTimeMillis());
		    }
			
			
			
			catch (Exception ex) {
		      ex.printStackTrace();
		       
		      
		    }
			
	}
}