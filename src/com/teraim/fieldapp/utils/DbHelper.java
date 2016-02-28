package com.teraim.fieldapp.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.ArrayVariable;
import com.teraim.fieldapp.dynamic.types.Table;
import com.teraim.fieldapp.dynamic.types.VarCache;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisConstants;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisObject;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.non_generics.DelyteManager;
import com.teraim.fieldapp.non_generics.NamedVariables;
import com.teraim.fieldapp.synchronization.DataSyncSessionManager;
import com.teraim.fieldapp.synchronization.SyncEntry;
import com.teraim.fieldapp.synchronization.SyncEntryHeader;
import com.teraim.fieldapp.synchronization.SyncReport;
import com.teraim.fieldapp.synchronization.SyncStatus;
import com.teraim.fieldapp.synchronization.SyncStatusListener;
import com.teraim.fieldapp.synchronization.VariableRowEntry;
import com.teraim.fieldapp.ui.MenuActivity.UIProvider;
import com.teraim.fieldapp.utils.Exporter.ExportReport;
import com.teraim.fieldapp.utils.Exporter.Report;

public class DbHelper extends SQLiteOpenHelper {

	// Database Version
	public static final int DATABASE_VERSION = 6;
	// Books table name
	private static final String TABLE_VARIABLES = "variabler";
	public static final String TABLE_AUDIT = "audit";

	private static final String VARID = "var",VALUE="value",TIMESTAMP="timestamp",LAG="lag",AUTHOR="author";
	private static final String[] VAR_COLS = new String[] { TIMESTAMP, AUTHOR, LAG, VALUE };
	//	private static final Set<String> MY_VALUES_SET = new HashSet<String>(Arrays.asList(VAR_COLS));

	private static final int NO_OF_KEYS = 10;
	private static final String SYNC_SPLIT_SYMBOL = "_$_";
	private SQLiteDatabase db;
	private PersistenceHelper globalPh=null,ph=null;

	private final Map<String,String> keyColM = new HashMap<String,String>();
	private Map<String,String> colKeyM = new HashMap<String,String>();

	Context ctx;


	//Helper class that wraps the Cursor.
	public class DBColumnPicker {
		Cursor c;
		private static final String NAME = "var",VALUE="value",TIMESTAMP="timestamp",LAG="lag",CREATOR="author";

		public DBColumnPicker(Cursor c) {
			this.c=c;
		}

		public StoredVariableData getVariable() {
			return new StoredVariableData(pick(NAME),pick(VALUE),pick(TIMESTAMP),pick(LAG),pick(CREATOR));
		}
		public Map<String,String> getKeyColumnValues() {
			Map<String,String> ret = new HashMap<String,String>();
			Set<String> keys = keyColM.keySet();
			String col=null;
			for(String key:keys) {
				col = keyColM.get(key);
				if (col==null)
					col=key;
				if (pick(col)==null)
					continue;
				else
					ret.put(key, pick(col));
			}
			//Log.d("nils","getKeyColumnValues returns "+ret.toString());
			return ret; 
		}

		private String pick(String key) {
			return c.getString(c.getColumnIndex(key));
		}

		public boolean moveToFirst() {
			if (c==null)
				return false;
			else
				return c.moveToFirst();
		}

		public boolean next() {
			boolean b = c.moveToNext();
			if (!b)
				c.close();
			return b;
		}

		public void close() {
			c.close();
		}

	}


	public DbHelper(Context context,Table t, PersistenceHelper globalPh,PersistenceHelper appPh,String bundleName) {
		super(context, bundleName, null, DATABASE_VERSION);  
		Log.d("vortex","Bundle name: "+bundleName);
		ctx = context;
		if (db!=null && db.isOpen())
			db.close();
		db = this.getWritableDatabase();
		
		this.globalPh=globalPh;
		this.ph = appPh;
		if (t!=null)
			init(t.getKeyParts());
		else {
			Log.d("nils","Table doesn't exist yet...postpone init");
		}

	}

	public void closeDatabaseBeforeExit() {
		if (db!=null) {
			db.close();
			Log.e("vortex","database is closed!");
		}
	}

	public void init(ArrayList<String> keyParts) {

		//check if keyParts are known or if a new is needed.

		//Load existing map from sharedStorage.
		String colKey="";
		Log.d("nils","DBhelper init");
		for(int i=1;i<=NO_OF_KEYS;i++) {

			colKey = ph.get("L"+i);
			//If empty, I'm done.
			if (colKey.equals(PersistenceHelper.UNDEFINED)) {
				Log.d("nils","didn't find key L"+i);
				break;
			}
			else {
				keyColM.put(colKey,"L"+i);
				colKeyM.put("L"+i, colKey);
			}
		}
		//Now check the new keys. If a new key is found, add it.
		if (keyParts == null) {
			Log.e("nils","Keyparts were null in DBHelper");
		} else {
			Log.e("nils","Keyparts has"+keyParts.size()+" elements");
			for(int i=0;i<keyParts.size();i++) {
				Log.d("nils","checking keypart "+keyParts.get(i));
				if (keyColM.containsKey(keyParts.get(i))) {
					Log.d("nils","Key "+keyParts.get(i)+" already exists..skipping");
					continue;
				} else if (staticColumn(keyParts.get(i))) {
					Log.e("nils","Key "+keyParts.get(i)+" is a static key. Sure this ok??");

				}				
				else {
					Log.d("nils","Found new column key "+keyParts.get(i));
					if (keyParts.get(i).isEmpty()) {
						Log.d("nils","found empty keypart! Skipping");
					} else {
						String colId = "L"+(keyColM.size()+1);
						//Add key to memory
						keyColM.put(keyParts.get(i),colId);
						colKeyM.put(colId,keyParts.get(i));
						//Persist new column identifier.
						ph.put(colId, keyParts.get(i));
					}
				}

			}
		}
		Log.d("nils","Keys added: ");
		Set<String> s = keyColM.keySet();
		for (String e:s)
			Log.d("nils","Key: "+e+"Value:"+keyColM.get(e));


	}



	private boolean staticColumn(String col) {
		for (String staticCol:VAR_COLS) {
			if (staticCol.equals(col))
				return true;
		}
		return false;
	}



	@Override
	public void onCreate(SQLiteDatabase db) {

		// create variable table Lx columns are key parts.
		String CREATE_VARIABLE_TABLE = "CREATE TABLE variabler ( " +
				"id INTEGER PRIMARY KEY ," + 
				"L1 TEXT , "+
				"L2 TEXT , "+
				"L3 TEXT , "+
				"L4 TEXT , "+
				"L5 TEXT , "+
				"L6 TEXT , "+
				"L7 TEXT , "+
				"L8 TEXT , "+
				"L9 TEXT , "+
				"L10 TEXT , "+
				"var TEXT COLLATE NOCASE, "+
				"value TEXT, "+
				"lag TEXT, "+
				"timestamp TEXT, "+
				"author TEXT ) ";

		//audit table to keep track of all insert,updates and deletes. 
		String CREATE_AUDIT_TABLE = "CREATE TABLE audit ( " +
				"id INTEGER PRIMARY KEY ," + 				
				"timestamp TEXT, "+
				"action TEXT, "+
				"target TEXT, "+
				"changes TEXT ) ";

		// 
		db.execSQL(CREATE_VARIABLE_TABLE);
		db.execSQL(CREATE_AUDIT_TABLE);

		Log.d("NILS","DB CREATED");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older books table if existed
		db.execSQL("DROP TABLE IF EXISTS variabler");
		db.execSQL("DROP TABLE IF EXISTS audit");

		// create fresh books table
		this.onCreate(db);
	}

