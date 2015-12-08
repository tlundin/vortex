package com.teraim.vortex.dynamic.workflow_realizations;

import java.util.ArrayList;

import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.teraim.vortex.R;
import com.teraim.vortex.dynamic.types.Rule;

public class WF_ClickableField_Selection extends WF_ClickableField {


	public WF_ClickableField_Selection(String headerT, String descriptionT,
			WF_Context context, String id,boolean isVisible) {
		super(headerT,descriptionT, context, id,
				LayoutInflater.from(context.getContext()).inflate(R.layout.selection_field_normal,null),
				isVisible);


	}

	@Override
	public LinearLayout getFieldLayout() {
		//LayoutInflater.from(context.getContext()).inflate(R.layout.clickable_field_normal,null)
		//return 	(LinearLayout)LayoutInflater.from(ctx).inflate(R.layout.output_field,null);
		//o.setText(varId.getLabel()+": "+value);	
		//u.setText(" ("+varId.getPrintedUnit()+")");

		return (LinearLayout)LayoutInflater.from(myContext.getContext()).inflate(R.layout.output_field_selection_element,null);
	}

	public void attachRule(Rule r) {
		if (myRules == null)
			myRules = new ArrayList<Rule>();
		myRules.add(r);
		Log.d("vortex","Added rule "+r.getCondition());
	}






}
