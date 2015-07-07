package com.teraim.vortex.dynamic.workflow_realizations;

import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.teraim.vortex.R;

public class WF_Cell_Widget extends WF_ClickableField {


	public WF_Cell_Widget(String headerT, String descriptionT,
			WF_Context context, String id,boolean isVisible) {
		super(headerT,descriptionT, context, id,
				LayoutInflater.from(context.getContext()).inflate(R.layout.cell_field_normal,null),
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
	
}
