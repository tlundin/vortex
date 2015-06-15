package com.teraim.vortex.dynamic.workflow_realizations.gis;

import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

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
	private TextView avstT,riktT;
	private TextSwitcher avstTS;
	private TextSwitcher riktTS;
	private Button unlockB,startB,centerB;
	
	public WF_Gis_Map(String id, View mapView, boolean isVisible, String picUrlorName,
			final WF_Context myContext, PhotoMeta photoMeta, View avstriktF) {
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
		
		avstTS = (TextSwitcher)avstRiktF.findViewById(R.id.avstTS);
		riktTS = (TextSwitcher)avstRiktF.findViewById(R.id.riktTS);
		
		centerB = (Button)mapView.findViewById(R.id.centerUserB);
		
		centerB.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				gisImageView.centerOnUser();
				
			}
		});
		
		
		unlockB = (Button)avstRiktF.findViewById(R.id.unlockB);
		
		unlockB.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				gisImageView.unSelectGop();
				
			}
		});
		
		startB = (Button)avstRiktF.findViewById(R.id.startB);
		
		startB.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				gisImageView.runSelectedWf();
				
			}
		});
		
		avstTS.setFactory(new ViewFactory() {

		 public View makeView() {
		  TextView myText = new TextView(myContext.getContext());
		  myText.setGravity(Gravity.CENTER);

		  FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
		    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
		    Gravity.CENTER);
		  myText.setLayoutParams(params);

		  myText.setTextSize(36);
		  myText.setTextColor(Color.WHITE);
		  return myText;
		 }});
		riktTS.setFactory(new ViewFactory() {

			 public View makeView() {
			  TextView myText = new TextView(myContext.getContext());
			  myText.setGravity(Gravity.CENTER);

			  FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
			    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
			    Gravity.CENTER);
			  myText.setLayoutParams(params);

			  myText.setTextSize(36);
			  myText.setTextColor(Color.WHITE);
			  return myText;
			 }});
	}

	public GisImageView getGis() {
		return gisImageView;
	}
	
	int noA=5,noR=5;
	public void setAvstTxt(String text) {
		String ct = ((TextView)avstTS.getCurrentView()).getText().toString();
		if (ct.equals(text)&&noA-->0)
			return;
		else {
			noA=5;
			avstTS.setText(text);
		}
	}
	public void setRiktTxt(String text) {
		String ct = ((TextView)riktTS.getCurrentView()).getText().toString();
		if (ct.equals(text)&&noR-->0)
			return;
		else {
			noR=5;
			riktTS.setText(text);
		}
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
