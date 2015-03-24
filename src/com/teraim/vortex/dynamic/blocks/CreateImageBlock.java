/**
 * 
 */
package com.teraim.vortex.dynamic.blocks;

import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.teraim.vortex.dynamic.workflow_realizations.WF_Container;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Widget;

/**
 * @author Terje
 *
 */
public class CreateImageBlock extends Block {

	private String name;
	private String container;
	private String source;
	private String scale;
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
		ScaleType scaleT=ScaleType.FIT_XY;
		ImageView img = new ImageView(myContext.getContext());
		new DownloadImageTask(img)
        .execute(source);
		Log.d("vortex","Source is "+source);
		WF_Container myContainer = (WF_Container)myContext.getContainer(container);
		if (scale!=null || scale.length()>0)
			scaleT = ScaleType.valueOf(scale);
		img.setScaleType(scaleT);
		WF_Widget myWidget= new WF_Widget(blockId,img,isVisible,myContext);	
		myContainer.add(myWidget);
		
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
}
