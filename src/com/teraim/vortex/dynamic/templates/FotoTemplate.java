package com.teraim.vortex.dynamic.templates;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnLongClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.teraim.vortex.R;
import com.teraim.vortex.dynamic.Executor;
import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.dynamic.types.SweLocation;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.types.Variable.DataType;
import com.teraim.vortex.dynamic.workflow_realizations.WF_ClickableField;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Container;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.non_generics.NamedVariables;
import com.teraim.vortex.utils.Geomatte;
import com.teraim.vortex.utils.ImageHandler;
import com.teraim.vortex.utils.PersistenceHelper;
import com.teraim.vortex.utils.Tools;


public class FotoTemplate extends Executor implements OnGesturePerformedListener, LocationListener {

	List<WF_Container> myLayouts;
	
	private GestureLibrary gestureLib;
	private ToggleButton gpsB;
	private TextView gpsT,GPS_X,GPS_Y;
	private boolean fixed = false;
	private TextView GPS_Acc;
	private ImageButton norr;
	private ImageButton syd;
	private ImageButton ost;
	private ImageButton vast;
	private ImageButton sp;
	private ImageHandler imgHandler;
	private LocationManager lm;
	private HashMap<String, ImageButton> buttonM = new HashMap<String,ImageButton>();
	private SweLocation cords;
	private ToggleButton avstandB;
	private TextView sydT,vastT,ostT,spT,norrT;
	private ActionMode mActionMode;
	
	private Variable n,e;

	private View v;
	
