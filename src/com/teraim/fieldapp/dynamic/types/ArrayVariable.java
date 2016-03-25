package com.teraim.fieldapp.dynamic.types;

import java.util.List;
import java.util.Map;

import com.teraim.fieldapp.GlobalState;

public class ArrayVariable extends Variable {

	public ArrayVariable(String name, String label, List<String> row,
			Map<String, String> keyChain, GlobalState gs, String valueColumn,
			String defaultOrExistingValue, Boolean valueIsPersisted, String historicalValue) {
		super(name, label, row, keyChain, gs, valueColumn, defaultOrExistingValue,
				valueIsPersisted,historicalValue);
		// TODO Auto-generated constructor stub
	}

	private static final long serialVersionUID = 4404839378820201885L;

	@Override
	protected void insert(String value, boolean isSynchronized) {
		
		myDb.insertVariableSnap(this,value, isSynchronized);
	}

	
	
}
