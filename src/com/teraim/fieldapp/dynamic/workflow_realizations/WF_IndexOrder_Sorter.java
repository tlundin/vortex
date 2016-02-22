package com.teraim.fieldapp.dynamic.workflow_realizations;

import java.util.Collections;
import java.util.List;

import android.util.Log;

import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Sorter;


public class WF_IndexOrder_Sorter implements Sorter {

	@Override
	public List<? extends Listable> sort(List<? extends Listable> list) {
//		Log.d("nils","Before TIME Sort: ");
//	for(Listable l:list)
//			Log.d("nils",l.getLabel());
		Collections.sort(list, WF_ListEntry.Comparators.Index);
//		Log.d("nils","After TIME Sort: ");
//		for(Listable l:list)
//			Log.d("nils",l.getLabel());
		return list;
	}

}