	/*
	public void exportAllData() {
		Cursor c = db.query(TABLE_VARIABLES,null,
				null,null,null,null,null,null);
		if (c!=null) {

			//"timestamp","lag","author"
			Log.d("nils","Variables found in db:");
			String L[] = new String[keyColM.size()];
			String var,value,timeStamp,lag,author;
			while (c.moveToNext()) {
				var = c.getString(c.getColumnIndex("var"));
				value = c.getString(c.getColumnIndex("value"));
				timeStamp = c.getString(c.getColumnIndex("timestamp"));
				lag = c.getString(c.getColumnIndex("lag"));
				author = c.getString(c.getColumnIndex("author"));
				for (int i=0;i<L.length;i++)
					L[i]=c.getString(c.getColumnIndex("L"+(i+1)));	

			}
		}
	}
	 */

	//Export a specific context with a specific Exporter.
	public Report export(Map<String,String> context, Exporter exporter, String exportFileName) {
		//Check LagID.
		if (exporter == null)
			return new Report(ExportReport.EXPORTFORMAT_UNKNOWN);
		Log.d("nils","Started export");
		Log.d("vortex","filename: "+exportFileName);
		String selection = null;
		List<String> selArgs = null;
		if (context!=null) {
			Log.d("vortex","context: "+context.toString());
			selection = "";
			int i=0;
			String col = null;
			//Build query
			Iterator <String>it = context.keySet().iterator();
			while (it.hasNext()) {
				String key = it.next();
				col = this.getColumnName(key);
				if (col==null) {
					Log.e("nils","Could not find column mapping to columnHeader "+key);
					return new Report(ExportReport.COLUMN_DOES_NOT_EXIST);
				}
				if (context.get(key).equals("*")) {
					selection += col + " LIKE '%'";
					if (it.hasNext())
						selection += " AND ";
				} else {
					selection += col+(it.hasNext()?"=? AND ":"=?");			
					if (selArgs==null) 
						selArgs = new ArrayList<String>();
					selArgs.add(context.get(key));
				}
			}
		}
		//Select.
		Log.d("vortex","selection is "+selection);
		Log.d("vortex","Args is "+selArgs);
		String[] selArgsA = null;
		if (selArgs !=null)
			selArgsA = selArgs.toArray(new String[selArgs.size()]);
		Cursor c = db.query(TABLE_VARIABLES,null,selection,
				selArgsA,null,null,null,null);	
		if (c!=null) {
			Log.d("nils","Variables found in db for context "+context);
			//Wrap the cursor in an object that understand how to pick it!
			Report r = exporter.writeVariables(new DBColumnPicker(c));
			if (r!=null&&r.noOfVars>0) {
				Report res;
				if (Tools.writeToFile(Constants.EXPORT_FILES_DIR+exportFileName+"."+exporter.getType(),r.result)) {
					Log.d("nils","Exported file succesfully");
					c.close();
					res=r;
				} else {
					Log.e("nils","Export of file failed");
					c.close();
					GlobalState.getInstance().getLogger().addRow("EXPORT FILENAME: ["+Constants.EXPORT_FILES_DIR+exportFileName+"."+exporter.getType()+"]");
					res = new Report(ExportReport.FILE_WRITE_ERROR);
				}
				GlobalState.getInstance().getBackupManager().backupExportData(exportFileName+"."+exporter.getType(),r.result);
				return res;
			} else 
				c.close();
			return new Report(ExportReport.NO_DATA);
		} else {
			Log.e("nils","NO Variables found in db for context "+context);
			return new Report(ExportReport.NO_DATA);
		}
	}



	public void printAuditVariables() {
		Cursor c = db.query(TABLE_AUDIT,null,
				null,null,null,null,null,null);
		if (c!=null) {
			Log.d("nils","Variables found in db:");
			while (c.moveToNext()) {
				Log.d("nils","ACTION: "+c.getString(c.getColumnIndex("action"))+
						"CHANGES: "+c.getString(c.getColumnIndex("changes"))+
						"TARGET: "+c.getString(c.getColumnIndex("target"))+
						"TIMESTAMP: "+c.getString(c.getColumnIndex("timestamp")));

			}
		} 
		else 
			Log.e("nils","NO AUDIT VARIABLES FOUND");
		c.close();
	}


	public void printAllVariables() {
		String fileName = Constants.VORTEX_ROOT_DIR+
				globalPh.get(PersistenceHelper.BUNDLE_NAME)+"/dbdump.txt";
		File file = new File(fileName);
		Log.d("vortex","file created at: "+fileName);		
		PrintWriter writer=null;
		Cursor c = null;
		try {
			boolean cre = file.createNewFile();
			if (!cre)
				Log.d("vortex","file was already there");
			writer = new PrintWriter(file, "UTF-8");
			String header = "";
			Log.d("nils","Variables found in db:");
			for (int i = 1;i<=DbHelper.NO_OF_KEYS;i++)
				header+=colKeyM.get("L"+i)+"|";
			header +="var|value";
			Log.d("vortex",header);
			writer.println(header);
			int offSet = 0; boolean notDone=true;

			//Ask for 500 rows per query.
			String row;
			//while (notDone) {

			c = db.query(TABLE_VARIABLES,null,
					null,null,null,null,null,null);//offSet+",100"
			if (c!=null) {

				int i = 0;
				while (c.moveToNext()) {
					row=(
							c.getString(c.getColumnIndex("L1"))+"|"+
									c.getString(c.getColumnIndex("L2"))+"|"+
									c.getString(c.getColumnIndex("L3"))+"|"+
									c.getString(c.getColumnIndex("L4"))+"|"+
									c.getString(c.getColumnIndex("L5"))+"|"+
									c.getString(c.getColumnIndex("L6"))+"|"+
									c.getString(c.getColumnIndex("L7"))+"|"+
									c.getString(c.getColumnIndex("L8"))+"|"+
									c.getString(c.getColumnIndex("L9"))+"|"+
									c.getString(c.getColumnIndex("L10"))+"|"+
									c.getString(c.getColumnIndex("var"))+"|"+
									c.getString(c.getColumnIndex("value")));
					writer.println(row);
					i++;
				}
				c.close();
			}
			/*
					Log.d("vortex","i is "+i);
					if (i<100)
						notDone = false;
				} else {
					Log.d("vortex","C was null!");
					notDone = false;
				}
				offSet+=100;
			}
			 */
		} catch (Exception e) {
			e.printStackTrace();
			if (c!=null)
				c.close();
		}


		if ( writer != null ) 
			writer.close();
	}


	enum ActionType {
		insert,
		delete
	}
	/*
	public void insertAudit(Variable var, ActionType a){
		//for logging
		Log.d("nils", "Audit"); 
		// 1. get reference to writable DB
		//SQLiteDatabase db = this.getWritableDatabase();

		// 2. create ContentValues to add key "column"/value
		ContentValues values = new ContentValues();
		values.put("ruta", var.getRutId()); // get ruta
		values.put("provyta", var.getProvytaId()); // get provyta
		values.put("delyta", var.getDelytaId()); // get delyta
		values.put("smayta", var.getSmaytaId());
		values.put("var", var.getVarId());
		values.put("timestamp", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
		values.put("action", (a==ActionType.insert)?"i":"d");

		// 3. insert
		long rId = db.insert(TABLE_AUDIT, // table
				null, //nullColumnHack
				values); // key/value -> keys = column names/ values = column values

		// 4. close
		//db.close(); 
		var.setDatabaseId(rId);
		Log.d("nils","Inserted new variable with ID "+rId);
	} 
	 */




	public void deleteVariable(String name,Selection s,boolean isSynchronized) {
		// 1. get reference to writable DB
		//SQLiteDatabase db = this.getWritableDatabase();


		int aff = db.delete(TABLE_VARIABLES, //table name
				s.selection,  // selections
				s.selectionArgs); //selections args

		//if(aff==0) 
		//	Log.e("nils","Couldn't delete "+name+" from database. Not found. Sel: "+s.selection+" Args: "+print(s.selectionArgs));
		//else 
		//	Log.d("nils","DELETED: "+ name);

		if (isSynchronized)
			insertDeleteAuditEntry(s,name);
	}



