package com.teraim.vortex.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.bluetooth.BluetoothConnectionService;
import com.teraim.vortex.bluetooth.SyncEntry;
import com.teraim.vortex.bluetooth.SyncEntryHeader;
import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.dynamic.types.Table;
import com.teraim.vortex.dynamic.types.VarCache;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.non_generics.DelyteManager;
import com.teraim.vortex.non_generics.NamedVariables;
import com.teraim.vortex.utils.JSONExporter.Report;

public class DbHelper extends SQLiteOpenHelper {

	// Database Version
	private static final int DATABASE_VERSION = 6;
	// Database Name
	private static final String DATABASE_NAME = "Nils";

	// Books table name
	private static final String TABLE_VARIABLES = "variabler";
	private static final String TABLE_AUDIT = "audit";

	private static final String VARID = "var",VALUE="value",TIMESTAMP="timestamp",LAG="lag",AUTHOR="author";
	private static final String[] VAR_COLS = new String[] { TIMESTAMP, AUTHOR, LAG, VALUE };
	private static final Set<String> MY_VALUES_SET = new HashSet<String>(Arrays.asList(VAR_COLS));

	private static final int NO_OF_KEYS = 10;
	private static final String SYNC_SPLIT_SYMBOL = "_$_";
	private final SQLiteDatabase db;
	private final PersistenceHelper ph;

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
			//Type is stored in extended data. Grab it first.

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


