package com.teraim.vortex.dynamic.blocks;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.workflow_abstracts.Container;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;
import com.teraim.vortex.dynamic.workflow_realizations.WF_SorterWidget;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Static_List;
import com.teraim.vortex.dynamic.workflow_realizations.WF_TextBlockWidget;

public class TextFieldBlock extends Block {

	String label,containerId;
	boolean isVisible = true;
	
	public TextFieldBlock(String id, String label, String containerId,boolean isVisible) {
		this.blockId=id;
		this.label=label;
		this.containerId=containerId;
		this.isVisible = isVisible;
	}

	
	
	
	public void create(WF_Context ctx) {
		o = GlobalState.getInstance(ctx.getContext()).getLogger();
		//Identify targetList. If no list, no game.
		Container myContainer = ctx.getContainer(containerId);
		if (myContainer == null)  {
			o.addRow("");
			o.addRedText("Warning: No container defined or found for component TextFieldBlock: "+containerId);
		} else {
			myContainer.add(new WF_TextBlockWidget(ctx,label,blockId,isVisible));
			o.addRow("Added new TextField with ID"+blockId);
		}
		
	}	
	
			
		
		
}


