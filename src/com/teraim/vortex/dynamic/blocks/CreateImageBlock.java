/**
 * 
 */
package com.teraim.vortex.dynamic.blocks;

import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Display;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.workflow_abstracts.Event;
import com.teraim.vortex.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.vortex.dynamic.workflow_abstracts.EventListener;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Container;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Widget;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.Tools;

/**
 * @author Terje
 *
 */
public class CreateImageBlock extends Block implements EventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5781622495945524716L;
	private String name;
	private String container;
	private String source;
	private String scale;
	private ImageView img = null;
	private WF_Context myContext;
	private boolean isVisible;

	public CreateImageBlock(String id, String nName, String container,
			String source, String scale, boolean isVisible) {
		this.blockId=id;
		this.name=nName;
		this.container = container;
		this.source=source;
		this.scale = scale;
		this.isVisible = isVisible;
		Log.d("vortex","Create image block with source "+source);
	}
	
	
	public void create(WF_Context myContext) {
		this.myContext = myContext;
		o = GlobalState.getInstance().getLogger();
		WF_Container myContainer = (WF_Container)myContext.getContainer(container);
		if (myContainer != null && source!=null) {
		ScaleType scaleT=ScaleType.FIT_XY;
		img = new ImageView(myContext.getContext());
		if (Tools.isURL(source)) {
			new DownloadImageTask(img).execute(source);
		} else {
			setImageFromFile(myContext);
		}
		Log.d("vortex","Source is "+source);
		if (scale!=null || scale.length()>0)
			scaleT = ScaleType.valueOf(scale);
		img.setScaleType(scaleT);
		WF_Widget myWidget= new WF_Widget(blockId,img,isVisible,myContext);	
		myContainer.add(myWidget);
		myContext.addEventListener(this, EventType.onActivityResult);
		} else {
			if (source==null) {
				o.addRow("");
				o.addRedText("Failed to add image with block id "+blockId+" - missing source tag");				
			}
			o.addRow("");
			o.addRedText("Failed to add image with block id "+blockId+" - missing container "+container);
		}
	}
	

	
	private void setImageFromFile(WF_Context myContext) {
		final int divisor = 1;
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds=true;
		Bitmap bip = BitmapFactory.decodeFile(Constants.PIC_ROOT_DIR+source,options);		
		int realW = options.outWidth;
		int realH = options.outHeight;
		if (realW>0) {
			double ratio = realH/realW;
			Display display = myContext.getActivity().getWindowManager().getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			int sWidth = size.x;
			double tWidth = sWidth/divisor;
			int tHeight = (int) (tWidth*ratio);
			options.inSampleSize = Tools.calculateInSampleSize(options, (int)tWidth, tHeight);
			options.inJustDecodeBounds = false;
			bip = BitmapFactory.decodeFile(Constants.PIC_ROOT_DIR+source,options);
			img.setImageBitmap(bip);
		}
		else {
			Log.d("nils","Did not find picture "+source);
		}
	}



	class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
	    ImageView bmImage;

	    public DownloadImageTask(ImageView bmImage) {
	        this.bmImage = bmImage;
	    }

	    protected Bitmap doInBackground(String... urls) {
	        String urldisplay = urls[0];
	        Bitmap mIcon11 = null;
	        try {
	            InputStream in = new java.net.URL(urldisplay).openStream();
	            mIcon11 = BitmapFactory.decodeStream(in);
	        } catch (Exception e) {
	            Log.e("Error", e.getMessage());
	            e.printStackTrace();
	        }
	        return mIcon11;
	    }

	    protected void onPostExecute(Bitmap result) {
	        bmImage.setImageBitmap(result);
	    }
	}



	@Override
	public void onEvent(Event e) {
		Log.d("vortex","Img was taken");
		setImageFromFile(myContext);
	}
}
