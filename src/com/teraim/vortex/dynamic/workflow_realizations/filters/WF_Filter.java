package com.teraim.vortex.dynamic.workflow_realizations.filters;

import com.teraim.vortex.dynamic.workflow_abstracts.Filter;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Thing;

public abstract class WF_Filter extends WF_Thing implements Filter {

	public WF_Filter(String id) {
		super(id);
	}

	
}

