package com.teraim.nils.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.util.Log;

import com.teraim.nils.dynamic.types.Variable;

public class FilterFactory {

	public static final int MAX_MIN = 999999999;

	static FilterFactory singleton;
	
	private Map<String, CombinedRangeAndListFilter> filterCache = new HashMap<String, CombinedRangeAndListFilter>();

	private int currentMin;
	
	public static class Range {
		public String minS,maxS;
		int min, max;
		public Range(int min, int max) {
			this.min=min;
			this.max=max;
			minS = min+"";
			maxS = max+"";
		}
		public Range(String min,String max) {
			this.maxS=max;
			this.minS=min;
			try {
			this.min = Integer.parseInt(minS);
			this.max = Integer.parseInt(maxS);
			} catch (NumberFormatException e) {
				Log.e("nils","NumberformatException with range "+min+","+max);
				this.min = 0;
				this.max = 0;
			}
		}
		
		
	}
	
	
	
	private FilterFactory() {
		
	}
	
	
	public static FilterFactory getInstance() {
		
		if (singleton == null)
			singleton = new FilterFactory();
		return singleton;			
	}
	
	public CombinedRangeAndListFilter createLimitFilter(Variable myVar,String limitDesc) {
		limitDesc = limitDesc.replaceAll("\\s+","");
		CombinedRangeAndListFilter cached = filterCache.get(limitDesc);
		if (cached!=null)
			return cached;
		//I know the limitDesc is not null or empty.
		limitDesc = limitDesc.replace("\"", "");
		Set<String> allowedValues= new HashSet<String>();
		List<Range> allowedRanges= new ArrayList<Range>();
		Log.d("nils","Creating input filter from: "+limitDesc);
		//Each element in the string is comma separated.
		String[] elems = limitDesc.split(",");
		for (String elem:elems) {
			String[] pair = elem.split("-");
			if (pair.length==1) 
				//This should be a number.
				allowedValues.add(pair[0]);
			else
				//this should be a range.
				allowedRanges.add(new Range(pair[0],pair[1]));
		}
		if (allowedValues.size()==0)
			allowedValues = null;
		if (allowedRanges.size()==0)
			allowedRanges = null;
		filterCache.put(limitDesc, cached);
		currentMin = MAX_MIN;
		if (allowedRanges!=null) {
			for (Range r:allowedRanges) {
				if (r.min<currentMin)
					currentMin = r.min;
			}
			Log.d("nils","CurrentMin is set to "+currentMin);
		} else
			Log.d("nils","allowedranges was null for "+limitDesc);
		boolean hasDefaultFilter = false;
		if (currentMin>0 && currentMin<MAX_MIN) {
			allowedRanges.add(0,new Range(0,currentMin-1));
			hasDefaultFilter = true;
		}
		cached = new CombinedRangeAndListFilter(myVar,allowedValues,allowedRanges,hasDefaultFilter);
		return cached;
	}

}
