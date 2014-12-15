/**
 * 
 */
package com.teraim.nils.dynamic.blocks;

/**
 * @author tlundin
 *
 */

public class MenuEntryBlock extends Block {
	String target,type,label;
	public MenuEntryBlock(String id, String target, String type, String label) {
		this.blockId=id;
		this.target=target;
		this.type=type;
		this.label=label;
	}

}
