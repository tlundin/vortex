package com.teraim.vortex.dynamic.templates;

import java.util.ArrayList;
import java.util.List;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.teraim.vortex.R;
import com.teraim.vortex.Start;
import com.teraim.vortex.dynamic.Executor;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Container;
import com.teraim.vortex.ui.FixPunktFragment;


public class FixPunktTemplate extends Executor implements OnGesturePerformedListener {
	List<WF_Container> myLayouts;


	
	
	
	/* (non-Javadoc)
	 * @see android.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
	 */
	//ViewGroup myContainer = null;
	private GestureLibrary gestureLib;
	View v;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		myContext.resetState();
		v = inflater.inflate(R.layout.template_fixpunkt_wf, container, false);	

		myContext.addContainers(getContainers());
	
		//Gestures
	    GestureOverlayView gestureOverlayView = (GestureOverlayView)v.findViewById(R.id.gesture_overlay);
	    gestureOverlayView.setGestureVisible(false);
	    gestureOverlayView.addOnGesturePerformedListener(this);
	    gestureLib = GestureLibraries.fromRawResource(this.getActivity(), R.raw.gestures);
	    if (!gestureLib.load()) {      	
	    	        Log.i("nils", "Load gesture libraries failed.");  
	    	    }  
	    Button framB = (Button)v.findViewById(R.id.framB);
	    
	    framB.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				final FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction(); 
	  			FixPunktFragment gs = new FixPunktFragment();  			
	  			ft.replace(R.id.content_frame, gs);
	  			ft.addToBackStack(null);
	  			ft.commit(); 
			}
		});

		if (wf!=null) {
			run();
		}		
		
		
		Toast.makeText(this.getActivity(), "<<<<<< Svep �t v�nster f�r fixpunkt grafik!", Toast.LENGTH_LONG).show();
		return v;

	}



	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
	}



	/* (non-Javadoc)
	 * @see android.app.Fragment#onStart()
	 */
	@Override
	public void onStart() {
		super.onStart();

		Log.d("nils","in onStart fixpunkter");
	
	}



	@Override
	protected List<WF_Container> getContainers() {
		myLayouts = new ArrayList<WF_Container>();
		Log.d("nils","in onCreateView of fixpunkt_template");
		//myContainer = container;
		WF_Container root = new WF_Container("root", (LinearLayout)v.findViewById(R.id.root), null);
		myLayouts.add(root);
		myLayouts.add(new WF_Container("Field_List_panel_1", (LinearLayout)v.findViewById(R.id.fieldList), root));
		myLayouts.add(new WF_Container("Aggregation_panel_3", (LinearLayout)v.findViewById(R.id.aggregates), root));
		myLayouts.add(new WF_Container("Description_panel_2", (FrameLayout)v.findViewById(R.id.Description), root));
		return myLayouts;
	}

	public void execute(String name, String target) {

	}



	@Override
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
	    Log.d("nils","Number of gestures available: "+gestureLib.getGestureEntries().size());
	    ArrayList<Prediction> predictions = gestureLib.recognize(gesture);
	    Log.d("nils","Number of predictions: "+predictions.size());
	    for (Prediction prediction : predictions) {
	      if (prediction.score > .5) {
	  		Log.d("nils","MATCH!!");
	  		if (prediction.name.equals("left")) {
	  			final FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction(); 
	  			FixPunktFragment gs = new FixPunktFragment();  			
	  			ft.replace(R.id.content_frame, gs);
	  			ft.addToBackStack(null);
	  			ft.commit(); 
	  		} else 
				Toast.makeText(getActivity(), "Fel h�ll", Toast.LENGTH_SHORT).show();
	  			
	      }
	    }		
	}






}