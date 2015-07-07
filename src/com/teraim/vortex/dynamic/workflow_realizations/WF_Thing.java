package com.teraim.vortex.dynamic.workflow_realizations;

import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.teraim.vortex.log.LoggerI;


public abstract class WF_Thing {

	private String myId;
	private ViewGroup myWidget;
	protected LoggerI o;

	public WF_Thing(String id) {
		myId = id;
	}
	
	public String getId() {
		return myId;
	}


};