	private void insertDeleteDelytorAuditEntry(String rId,String pyId) {		
		storeAuditEntry("E",rId+"|"+pyId,null);
		Log.d("nils","inserted Erase Delytor into audit for R:"+rId+" P:"+pyId);
	}
	private void insertDeleteProvytaAuditEntry(String rId,String pyId) {		
		storeAuditEntry("P",rId+"|"+pyId,null);
		Log.d("nils","inserted Erase Provyta into audit for R:"+rId+" P:"+pyId);
	}

	private Map<String, String> createAuditEntry(Variable var,String newValue,
			String timeStamp) {
		Map <String,String> valueSet = new HashMap<String,String>();
		//if (!var.isKeyVariable())
		valueSet.put(var.getValueColumnName(), newValue);
		valueSet.put("lag",globalPh.get(PersistenceHelper.LAG_ID_KEY));
		valueSet.put("timestamp", timeStamp);
		valueSet.put("author", globalPh.get(PersistenceHelper.USER_ID_KEY));
		return valueSet;
	}

	private void insertDeleteAuditEntry(Selection s,String varName) {
		//package the value array.
		String dd = "";

		if (s.selectionArgs!=null) {
			String realColNames[] = new String[s.selectionArgs.length];
			//get the true column names.
			String selection = s.selection;
			if (selection == null) {
				Log.e("vortex","Selection was null...no variable name!");
				return;
			}
			String selA="";
			for(String ss:s.selectionArgs)
				selA+=ss+",";
			Log.d("vortex","SelectionArgs: "+selA);
			String zel[] = selection.split("=");
			for (int ii = 0; ii<s.selectionArgs.length;ii++) {
				String z = zel[ii];
				int iz = z.indexOf("L");
				if (iz==-1) {
					if (!z.isEmpty()) {
						int li = z.lastIndexOf(" ");
						String last = z.substring(li+1, z.length());
						if (li!=-1) {
							Log.e("vortex","var is "+last);
							realColNames[ii]=last;
						}
					}
					Log.d("vortex","Found column: "+z);
					Log.d("vortex","real name: "+z);

				} else {
					String col = z.substring(iz,z.length());
					Log.d("vortex","Found column: "+col);
					Log.d("vortex","real name: "+colKeyM.get(col));
					realColNames[ii]=colKeyM.get(col);
				}

			}
			for (int i = 0; i<s.selectionArgs.length;i++)
				dd+=realColNames[i]+"="+s.selectionArgs[i]+"|";
			dd=dd.substring(0,dd.length()-1);
		} else
			dd=null;
		//store
		storeAuditEntry("D",dd,varName);
		//Log.d("nils","INSERT Delete audit entry. Args:  "+dd);
	}

	private void insertAuditEntry(Variable v,Map<String,String> valueSet,String action) {
		String changes = "";
		//First the keys.
		//Log.d("vortex","Inserting Audit entry!");
		Map<String, String> keyChain = v.getKeyChain();
		Iterator<Entry<String,String>> it;
		if (keyChain!=null) {
			it = keyChain.entrySet().iterator();		
			while (it.hasNext()) {			
				Entry<String, String> entry = it.next();			 
				String value = entry.getValue();
				//String column = getColumnName(entry.getKey());
				//Log.d("vortex","column: "+column+" maps to: "+entry.getKey());
				changes+=entry.getKey()+"="+value+"|";
			}
		}
		changes+="var="+v.getId();
		changes += SYNC_SPLIT_SYMBOL;
		//Now the values
		it = valueSet.entrySet().iterator();
		while (it.hasNext()) {			
			Entry<String, String> entry = it.next();			 
			String value = entry.getValue();
			//String column = getColumnName(entry.getKey());
			changes+=entry.getKey()+"="+value;
			if (it.hasNext()) 
				changes+="�";
			else 
				break;	

		}
		//Log.d("nils","Variable name: "+v.getId());
		//Log.d("nils","Audit entry: "+changes);
		storeAuditEntry(action,changes,v.getId());
	}



