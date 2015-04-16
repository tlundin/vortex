package com.teraim.vortex.utils;

import java.io.File;

import android.R;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.non_generics.Constants;

public class ImageHandler {

	Fragment c;
	GlobalState gs;
	VariableConfiguration al;

	public ImageHandler(GlobalState gs, Fragment a) {
		c = a;

		this.gs = gs;
		this.al = gs.getVariableConfiguration();
	}


	private String createFileName(String name,boolean isHistorical) {

		String nameWithNum = name;
		if (name.equals("SYD"))
			nameWithNum = "3SYD_";
		else if (name.equals("NORR"))
			nameWithNum = "1NORR";
		else if (name.equals("OST"))
			nameWithNum = "2OST_";
		else if (name.equals("VAST"))
			nameWithNum = "4VAST";
		else if (name.equals("SMA"))
			nameWithNum = "5SMA_";
		else if (name.equals("AVST"))
			nameWithNum = "6AVST";
		String rutID = al.getCurrentRuta();
		String pyID = al.getCurrentProvyta();
		if (rutID!=null&&pyID!=null) {
			int paddingSize=4-rutID.length();
			for (int i=0;i<paddingSize;i++)
				rutID = "0"+rutID;
			Log.d("nils"," PADDINGSIZE: "+paddingSize+" rutaWITHZ: "+rutID);
			paddingSize=4-pyID.length();
			for (int i=0;i<paddingSize;i++)
				pyID = "0"+pyID;		
			Log.d("nils"," PADDINGSIZE: "+paddingSize+" pyWITHZ: "+pyID);
			return "R"+rutID+"_"+pyID+"_"+nameWithNum+"_"+(isHistorical?(Constants.getHistoricalPictureYear()):Constants.getYear())+".JPG";
		}
		return null;
	}


	public boolean drawButton(ImageButton b, String name, int divisor,boolean historical) {

		//Try to load pic from disk, if any.
		//To avoid memory issues, we need to figure out how big bitmap to allocate, approximately
		//Picture is in landscape & should be approx half the screen width, and 1/5th of the height.
		//First get the ration between h and w of the pic.
		final BitmapFactory.Options options = new BitmapFactory.Options();

		final String fileName = createFileName(name,historical);
		if (fileName == null) {			
			return false;
		}
		options.inJustDecodeBounds=true;
		Bitmap bip = BitmapFactory.decodeFile((historical?Constants.OLD_PIC_ROOT_DIR:Constants.PIC_ROOT_DIR)+fileName,options);		

		//there is a picture..
		int realW = options.outWidth;
		int realH = options.outHeight;


		//check if file exists
		if (realW>0) {
			double ratio = realH/realW;
			//Height should not be higher than width.
			if (ratio >0) {
				Log.d("nils", "picture is not landscape. its portrait..");
			}
			Log.d("nils", "realW realH"+realW+" "+realH);

			//Find out screen size.
			Display display = c.getActivity().getWindowManager().getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			int sWidth = size.x;

			//Target width should be about half the screen width.

			double tWidth = sWidth/divisor;
			//height is then the ratio times this..
			int tHeight = (int) (tWidth*ratio);

			//use target values to calculate the correct inSampleSize
			options.inSampleSize = Tools.calculateInSampleSize(options, (int)tWidth, tHeight);

			Log.d("nils"," Calculated insamplesize "+options.inSampleSize);
			//now create real bitmap using insampleSize

			options.inJustDecodeBounds = false;
			Log.d("nils","Filename: "+fileName);
			bip = BitmapFactory.decodeFile((historical?Constants.OLD_PIC_ROOT_DIR:Constants.PIC_ROOT_DIR)+fileName,options);
			if (bip!=null) {
				b.setImageBitmap(bip);
				return true;
			} else {
				Log.d("bils","Picture was null after decode");
				return false;
			}

		}
		else {
			Log.d("nils","Did not find picture "+fileName);
			//need to set the width equal to the height...
			return false;
		}
	}
	
	public boolean deleteImage(final String name) {
		String fileName = createFileName(name,false);
		File file = new File(Constants.PIC_ROOT_DIR, fileName);
		return file.delete();
	}

	public void addListener(ImageButton b, final String name) {

		b.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				Log.d("nils","in the listener for image button");
				String fileName = createFileName(name,false);

				//String fileName = "R00al.getCurrentRuta()
				//Toast.makeText(c.getActivity(),
				//		"pic" + name + " selected",
				//		Toast.LENGTH_SHORT).show();

				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

				File file = new File(Constants.PIC_ROOT_DIR, fileName);

				currSaving=name;
				Uri outputFileUri = Uri.fromFile(file);

				intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
				//				intent.putExtra(Strand.KEY_PIC_NAME, name);
				c.startActivityForResult(intent, TAKE_PICTURE);

			}


		});


	}
	



	public final static int TAKE_PICTURE = 133;

	private String currSaving=null;

	public String getCurrentlySaving() {
		return currSaving;
	}

	private void displayErrorMsg() {
		new AlertDialog.Builder(c.getActivity())
		.setTitle("Ingen ruta/provyta vald")
		.setMessage("F�r att spara och visa bilder m�ste f�rst ruta och provyta v�ljas")
		.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) { 
				// continue with delete
			}
		})

		.setIcon(android.R.drawable.ic_dialog_alert)
		.show();
	}

}
