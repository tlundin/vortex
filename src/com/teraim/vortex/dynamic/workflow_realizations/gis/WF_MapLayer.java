package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.util.List;

import android.view.View;

import com.teraim.vortex.dynamic.workflow_abstracts.Drawable;
import com.teraim.vortex.dynamic.workflow_abstracts.Filter;
import com.teraim.vortex.dynamic.workflow_abstracts.Filterable;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Thing;
import com.teraim.vortex.loadermodule.configurations.GisPolygonConfiguration.GisBlock;

public class WF_MapLayer extends WF_Thing   {

	List<Drawable> myChildren;
	List<GisBlock> mBlocks;
	
	boolean isVisible;
	
	public boolean isEmpty() {
		return myChildren.isEmpty();
		
	}
	
	public boolean isVisible() {
		return isVisible;
	}
	
	public void setVisible(boolean visible) {
		isVisible = visible;
	}
	
	
	public WF_MapLayer(String id) {
		super(id);
	}

	
	
	public void draw() {
		if (this.isVisible&&!this.isEmpty()) {
//			for (GisObjectType got:gisObjectTypes) {
//				for (WF_Gis_Object go:got.generate()) 
//					go.draw();
//			}
		}
	}
}
