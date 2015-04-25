package com.teraim.vortex.dynamic.blocks;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.types.Workflow.Unit;
import com.teraim.vortex.dynamic.workflow_abstracts.Container;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Gis_Widget;
import com.teraim.vortex.gis.GisImageView;
import com.teraim.vortex.utils.Tools;

public class CreateGisBlock extends Block {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2013870148670474248L;
	String name,type,label,containerId,postLabel,initialValue;
	Unit unit;
	GlobalState gs;
	boolean isVisible = false,showHistorical;
	String format;
	private String picUrlorName;

	public CreateGisBlock(String id,String name, 
			String containerId,boolean isVisible,String picUrlorName) {
		super();
		this.name = name;
		this.containerId=containerId;
		this.isVisible=isVisible;
		this.blockId=id;
		this.picUrlorName=picUrlorName;
		
		

	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}



	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}






	public void create(WF_Context myContext) {
		gs = GlobalState.getInstance();
		Container myContainer = myContext.getContainer(containerId);
		o = gs.getLogger();

		if(myContainer !=null) {
				myContainer.add(new WF_Gis_Widget(blockId, new GisImageView(myContext.getContext()), isVisible, picUrlorName,myContext));
			
		} else {
			Log.e("vortex","Container null! Cannot add GisImageView!");
			o.addRow("");
			o.addRedText("Adding GisImageView to "+containerId+" failed. Container not configured");
		}
	}




}