	private void storeAuditEntry(String action, String changes,String varName) {
		ContentValues values=new ContentValues();
		values.put("action",action);
		values.put("changes",changes);
		values.put("target", varName);
		values.put("timestamp", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
		//need to save timestamp + value
		db.insert(TABLE_AUDIT, null, values);
	}


	public StoredVariableData getVariable(String name, Selection s) {

		Cursor c = db.query(TABLE_VARIABLES,new String[]{"value","timestamp","lag","author"},
				s.selection,s.selectionArgs,null,null,null,null);
		if (c != null && c.moveToFirst() ) {
			StoredVariableData sv = new StoredVariableData(name,c.getString(0),c.getString(1),c.getString(2),c.getString(3));

			//Log.d("nils","Found value and ts in db for "+name+" :"+sv.value+" "+sv.timeStamp);
			c.close();
			return sv;
		} 
		Log.e("nils","Variable "+name+" not found in getVariable DbHelper");
		c.close();
		return null;
	}


	public class StoredVariableData {
		public StoredVariableData(String name,String value, String timestamp,
				String lag, String author) {
			this.timeStamp=timestamp;
			this.value=value;
			this.lagId=lag;
			this.creator=author;
			this.name=name;
		}
		public String name;
		public String timeStamp;
		public String value;
		public String lagId;
		public String creator;
	}

	public final static int MAX_RESULT_ROWS = 500;
	public List<String[]> getValues(String[] columns,Selection s) {

		//Log.d("nils","In getvalues with columns "+dd+", selection "+s.selection+" and selectionargs "+print(s.selectionArgs));
		//Substitute if possible.		
		String[] substCols = new String[columns.length];
		String subs;
		for (int i = 0; i< columns.length;i++) {
			subs = keyColM.get(columns[i]);
			if (subs!=null)
				substCols[i]=subs;
			else
				substCols[i]=columns[i];
		}
		//Get cached selectionArgs if exist.
		//this.printAllVariables();
		Cursor c = db.query(TABLE_VARIABLES,substCols,
				s.selection,s.selectionArgs,null,null,null,null);
		if (c != null && c.moveToFirst()) {
			List<String[]> ret = new ArrayList<String[]>();
			String[] row;
			do {
				row = new String[c.getColumnCount()];
				boolean nullRow = true;
				for (int i=0;i<c.getColumnCount();i++) {
					if (c.getString(i)!=null) {
						if (c.getString(i).equalsIgnoreCase("null"))
							Log.e("nils","StringNull!");					
						row[i]=c.getString(i);
						nullRow = false;
					}

				}
				if (!nullRow) {
					//Log.d("nils","GetValues found row. First elem: "+c.getString(0));
					//only add row if one of the values is not null.
					ret.add(row);
				}
			} while (c.moveToNext());
			//if (ret.size()==0)
			//	Log.d("nils","Found no values in GetValues");
			c.close();
			return ret;
		} 
		String su="[";
		for (String ss:columns)
			su+=ss+",";
		su+="]";
		//Log.d("nils","Did NOT find value in db for columns "+su);
		c.close();
		return null;
	}

	public List<String> getValues(Selection s) {
		Cursor c = db.query(TABLE_VARIABLES,new String[] {"value"},
				s.selection,s.selectionArgs,null,null,null,null);
		List<String> ret = null;
		if (c != null && c.moveToFirst()) {
			ret = new ArrayList<String>();
			do {
				ret.add(c.getString(0));
			} while (c.moveToNext());

		}
		if (c!=null)
			c.close();
		return ret;
	}



	public String getValue(String name, Selection s,String[] valueCol) {
		//Get cached selectionArgs if exist.
		//this.printAllVariables();
		if (!db.isOpen()) {
			Log.e("vortex","Database was gone!!! Creating new");
			Log.d("nils","In getvalue with name "+name+" and selection "+s.selection+" and selectionargs "+print(s.selectionArgs));
			db = this.getWritableDatabase();
		}
		Log.d("nils","In getvalue with name "+name+" and selection "+s.selection+" and selectionargs "+print(s.selectionArgs));
		Cursor c =null;
		if (checkForNulls(s.selectionArgs)) {
		c= db.query(TABLE_VARIABLES,valueCol,
				s.selection,s.selectionArgs,null,null,null,null);
		if (c != null && c.moveToFirst()) {
			//Log.d("nils","Cursor count "+c.getCount()+" columns "+c.getColumnCount());
			String value = c.getString(0);
			Log.d("nils","GETVALUE ["+name+" :"+value+"] Value = null? "+(value==null));
			c.close();		
			return value;
		} 
		}

		Log.d("nils","Did NOT find value in db for "+name+". Key arguments:");

		String sel = s.selection;
		int cc=0;
		String k[]= new String[100];
		for (int i=0;i<sel.length();i++){
			if (sel.charAt(i)=='L') {
				i++;
				k[cc++] = "L"+sel.charAt(i);
			}
		}
		Set<Entry<String, String>> x = colKeyM.entrySet();
		for(Entry e:x) 
			Log.d("nils","kolkey KEY:"+e.getKey()+" "+e.getValue());
		for (int i=0;i<cc;i++) {


			Log.d("nils"," Key: ("+k[i]+") "+colKeyM.get(k[i])+" = "+s.selectionArgs[i]);
		}	

		if (c!=null)
			c.close();
		return null;

	}



	private boolean checkForNulls(String[] selectionArgs) {
		for(String s:selectionArgs)
			if (s==null)
				return false;
		return true;
	}

	public int getId(String name, Selection s) {
		//Log.d("nils","In getId with name "+name+" and selection "+s.selection+" and selectionargs "+print(s.selectionArgs));
		Cursor c = db.query(TABLE_VARIABLES,new String[]{"id"},
				s.selection,s.selectionArgs,null,null,null,null);
		if (c != null && c.moveToFirst()) {
			//Log.d("nils","Cursor count "+c.getCount()+" columns "+c.getColumnCount());
			int value = c.getInt(0);
			//Log.d("nils","Found id in db for "+name+" :"+value);
			c.close();
			return value;
		} 
		//Log.d("nils","Did NOT find value in db for "+name);
		if (c!=null)
			c.close();
		return -1;
	}

	public class TimeAndId {
		int id;
		String time;

	}

	public TimeAndId getIdAndTimeStamp(String name, Selection s) {
		//Log.d("nils","In getId with name "+name+" and selection "+s.selection+" and selectionargs "+print(s.selectionArgs));
		Cursor c = db.query(TABLE_VARIABLES,new String[]{"id","timestamp"},
				s.selection,s.selectionArgs,null,null,null,null);
		if (c != null && c.moveToFirst()) {
			//Log.d("nils","Cursor count "+c.getCount()+" columns "+c.getColumnCount());
			TimeAndId ret = new TimeAndId();
			ret.id = c.getInt(0);
			ret.time = c.getString(1);
			//Log.d("nils","Found id in db for "+name+" :"+value);
			c.close();
			return ret;


		} 
		//Log.d("nils","Did NOT find value in db for "+name);
		if (c!=null)
			c.close();
		return null;
	}


	private String print(String[] selectionArgs) {
		if (selectionArgs == null)
			return "NULL";
		String ret="";
		for(int i=0;i<selectionArgs.length;i++)
			ret+=("["+i+"]: "+selectionArgs[i]+" ");
		return ret;
	}


	//Insert or Update existing value. Synchronize tells if var should be synched over blutooth.
	//This is done in own thread.

	public void insertVariable(Variable var,String newValue,boolean syncMePlease){


		boolean isReplace = false;
		long milliStamp = System.currentTimeMillis();
		String timeStamp = TimeUnit.MILLISECONDS.toSeconds(milliStamp)+"";

		//for logging
		//Log.d("nils", "INSERT VALUE ["+var.getId()+": "+var.getValue()+"] Local: "+isLocal+ "NEW Value: "+newValue); 

		//Delete any existing value.
		deleteVariable(var.getId(),var.getSelection(),false);

		ContentValues values = new ContentValues();
		//Key need to be updated?
		if (var.isKeyVariable()) {
			//Log.d("nils","updating key to "+newValue);
			//String oldValue = var.getKeyChain().put(var.getValueColumnName(), newValue);
			var.getSelection().selectionArgs=createSelectionArgs(var.getKeyChain(),var.getId());
			//Check if the new chain leads to existing variable.
			int id = getId(var.getId(),var.getSelection());
			//Found match. Replace.
			if (id!=-1) {
				//Log.d("nils","variable exists");
				values.put("id", id);
				isReplace=true;
			}
		}
		// 1. create ContentValues to add key "column"/value

		createValueMap(var,newValue,values,timeStamp);
		// 3. insert
		long rId;
		if (isReplace) {
			rId = db.replace(TABLE_VARIABLES, // table
					null, //nullColumnHack
					values
					); 

		} else {
			rId = db.insert(TABLE_VARIABLES, // table
					null, //nullColumnHack
					values
					); 
		}

		if (rId==-1) {
			Log.e("nils","Could not insert variable "+var.getId());
		} else {
			//Log.d("nils","Inserted "+var.getId()+" into database. Values: "+values.toString());//==null?"null":(var.getKeyChain().values()==null?"null":var.getKeyChain().values().toString()));

			//If this variable is not local, store the action for synchronization.
			if (syncMePlease) {
				insertAuditEntry(var,createAuditEntry(var,newValue,timeStamp),"I");

			}
			//else
			//	Log.d("nils","Variable "+var.getId()+" not inserted in Audit: local");
		}
		Log.d("vortex","time used: "+(System.currentTimeMillis()-milliStamp)+"");
	}


	private void createValueMap(Variable var,String newValue,ContentValues values, String timeStamp) {
		//Add column,value mapping.
		Log.d("vortex","in createvaluemap");
		Map<String,String> keyChain=var.getKeyChain();
		//If no key column mappings, skip. Variable is global with Id as key.
		if (keyChain!=null) {
						Log.d("nils","keychain for "+var.getLabel()+" has "+keyChain.size()+" elements");
			for(String key:keyChain.keySet()) { 
				String value = keyChain.get(key);
				String column = getColumnName(key);
				values.put(column,value);
								Log.d("nils","Adding column "+column+"(key):"+key+" with value "+value);
			}
		} else
			Log.d("nils","Inserting global variable "+var.getId()+" value: "+newValue);
		values.put("var", var.getId());
		//if (!var.isKeyVariable()) {
		Log.d("nils","Inserting new value into column "+var.getValueColumnName()+" ("+getColumnName(var.getValueColumnName())+")");
		values.put(getColumnName(var.getValueColumnName()), newValue);
		//}
		values.put("lag",globalPh.get(PersistenceHelper.LAG_ID_KEY));
		values.put("timestamp", timeStamp);
		values.put("author", globalPh.get(PersistenceHelper.USER_ID_KEY));

	}


	//Adds a value for the variable but does not delete any existing value. 
	//This in effect creates an array of values for different timestamps. 
	public void insertVariableSnap(ArrayVariable var, String newValue,
			boolean syncMePlease) {
		//Log.d("vortex","I am in snap insert for variable "+var.getId());
		String timeStamp = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())+"";
		ContentValues values = new ContentValues();
		createValueMap(var,newValue,values,timeStamp);

		long rId = db.insert(TABLE_VARIABLES, // table
				null, //nullColumnHack
				values
				); 
		if (syncMePlease) {
			insertAuditEntry(var,createAuditEntry(var,newValue,timeStamp),"A");

		}
	}



	public void importEverything(VariableRowEntry[] vRows,UIProvider ui) {
		ContentValues cv;
		if (vRows == null)
			return ;

		int counter = 0;
		int total = vRows.length;
		for (VariableRowEntry vRow:vRows) {
			cv = new ContentValues();
			for (int i = 0; i < vRow.valueColumns.size();i++)
				cv.put("L"+i, vRow.valueColumns.get(i));
			cv.put(VARID, vRow.var);
			cv.put(VALUE, vRow.value);
			cv.put(LAG, vRow.lag);
			cv.put(TIMESTAMP, vRow.timeStamp);
			cv.put(AUTHOR, vRow.author);

			db.insert(TABLE_VARIABLES, // table
					null, //nullColumnHack
					cv
					); 	
			counter++;
			if (ui!=null)
				ui.setInfo(counter+"/"+total);
		}

	}

