package com.teraim.vortex.dynamic.blocks;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.types.Workflow.Unit;
import com.teraim.vortex.dynamic.workflow_abstracts.Container;
import com.teraim.vortex.dynamic.workflow_abstracts.EventGenerator;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;
import com.teraim.vortex.dynamic.workflow_realizations.WF_DisplayValueField;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Event_OnSave;

public class DisplayValueBlock extends Block implements EventGenerator {

	private static final long serialVersionUID = 9151756426062334462L;
	private String namn,label,formula, containerId,format;
	boolean isVisible = false;
	Unit unit;
	GlobalState gs;
	private String textColor;
	private String bgColor;
	
	public DisplayValueBlock(String id,String namn, String label,Unit unit,
			String formula, String containerId,boolean isVisible,String format, String textColor, String bgColor) {
		this.blockId=id;
		this.unit=unit;
		this.namn=namn;;
		this.label=label;
		this.formula=formula;
		this.containerId=containerId;
		this.isVisible=isVisible;
		this.format=format;
		this.textColor = textColor;
		this.bgColor = bgColor;
	}

	public void create(final WF_Context myContext) {
		gs = GlobalState.getInstance();
		o=gs.getLogger();
		Container myContainer = myContext.getContainer(containerId);
		if (myContainer != null) {
		
		WF_DisplayValueField vf = new WF_DisplayValueField(namn,formula,myContext,unit,label,isVisible,format, bgColor, textColor);
		myContainer.add(vf);
		vf.onEvent(new WF_Event_OnSave(null));
		} else {
			Log.d("nils","ContainerID: "+containerId);
			o.addRow("");
			o.addRedText("Could not find container for DisplayValueBlcok with name (container): "+containerId+" (block): "+namn);
		}
			
	}
}