	public DbHelper(Context context,Table t, PersistenceHelper ph) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);  
		ctx = context;
		db = this.getWritableDatabase();
		this.ph=ph;
		if (t!=null)
			init(t.getKeyParts());
		else {
			Log.d("nils","Table doesn't exist yet...postpone init");
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

		fastPrep();
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

	public enum JsonReport {
		OK,
		NO_DATA,
		FILE_WRITE_ERROR,
		COLUMN_DOES_NOT_EXIST
	}
	//Export all rows that have Column = Value
	public JsonReport export(String column,String key, boolean isKlar, String rutaSorteringsTyp) {
		//Check LagID.
		String lagID = ph.get(PersistenceHelper.LAG_ID_KEY);

		Log.d("nils","Started exportRuta");
		JSONExporter exporter = JSONExporter.getInstance(ctx);
		String col = keyColM.get(column);
		if (col==null) {
			Log.e("nils","Could not find column mapping to columnHeader "+column);
			return JsonReport.COLUMN_DOES_NOT_EXIST;
		}
		else {
			String selection = col+"= ? and "+keyColM.get(VariableConfiguration.KEY_YEAR)+"= ?";
			String selectionArgs[] = new String[] {key,Constants.CurrentYear};
			Cursor c = db.query(TABLE_VARIABLES,null,selection,
					selectionArgs,null,null,null,null);	
			if (c!=null) {
				Log.d("nils","Variables found in db for column "+column);
				//Wrap the cursor in an object that understand how to pick it!
				Report r = exporter.writeVariables(new DBColumnPicker(c));
				if (r!=null) {
					DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HH_mm");
					Date date = new Date();
					String dS = (dateFormat.format(date));

					String typPrefix = rutaSorteringsTyp==null||rutaSorteringsTyp.equals("Normal")?"N_":
						(rutaSorteringsTyp.equals("Kontroll")?"K_":(rutaSorteringsTyp.equals("Flaggskepp")?"F_":"N_"));

					if (Tools.writeToFile(Constants.EXPORT_FILES_DIR+typPrefix+lagID+"_"+column+"_"+key+"_"+dS+(isKlar?"_KLAR":""),r.result)) {
						Log.d("nils","Exported json file succesfully");
						return JsonReport.OK;
					} else {
						Log.d("nils","Export of json file failed");
						return JsonReport.FILE_WRITE_ERROR;
					}
				} else {
					Log.e("nils", "Got NULL back from JSONwriter!");
					return JsonReport.NO_DATA;
				}

			} else {
				Log.e("nils","NO Variables found in db for column "+column+" with key value "+key);
				return JsonReport.NO_DATA;
			}
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

		Cursor c = db.query(TABLE_VARIABLES,null,
				null,null,null,null,null,null);
		if (c!=null) {
			Log.d("nils","Variables found in db:");
			while (c.moveToNext()) {
				Log.d("nils","VAR:"+c.getString(c.getColumnIndex("var"))+
						"VALUE:"+c.getString(c.getColumnIndex("value"))+
						"L1:"+c.getString(c.getColumnIndex("L1"))+
						"L2:"+c.getString(c.getColumnIndex("L2"))+
						"L3:"+	c.getString(c.getColumnIndex("L3"))+
						"L4:"+	c.getString(c.getColumnIndex("L4"))+
						"L5:"+	c.getString(c.getColumnIndex("L5"))+
						"L6:"+	c.getString(c.getColumnIndex("L6"))+
						"L7:"+	c.getString(c.getColumnIndex("L7"))+
						"L8:"+	c.getString(c.getColumnIndex("L8"))+
						"L9:"+	c.getString(c.getColumnIndex("L9"))+
						"L10:"+	c.getString(c.getColumnIndex("L10")));
			}
		}
		c.close();
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

	private void insertDeleteAuditEntry(Selection s,String varName) {
		//package the value array.
		String dd = "";
		if (s.selectionArgs!=null) {
			for (int i = 0; i<s.selectionArgs.length-1;i++)
				dd+=s.selectionArgs[i]+"|";
			dd+=s.selectionArgs[s.selectionArgs.length-1];
		} else
			dd=null;
		//store
		storeAuditEntry("D",s.selection+SYNC_SPLIT_SYMBOL+dd,varName);
		//Log.d("nils","INSERT Delete audit entry. Args:  "+dd);
	}

	private void insertDeleteDelytorAuditEntry(String rId,String pyId) {		
		storeAuditEntry("E",rId+"|"+pyId,null);
		Log.d("nils","inserted Erase Delytor into audit for R:"+rId+" P:"+pyId);
	}
	private void insertDeleteProvytaAuditEntry(String rId,String pyId) {		
		storeAuditEntry("P",rId+"|"+pyId,null);
		Log.d("nils","inserted Erase Provyta into audit for R:"+rId+" P:"+pyId);
	}
	private void insertAuditEntry(Variable v,Map<String,String> valueSet) {
		String changes = "";
		//First the keys.
		Map<String, String> keyChain = v.getKeyChain();
		Iterator<Entry<String,String>> it;
		if (keyChain!=null) {
			it = keyChain.entrySet().iterator();		
			while (it.hasNext()) {			
				Entry<String, String> entry = it.next();			 
				String value = entry.getValue();
				String column = getColumnName(entry.getKey());
				changes+=column+"="+value+"|";
			}
		}
		changes+="var="+v.getId();
		changes += SYNC_SPLIT_SYMBOL;
		//Now the values
		it = valueSet.entrySet().iterator();
		while (it.hasNext()) {			
			Entry<String, String> entry = it.next();			 
			String value = entry.getValue();
			String column = getColumnName(entry.getKey());
			changes+=column+"="+value;
			if (it.hasNext()) 
				changes+="§";
			else 
				break;	

		}

		//Log.d("nils","Audit entry: "+changes);
		storeAuditEntry("I",changes,v.getId());
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
			c.close();

		}
		return ret;
	}



	public String getValue(String name, Selection s,String[] valueCol) {
		//Log.d("nils","In getvalue with name "+name+" and selection "+s.selection+" and selectionargs "+print(s.selectionArgs));
		//Get cached selectionArgs if exist.
		//this.printAllVariables();
		Cursor c = db.query(TABLE_VARIABLES,valueCol,
				s.selection,s.selectionArgs,null,null,null,null);
		if (c != null && c.moveToFirst()) {
			//Log.d("nils","Cursor count "+c.getCount()+" columns "+c.getColumnCount());
			String value = c.getString(0);
			//Log.d("nils","GETVALUE ["+name+" :"+value+"] Value = null? "+(value==null));
			c.close();		
			return value;
		} 
		/*
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
		 */
		c.close();
		return null;

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
		c.close();
		return -1;
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


	public void insertVariable(Variable var,String newValue,boolean syncMePlease){
		boolean isReplace = false;
		String timeStamp = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())+"";
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

		//Add column,value mapping.
		Map<String,String> keyChain=var.getKeyChain();
		//If no key column mappings, skip. Variable is global with Id as key.
		if (keyChain!=null) {
			//			Log.d("nils","keychain has "+keyChain.size()+" elements");
			for(String key:keyChain.keySet()) { 
				String value = keyChain.get(key);
				String column = getColumnName(key);
				values.put(column,value);
				//				Log.d("nils","Adding column "+column+"(key):"+key+" with value "+value);
			}
		} //else
		//	Log.d("nils","Inserting global variable "+var.getId()+" value: "+newValue);
		values.put("var", var.getId());
		//if (!var.isKeyVariable()) {
		//Log.d("nils","Inserting new value into column "+var.getValueColumnName()+" ("+getColumnName(var.getValueColumnName())+")");
		values.put(getColumnName(var.getValueColumnName()), newValue);
		//}
		values.put("lag",ph.get(PersistenceHelper.LAG_ID_KEY));
		values.put("timestamp", timeStamp);
		values.put("author", ph.get(PersistenceHelper.USER_ID_KEY));

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
				Map <String,String> valueSet = new HashMap<String,String>();
				//if (!var.isKeyVariable())
				valueSet.put(getColumnName(var.getValueColumnName()), newValue);
				valueSet.put("lag",ph.get(PersistenceHelper.LAG_ID_KEY));
				valueSet.put("timestamp", timeStamp);
				valueSet.put("author", ph.get(PersistenceHelper.USER_ID_KEY));
				insertAuditEntry(var,valueSet);

			}
			//else
			//	Log.d("nils","Variable "+var.getId()+" not inserted in Audit: local");
		}
	}



	//private SyncEntry[] mySyncEntries;

	public SyncEntry[] getChanges() {
		long maxStamp = 0;
		SyncEntry[] sa=null;
		String timestamp = ph.get(PersistenceHelper.TIME_OF_LAST_SYNC);
		if (timestamp==null||timestamp.equals(PersistenceHelper.UNDEFINED))
			timestamp = "0";
		Log.d("nils","Time of last sync is "+timestamp+" in getChanges (dbHelper)");
		Cursor c = db.query(TABLE_AUDIT,null,
				"timestamp > ?",new String[] {timestamp},null,null,"timestamp asc",null);
		if (c != null && c.getCount()>0 && c.moveToFirst()) {
			int cn = 1;
			sa = new SyncEntry[c.getCount()+1];
			String entryStamp,action,changes,target;
			maxStamp=0;
			do {
				action = 	 c.getString(c.getColumnIndex("action"));
				changes =	 c.getString(c.getColumnIndex("changes"));
				entryStamp = c.getString(c.getColumnIndex("timestamp"));
				target = c.getString(c.getColumnIndex("target"));
				long es = Long.parseLong(entryStamp);
				if (es>maxStamp)
					maxStamp=es;
				sa[cn] = new SyncEntry(action,changes,entryStamp,target);
				//Log.d("nils","Added sync entry : "+action+" changes: "+changes+" index: "+cn);
				cn++;				
			} while(c.moveToNext());
			SyncEntryHeader seh = new SyncEntryHeader(maxStamp);
			sa[0]=seh;	
		} else 
			Log.d("nils","no sync needed...no new audit data");
		//mySyncEntries = ret;	
		return sa;
	}


	Map <Set<String>,String>cachedSelArgs = new HashMap<Set<String>,String>();

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

				cachedSelArgs.put(keySet.keySet(), selection);

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
	public void synchronise(SyncEntry[] ses,boolean isMaster, VarCache vc,GlobalState gs) {
		String name=null;
		/*
		if (isMaster) {
			SyncEntry[] myChanges = getMyChanges();
			//Check if incoming entry has changed by me.
			//Arrange the variables I have touched into Set.
			
			if (myChanges!=null) {
				Set<String> myChangesSet=new HashSet<String>();
			
				for (SyncEntry s:myChanges) {							
					myChangesSet.add(s.getKeys());
				}
				//Go through incoming. Erase changes that conflicts with master.
				for (SyncEntry s:ses) {
					if (myChangesSet.contains(s.getKeys())) {
						Log.d("nils","Master has touched variable ["+s.getTarget()+"]. Change will *NOT* be regarded!");
						s.markAsInvalid();
					} else {
						Log.d("nils","Master changeset did not contain "+s.getTarget()+". It contains "+myChangesSet.toString());
					}
				}					
			}
		}
		*/
		synC=0;
		if (ses==null||ses.length<2) {
			Log.e("nils","either syncarray is short or null. no data to sync.");
			return;
		}
			Log.d("nils","In Synchronize with "+ses.length+" arguments. I am "+(isMaster?"Master":"Client"));
		for (SyncEntry s:ses) {
			
			if (synC++%10==0)
				gs.sendEvent(BluetoothConnectionService.PING_FROM_UPDATE);			
			if (s.isInvalid()) {
				Log.e("nils"," Target "+s.getTarget()+" is invalid. Skipping..");
				continue;
			}

			if (s.isInsert()) {				
				Map<String, String> keySet=null; 
				ContentValues cv = new ContentValues();
				if (s.getKeys()==null||s.getValues()==null) {
					Log.e("nils","Synkmessage with "+s.getTarget()+" is invalid. Skipping");				
					continue;
				}
				String[] keys = s.getKeys().split("\\|");
				String[] values = s.getValues().split("§");
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
							if (keySet == null)
								keySet = new HashMap<String, String>();
							keySet.put(pair[0],pair[1]);
						}
						//cv contains all elements, except id.
						cv.put(pair[0],pair[1]);													
					}									
					else 
						Log.e("nils","Something not good in synchronize (dbHelper). A valuepair was null ");
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
						cv.put(pair[0],pair[1]);
					}
				}
				//if (keySet == null) 
				//	Log.d("nils","Keyset was null");
				//Log.d("nils","SYNC WITH PARAMETER NAMED "+name);
				Selection sel = this.createSelection(keySet, name);
				int id = this.getId(name, sel);
				long rId=-1;
				if (id==-1) {
					Log.d("nils","Vairable doesn't exist. Inserting..");
					//now there should be ContentValues that can be inserted.
					rId = db.insert(TABLE_VARIABLES, // table
							null, //nullColumnHack
							cv
							); 	


				} else {
					Log.d("nils","Vairable exists! Replacing..");
					cv.put("id", id);
					rId = db.replace(TABLE_VARIABLES, // table
							null, //nullColumnHack
							cv
							); 	

					if (rId!=id) 
						Log.e("nils","CRY FOUL!!! New Id not equal to found! "+" ID: "+id+" RID: "+rId);
				}
				if (rId==-1) 
					Log.e("nils","Could not insert row "+cv.toString());

				//else
				//	Log.d("nils","Insert row: "+cv.toString());

				//Invalidate variables with this id in the cache..
				vc.invalidateOnName(name);
			} else if (s.isDelete()) {
				//Log.d("nils","Got Delete for: "+s.getTarget());
				if (s.getValues()!=null) {
					String selection = s.getKeys();
					String[] selectionArgs = s.getValues().split("\\|");
					if (selectionArgs == null||selectionArgs.length<2) {
						Log.d("nils","something wrong with Delete selectionArgs: "+s.getValues());
					} else {
						Selection sel = new Selection();
						sel.selection=selection;
						sel.selectionArgs=selectionArgs;
						//Log.d("nils","Calling delete with Selection: "+sel.selection+" args: "+print(sel.selectionArgs));
						this.deleteVariable(s.getTarget(), sel,false);
						//Invalidate variables with this id in the cache..
						vc.invalidateOnName(s.getTarget());
					}
				} else {
					Log.e("nils","Getvalues null in Delete Sync. S: "+s.getTarget()+" K: "+s.getKeys());
				}

			} else if (s.isDeleteDelytor()) {
				String ids = s.getChange();
				if (ids!=null) {
					String[] idA = ids.split("\\|"); 
					if (idA!=null && idA.length==2) {
						Log.d("nils","Got EraseDelytor sync message for ruta "+idA[0]+" and provyta "+idA[1]);					
						this.eraseDelytor(gs,idA[0], idA[1],false);
						//Invalidate Cache.
						vc.invalidateOnKey(Tools.createKeyMap("ruta",idA[0],"provyta",idA[1]));
					}
				}
			} else if (s.isDeleteProvyta()) {
				String ids = s.getChange();
				if (ids!=null) {
					String[] idA = ids.split("\\|"); 
					if (idA!=null && idA.length==2) {
						Log.d("nils","Got EraseProvyta sync message for ruta "+idA[0]+" and provyta "+idA[1]);					
						this.eraseProvyta(idA[0], idA[1],false);
						//Invalidate cache.
						vc.invalidateOnKey(Tools.createKeyMap("ruta",idA[0],"provyta",idA[1]));

					}
				}			
			}

		}

	}

	private boolean partOfValues(String key) {
		return (MY_VALUES_SET.contains(key));		
	}

	public void syncDone(long timeStamp) {
		String lastS = ph.get(PersistenceHelper.TIME_OF_LAST_SYNC);
		long l = Long.parseLong(lastS);
		if (timeStamp > l) 
			ph.put(PersistenceHelper.TIME_OF_LAST_SYNC, Long.toString(timeStamp));
		else 
			Log.e("nils","The timestamp of this sync message is LESS than the current timestamp");
		//else
		//	Log.d("nils","maxstamp 0");
	}
	
	
	public int getNumberOfUnsyncedEntries() {
		String timestamp = ph.get(PersistenceHelper.TIME_OF_LAST_SYNC);
		if (timestamp==null||timestamp.equals(PersistenceHelper.UNDEFINED))
			timestamp = "0";
		Log.d("nils","Time of last sync is "+timestamp+" in getNumberOfUnsyncedEntries (dbHelper)");
			Cursor c = db.query(TABLE_AUDIT,null,
					"timestamp > ?",new String[] {timestamp},null,null,"timestamp asc",null);
		if (c != null && c.getCount()>0) {
			return c.getCount();
		}
		else 
			return 0;
		
	}



	public void eraseDelytor(GlobalState gs, String currentRuta, String currentProvyta,boolean synk) {
		String yCol = keyColM.get("�r");
		String rCol = keyColM.get("ruta");
		String pyCol = keyColM.get("provyta");
		String dyCol = keyColM.get("delyta");
		Log.d("nils","In eraseDelytor with rutaCol = "+rCol+" and pyCol = "+pyCol+" and dyCol "+dyCol);
		//delete all rows and send sync message.
		int affRows = db.delete(DbHelper.TABLE_VARIABLES, 
				yCol+"=? AND "+ rCol+"=? AND "+pyCol+"=? and "+dyCol+" NOT NULL", new String[] {Constants.CurrentYear, currentRuta,currentProvyta});
		Log.d("nils","eraseDelytor affected "+affRows+" rows");
		if (synk&&affRows>0) {
			Log.d("nils","Adding sync message");
			insertDeleteDelytorAuditEntry(currentRuta,currentProvyta);
		}
		Map<String, String> baseKey = gs.getVariableConfiguration().createProvytaKeyMap();
		for (int delyteID=0;delyteID<=DelyteManager.MAX_DELYTEID;delyteID++) {
			baseKey.put("delyta", delyteID+"");	
			if (delyteID >0) {
				Variable tagV = gs.getVariableConfiguration().getVariableUsingKey(baseKey,NamedVariables.DELNINGSTAG);
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
				yCol+"=? AND "+rCol+"=? AND "+pyCol+"=?", new String[] {Constants.CurrentYear,currentRuta,currentProvyta});
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
				yCol+"=? AND "+rCol+"=? AND "+pyCol+"=? AND var = 'Delyta'", new String[] {Constants.CurrentYear,currentRuta,currentProvyta});
		Log.d("nils","Affected rows in eraseSmaProvyDelytaAssoc: "+affRows);
	}


	final ContentValues valuez = new ContentValues();
	final static String NULL = "null";

	public void fastInsert(Map<String,String> key, String varId, String value) {
		valuez.clear();
		String timeStamp = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())+"";

		for (String k:key.keySet()) 			
			valuez.put(getColumnName(k), key.get(k));	
		valuez.put("var", varId);
		valuez.put("value", value);
		valuez.put("lag",ph.get(PersistenceHelper.LAG_ID_KEY));
		valuez.put("timestamp", timeStamp);
		valuez.put("author", ph.get(PersistenceHelper.USER_ID_KEY));


		Log.d("nils","inserting:  "+valuez.toString());
		db.insert(TABLE_VARIABLES, // table
				null, //nullColumnHack
				valuez
				); 	

	}

	String rc,pc,dc,sc,ac;
	public void fastPrep() {
		rc= getColumnName("ruta");
		pc= getColumnName("provyta");
		dc= getColumnName("delyta");
		sc= getColumnName("smaprovyta");
		ac= getColumnName("�r");
	}
	
	public void deleteHistory() {
		Log.d("nils","deleting all historical values");
		int rows = db.delete(TABLE_VARIABLES, getColumnName("�r")+"= ?", new String[]{VariableConfiguration.HISTORICAL_MARKER});
		Log.d("nils","Deleted "+rows+" rows of history");
	}
	
	public void fastHistoricalInsert(String rID,String pID,String dID,String sID,
			String varId, String value) {

		valuez.clear();
		valuez.put(ac,VariableConfiguration.HISTORICAL_MARKER);		
		if (!rID.equals(NULL))
			valuez.put(rc,rID);
		if (!pID.equals(NULL))
			valuez.put(pc,pID);
		if (!dID.equals(NULL))
			valuez.put(dc,dID);
		if (!sID.equals(NULL))
			valuez.put(sc,sID);
		//		values.put(getColumnName("linje"),lID);
		//		values.put(getColumnName("abo"),aID);	
		valuez.put("var", varId);
		valuez.put("value", value);
		db.insert(TABLE_VARIABLES, // table
				null, //nullColumnHack
				valuez
				); 	
		//Log.d("nils",".");
	}
