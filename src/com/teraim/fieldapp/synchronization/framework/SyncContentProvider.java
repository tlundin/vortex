package com.teraim.fieldapp.synchronization.framework;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.synchronization.SyncEntry;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.PersistenceHelper;

public class SyncContentProvider extends ContentProvider {

	private static final String TAG = "SynkDataProvider";	   

	public static final String AUTHORITY = "com.teraim.fieldapp.provider";

	private DatabaseHelper db;

	private SharedPreferences ph;

	private DatabaseHelper dbHelper;


	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context, String databaseName) {

			super(context, databaseName, null, DbHelper.DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.e("vortex","Not creating anything with this helper!!");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.e("vortex","Not upgrading anything with this helper!!");
		}
	}







	@Override
	public boolean onCreate() {
		Log.d("vortex","ON CREATE CALLED FOR SYNC CONTENT PROVIDER!!!");
		return true;
	}







	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		Context ctx = getContext();

		SharedPreferences globalPh = ctx.getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_MULTI_PROCESS);
		String bundleName = globalPh.getString(PersistenceHelper.BUNDLE_NAME,null);

		if (bundleName == null || bundleName.length()==0) {
			Log.e("vortex","Bundlename was null in content provider!");
			return null;
		} else {
			ph = ctx.getSharedPreferences(bundleName, Context.MODE_MULTI_PROCESS);

			if (ph == null) {
				Log.e("vortex","persistencehelper null in onCreate");
				return null;
			} else
				if (dbHelper == null) 
					dbHelper = new DatabaseHelper(getContext(),bundleName);



			String timestamp = ph.getString(PersistenceHelper.TIME_OF_LAST_SYNC_INTERNET,"0");
			SQLiteDatabase db = dbHelper.getReadableDatabase();
			Cursor c = db.query(DbHelper.TABLE_AUDIT,null,
					"timestamp > ?",new String[] {timestamp},null,null,"timestamp asc",null);
			return c;
		}
	}







	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}







	@Override
	public Uri insert(Uri uri, ContentValues values) {
		//used to update the timestamp counter.
		
			int i=0;
			byte[] b;
			while (true) {
				b = values.getAsByteArray(i+"");
				if (b==null) {
					Log.d("vortex","found no entry for "+i+" so i am done. Saving the time of last sync!");
					break;
				}
				//turn byte array into sync entry[]
				Log.d("vortex","B is instance of "+b.getClass().getCanonicalName());
				Log.d("vortex","And this is a "+this.bytesToObject(b).getClass());
				Log.d("vortex","Looks like "+this.bytesToObject(b));

				Object o = this.bytesToObject(b);
				if (o!=null && o instanceof SyncEntry[]) {
					SyncEntry[] se = (SyncEntry[])o;

					Log.d("vortex","Have a SyncEntry[] object!");
					i++;
				} else {
					Log.e("vortex","Database corrupt...object either null or not syncentry array: "+o);
					break;
				}

			} 



			String ts = values.getAsString("timestamp");
			if (ts!=null) 
				ph.edit().putString(PersistenceHelper.TIME_OF_LAST_SYNC_INTERNET, ts).commit();
			else
				Log.d("vortex","no timestamp change in insert!");
		return null;

	}


	private Object bytesToObject(byte[] inB) {
		ByteArrayInputStream bis = new ByteArrayInputStream(inB);
		ObjectInput in = null;
		Object o =null;
		try {
			in = new ObjectInputStream(bis);
			o = in.readObject(); 

		} catch (IOException e) {

			e.printStackTrace();
			return null;
		} catch (ClassNotFoundException e) {

			e.printStackTrace();
			return null;
		} finally {
			try {
				bis.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		if (o==null)
			Log.d("vortex","returning null in bytestoobject for "+inB+" with l "+inB.length+" inBS "+inB.toString());
		return o;
	}




	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}







	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}




}
