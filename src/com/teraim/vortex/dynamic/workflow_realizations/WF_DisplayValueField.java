package com.teraim.vortex.dynamic.workflow_realizations;

import java.util.List;

import android.graphics.Color;
import android.text.TextUtils.StringSplitter;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.R;
import com.teraim.vortex.dynamic.types.Workflow.Unit;
import com.teraim.vortex.dynamic.workflow_abstracts.Event;
import com.teraim.vortex.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.vortex.dynamic.workflow_abstracts.EventListener;
import com.teraim.vortex.utils.RuleExecutor;
import com.teraim.vortex.utils.RuleExecutor.SubstiResult;
import com.teraim.vortex.utils.RuleExecutor.TokenType;
import com.teraim.vortex.utils.RuleExecutor.TokenizedItem;
import com.teraim.vortex.utils.Tools;

public class WF_DisplayValueField extends WF_Widget implements EventListener {

	private String formula,label;
	protected GlobalState gs;
	protected Unit unit;
	private List<TokenizedItem> myTokens;
	RuleExecutor ruleExecutor;
	private String format;

	public WF_DisplayValueField(String id, String formula,WF_Context ctx, Unit unit, 
			String label, boolean isVisible,String format,String bgColor, String textColor) {
		super(id, LayoutInflater.from(ctx.getContext()).inflate(R.layout.display_value_textview,null), isVisible,ctx);
		TextView header = (TextView)getWidget().findViewById(R.id.header);
		LinearLayout bg = (LinearLayout)getWidget().findViewById(R.id.background);
		header.setText(label);
		this.label=label;
		if (bgColor!=null)
			bg.setBackgroundColor(Color.parseColor(bgColor));
		if (textColor!=null)
			header.setTextColor(Color.parseColor(textColor));
		gs = GlobalState.getInstance();
		ruleExecutor = gs.getRuleExecutor();
		o = gs.getLogger();
		this.formula = formula;
		Log.d("nils","In WF_DisplayValueField Create");	
		ctx.addEventListener(this, EventType.onSave);	
		this.unit=unit;
		myTokens = ruleExecutor.findTokens(formula,null);
		if (myTokens==null)
		{
			o.addRow("");
			o.addRedText("Parsing of formula for DisplayValueBlock failed. Formula: "+formula);
		}
		this.format = format;
		
		this.onEvent(new WF_Event_OnSave("display_value_field"));
	}

	//update variable.
	@Override
	public void onEvent(Event e) {

		String strRes="";
		String subst;
		SubstiResult sr;
		Log.d("nils","Got event in WF_DisplayValueField");	
		sr = ruleExecutor.substituteForValue(myTokens,formula,false);
		//Do not evaluate if the expression is evaluated to be a literal or defined as literal.
		boolean isLiteral = (format!=null && format.equalsIgnoreCase("S"))||sr.iAmAString(); 
		if (!isLiteral) {
			subst = sr.result;
			if (Tools.isNumeric(subst)) {
				//check if all tokens are boolean.	
				strRes = subst;
			}
			else {
				strRes = ruleExecutor.parseExpression(formula,subst);
				if (strRes==null) {
					o.addRow("");
					o.addText("Formula "+formula+" returned null");	
					((TextView)this.getWidget().findViewById(R.id.outputValueField)).setText("");
					((TextView)this.getWidget().findViewById(R.id.outputUnitField)).setText("");
					return;
				}
			}
		} else
			strRes = sr.result;
		if (format!=null && format.equalsIgnoreCase("B")) {
			if (strRes.equals("1"))
					strRes = GlobalState.getInstance().getContext().getString(R.string.yes);
			else if (strRes.equals("0"))
				strRes = GlobalState.getInstance().getContext().getString(R.string.no);
		}
			
		o.addRow("");
		o.addText("Text in DisplayField "+label+" is [");o.addGreenText(strRes); o.addText("]");
		((TextView)this.getWidget().findViewById(R.id.outputValueField)).setText(strRes);
		((TextView)this.getWidget().findViewById(R.id.outputUnitField)).setText(Tools.getPrintedUnit(unit));
	}

}





