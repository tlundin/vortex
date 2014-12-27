package com.teraim.vortex.dynamic.workflow_realizations;

import com.teraim.vortex.dynamic.workflow_abstracts.Event;

public class WF_Event_OnLinjeStatusChanged extends Event {

	public WF_Event_OnLinjeStatusChanged(String linjeId,boolean started) {
		super(linjeId,started?EventType.nyLinjeStarted:EventType.linjeEnded);
		
	}

}
