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
		final String bundleName = globalPh.getString(PersistenceHelper.BUNDLE_NAME,null);
		final String teamName =  globalPh.getString(PersistenceHelper.LAG_ID_KEY,"");
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

			
			//Timestamp key includes team name, since change of team name should lead to resync from zero.
			String timestamp = ph.getString(PersistenceHelper.TIME_OF_LAST_SYNC_INTERNET+teamName,"0");
			Log.d("vortex","Timestamp for last sync in Query is "+timestamp);
			SQLiteDatabase db = dbHelper.getReadableDatabase();
			Cursor c = db.query(DbHelper.TABLE_AUDIT,null,
					"timestamp > ?",new String[] {timestamp},null,null,"timestamp asc",null);
			return c;
		}
	}







	@Override
	public String getType(Uri uri) {
		return null;
	}







	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		db.insert(DbHelper.TABLE_SYNC, null, values);
		return null;
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
