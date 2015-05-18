package com.teraim.vortex.dynamic.blocks;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;

import com.teraim.vortex.FileLoadedCb;
import com.teraim.vortex.GlobalState;
import com.teraim.vortex.R;
import com.teraim.vortex.dynamic.types.GisLayer;
import com.teraim.vortex.dynamic.types.PhotoMeta;
import com.teraim.vortex.dynamic.types.Workflow.Unit;
import com.teraim.vortex.dynamic.workflow_abstracts.Container;
import com.teraim.vortex.dynamic.workflow_abstracts.Drawable;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;
import com.teraim.vortex.dynamic.workflow_realizations.gis.WF_Gis_Map;
import com.teraim.vortex.gis.GisImageView;
import com.teraim.vortex.loadermodule.ConfigurationModule;
import com.teraim.vortex.loadermodule.FileLoader;
import com.teraim.vortex.loadermodule.LoadResult;
import com.teraim.vortex.loadermodule.ConfigurationModule.Source;
import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;
import com.teraim.vortex.loadermodule.configurations.AirPhotoMetaData;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.PersistenceHelper;

public class AddGisLayerBlock extends Block {

	
	private String id, name, label,target;
	private boolean isVisible, hasWidget;
	private WF_Gis_Map myGis;
	private boolean showLabels;
	
	public AddGisLayerBlock(String id, String name, String label,
			String target, boolean isVisible, boolean hasWidget, boolean showLabels) {
		super();
		this.id = id;
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
			myGis.getGis().addLayer(gisLayer,name);
			if (hasWidget) {
				Log.d("vortex","Layer "+name+" has a widget");
				LinearLayout layersL = (LinearLayout)myGis.getWidget().findViewById(R.id.LayersL);
				LayoutInflater li = LayoutInflater.from(myContext.getContext());
				View layersRow = li.inflate(R.layout.layers_row, null);
				CheckBox lShow = (CheckBox)layersRow.findViewById(R.id.cbShow);
				CheckBox lLabels = (CheckBox)layersRow.findViewById(R.id.cbLabels);
				lShow.setChecked(isVisible);
				lShow.setChecked(showLabels);
				lShow.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {						
						gisLayer.setVisible(isChecked);
						myGis.getGis().invalidate();
					}
				});
				lShow.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {						
						gisLayer.setShowLabels(isChecked);
						myGis.getGis().invalidate();
					}
				});
				layersL.addView(layersRow);
				
				
			}
		}
		
	}
	
	
}