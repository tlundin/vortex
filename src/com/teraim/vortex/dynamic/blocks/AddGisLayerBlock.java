package com.teraim.vortex.dynamic.blocks;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.teraim.vortex.R;
import com.teraim.vortex.dynamic.types.GisLayer;
import com.teraim.vortex.dynamic.workflow_abstracts.Drawable;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;
import com.teraim.vortex.dynamic.workflow_realizations.gis.WF_Gis_Map;

public class AddGisLayerBlock extends Block {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4149408006972701777L;
	private String name, label,target;
	private boolean isVisible, hasWidget;
	private WF_Gis_Map myGis;
	private boolean showLabels;
	
	public AddGisLayerBlock(String id, String name, String label,
			String target, boolean isVisible, boolean hasWidget, boolean showLabels) {
		super();
		this.blockId = id;
		this.name = name;
		this.label = label;
		this.target = target;
		this.isVisible = isVisible;
		this.hasWidget = hasWidget;
		this.showLabels = showLabels;
		
	}
	

	public void create(WF_Context myContext) {
		Drawable gisMap = myContext.getDrawable(target);
		if (gisMap!=null && gisMap instanceof WF_Gis_Map) {
			myGis = ((WF_Gis_Map)gisMap);
			final GisLayer gisLayer = new GisLayer(name,label,isVisible,hasWidget,showLabels);		
			myGis.getGis().addLayer(gisLayer);
			if (hasWidget) {
				Log.d("vortex","Layer "+name+" has a widget");
				LinearLayout layersL = (LinearLayout)myGis.getWidget().findViewById(R.id.LayersL);
				LayoutInflater li = LayoutInflater.from(myContext.getContext());
				View layersRow = li.inflate(R.layout.layers_row, null);
				TextView filterNameT = (TextView)layersRow.findViewById(R.id.filterName);
				CheckBox lShow = (CheckBox)layersRow.findViewById(R.id.cbShow);
				CheckBox lLabels = (CheckBox)layersRow.findViewById(R.id.cbLabels);
				filterNameT.setText(label);
				lShow.setChecked(isVisible);
				lLabels.setChecked(showLabels);
				lShow.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {						
						gisLayer.setVisible(isChecked);
						isVisible=isChecked;
						myGis.getGis().invalidate();
					}
				});
				lLabels.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {						
						gisLayer.setShowLabels(isChecked);
						showLabels=isChecked;
						myGis.getGis().invalidate();
					}
				});
				layersL.addView(layersRow);
				
				
			}
		}
		
	}
	
	
}