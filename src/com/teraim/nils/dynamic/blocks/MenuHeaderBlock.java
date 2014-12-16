/**
 * 
 */
package com.teraim.nils.dynamic.blocks;

import android.util.Log;

import com.teraim.nils.GlobalState;
import com.teraim.nils.dynamic.workflow_realizations.WF_Context;

/**
 * @author tlundin
 *
 */


public class MenuHeaderBlock extends Block {

	String label,textColor, bgColor;

	public MenuHeaderBlock(String id, String label, String textColor,
			String bgColor) {
		this.blockId=id;
		this.label=label;
		this.textColor=textColor;
		this.bgColor=bgColor;
	}

	public void create(WF_Context myContext) {
		Log.d("vortex","In create menuheader");
		
		GlobalState gs = GlobalState.getInstance(myContext.getContext());
		
		gs.getDrawerMenu().addHeader(label);
		
		
	}

}
