package com.teraim.vortex.dynamic.workflow_realizations.gis;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.R;
import com.teraim.vortex.dynamic.types.PhotoMeta;
import com.teraim.vortex.dynamic.workflow_abstracts.Drawable;
import com.teraim.vortex.dynamic.workflow_abstracts.Event;
import com.teraim.vortex.dynamic.workflow_abstracts.EventListener;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Widget;
import com.teraim.vortex.gis.GisImageView;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.PersistenceHelper;
import com.teraim.vortex.utils.Tools;

/**
 * 
 * @author Terje
 * Copyright Teraim 2015
 * Do not redistribute or change without prior agreement with copyright owners.
 * 
 * 
 * Implements A GIS Map widget. Based on  
 */
public class WF_Gis_Map extends WF_Widget implements Drawable, EventListener {

	private PersistenceHelper globalPh,ph;
	private String gisDir;
	private GisImageView gisImageView;
	private LinearLayout filtersC,layersC;
	private final WF_Context myContext;
	private View avstRiktF;
	private TextView avstRiktT;
	
	public WF_Gis_Map(String id, View mapView, boolean isVisible, String picUrlorName,
			WF_Context myContext, PhotoMeta photoMeta, View avstriktF) {
		super(id, mapView, isVisible, myContext);
		GlobalState gs = GlobalState.getInstance();
		globalPh = gs.getGlobalPreferences();
		String fullPicFileName = Constants.VORTEX_ROOT_DIR+globalPh.get(PersistenceHelper.BUNDLE_NAME)+picUrlorName;
		Context ctx = myContext.getContext();	
		ph = gs.getPreferences();
		Bitmap bmp = Tools.getScaledImage(ctx,fullPicFileName);
		gisImageView = (GisImageView)mapView.findViewById(R.id.GisV);		
		gisImageView.setImageBitmap(bmp);
		gisImageView.initialize(this,photoMeta);
		myContext.addGis(id,this);
		this.myContext=myContext;
		this.avstRiktF = avstriktF;
		this.avstRiktT = (TextView)avstRiktF.findViewById(R.id.avstriktT);
	}

	public GisImageView getGis() {
		return gisImageView;
	}
	
	public TextView getAvstRiktView() {
		return avstRiktT;
	}
	
	public void setVisibleAvstRikt(boolean isVisible) {
		if (isVisible)
			avstRiktF.setVisibility(View.VISIBLE);
		else
			avstRiktF.setVisibility(View.INVISIBLE);
	}


	@Override
	public void onEvent(Event e) {
		
		Log.d("vortex","In GIS_Map Event Handler");
	}

	//Relay event to myContext without exposing context to caller.
	public void registerEvent(Event event) {
		myContext.registerEvent(event);
	}


	
	
}
