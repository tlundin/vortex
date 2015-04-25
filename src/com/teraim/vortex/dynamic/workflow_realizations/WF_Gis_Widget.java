package com.teraim.vortex.dynamic.workflow_realizations;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.teraim.vortex.FileLoadedCb;
import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.types.PhotoMeta;
import com.teraim.vortex.gis.GisImageView;
import com.teraim.vortex.loadermodule.ConfigurationModule;
import com.teraim.vortex.loadermodule.ConfigurationModule.Source;
import com.teraim.vortex.loadermodule.FileLoader;
import com.teraim.vortex.loadermodule.LoadResult;
import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;
import com.teraim.vortex.loadermodule.configurations.AirPhotoMetaData;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.PersistenceHelper;
import com.teraim.vortex.utils.Tools;

public class WF_Gis_Widget extends WF_Widget {

	private PhotoMeta photoMetaData;
	private PersistenceHelper globalPh,ph;
	private String gisDir;

	public WF_Gis_Widget(String id, GisImageView gisImageView, boolean isVisible, String picUrlorName,
			WF_Context myContext) {
		super(id, gisImageView, isVisible, myContext);
		GlobalState gs = GlobalState.getInstance();
		globalPh = gs.getGlobalPreferences();
		gisDir = Constants.VORTEX_ROOT_DIR+globalPh.get(PersistenceHelper.BUNDLE_NAME)+"/flygdata/";
		Context ctx = myContext.getContext();
		String picName = "207.jpg";
		loadImageMetaData(picName);
		
		ph = gs.getPreferences();
		Bitmap bmp = Tools.getScaledImage(ctx,gisDir+picName);
		gisImageView.setImageBitmap(bmp);
	}

	
	private void loadImageMetaData(String imgName) {
		assert(imgName!=null);

		String metaFile = imgName.split("\\.")[0];
		final ConfigurationModule meta = new AirPhotoMetaData(globalPh,ph,Source.file,gisDir,metaFile,""); 
		new FileLoader(null, null, new FileLoadedCb(){
			@Override
			public void onFileLoaded(LoadResult res) {

				if (res.errCode==ErrorCode.frozen) {
					PhotoMeta pm = (PhotoMeta)meta.getEssence();
					Log.d("vortex","img top, botton, left, right "+pm.top+","+pm.bottom+","+pm.left+","+pm.right);
					WF_Gis_Widget.this.setMetaData(pm);
				}
				else
					Log.d("vortex","Failed to parse image location. Errorcode "+res.errCode.name());
			}
			@Override
			public void onFileLoaded(ErrorCode errCode, String version) {
			}
			@Override
			public void onUpdate(Integer... args) {
			}},
			false).execute(meta);

	}

	protected void setMetaData(PhotoMeta pm) {
		this.photoMetaData=pm;
	}	
}
