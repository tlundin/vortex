/**
 * 
 */
package com.teraim.nils.dynamic.blocks;

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
		
	}

}
