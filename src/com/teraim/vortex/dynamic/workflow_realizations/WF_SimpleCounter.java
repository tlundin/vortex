package com.teraim.vortex.dynamic.workflow_realizations;

import java.util.List;

import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.R;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.non_generics.NamedVariables;


public class WF_SimpleCounter extends WF_Not_ClickableField {
	Variable myTarget;
	GlobalState gs;

	public WF_SimpleCounter(String targetId, String label, String descriptionT,
			WF_Context myContext, boolean isVisible) {
		super(label, descriptionT, myContext, 
				LayoutInflater.from(myContext.getContext()).inflate(R.layout.selection_field_normal,null), isVisible);

		gs = GlobalState.getInstance(myContext.getContext());
		myTarget = gs.getVariableConfiguration().getVariableInstance(targetId);
		if (myTarget == null) {
			o.addRow("");
			o.addRedText("Missing target variable "+targetId+" in SimpleCounter");
		} else {
			List<String> listElems = gs.getVariableConfiguration().getListElements(myTarget.getBackingDataSet());
			int count=0;
			if (listElems != null)
				count = listElems.size();
			Variable noOfAvslutade = gs.getVariableConfiguration().getVariableInstance(NamedVariables.NO_OF_AVSLUTADE);
			noOfAvslutade.setValue(count+"");
			this.addVariable(noOfAvslutade, true, null, true);
			this.refreshOutputFields();
		}
	}

	@Override
	public LinearLayout getFieldLayout() {
		return (LinearLayout)LayoutInflater.from(myContext.getContext()).inflate(R.layout.output_field_selection_element,null);
	}





}