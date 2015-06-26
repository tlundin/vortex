package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
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
public class WF_Gis_Map extends WF_Widget implements Drawable, EventListener, AnimationListener {

	private PersistenceHelper globalPh,ph;
	private String gisDir;
	private GisImageView gisImageView;
	private LinearLayout filtersC,layersC;
	private final WF_Context myContext;
	private View avstRiktF,createMenuL;
	private TextView avstT,riktT;
	private TextSwitcher avstTS;
	private TextSwitcher riktTS;
	private Button unlockB,startB,centerB;
	private ImageButton objectMenuB;
	private Animation popupShow;
	private Animation popupHide;
	private GisObjectsMenu gisObjectMenu;
	private View gisObjectsPopUp;
	private boolean gisObjMenuOpen=false;
	protected boolean animationRunning=false;
	private List<FullGisObjectConfiguration> myGisObjectTypes;
	private Button createBackB;
	private Button createOkB;

	public WF_Gis_Map(String id, final FrameLayout mapView, boolean isVisible, String picUrlorName,
			final WF_Context myContext, PhotoMeta photoMeta, View avstRL, View createMenuL) {
		super(id, mapView, isVisible, myContext);
		GlobalState gs = GlobalState.getInstance();
		globalPh = gs.getGlobalPreferences();
		String fullPicFileName = Constants.VORTEX_ROOT_DIR+globalPh.get(PersistenceHelper.BUNDLE_NAME)+picUrlorName;
		final Context ctx = myContext.getContext();	
		ph = gs.getPreferences();
		Bitmap bmp = Tools.getScaledImage(ctx,fullPicFileName);
		gisImageView = (GisImageView)mapView.findViewById(R.id.GisV);		
		gisImageView.setImageBitmap(bmp);
		gisImageView.initialize(this,photoMeta);
		myContext.addGis(id,this);
		this.myContext=myContext;
		this.avstRiktF = avstRL;
		
		this.createMenuL=createMenuL;

		avstTS = (TextSwitcher)avstRiktF.findViewById(R.id.avstTS);
		riktTS = (TextSwitcher)avstRiktF.findViewById(R.id.riktTS);

		LayoutInflater li = LayoutInflater.from(ctx);
		gisObjectsPopUp = li.inflate(R.layout.gis_object_menu_pop,null);
		gisObjectMenu = (GisObjectsMenu)gisObjectsPopUp.findViewById(R.id.gisObjectsMenu);
		gisObjectsPopUp.setVisibility(View.GONE);
		mapView.addView(gisObjectsPopUp);

		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);

		params.gravity=Gravity.RIGHT;
		params.topMargin=300;

		gisObjectsPopUp.setLayoutParams(params);
		Button cancelB = (Button)gisObjectsPopUp.findViewById(R.id.cancelB);
		
		cancelB.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				gisObjectsPopUp.startAnimation(popupHide);
			}
		});
		
		objectMenuB = (ImageButton)mapView.findViewById(R.id.objectMenuB);
		objectMenuB.setVisibility(View.GONE);
		objectMenuB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				if (!animationRunning) {
					if (!gisObjMenuOpen) {
						gisImageView.cancelGisObjectCreation();
						gisObjectsPopUp.startAnimation(popupShow);
						mapView.invalidate();
						
						gisImageView.centerOnUser();
					}
					
				}
			}
		});
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
				TextView myText = new TextView(ctx);
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
				TextView myText = new TextView(ctx);
				myText.setGravity(Gravity.CENTER);

				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
						Gravity.CENTER);
				myText.setLayoutParams(params);

				myText.setTextSize(36);
				myText.setTextColor(Color.WHITE);
				return myText;
			}});

		createBackB = (Button)mapView.findViewById(R.id.createBackB);

		createBackB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (gisImageView.goBack())
					setVisibleCreate(false);
			}
		});
		
		
		createOkB = (Button)mapView.findViewById(R.id.createOkB);

		createOkB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				gisImageView.createOk();
				setVisibleCreate(false);
			}
		});
		
		
		popupShow = AnimationUtils.loadAnimation(ctx, R.anim.popup_show);
		popupShow.setAnimationListener(this);
		popupHide = AnimationUtils.loadAnimation(ctx, R.anim.popup_hide);
		popupHide.setAnimationListener(this);

		myGisObjectTypes = new ArrayList<FullGisObjectConfiguration>();
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
	
	public void setVisibleCreate(boolean isVisible) {
		
		if (isVisible)
			createMenuL.setVisibility(View.VISIBLE);
		else
			createMenuL.setVisibility(View.INVISIBLE);
	}


	@Override
	public void onEvent(Event e) {

		Log.d("vortex","In GIS_Map Event Handler");
	}

	//Relay event to myContext without exposing context to caller.
	public void registerEvent(Event event) {
		myContext.registerEvent(event);
	}

	@Override
	public void onAnimationStart(Animation animation) {
		if (animation.equals(popupShow)) {
			Log.d("vortex","gets here!");
			gisObjectsPopUp.setVisibility(View.VISIBLE);
			gisObjectMenu.setMenuItems(myGisObjectTypes,gisImageView,this);
		}
		animationRunning = true;
	}

	@Override
	public void onAnimationEnd(Animation animation) {
		animationRunning = false;
		if (animation.equals(popupShow))
			gisObjMenuOpen = true;
		else {
			gisObjectsPopUp.setVisibility(View.GONE);
			gisObjMenuOpen = false;
		}
	}

	@Override
	public void onAnimationRepeat(Animation animation) {
		// TODO Auto-generated method stub

	}

	//Add a gisobject to the createMenu.
	public void addGisObjectType(FullGisObjectConfiguration gop) {
		myGisObjectTypes.add(gop);
		objectMenuB.setVisibility(View.VISIBLE);
	}

	public void startGisObjectCreation(FullGisObjectConfiguration fop) {
		gisObjectsPopUp.startAnimation(popupHide);
		boolean firstTime = (globalPh.get(PersistenceHelper.GIS_CREATE_FIRST_TIME_KEY).equals(PersistenceHelper.UNDEFINED));
		if (firstTime) {
			globalPh.put(PersistenceHelper.GIS_CREATE_FIRST_TIME_KEY, "notfirstanymore!");
			new AlertDialog.Builder(myContext.getContext())
			.setTitle("Creating GIS objects")
			.setMessage("You are creating your first GIS object!\nClick on the location where you want to place the object, or the objects first point. You will then get options how to proceed in the righthand side menu.")
			.setIcon(android.R.drawable.ic_dialog_info)
			.setCancelable(false)
			.setNeutralButton("Ok!",new Dialog.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
				
				}
			} )
			.show();
		}
		//Put Map and GisViewer into create mode.
		
		//swap in buttons for create mode. 
		gisImageView.startGisObjectCreation(fop);
	}



}
