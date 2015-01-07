package com.teraim.vortex.dynamic.workflow_realizations;

import com.teraim.vortex.dynamic.workflow_abstracts.Event;

public class WF_Event_OnAttach extends Event {

	public WF_Event_OnAttach(String fromId) {
		super(fromId, EventType.onAttach);
	}

}
