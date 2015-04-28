package com.teraim.vortex.dynamic.blocks;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.teraim.vortex.FileLoadedCb;
import com.teraim.vortex.GlobalState;
import com.teraim.vortex.R;
import com.teraim.vortex.dynamic.types.PhotoMeta;
import com.teraim.vortex.dynamic.types.Workflow.Unit;
import com.teraim.vortex.dynamic.workflow_abstracts.Container;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;
import com.teraim.vortex.dynamic.workflow_realizations.gis.WF_Gis_Map;
import com.teraim.vortex.gis.GisImageView;
import com.teraim.vortex.loadermodule.ConfigurationModule;
import com.teraim.vortex.loadermodule.FileLoader;
import com.teraim.vortex.loadermodule.LoadResult;
import com.teraim.vortex.loadermodule.ConfigurationModule.Source;
import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;
import com.teraim.vortex.loadermodule.configurations.AirPhotoMetaData;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.PersistenceHelper;

public class CreateGisBlock extends Block {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2013870148670474248L;
	String id,type,label,containerId,postLabel,initialValue;
	Unit unit;
	GlobalState gs;
	boolean isVisible = false,showHistorical;
	String format;
	private String picUrlorName;
	private String topN,topE,bottomN,bottomE;
	private PhotoMeta photoMetaData;
	
	public CreateGisBlock(String id,String name, 
			String containerId,boolean isVisible,String source,String topN,String topE, String bottomN,String bottomE) {
		super();
		
		this.id = name;
		this.containerId=containerId;
		this.isVisible=isVisible;
		this.blockId=id;
		this.picUrlorName=source;
		this.topE=topE;
		this.topN=topN;
		this.bottomE=bottomE;
		this.bottomN=bottomN;
	}

	
	public void create(WF_Context myContext) {
		gs = GlobalState.getInstance();
		Container myContainer = myContext.getContainer(containerId);
		o = gs.getLogger();

		if(myContainer !=null && picUrlorName!=null) {
			if (!picUrlorName.startsWith("/"))
				picUrlorName="/"+picUrlorName;
			 LayoutInflater li = LayoutInflater.from(myContext.getContext());
			 View mapView = li.inflate(R.layout.image_gis_layout, null);
				WF_Gis_Map gis = new WF_Gis_Map(blockId, mapView, isVisible, picUrlorName,myContext);
				myContainer.add(gis);
				myContext.addDrawable(id,gis);
				loadImageMetaData();
		} else {
			Log.e("vortex","Container null! Cannot add GisImageView!");
			o.addRow("");
			o.addRedText("Adding GisImageView to "+containerId+" failed. Container not configured");
		}
	}

	private void loadImageMetaData() {		
		//Did we get image meta data in xml tags?
		//If not, try to load it from file.
		if (topE==null||topN==null||bottomE==null||bottomN==null||topE.length()==0) 
			loadImageMetaFromFile();
		else {
			Log.e("vortex","Found tags for photo meta");
			setMetaData(new PhotoMeta(topN,topE,bottomN,bottomE));
		}
	}
	
	private void loadImageMetaFromFile() {
		
		String tmp[] = picUrlorName.split("/");
		if (tmp.length>0) {
			//figure out name of metadatafile. Assume to be same as picture name.
			String[]tmp2 = tmp[tmp.length-1].split("\\.");

		if (tmp2!=null && tmp2.length!=0) {
			String metaFileName = tmp2[0];
			String metaFolder = picUrlorName.substring(0, picUrlorName.length()-tmp[tmp.length-1].length());
			Log.d("vortex","metafilename: "+metaFileName+" metaFolder"+metaFolder);
			final ConfigurationModule meta = new AirPhotoMetaData(gs.getGlobalPreferences(),gs.getPreferences(),Source.file,
				Constants.VORTEX_ROOT_DIR+gs.getGlobalPreferences().get(PersistenceHelper.BUNDLE_NAME)+"/"+metaFolder,metaFileName,""); 
		new FileLoader(null, null, new FileLoadedCb(){
			@Override
			public void onFileLoaded(LoadResult res) {

				if (res.errCode==ErrorCode.frozen) {
					PhotoMeta pm = (PhotoMeta)meta.getEssence();
					Log.d("vortex","img top, botton, left, right "+pm.top+","+pm.bottom+","+pm.left+","+pm.right);
					setMetaData(pm);
				}
				else
					Log.d("vortex","Failed to parse image location. Errorcode "+res.errCode.name());
			}
			@Override
			public void onFileLoaded(ErrorCode errCode, String version) {
				Log.e("vortex","Error loading foto metadata! ErrorCode: "+errCode);
			}
			@Override
			public void onUpdate(Integer... args) {
			}},
			false).execute(meta);
		}
		}
	}

	protected void setMetaData(PhotoMeta pm) {
		this.photoMetaData=pm;
	}



}


