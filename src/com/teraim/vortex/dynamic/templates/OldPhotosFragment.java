package com.teraim.vortex.dynamic.templates;

import java.io.File;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
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
import android.widget.ImageButton;
import android.widget.Toast;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.R;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.ImageHandler;

public class OldPhotosFragment extends Fragment implements OnGesturePerformedListener {

	private GestureLibrary gestureLib;
	private View v;
	private ImageButton norr;
	private ImageButton syd;
	private ImageButton ost;
	private ImageButton vast;
	private ImageButton sp;
	private ImageHandler imgHandler;
	private GlobalState gs;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		gs = GlobalState.getInstance(getActivity());
		Log.d("nils","in onCreateView of old photos");
		v = inflater.inflate(R.layout.template_foto_right, container, false);	

		GestureOverlayView gestureOverlayView = (GestureOverlayView)v.findViewById(R.id.gesture_overlay);
		gestureOverlayView.setGestureVisible(false);
		gestureOverlayView.addOnGesturePerformedListener(this);
		gestureLib = GestureLibraries.fromRawResource(this.getActivity(), R.raw.gestures);
		if (!gestureLib.load()) {      	
			Log.i("nils", "Load gesture libraries failed.");  
		}  

		norr = (ImageButton)v.findViewById(R.id.pic_norr);
		norr.setOnClickListener(ruby(Type.NORR));	
		syd = (ImageButton)v.findViewById(R.id.pic_soder);
		syd.setOnClickListener(ruby(Type.SYD));	
		ost= (ImageButton)v.findViewById(R.id.pic_ost);
		ost.setOnClickListener(ruby(Type.OST));	
		vast = (ImageButton)v.findViewById(R.id.pic_vast);
		vast.setOnClickListener(ruby(Type.VAST));	
		sp= (ImageButton)v.findViewById(R.id.pic_sp);
		sp.setOnClickListener(ruby(Type.SMA));	

		imgHandler = new ImageHandler(gs,this);	

		File folder = new File(Constants.OLD_PIC_ROOT_DIR);

		if(!folder.mkdirs())
			Log.e("NILS","Failed to create pic root folder");
		initPic(norr,Constants.NORR);
		initPic(syd,Constants.SYD);
		initPic(ost,Constants.OST);
		initPic(vast,Constants.VAST);
		initPic(sp,"SMA");

		return v;
	}



	private OnClickListener ruby(final Type typ) {

		return new OnClickListener() {

			@Override
			public void onClick(View v) {

				ImageButton b = new ImageButton(getActivity());
				boolean result = imgHandler.drawButton(b, typ.name(), 1,true);
				if (result) {

					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
					builder.setView(b);
					builder.setPositiveButton("Tack", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {

						}
					});
					builder.show();
				}
			}

		};
	}


	private void initPic(final ImageButton b, final String name) {
		imgHandler.drawButton(b,name,2,true);
	}



	enum Type {
		NORR,
		SYD,
		OST,
		VAST,
		SMA
	}



	@Override
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
		Log.d("nils","Number of gestures available: "+gestureLib.getGestureEntries().size());
		ArrayList<Prediction> predictions = gestureLib.recognize(gesture);
		Log.d("nils","Number of predictions: "+predictions.size());
		for (Prediction prediction : predictions) {
			if (prediction.score > .5) {
				Log.d("nils","MATCH!!");
				if (prediction.name.equals("right")) {
					getFragmentManager().popBackStackImmediate();

				} else 
					Toast.makeText(getActivity(), "vänster till höger", Toast.LENGTH_SHORT).show();

			}
		}		
	}

}
