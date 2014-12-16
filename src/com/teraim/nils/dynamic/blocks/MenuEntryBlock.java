/**
 * 
 */
package com.teraim.nils.dynamic.blocks;

import android.util.Log;

import com.teraim.nils.GlobalState;
import com.teraim.nils.dynamic.types.Workflow;
import com.teraim.nils.dynamic.workflow_realizations.WF_Context;

/**
 * @author tlundin
 *
 */

public class MenuEntryBlock extends Block {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6297016520560017438L;
	String target,type,label;
	public MenuEntryBlock(String id, String target, String type) {
		this.blockId=id;
		this.target=target;
		this.type=type;
	}
	public void create(WF_Context myContext) {
		Log.d("vortex","In create menuentry");
		
		GlobalState gs = GlobalState.getInstance(myContext.getContext());
		Workflow wf = gs.getWorkflow(target);
		if (wf == null)
			gs.getLogger().addRedText("Workflow "+target+" not found!!");
		else {
			label = wf.getLabel();		
			gs.getDrawerMenu().addItem(label,wf);
		}
	}

}