	public VariableRowEntry[] exportEverything(UIProvider ui) {
		VariableRowEntry[] vRows = null;
		List<String> values;
		//Ask for everything.
		Cursor c = db.query(TABLE_VARIABLES,null,null,
				null,null,null,null,null);
		if (c != null && c.getCount()>0 && c.moveToFirst()) {
			vRows = new VariableRowEntry[c.getCount()+1];
			int cn=0;
			do {
				values = new ArrayList<String>(NO_OF_KEYS);
				for (int i=1;i<=NO_OF_KEYS;i++) 
					values.add(c.getString( c.getColumnIndex("L"+i)));

				vRows[cn] = new VariableRowEntry(c.getString(c.getColumnIndex(VARID)),
						c.getString(c.getColumnIndex(VALUE)),
						c.getString(c.getColumnIndex(LAG)),
						c.getString(c.getColumnIndex(TIMESTAMP)),
						c.getString(c.getColumnIndex(AUTHOR)),
						values);

				cn++;
				if (ui!= null)
					ui.setInfo(cn+"/"+c.getCount());
			} while (c.moveToNext());
		}
		c.close();
		return vRows;
	}

	public SyncEntry[] getChanges(UIProvider ui) {
		long maxStamp = 0;
		SyncEntry[] sa=null;
		String timestamp = ph.get(PersistenceHelper.TIME_OF_LAST_SYNC);
		if (timestamp==null||timestamp.equals(PersistenceHelper.UNDEFINED))
			timestamp = "0";
		//Log.d("nils","Time of last sync is "+timestamp+" in getChanges (dbHelper)");
		Cursor c = db.query(TABLE_AUDIT,null,
				"timestamp > ?",new String[] {timestamp},null,null,"timestamp asc",null);
		if (c != null && c.getCount()>0 && c.moveToFirst()) {
			int cn = 1;
			sa = new SyncEntry[c.getCount()+1];
			String entryStamp,action,changes,target;
			maxStamp=0;
			do {
				action 		=	c.getString(c.getColumnIndex("action"));
				changes 	=	c.getString(c.getColumnIndex("changes"));
				entryStamp	=	c.getString(c.getColumnIndex("timestamp"));
				target 		= 	c.getString(c.getColumnIndex("target"));

				long es = Long.parseLong(entryStamp);
				if (es>maxStamp)
					maxStamp=es;
				sa[cn] = new SyncEntry(action,changes,entryStamp,target);
				//Log.d("nils","Added sync entry : "+action+" changes: "+changes+" index: "+cn);
				if (ui!= null)
					ui.setInfo(cn+"/"+c.getCount());
				cn++;				
			} while(c.moveToNext());
			SyncEntryHeader seh = new SyncEntryHeader(maxStamp);
			sa[0]=seh;	
		} else 
			Log.d("nils","no sync needed...no new entries");
		//mySyncEntries = ret;	
		if (c!=null)
			c.close();
		return sa;
	}


	//Map <Set<String>,String>cachedSelArgs = new HashMap<Set<String>,String>();

	public static class Selection {
		public String[] selectionArgs=null;
		public String selection=null;
	}

	public Selection createSelection(Map<String, String> keySet, String name) {

		Selection ret = new Selection();
		//Create selection String.

		//If keyset is null, the variable is potentially global with only name as a key.
		String selection="";
		if (keySet!=null) {
			//selection = cachedSelArgs.get(keySet.keySet());
			//if (selection==null) {
			//Log.d("nils","found cached selArgs: "+selection);

			//Log.d("nils","selection null...creating");
			//Does not exist...need to create.
			selection="";
			//1.find the matching column.
			for (String key:keySet.keySet()) {
				key = getColumnName(key);

				selection+=key+"= ? and ";

			}
			//cachedSelArgs.put(keySet.keySet(), selection);

			//} else {
			//	Log.d("nils","Found cached selection Args: "+selection);
			//}
		}
		selection+="var= ?";

		ret.selection=selection;	

		//Log.d("nils","created new selection: "+selection);

		ret.selectionArgs=createSelectionArgs(keySet,name);
		//Log.d("nils","CREATE SELECTION RETURNS: "+ret.selection+" "+print(ret.selectionArgs));
		return ret;
	}



	private String[] createSelectionArgs(Map<String, String> keySet,String name) {
		String[] selectionArgs;
		if (keySet == null) {
			selectionArgs=new String[] {name};
		} else {
			selectionArgs = new String[keySet.keySet().size()+1];
			int c=0;
			for (String key:keySet.keySet()) {			
				selectionArgs[c++]=keySet.get(key);
				//Log.d("nils","Adding selArg "+keySet.get(key)+" for key "+key);
			}
			//add name part
			selectionArgs[keySet.keySet().size()]=name;
		}
		return selectionArgs;
	}



	public Selection createCoulmnSelection(Map<String, String> keySet) {
		Selection ret = new Selection();
		//Create selection String.

		//If keyset is null, the variable is potentially global with only name as a key.
		String selection=null;
		if (keySet!=null) {
			//selection = cachedSelArgs.get(keySet.keySet());
			//if (selection!=null) {
			//	Log.d("nils","found cached selArgs: "+selection);
			//} else {
			//Log.d("nils","selection null...creating");
			//Does not exist...need to create.
			String col;
			selection="";
			//1.find the matching column.
			List<String>keys = new ArrayList<String>();
			keys.addAll(keySet.keySet());
			for (int i=0;i<keys.size();i++) {
				String key = keys.get(i);

				col = getColumnName(key);
				selection+=col+"= ?"+((i < (keys.size()-1))?" and ":"");


				//		}

				//cachedSelArgs.put(keySet.keySet(), selection);

			} 

			ret.selection=selection;
			String[] selectionArgs = new String[keySet.keySet().size()];
			int c=0;
			for (String key:keySet.keySet()) 		
				selectionArgs[c++]=keySet.get(key);
			ret.selectionArgs=selectionArgs;		
		} 
		return ret;
	}


	//Try to map ColId.
	//If no mapping exist, return colId.

	public String getColumnName(String colId) {
		if (colId==null||colId.length()==0)
			return null;
		String ret = keyColM.get(colId);
		if (ret == null)
			return colId;
		else
			return ret;
	}


	int synC=0;




