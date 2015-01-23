package com.teraim.vortex.utils;

import java.util.Map;

import android.content.Context;
import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.utils.DbHelper.DBColumnPicker;


public abstract class Exporter {
	
	
	public enum ExportReport {
		OK,
		NO_DATA,
		FILE_WRITE_ERROR,
		COLUMN_DOES_NOT_EXIST,
		EXPORTFORMAT_UNKNOWN
	}
	public static class Report {
		public Report(String res,int vars) {
			this.result=res;
			this.noOfVars=vars;
		}
		public Report(ExportReport problem) {
			er = problem;
		}
		public String result;
		public int noOfVars = 0;
		public ExportReport er = ExportReport.OK;

	}
	
	protected GlobalState gs;
	protected VariableConfiguration al;
	protected PersistenceHelper ph;
	protected PersistenceHelper globalPh;
	protected static Exporter instance; 
	
	public static Exporter getInstance(Context ctx, String type) {
		if (type==null||type.equalsIgnoreCase("csv"))
			return new CSVExporter(ctx);
		else
			if (type.equalsIgnoreCase("json"))
				return new JSONExporter(ctx);
		return null;
	}
	protected Exporter(Context ctx) {
		this.gs=GlobalState.getInstance(ctx);
		al = gs.getVariableConfiguration();
		ph = gs.getPreferences();
		globalPh = gs.getGlobalPreferences();
		
	}
	public abstract Report writeVariables(DBColumnPicker cp);
	public abstract String getType();
	

}
