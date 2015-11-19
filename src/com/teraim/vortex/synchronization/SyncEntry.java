package com.teraim.vortex.synchronization;

import android.util.Log;

public class SyncEntry extends SyncMessage {

	enum Type {
		insert,
		delete,
		deleteDelytor,
		deleteProvyta,
		unknown, insertArray
	}
	private static final long serialVersionUID = 862826293136691824L;
	private Type mType;
	private String keys;
	private String values;
	private String changes;
	private String timeStamp;
	private String target;
	private boolean invalid = false;
	public SyncEntry() {};
	public SyncEntry(String a,String changes,String timeStamp,String target) {
		this.changes=changes;
		if (a.equals("I"))
			mType = Type.insert;
		else if (a.equals("D"))
			mType = Type.delete;
		else if (a.equals("E"))
			mType = Type.deleteDelytor;
		else if (a.equals("P"))
			mType = Type.deleteProvyta;
		else if (a.equals("A"))
			mType = Type.insertArray;
		else {
			Log.e("nils","Unknown type of Sync action!: "+a);
			mType = Type.unknown;
		}
		if(!isDeleteDelytor() && !isDeleteProvyta()&&!isDelete()) {	
			String[] tmp = changes.split("_\\$_");
			if (tmp==null||tmp.length!=2) 
				Log.e("nils","something wrong with syncentry with changes: ["+changes+"]");
			else {
				keys = tmp[0];
				values = tmp[1];
			}
		}
		this.timeStamp=timeStamp;
		this.target=target;
	}

	public Type getAction() {
		return mType;
	}

	public String getKeys() {
		return keys;
	}
	
	public String getChange() {
		return changes;
	}
	
	public String getValues() {
		return values;
	}
	
	public boolean isInsert() {
		return (mType==Type.insert);
	}
	
	public boolean isInsertArray() {
		return (mType==Type.insertArray);
	}
	
	public boolean isDelete() {
		return (mType==Type.delete);
	}
	
	public boolean isDeleteDelytor() {
		return (mType==Type.deleteDelytor);
	}

	public boolean isDeleteProvyta() {
		return (mType==Type.deleteProvyta);
	}
	
	public String getTimeStamp() {
		return timeStamp;
	}

	public String getTarget() {
		return target;
	}

	public void markAsInvalid() {
		invalid=true;
	}

	public boolean isInvalid() {
		return invalid;
	}


}