	public SyncReport synchronise(SyncEntry[] ses, UIProvider ui, LoggerI o, SyncStatusListener syncListener) {
		if (ses == null) {
			Log.d("sync","ses �r tom! i synchronize");
			return null;
		}
		Set<String> touchedVariables = new HashSet<String>();
		SyncReport changes = new SyncReport();
		GlobalState gs = GlobalState.getInstance();
		VarCache vc = gs.getVariableCache();

		int size = ses.length-1;

		Log.d("sync","LOCK!");
		db.beginTransaction();
		String name=null;

		synC=0;
		if (ses==null||ses.length<2) {
			Log.e("sync","either syncarray is short or null. no data to sync.");
			db.endTransaction();
			return null;
		}
		Log.d("nils","In Synchronize with "+ses.length+" arguments.");
		ContentValues cv = new ContentValues();
		Map<String, String> keySet= new HashMap<String,String>();
		for (SyncEntry s:ses) {
			synC++;
			if (synC%10==0) {
				String syncStatus = synC+"/"+size;
				if (ui!=null) 
					ui.setInfo(syncStatus);
				syncListener.send(new SyncStatus(syncStatus));
			}
			if (s.isInsert()||s.isInsertArray()) {				
				keySet.clear();
				cv.clear();

				if (s.getKeys()==null||s.getValues()==null) {
					Log.e("nils","Synkmessage with "+s.getTarget()+" is invalid. Skipping");
					changes.faults++;
					continue;
				}
				String[] keys = s.getKeys().split("\\|");
				String[] values = s.getValues().split("�");
				String[] pair;

				for (String keyPair:keys) {
					pair = keyPair.split("=");
					if (pair!=null) {	
						if (pair.length==1) {
							String k = pair[0];
							pair = new String[2];
							pair[0] = k;
							pair[1] = "";
						}
						//Log.d("nils","Pair "+(c++)+": Key:"+pair[0]+" Value: "+pair[1]);

						if (pair[0].equals("var")) {
							name = pair[1];
						} else 							
							keySet.put(getColumnName(pair[0]),pair[1]);

						//cv contains all elements, except id.
						cv.put(getColumnName(pair[0]),pair[1]);													
					}									
					else {
						changes.faults++;
						Log.e("sync","Something not good in synchronize (dbHelper). A valuepair was null ");
					}
				}
				for (String value:values) {
					pair = value.split("=");
					if (pair!=null) {	
						if (pair.length==1) {
							String k = pair[0];
							pair = new String[2];
							pair[0] = k;
							pair[1] = "";
						}
						cv.put(getColumnName(pair[0]),pair[1]);
					}
				}
				//if (keySet == null) 
				//	Log.d("nils","Keyset was null");
				Log.d("sync","SYNC WITH PARAMETER NAMED "+name);
				//Log.d("sync","Keyset:  "+keySet.toString());

				Selection sel = this.createSelection(keySet, name);
				//Log.d("sync","Selection:  "+sel.selection);
				if (sel.selectionArgs!=null) {
					String xor="";
					for (String sz:sel.selectionArgs)
						xor += sz+",";
					//Log.d("sync","Selection ARGS: "+xor);
				}
				TimeAndId ti = this.getIdAndTimeStamp(name, sel);
				long rId=-1;
				if (ti==null || s.isInsertArray()) {// || gs.getVariableConfiguration().getnumType(row).equals(DataType.array)) {
					Log.d("sync","Vairable doesn't exist or is an Array. Inserting..");
					//now there should be ContentValues that can be inserted.
					rId = db.insert(TABLE_VARIABLES, // table
							null, //nullColumnHack
							cv
							); 	
					changes.inserts++;

				} else {
					boolean existingTimestampIsMoreRecent = (Tools.existingTimestampIsMoreRecent(ti.time,s.getTimeStamp()));
					if (ti.time!=null && s.getTimeStamp()!=null) {
						if (!existingTimestampIsMoreRecent) {
							Log.d("sync","Existing variable is less recent. Replace!");
							cv.put("id", ti.id);
							rId = db.replace(TABLE_VARIABLES, // table
									null, //nullColumnHack
									cv
									); 	
							changes.updates++;

							if (rId!=ti.id) 
								Log.e("sync","CRY FOUL!!! New Id not equal to found! "+" ID: "+ti.id+" RID: "+rId);
						} else {
							changes.refused++;
							o.addRow("");
							o.addYellowText("DB_INSERT REFUSED: "+name+" Timestamp incoming: "+s.getTimeStamp()+" Time existing: "+ti.time);
							Log.d("vortex","DB_INSERT REFUSED: "+name+" Timestamp incoming: "+s.getTimeStamp()+" Time existing: "+ti.time);
						}
					} else
						Log.e("sync","A timestamp was null!!");
				}
				if (rId!=-1)
					touchedVariables.add(name);
				//					Log.e("sync","Did not insert row "+cv.toString());


				//else
				//	Log.d("nils","Insert row: "+cv.toString());

				//Invalidate variables with this id in the cache..

			} else if (s.isDelete()) {
				keySet.clear();
				Log.d("sync","Got Delete for: "+s.getTarget());
				String[] keys = s.getChange().split("\\|");
				String[] pair;
				for (String keyPair:keys) {
					pair = keyPair.split("=");
					if (pair!=null) {	
						if (pair.length==1) {
							String k = pair[0];
							pair = new String[2];
							pair[0] = k;
							pair[1] = "";
						}
						//Log.d("nils","Pair "+(c++)+": Key:"+pair[0]+" Value: "+pair[1]);

						if (pair[0].equals("var")) {
							name = pair[1];
						} else {
							keySet.put(getColumnName(pair[0]),pair[1]);
						}

					}									
					else 
						Log.e("sync","Something not good in synchronize (dbHelper). Could not split on '=': "+keyPair);
				}
				Log.d("sync","DELETE WITH PARAMETER NAMED "+name);
				//Log.d("sync","Keyset:  "+keySet.toString());

				Selection sel = this.createSelection(keySet, name);
				//Log.d("sync","Selection:  "+sel.selection);
				if (sel.selectionArgs!=null) {
					String xor="";
					for (String sz:sel.selectionArgs)
						xor += sz+",";
					Log.d("nils","Selection ARGS: "+xor);
					//Log.d("nils","Calling delete with Selection: "+sel.selection+" args: "+print(sel.selectionArgs));
					//Check timestamp. If timestamp is older, delete. Otherwise skip.
					s.getTimeStamp();
					StoredVariableData sv = this.getVariable(s.getTarget(), sel);
					boolean keep = true;
					if (sv!=null)
						keep=Tools.existingTimestampIsMoreRecent(sv.timeStamp, s.getTimeStamp());
					else
						Log.d("vortex","Did not find variable to delete: "+s.getTarget());
					if (!keep) {
						Log.d("sync","Deleting "+name);
						this.deleteVariable(s.getTarget(), sel,false);
						changes.deletes++;
					}
					else {
						changes.refused++;
						Log.d("sync","Did not delete...a newer entry exists in database.");
						o.addRow("");
						o.addYellowText("DB_DELETE REFUSED: "+name);
						if(sv!=null)
							o.addYellowText(" Timestamp incoming: "+s.getTimeStamp()+" Time existing: "+sv.timeStamp);
						else
							o.addYellowText(", since this variable has no value in my database");
					}
					//Invalidate variables with this id in the cache..
					vc.invalidateOnName(s.getTarget());

				} else
					Log.e("sync","SelectionArgs null in Delete Sync. S: "+s.getTarget()+" K: "+s.getKeys());

			} else if (s.isDeleteDelytor()) {
				String ids = s.getChange();
				if (ids!=null) {
					String[] idA = ids.split("\\|"); 
					if (idA!=null && idA.length==2) {
						Log.d("sync","Got EraseDelytor sync message for ruta "+idA[0]+" and provyta "+idA[1]);					
						this.eraseDelytor(gs,idA[0], idA[1],false);
						//Invalidate Cache.
						vc.invalidateOnKey(Tools.createKeyMap("ruta",idA[0],"provyta",idA[1]));
						o.addRow("");
						o.addGreenText("DB_DELETE All values for Delyta "+idA[0]+" and Provyta "+idA[1]+" was deleted.");

					} else {
						o.addRow("");
						o.addRedText("DB_DELETE Delete Delytor Failed. Message corrupt");
						changes.faults++;
					}
				}
			} else if (s.isDeleteProvyta()) {
				String ids = s.getChange();
				if (ids!=null) {
					String[] idA = ids.split("\\|"); 
					if (idA!=null && idA.length==2) {
						Log.d("sync","Got EraseProvyta sync message for ruta "+idA[0]+" and provyta "+idA[1]);					
						this.eraseProvyta(idA[0], idA[1],false);
						//Invalidate cache.
						vc.invalidateOnKey(Tools.createKeyMap("ruta",idA[0],"provyta",idA[1]));
						o.addRow("");
						o.addGreenText("DB_DELETE All values deleted for Provyta"+idA[1]+" in ruta "+idA[0]);					

					} else {
						changes.faults++;
						o.addRow("");
						o.addRedText("DB_DELETE Delete Provyta Failed. Message corrupted");
					}
				}			
			}

		}
		//Invalidate all variables touched.
		for (String varName:touchedVariables) 
			vc.invalidateOnName(varName); 
		Log.d("vortex", "Touched variables: "+touchedVariables.toString());
		if (ui!=null)
			ui.setInfo(synC+"/"+size);
		Log.d("sync","UNLOCK!");
		endTransactionSuccess();
		return changes;
	}


