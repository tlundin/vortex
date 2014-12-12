package com.teraim.nils.dynamic.workflow_realizations;

import java.util.Set;

import android.util.Log;

import com.teraim.nils.dynamic.types.Variable;
import com.teraim.nils.dynamic.workflow_abstracts.Event;
import com.teraim.nils.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.nils.dynamic.workflow_abstracts.EventListener;
import com.teraim.nils.utils.RuleExecutor;

public class WF_ClickableField_Selection_OnSave extends WF_ClickableField_Selection implements EventListener {

	
	
	public WF_ClickableField_Selection_OnSave(String headerT, String descriptionT,
			WF_Context context, String id,boolean isVisible) {
		super(headerT,descriptionT, context, id,isVisible);
		context.addEventListener(this, EventType.onSave);
		
	}

	@Override
	public void onEvent(Event e) {
		if (!e.getProvider().equals(getId())) {
			Log.d("nils","In onEvent for WF_ClickableField_Selection_OnSave. Provider: "+e.getProvider());
			if (iAmOpen)
				refreshInputFields();
			refreshOutputFields();
		} else
			Log.d("nils","Discarded...from me");
	}


	



}