/*	
	public String getHistoricalValue(String varName,Map<String, String> keyChain) {
		HashMap<String, String> histKeyChain = new HashMap<String,String>(keyChain);
		histKeyChain.put(VariableConfiguration.KEY_YEAR, VariableConfiguration.HISTORICAL_MARKER);		
		return getValue(varName,createSelection(histKeyChain,varName),new String[] {VALUE});
	}
*/

	//Generates keychains for all instances.
	public Set<Map<String,String>> getAllInstances(String varID,
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
			c.close();

		}

		return ret;
	}



	public Map<String, String> preFetchValue(Map<String,String> keyChain, String namePrefix) {		

		String query = "SELECT "+VARID+",value FROM "+TABLE_VARIABLES+
				" WHERE "+VARID+" LIKE '"+namePrefix+"%'";

		//Add keychain parts.
		String[] selArgs = new String[keyChain.size()];
		int i=0;
		for (String key:keyChain.keySet()) {
			query += " AND "+this.getColumnName(key)+"= ?";
			selArgs[i++]=keyChain.get(key);
		}

		Cursor c = db.rawQuery(query, selArgs);
		Log.d("nils","Got "+c.getCount()+" results. PrefetchValue."+namePrefix+" with key: "+keyChain.toString());
		Map<String,String> ret = new HashMap<String,String>();
		if (c.moveToFirst() ) {
			do {
				ret.put(c.getString(0),c.getString(1));
			} while (c.moveToNext());

		}
		c.close();
		return ret;

	}

	//Fetch all instacnces. Map varId to a Map of Variator, Value.
	public Map<String, Map<String,String>> preFetchValues(Map<String,String> keyChain, String namePrefix, String variatorColumn) {		

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
		Cursor c = db.rawQuery(query, selArgs);
		Log.d("nils","In prefetchValues. Got "+c.getCount()+" results. PrefetchValues "+namePrefix+" with key "+keyChain.toString());
		Map<String, Map<String,String>> ret = new HashMap<String, Map<String,String>>();
		if (c.moveToFirst() ) {
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
				//Log.d("nils","varid: "+c.getString(0)+" variator: "+c.getString(1)+" value: "+c.getString(2));
			} while (c.moveToNext());

		}
		c.close();
		return ret;

	}
}