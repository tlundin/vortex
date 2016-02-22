package com.teraim.fieldapp.dynamic.templates;

import java.io.File;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.non_generics.NamedVariables;
import com.teraim.fieldapp.utils.ImageHandler;

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

		gs = GlobalState.getInstance();
		Log.d("nils","in onCreateView of old photos");
		v = inflater.inflate(R.layout.template_foto_right, container, false);	
		TextView header = (TextView)v.findViewById(R.id.header);
		header.setText("Historiska foton h�rifr�n ("+Constants.getHistoricalPictureYear()+")");
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
		
		Button bildFelB = (Button)v.findViewById(R.id.felibild);
		
		bildFelB.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				final Variable fotoKommentar = gs.getVariableCache().getVariableUsingKey(gs.getVariableConfiguration().createProvytaKeyMap(), NamedVariables.FOTO_KOMMENTAR);				
				final Context ctx = OldPhotosFragment.this.getActivity();
				// get prompts.xml view
				LayoutInflater li = LayoutInflater.from(ctx);
				View promptsView = li.inflate(R.layout.picture_issue, null);
 
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
						ctx);
 
				// set prompts.xml to alertdialog builder
				alertDialogBuilder.setView(promptsView);
 
				final EditText userInput = (EditText) promptsView
						.findViewById(R.id.felibildE);
			
				if (fotoKommentar!=null) {
					String currentComment = fotoKommentar.getValue();
					if (currentComment!=null)
						userInput.setText(currentComment);
					alertDialogBuilder
					.setCancelable(false)
					.setPositiveButton(gs.getString(R.string.save),
					  new DialogInterface.OnClickListener() {
					    public void onClick(DialogInterface dialog,int id) {
						Editable input = userInput.getText();
						if (input != null && input.length()!=0) {
							//Save this into a variable.
							
							fotoKommentar.setValue(input.toString());
							Log.e("vortex","Fotokommentar satt till: "+input);
							Toast.makeText(ctx, "Din kommentar har sparats och kommer skickas till Ume�", Toast.LENGTH_SHORT).show();
						}
							
						
					    }
					  })
					.setNegativeButton(gs.getString(R.string.cancel),
					  new DialogInterface.OnClickListener() {
					    public void onClick(DialogInterface dialog,int id) {
						dialog.cancel();
					    }
					  });

				// create alert dialog
				AlertDialog alertDialog = alertDialogBuilder.create();

				// show it
				alertDialog.show();
					
				} else {
					gs.getLogger().addRow("");
					gs.getLogger().addRedText("The Variable FotoKommentar is missing from Variables.csv!!! Fotokommentarer will not be saved!!");
					new AlertDialog.Builder(ctx)
					.setTitle("Konfigureringsfel")
					.setMessage("Fotokommentar saknas i variabellistan. Programmet kan inte spara din kommentar") 
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setCancelable(false)
					.setNeutralButton("Ok",new Dialog.OnClickListener() {				
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub

						}
					} )
					.show();
				}
				
			}
		});
		
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
					builder.setPositiveButton("St�ng", new DialogInterface.OnClickListener() {
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
		SYD,
		NORR,
		VAST,
		OST,
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
					Toast.makeText(getActivity(), "v�nster till h�ger", Toast.LENGTH_SHORT).show();

			}
		}		
	}

}