	public void syncDone(long timeStamp) {
		Log.d("vortex","in syncdone with timestamp "+timeStamp);
		String lastS = ph.get(PersistenceHelper.TIME_OF_LAST_SYNC);
		if (lastS==null||lastS.equals(PersistenceHelper.UNDEFINED))
			lastS= "0";
		long lastTimeStamp = Long.parseLong(lastS);
		if (timeStamp > lastTimeStamp) 
			ph.put(PersistenceHelper.TIME_OF_LAST_SYNC, Long.toString(timeStamp));
		else 
			Log.e("nils","The timestamp of this sync message is LESS than the current timestamp");
		//else
		//	Log.d("nils","maxstamp 0");
	}


	public int getNumberOfUnsyncedEntries() {
		int ret = 0;
		if (globalPh.get(PersistenceHelper.SYNC_METHOD).equals("Bluetooth")) {
			
			String timestamp = ph.get(PersistenceHelper.TIME_OF_LAST_SYNC);
			if (timestamp==null||timestamp.equals(PersistenceHelper.UNDEFINED))
				timestamp = "0";
			//Log.d("nils","Time of last sync is "+timestamp+" in getNumberOfUnsyncedEntries (dbHelper)");
			Cursor c = db.query(TABLE_AUDIT,null,
					"timestamp > ?",new String[] {timestamp},null,null,"timestamp asc",null);
			if (c != null && c.getCount()>0) 
				ret = c.getCount();
			if (c!=null)
				c.close();
			Log.d("vortex","Get SyncEtries BT: "+ret);
			return ret;
		}
		if (globalPh.get(PersistenceHelper.SYNC_METHOD).equals("Internet")) {			
			String timestamp = ph.get(PersistenceHelper.TIME_OF_LAST_SYNC_INTERNET);
			if (timestamp==null||timestamp.equals(PersistenceHelper.UNDEFINED)) {
				timestamp = "0";
				Log.d("vortex","timestamp was null or undefined...will be set to zero");
			}
			//Log.d("nils","Time of last sync is "+timestamp+" in getNumberOfUnsyncedEntries (dbHelper)");
			Cursor c = db.query(TABLE_AUDIT,null,
					"timestamp > ?",new String[] {timestamp},null,null,"timestamp asc",null);
			if (c != null && c.getCount()>0) 
				ret = c.getCount();
			if (c!=null)
				c.close();
			Log.d("vortex","Get SyncEtries Internet: "+ret);
			return ret;
		}		
		return -1;
	}



	public void eraseDelytor(GlobalState gs, String currentRuta, String currentProvyta,boolean synk) {
		String yCol = keyColM.get("�r");
		String rCol = keyColM.get("ruta");
		String pyCol = keyColM.get("provyta");
		String dyCol = keyColM.get("delyta");
		Log.d("nils","In eraseDelytor with rutaCol = "+rCol+" and pyCol = "+pyCol+" and dyCol "+dyCol);
		//delete all rows and send sync message.
		int affRows = db.delete(DbHelper.TABLE_VARIABLES, 
				yCol+"=? AND "+ rCol+"=? AND "+pyCol+"=? and "+dyCol+" NOT NULL", new String[] {Constants.getYear(), currentRuta,currentProvyta});
		Log.d("nils","eraseDelytor affected "+affRows+" rows");
		if (synk&&affRows>0) {
			Log.d("nils","Adding sync message");
			insertDeleteDelytorAuditEntry(currentRuta,currentProvyta);
		}
		Map<String, String> baseKey = gs.getVariableConfiguration().createProvytaKeyMap();
		for (int delyteID=0;delyteID<=DelyteManager.MAX_DELYTEID;delyteID++) {
			baseKey.put("delyta", delyteID+"");	
			if (delyteID >0) {
				Variable tagV = gs.getVariableCache().getVariableUsingKey(baseKey,NamedVariables.DELNINGSTAG);
				//Hack to prevent synk.

				if (tagV!=null) {
					if (synk)
						tagV.deleteValue();
					else
						tagV.deleteValueNoSync();
				}
				else
					break;
			}
			gs.getVariableCache().invalidateOnKey(baseKey);															

		}

	}

	public void eraseProvyta(String currentRuta, String currentProvyta,boolean synk) {
		String yCol = keyColM.get("�r");
		String rCol = keyColM.get("ruta");
		String pyCol = keyColM.get("provyta");
		Log.d("nils","In eraseProvyta with rutaCol = "+rCol+" and pyCol = "+pyCol);
		int affRows = db.delete(DbHelper.TABLE_VARIABLES, 
				yCol+"=? AND "+rCol+"=? AND "+pyCol+"=?", new String[] {Constants.getYear(),currentRuta,currentProvyta});
		Log.d("nils","eraseProvyta affected "+affRows+" rows");
		if (synk&&affRows>0) {
			Log.d("nils","Adding sync message");
			insertDeleteProvytaAuditEntry(currentRuta,currentProvyta);
		}

	}

	public void eraseSmaProvyDelytaAssoc(String currentRuta, String currentProvyta) {
		String yCol = keyColM.get("�r");
		String rCol = keyColM.get("ruta");
		String pyCol = keyColM.get("provyta");
		int affRows = db.delete(DbHelper.TABLE_VARIABLES, 
				yCol+"=? AND "+rCol+"=? AND "+pyCol+"=? AND var = 'Delyta'", new String[] {Constants.getYear(),currentRuta,currentProvyta});
		Log.d("nils","Affected rows in eraseSmaProvyDelytaAssoc: "+affRows);
	}

	public void deleteAllVariablesUsingKey(Map<String,String> keyHash) {
		if (keyHash == null)
			return;
		String queryP="";
		String[] valA = new String[keyHash.keySet().size()];
		Iterator<String> it = keyHash.keySet().iterator();
		String key; int i=0;
		while (it.hasNext()) {
			key = it.next();
			queryP += getColumnName(key) + "= ?";
			if (it.hasNext())
				queryP += " AND ";
			valA[i++]=keyHash.get(key);
		}
		int affRows = db.delete(DbHelper.TABLE_VARIABLES, 
				queryP, valA);
		Log.d("vortex","Deleted "+affRows+"entries in deleteAllVariablesUsingKey");
	}


	final ContentValues valuez = new ContentValues();
	final static String NULL = "null";




	public boolean deleteHistory() {
		try {
			Log.d("nils","deleting all historical values");
			int rows = db.delete(TABLE_VARIABLES, getColumnName("�r")+"= ?", new String[]{VariableConfiguration.HISTORICAL_MARKER});
			Log.d("nils","Deleted "+rows+" rows of history");
		} catch (SQLiteException e) {
			Log.d("nils","not a nils db");
			return false;
		}
		return true;
	}

	public boolean deleteHistoryEntries(String typeColumn, String typeValue) {
		try {
			Log.d("nils","deleting historical values of type "+typeValue);
			int rows = db.delete(TABLE_VARIABLES, getColumnName("�r")+"= ? AND "+getColumnName(typeColumn)+"= ? COLLATE NOCASE", new String[]{VariableConfiguration.HISTORICAL_MARKER,typeValue});
			Log.d("nils","Deleted "+rows+" rows of history");
		} catch (SQLiteException e) {
			Log.d("nils","not a nils db");
			return false;
		}
		return true;
	}

	public boolean fastInsert(Map<String,String> key, String varId, String value) {
		valuez.clear();
		String timeStamp = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())+"";

		for (String k:key.keySet()) 			
			valuez.put(getColumnName(k), key.get(k));	
		valuez.put("var", varId);
		valuez.put("value", value);
		valuez.put("lag",globalPh.get(PersistenceHelper.LAG_ID_KEY));
		valuez.put("timestamp", timeStamp);
		valuez.put("author", globalPh.get(PersistenceHelper.USER_ID_KEY));


