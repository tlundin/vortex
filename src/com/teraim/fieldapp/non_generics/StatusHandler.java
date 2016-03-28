package com.teraim.fieldapp.non_generics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.DbHelper.Selection;
import com.teraim.fieldapp.utils.PersistenceHelper;

public class StatusHandler {


	private DbHelper dbh;
	private GlobalState gs;
	private Map<String, String> keySet;
	private int noOfProvytor=-1,noOfRutor=-1;
	private PersistenceHelper ph;


	public StatusHandler(GlobalState gs) {
		this.gs = gs;
		dbh = gs.getDb();
		ph = gs.getPreferences();
	}


	public class Kvot {
		int done;
		int tot;

		public Kvot(int done, int tot) {
			this.done=done;
			this.tot=tot;
		}

		public boolean allDone() {
			return done==tot;
		}

		public String toString() {
			return done+"/"+tot;
		}
	}


	private int getCount(Map<String, String> keySet,String columnName) {
		String[] column = new String[] {columnName};
		Selection s = gs.getDb().createCoulmnSelection(keySet);
		List<String[]> values = dbh.getValues(column, s);
		if (values == null)
			return 0;
		Set<String> set = new HashSet<String>();
		for (String[] ss:values)
			set.add(ss[0]);					
		Log.d("nils","getCount has this set: "+set.toString());
		return set.size();
	}



	private int getNumberOfElementsDone(Map<String, String> keyMap, String what) {
		Log.d("nils","in getNoOfElemsDone for "+what+" with keyMap: "+(keyMap==null?"null":keyMap.toString()));
		Selection statusSelector = dbh.createSelection(keyMap, what);
		List<String> statusL = dbh.getValues(statusSelector);
		if (statusL!=null) {
			Log.d("nils","Got "+statusL.toString()+" for "+what);
			return selectThoseDone(statusL);
		}
		else
			return 0;
	}


	public void setStatusProvyta() {
		Map<String, String> pyKeyMap = gs.getVariableConfiguration().createProvytaKeyMap();
		if (pyKeyMap == null)
			return;
		Kvot k1 = getStatusSmaProv();
		Kvot k2 = getStatusDelytor();
		boolean done = (k1.allDone() && k2.allDone());
		Variable provytaS = gs.getVariableCache().getVariable(pyKeyMap, 
				"STATUS:status_provyta");
		if (provytaS!=null)
			provytaS.setValue(done?Constants.STATUS_AVSLUTAD_OK:Constants.STATUS_INITIAL);

	}

	public void setStatusRuta() {
		Map<String, String> rutaKeyMap = gs.getVariableConfiguration().createRutaKeyMap();
		if (rutaKeyMap == null)
			return;
		Kvot k = getStatusProvytor();
		
		Variable rutaS = gs.getVariableCache().getVariable(rutaKeyMap, 
				"STATUS:status_ruta");
		if (rutaS!=null)
		rutaS.setValue(k.allDone()?Constants.STATUS_AVSLUTAD_OK:Constants.STATUS_INITIAL);
	}

	public Kvot getStatusSmaProv() {	
		keySet = gs.getVariableConfiguration().createProvytaKeyMap();
		int done = getNumberOfElementsDone(keySet,"STATUS:status_smaprovyta");
		int tot = Constants.isAbo(DelyteManager.getInstance().getPyID())?9:3;
		//int tot = getCount(keySet,"smaprovyta");
		return new Kvot(done,tot);
	}

	public Kvot getStatusDelytor() {
		keySet = gs.getVariableConfiguration().createProvytaKeyMap();
		int done = getNumberOfElementsDone(keySet,"STATUS:status_delyta");		
		int tot = 1;
		String totS = gs.getVariableCache().getVariable(gs.getVariableConfiguration().createProvytaKeyMap(), "noOfDelytor").getValue();
		if (totS != null)
			tot = Integer.parseInt(totS);
		return new Kvot(done,tot);
	}
	
	public Kvot getStatusLinjer() {
		keySet = gs.getVariableConfiguration().createRutaKeyMap();
		int done = getNumberOfElementsDone(keySet,"STATUS:status_linje");		
		int tot = Constants.MAX_NILS_LINJER;
		return new Kvot(done,tot);
	}

	public Kvot getStatusProvytor() {
		Log.d("nils","GetStatus: Provytor");
		keySet = gs.getVariableConfiguration().createRutaKeyMap();
		Log.d("nils","Keyset: "+keySet.toString());
		int done = getNumberOfElementsDone(keySet,"STATUS:status_provyta");
		//set?
		if (noOfProvytor==-1) {
			//persisted?
			noOfProvytor = ph.getI(PersistenceHelper.NO_OF_PROVYTOR);
			//last resort: count.
			if (noOfProvytor ==-1) {
			Map<String,String> p = new HashMap<String,String>();
			p.put("ruta", gs.getVariableConfiguration().getCurrentRuta());
			noOfProvytor = getCount(p,"provyta");
			}
		}
		return new Kvot(done,noOfProvytor);
	}

	public Kvot getStatusRutor() {
		Log.d("nils","GetStatus: Rutor");
		int done = getNumberOfElementsDone(null,"STATUS:status_ruta");
		if (noOfRutor==-1) {
			noOfRutor = ph.getI(PersistenceHelper.NO_OF_RUTOR);
			if (noOfRutor==-1)
				noOfRutor = getCount(null,"ruta");			
		}
		return new Kvot(done,noOfRutor);
	}

	public int selectThoseDone(List<String> statusL){
		int ret = 0;
		if (statusL != null) {		
			for (String val:statusL) {
				if (val.equals(Constants.STATUS_AVSLUTAD_OK))
					ret++;
			}
		}
		return ret;
	}
}