	private String selectedPictureName="";
	private ImageButton selectedPicture=null;
	
	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		// Called when the action mode is created; startActionMode() was called
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Inflate a menu resource providing context menu items
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.foto_deletemenu, menu);
			return true;
		}

		// Called each time the action mode is shown. Always called after onCreateActionMode, but
		// may be called multiple times if the mode is invalidated.
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false; // Return false if nothing is done
		}

		// Called when the user selects a contextual menu item
		@Override
		public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
			
			switch (item.getItemId()) {
			    
			case R.id.menu_delete:
				boolean result = imgHandler.deleteImage(selectedPictureName);
				
				if (result) {
					Log.d("vortex","delete success!");
					selectedPicture.setImageResource(R.drawable.case_no_pic);
					}
				else
					Log.e("vortex","failed to delete "+selectedPictureName);
				mode.finish(); // Action picked, so close the CAB
				return true;
			
			default:
				return false;
			}
		}

		// Called when the user exits the action mode
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;				
		}
	};
	
	
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		myContext.resetState();
		Log.d("nils","in onCreateView of foto template");
		
		v = inflater.inflate(R.layout.template_foto, container, false);	
		
		//		myLayouts.add(new WF_Container("Field_List_panel_1", (LinearLayout)v.findViewById(R.id.fieldList), root));
		//		myLayouts.add(new WF_Container("Aggregation_panel_3", (LinearLayout)v.findViewById(R.id.aggregates), root));
		//		myLayouts.add(new WF_Container("Description_panel_1", (FrameLayout)v.findViewById(R.id.Description), root));
		myContext.addContainers(getContainers());


		//Gestures
		GestureOverlayView gestureOverlayView = (GestureOverlayView)v.findViewById(R.id.gesture_overlay);
		gestureOverlayView.setGestureVisible(false);
		gestureOverlayView.addOnGesturePerformedListener(this);
		gestureLib = GestureLibraries.fromRawResource(this.getActivity(), R.raw.gestures);
		if (!gestureLib.load()) {      	
			Log.i("nils", "Load gesture libraries failed.");  
		}  
		
			
		avstandB = (ToggleButton)v.findViewById(R.id.avstandB);
		gpsB = (ToggleButton)v.findViewById(R.id.gpsBtn);
		gpsT = (TextView)v.findViewById(R.id.gpsText);
		GPS_X = (TextView)v.findViewById(R.id.GPS_X);
		GPS_Y = (TextView)v.findViewById(R.id.GPS_Y);
		GPS_Acc = (TextView)v.findViewById(R.id.Accuracy);
		norr = (ImageButton)v.findViewById(R.id.pic_norr);
		syd = (ImageButton)v.findViewById(R.id.pic_soder);
		ost= (ImageButton)v.findViewById(R.id.pic_ost);
		vast = (ImageButton)v.findViewById(R.id.pic_vast);
		sp= (ImageButton)v.findViewById(R.id.sp);
		sydT = (TextView)v.findViewById(R.id.sydT);
		vastT = (TextView)v.findViewById(R.id.vastT);
		ostT = (TextView)v.findViewById(R.id.ostT);
		spT = (TextView)v.findViewById(R.id.spT);
		norrT = (TextView)v.findViewById(R.id.norrT);
		
		buttonM.clear();
		gpsT.setText("");
		
		//Init pic handler
		imgHandler = new ImageHandler(gs,this);		
		//Init geoupdate
		lm = (LocationManager)this.getActivity().getSystemService(Context.LOCATION_SERVICE);
		//Create buttons.
		initPic(norr,Constants.NORR);
		initPic(syd,Constants.SYD);
		initPic(ost,Constants.OST);
		initPic(vast,Constants.VAST);
		initPic(sp,"SMA");
		File folder = new File(Constants.PIC_ROOT_DIR);
		if(!folder.mkdirs())
			Log.e("NILS","Failed to create pic root folder");

		gpsB.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (gpsB.isChecked()) {
					if (!setStartPoint())
						gpsB.setChecked(false);
				} else 
					fixed = false;					
				
			}
		});

		avstandB.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				toggleAvstand(avstandB.isChecked());
				
			}
		});
		gs.setKeyHash(al.createProvytaKeyMap());

		n = al.getVariableInstance(NamedVariables.CentrumGPSNS);
		e = al.getVariableInstance(NamedVariables.CentrumGPSEW);
		
		if (e.getValue()!=null &&n.getValue()!=null) {
			GPS_X.setText(e.getValue()+"");
			GPS_Y.setText(n.getValue()+"");
			gpsB.setChecked(true);
			fixed = true;
		} else {
			fixed = false;
			gpsB.setChecked(false);
		}
		
		
		
		if (gs.getPreferences().getB(PersistenceHelper.AVSTAND_IS_PRESSED)) {
			toggleAvstand(true);
			avstandB.setChecked(true);
		}
		
		VariableConfiguration al = gs.getVariableConfiguration();
		
		Toast.makeText(this.activity,"<<<< Svep åt vänster för historiska bilder!", Toast.LENGTH_SHORT).show();
		
		
		
			
		
		return v;

	}
	

	private void toggleAvstand(boolean avstand) {
		Log.d("nils","Avstånd is "+avstand);

		int status = avstand?View.INVISIBLE:View.VISIBLE;
		boolean luck = false;
		if (avstand) {
			sydT.setText("AVSTÅNDSBILD");
			luck = initPic(syd,"AVST");
		} else {
			sydT.setText("Mot syd");
			luck = initPic(syd,Constants.SYD);
		}
		if (!luck)
			syd.setImageResource(R.drawable.case_no_pic);
		
		norr.setVisibility(status);
		ost.setVisibility(status);
		vast.setVisibility(status);
		sp.setVisibility(status);
		gpsB.setVisibility(status);
		gpsT.setVisibility(status);
		GPS_X.setVisibility(status);
		GPS_Y.setVisibility(status);
		GPS_Acc.setVisibility(status);
		vastT.setVisibility(status);
		ostT.setVisibility(status);
		spT.setVisibility(status);
		norrT.setVisibility(status);
		gs.getPreferences().put(PersistenceHelper.AVSTAND_IS_PRESSED,avstand);
	}
	

	private boolean initPic(final ImageButton b, final String name) {
		boolean ret;
		buttonM.put(name, b);
		ret = imgHandler.drawButton(b,name,2,false);
		imgHandler.addListener(b,name);
		b.setLongClickable(true);
		b.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				selectedPictureName = name;
				selectedPicture = b;
				mActionMode = ((Activity)myContext.getContext()).startActionMode(mActionModeCallback);
				return true;
			}
		});
		return ret;
	}

	@Override
	public void onPause() {
		super.onPause();
		lm.removeUpdates(this);
	}

	@Override
	public void onResume() {
		super.onResume();

		//---request for location updates---
		lm.requestLocationUpdates(
				LocationManager.GPS_PROVIDER,
				0,
				1,
				this);

	}




	@Override
	public void onStart() {
		super.onStart();
		if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
			startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), 0);
		Log.d("nils","in onStart foto");

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent myI){
		Log.d("nils","Gets to onActivityResult");

		if (requestCode == ImageHandler.TAKE_PICTURE){
			if (resultCode == Activity.RESULT_OK) 
			{
				Log.d("Strand","picture was taken, result ok");
				//				String name = myI.getStringExtra(Strand.KEY_PIC_NAME);
				String currSaving = imgHandler.getCurrentlySaving();
				if (currSaving!=null) {
					ImageButton b = buttonM.get(currSaving);
					imgHandler.drawButton(b,currSaving,2,false);
					Log.d("Strand","Drew button!");
				} else
					Log.e("Strand","Did not find pic with name "+currSaving+" in onActRes in TakePic Activity");
			} else {
				Log.d("Strand","picture was NOT taken, result NOT ok");
			}

		}

	}



	@Override
	protected List<WF_Container> getContainers() {
		myLayouts = new ArrayList<WF_Container>();
		myLayouts.add(new WF_Container("root", (RelativeLayout)v.findViewById(R.id.root), null));
		return myLayouts;
	}

	public void execute(String name, String target) {

	}



	@Override
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
		ArrayList<Prediction> predictions = gestureLib.recognize(gesture);
		for (Prediction prediction : predictions) {
			if (prediction.score > .5) {
				Log.d("nils","MATCH!!");
				if (prediction.name.equals("left")) {
					final FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction(); 
					OldPhotosFragment gs = new OldPhotosFragment();  			
					ft.replace(R.id.content_frame, gs);
					ft.addToBackStack(null);
					ft.commit(); 
				} 

			}
		}		
	}


	@Override
	public void onLocationChanged(Location location) {
		//If no startpunkt set, update button
		if (!fixed) {
			cords = Geomatte.convertToSweRef(location.getLatitude(),location.getLongitude());
			gpsB.setVisibility(View.VISIBLE);
			GPS_X.setText(cords.east+"");
			GPS_Y.setText(cords.north+"");
			GPS_Acc.setText(location.getAccuracy()+"");
			//gpsB.setText("Sätt startpunkt\n(N: "+cords[0]+" \nÖ: "+cords[1]+")");
		} 
	}

	@Override
	public void onProviderDisabled(String provider) {
		gpsB.setVisibility(View.GONE);
		gpsT.setText("GPS avslagen.");
	}
	@Override
	public void onProviderEnabled(String arg0) {
		gpsT.setText("GPS påslagen. Söker..");
	}
	@Override
	public void onStatusChanged(String provider, int status, Bundle arg2) {
		if (status == LocationProvider.AVAILABLE) {
			gpsT.setText("GPS signal ok");
		} else if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
			gpsB.setVisibility(View.GONE);
			gpsT.setText("Söker GPS..");


		} else if (status == LocationProvider.OUT_OF_SERVICE) {
			gpsB.setVisibility(View.GONE);
			gpsT.setText("GPS tjänst förlorad");

		}


	}

	public boolean setStartPoint() {	
		if(cords!=null) {
			e.setValue(cords.east+"");
			n.setValue(cords.north+"");
			gpsT.setText("Startpunkt satt till:\nN: "+cords.north+"\nÖ: "+cords.east);
			fixed=true;
			myContext.registerEvent(new WF_Event_OnSave("fototemplate"));
			return true;
		} else {
			gpsT.setText("Kan inte sätta ett värde! Ingen GPS signal.");
			return false;
		}
	}



}