		Log.d("nils","inserting:  "+valuez.toString());
		try {
			db.insert(TABLE_VARIABLES, // table

					null, //nullColumnHack
					valuez
					);
		} catch (SQLiteException e) {
			e.printStackTrace();
			return false;
		}
		return true;

	}


	public boolean fastHistoricalInsert(Map<String,String> keys,
			String varId, String value) {

		valuez.clear();
		valuez.put(getColumnName("�r"),VariableConfiguration.HISTORICAL_MARKER);	

		String keyVal=null;
		for(String key:keys.keySet()) {
			keyVal = keys.get(key);
			if (keyVal!=null) {
				if(keyColM.get(key)!=null)
					valuez.put(keyColM.get(key),keyVal);
				else 
					//Column not found. Do not insert!!
					return false ;


			}
		}
		valuez.put("var", varId);
		valuez.put("value", value);
		try {
			db.insert(TABLE_VARIABLES, // table
					null, //nullColumnHack
					valuez
					);
		} catch (SQLiteException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}



	public void insertGisObject(GisObject go) {
		Variable gpsCoord = GlobalState.getInstance().getVariableCache().getVariableUsingKey(go.getKeyHash(), GisConstants.Location);
		Variable geoType = GlobalState.getInstance().getVariableCache().getVariableUsingKey(go.getKeyHash(), GisConstants.Geo_Type);
		if (gpsCoord == null || geoType == null) {
			LoggerI o = GlobalState.getInstance().getLogger();
			o.addRow("");
			o.addRedText("Insert failed for GisObject "+go.getLabel()+" since one or both of the required variables "+GisConstants.Location+" and "+GisConstants.Geo_Type+" is missing from Variables.csv. Please add these and check spelling");
			Log.e("vortex","Insert failed for GisObject "+go.getLabel()+" since one or both of the required variables "+GisConstants.Location+" and "+GisConstants.Geo_Type+" is missing from Variables.csv. Please add these and check spelling");
			return;
		}
		insertVariable(gpsCoord,go.coordsToString(),true);
		insertVariable(geoType,go.getGisPolyType().name(),true);		
		if (gpsCoord == null || geoType == null){
			Log.e("vortex","Insert failed for "+GisConstants.Location+". Hash: "+go.getKeyHash().toString());
		} else
			Log.d("vortex","succesfully inserted new gisobject");
	}

	/*	
	public String getHistoricalValue(String varName,Map<String, String> keyChain) {
		HashMap<String, String> histKeyChain = new HashMap<String,String>(keyChain);
		histKeyChain.put(VariableConfiguration.KEY_YEAR, VariableConfiguration.HISTORICAL_MARKER);		
		return getValue(varName,createSelection(histKeyChain,varName),new String[] {VALUE});
	}
	 */
	//Get values for all instances of a given variable, from a keychain with * values.

	public DBColumnPicker getAllVariableInstances(Selection s) {
		Cursor c = db.query(TABLE_VARIABLES,null,s.selection,
				s.selectionArgs,null,null,null,null);//"timestamp DESC","1");
		return new DBColumnPicker(c);
	}
	public DBColumnPicker getLastVariableInstance(Selection s) {
		Cursor c = db.query(TABLE_VARIABLES,null,s.selection,
				s.selectionArgs,null,null,"timestamp DESC","1");
		return new DBColumnPicker(c);
	}
	//Generates keychains for all instances.
	public Set<Map<String,String>> getKeyChainsForAllVariableInstances(String varID,
			Map<String, String> keyChain, String variatorColumn) {
		Set<Map<String,String>> ret = null;
		String variatorColTransl = this.getColumnName(variatorColumn);
		//Get all instances of variable for variatorColumn.
		Selection s = this.createSelection(keyChain, varID);
		Cursor c = db.query(TABLE_VARIABLES,new String[] {variatorColTransl},
				s.selection,s.selectionArgs,null,null,null,null);
		Map<String, String> varKeyChain;
		if (c != null && c.moveToFirst()) {
			String variatorInstance;
			do {
				variatorInstance=c.getString(0);
				//Log.d("nils","Found instance value "+variatorInstance+" for varID "+varID+" and variatorColumn "+variatorColumn+" ("+variatorColTransl+")");
				varKeyChain=new HashMap<String,String>(keyChain);
				varKeyChain.put(variatorColumn, variatorInstance);
				if (ret == null)
					ret = new HashSet<Map<String,String>>();
				ret.add(varKeyChain);
			} while (c.moveToNext());


		}
		if (c!=null)
			c.close();
		return ret;
	}



	public Map<String, String> preFetchValuesForAllMatchingKey(Map<String,String> keyChain, String namePrefix) {		

		String query = "SELECT "+VARID+",value FROM "+TABLE_VARIABLES+
				" WHERE "+VARID+" LIKE '"+namePrefix+"%'";

		//Add keychain parts.
		String[] selArgs = new String[keyChain.size()];
		int i=0;
		for (String key:keyChain.keySet()) {
			query += " AND "+this.getColumnName(key)+"= ?";
			selArgs[i++]=keyChain.get(key);
			Log.d("vortex","column: "+this.getColumnName(key)+" SelArg: "+keyChain.get(key));
		}
		Log.d("vortex","Selarg: "+selArgs.toString());
		
		
		
		Cursor c = db.rawQuery(query, selArgs);
		Log.d("nils","Got "+c.getCount()+" results. PrefetchValue."+namePrefix+" with key: "+keyChain.toString());
		Map<String,String> ret = new HashMap<String,String>();
		if (c!=null && c.moveToFirst() ) {
			do {
				ret.put(c.getString(0),c.getString(1));
			} while (c.moveToNext());

		}
		if (c!=null)
			c.close();
		return ret;

	}




	//Fetch all instances of Variables matching namePrefix (group id). Map varId to a Map of Variator, Value.
	public Map<String, Map<String,String>> preFetchValues(Map<String,String> keyChain, String namePrefix, String variatorColumn) {		

		Cursor c = getPrefetchCursor(keyChain,namePrefix,variatorColumn);		
		Map<String, Map<String,String>> ret = new HashMap<String, Map<String,String>>();
		if (c!=null && c.moveToFirst() ) {
			Log.d("nils","In prefetchValues. Got "+c.getCount()+" results. PrefetchValues "+namePrefix+" with key "+keyChain.toString());
			do {
				String varId = c.getString(0);
				if (varId!=null) {
					Map<String,String> varMap = ret.get(varId);
					if (varMap==null) {
						varMap=new HashMap<String,String>();
						ret.put(varId, varMap);
					}
					varMap.put(c.getString(1), c.getString(2));
				}
				Log.d("nils","varid: "+c.getString(0)+" variator: "+c.getString(1)+" value: "+c.getString(2));
			} while (c.moveToNext());

		}
		if (c!=null)
			c.close();
		return ret;

	}


	//Fetch all instances of Variables matching namePrefix. Map varId to a Map of Variator, Value.
	public Cursor getPrefetchCursor(Map<String,String> keyChain, String namePrefix, String variatorColumn) {		

		String query = "SELECT "+VARID+","+getColumnName(variatorColumn)+",value FROM "+TABLE_VARIABLES+
				" WHERE "+VARID+" LIKE '"+namePrefix+"%'";

		//Add keychain parts.
		String[] selArgs = new String[keyChain.size()];
		int i=0;
		for (String key:keyChain.keySet()) {
			query += " AND "+this.getColumnName(key)+"= ?";
			selArgs[i++]=keyChain.get(key);
		}

		Log.d("nils","Query: "+query);
		//Return cursor.
		return db.rawQuery(query, selArgs);

	}



	public void beginTransaction() {
		db.beginTransaction();
	}

	public void endTransactionSuccess() {
		db.setTransactionSuccessful();
		db.endTransaction();
	}
	
	
	
	/* Checks if external storage is available for read and write */
	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}


	



}