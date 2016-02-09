package com.teraim.vortex.dynamic.blocks;

import java.util.List;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.workflow_abstracts.Container;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;
import com.teraim.vortex.dynamic.workflow_realizations.WF_SorterWidget;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Static_List;
import com.teraim.vortex.dynamic.workflow_realizations.WF_TextBlockWidget;
import com.teraim.vortex.utils.Expressor;
import com.teraim.vortex.utils.Expressor.EvalExpr;

public class TextFieldBlock extends Block {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1134485697631003990L;
	String label,containerId;
	boolean isVisible = true;
	private List<EvalExpr> labelE;
	
	public TextFieldBlock(String id, String label, String containerId,boolean isVisible) {
		this.blockId=id;
		this.label=label;
		this.labelE=Expressor.preCompileExpression(label);
		this.containerId=containerId;
		this.isVisible = isVisible;
	}

	
	
	
	public void create(WF_Context ctx) {
		o = GlobalState.getInstance().getLogger();
		//Identify targetList. If no list, no game.
		Container myContainer = ctx.getContainer(containerId);
		if (myContainer != null)  {
			myContainer.add(new WF_TextBlockWidget(ctx,Expressor.analyze(labelE),blockId,isVisible));
			o.addRow("Added new TextField with ID"+blockId);
		} else {
			o.addRow("");
			o.addRedText("Failed to add text field block with id "+blockId+" - missing container "+myContainer);
		}
		
	}	
	
			
		
		
}


