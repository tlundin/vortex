package com.teraim.vortex.dynamic.workflow_realizations;

import java.util.Set;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.R;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.workflow_abstracts.Event;
import com.teraim.vortex.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.vortex.dynamic.workflow_abstracts.EventListener;
import com.teraim.vortex.dynamic.workflow_abstracts.Listable;

public class WF_Not_ClickableField_SumAndCountOfVariables extends
WF_Not_ClickableField implements EventListener {

	private WF_Static_List targetList;
	private WF_Context myContext;
	String myPattern;
	//Detta �r en id f�r widgeten - inte variabeln!!
	//private static final String MY_WIDGET_ID = "Antal Arter";
	
	public enum Type {
		sum,
		count
	}
	Type myType;

	public WF_Not_ClickableField_SumAndCountOfVariables(String header,String descriptionT, WF_Context myContext, 
			 String myTarget, String pattern,Type sumOrCount,boolean isVisible, String textColor, String bgColor) {
		super(header, descriptionT, myContext, LayoutInflater.from(myContext.getContext()).inflate(R.layout.selection_field_normal_colored,null),isVisible);
		this.myContext=myContext;
		o = GlobalState.getInstance().getLogger();
		targetList = myContext.getList(myTarget);
		myType = sumOrCount;
		myPattern = pattern;

		TextView text = (TextView)getWidget().findViewById(R.id.editfieldtext);
		LinearLayout bg = (LinearLayout)getWidget().findViewById(R.id.background);
		if (bgColor!=null)
			bg.setBackgroundColor(Color.parseColor(bgColor));
		if (textColor!=null)
			text.setTextColor(Color.parseColor(textColor));

		if (targetList == null) {
			o.addRow("");
			o.addRedText("Couldn't create "+header+" since target list: "+myTarget+" does not exist");
			Log.e("parser","couldn't create SumAndCountOfVariables - could not find target list "+myTarget);
		} else {

		myContext.addEventListener(this,EventType.onRedraw);
		}
	}

	@Override
	public LinearLayout getFieldLayout() {
		return (LinearLayout)LayoutInflater.from(myContext.getContext()).inflate(R.layout.output_field_selection_element,null);
	}



	@Override
	public void onEvent(Event e) {
		Log.d("nils","In ADDNUMBER event targetListId: "+targetList.getId()+" e.getProvider: "+e.getProvider()+
				"type of event: "+e.getType().name());
		if (e.getProvider().equals(targetList.getId())) {
			matchAndRecalculateMe();
			refresh();
		} else
			Log.d("nils","event discarded - from wrong list");

	}

	public void matchAndRecalculateMe() {
		String variablesWithNoValue = "[";
		Long sum=Long.valueOf(0);
		for (Listable l:targetList.getList()) {
			Set<Variable> vars = l.getAssociatedVariables();
			//Log.d("nils","now in matchandrecalculate with list "+vars.size());
			if (vars!=null && !vars.isEmpty()) {
				for (Variable v:vars) {
					if (v.getId().matches(myPattern)) {
						if (v.getValue()!=null) {
							//Log.d("nils","VALUE: "+v.getValue());
							if (myType == Type.count) {
								sum++;
							}
								
							else {
								String val=v.getValue();
								if (val!=null && !val.isEmpty()) {
									try {
									sum+=Long.parseLong(v.getValue());
									} catch (NumberFormatException e) {
										sum+=0;
									}
								}
							}
						} else {
							variablesWithNoValue += v.getId()+",";
						}
					} 
						

				}

			} else {
				Log.d("nils ","Vars for "+l.getLabel()+" empty");
			}
		}
		if (sum==0) {
			variablesWithNoValue+="]";
			o.addRow("");
			o.addYellowText("Sum zero in Count/Add Block. No value found for:");
			o.addRow(variablesWithNoValue);
		} else {
			o.addRow("");
			o.addGreenText("Found match(es) in Count/Add Block with pattern ["+myPattern+"]");
		}

		if (myVar !=null)
			myVar.setValue(sum.toString());

	}

}
