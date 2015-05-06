package com.teraim.vortex.dynamic.types;

import java.util.List;
import java.util.Map;

import com.teraim.vortex.GlobalState;

public class ArrayVariable extends Variable {

	public ArrayVariable(String name, String label, List<String> row,
			Map<String, String> keyChain, GlobalState gs, String valueColumn,
			String defaultOrExistingValue, Boolean valueIsPersisted) {
		super(name, label, row, keyChain, gs, valueColumn, defaultOrExistingValue,
				valueIsPersisted);
		// TODO Auto-generated constructor stub
	}

	private static final long serialVersionUID = 4404839378820201885L;

	@Override
	protected void insertVariable(String value, boolean isSynchronized) {
		
		myDb.insertVariableSnap(this,value, isSynchronized);
	}

	
	
}
