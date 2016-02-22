package com.teraim.fieldapp.dynamic.workflow_realizations.filters;

import java.util.Iterator;
import java.util.List;

import android.util.Log;

import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;

//Specialized filter. Will remove elements with a value.
public class WF_OnlyWithoutValue_Filter extends WF_Filter {
	
	
	
	public WF_OnlyWithoutValue_Filter(String id) {
		super(id);
		
	}

	@Override
	public List<? extends Listable> filter(List<? extends Listable> list) {
		Iterator<? extends Listable> it = list.iterator();
		while(it.hasNext()) {
			Listable l = it.next();
		if (l.hasValue())
			it.remove();
				//Log.d("nils","filter removes element "+l.getKey()+" because its value is null");
			
		}
		return list;
	}

	
}