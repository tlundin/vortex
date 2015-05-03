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
	private String N,E,S,W;
	private PhotoMeta photoMetaData;
	
	public CreateGisBlock(String id,String name, 
			String containerId,boolean isVisible,String source,String N,String E, String S,String W, PersistenceHelper ph, PersistenceHelper globalPh) {
		super();
		
		this.id = name;
		this.containerId=containerId;
		this.isVisible=isVisible;
		this.blockId=id;
		this.picUrlorName=source;
		this.N=N;
		this.E=E;
		this.S=S;
		this.W=W;
		
		if(picUrlorName!=null) {
			if (!picUrlorName.startsWith("/"))
				picUrlorName="/"+picUrlorName;
				loadImageMetaData(ph,globalPh);
		} else {
			Log.e("vortex","Pic url null! GisImageView will not load");
			o.addRow("");
			o.addRedText("Adding GisImageView failed. No picture defined!");
		}
	}

	
	public void create(WF_Context myContext) {
		gs = GlobalState.getInstance();
		o = gs.getLogger();
		Container myContainer = myContext.getContainer(containerId);
		if (myContainer!=null) {
		LayoutInflater li = LayoutInflater.from(myContext.getContext());
		View mapView = li.inflate(R.layout.image_gis_layout, null);
		WF_Gis_Map gis = new WF_Gis_Map(blockId, mapView, isVisible, picUrlorName,myContext,photoMetaData);
		myContainer.add(gis);
		myContext.addDrawable(id,gis);
		} else {
			Log.e("vortex","Container null! Cannot add GisImageView!");
			o.addRow("");
			o.addRedText("Adding GisImageView to "+containerId+" failed. Container not configured");
		}
		
	}

	private void loadImageMetaData(PersistenceHelper ph, PersistenceHelper globalPh) {		
		//Did we get image meta data in xml tags?
		//If not, try to load it from file.
		if (N==null||E==null||S==null||W==null||N.length()==0) 
			loadImageMetaFromFile(ph,globalPh);
		else {
			Log.e("vortex","Found tags for photo meta");
			setMetaData(new PhotoMeta(N,E,S,W));
		}
	}
	
	private void loadImageMetaFromFile(PersistenceHelper ph, PersistenceHelper globalPh) {
		
		String tmp[] = picUrlorName.split("/");
		if (tmp.length>0) {
			//figure out name of metadatafile. Assume to be same as picture name.
			String[]tmp2 = tmp[tmp.length-1].split("\\.");

		if (tmp2!=null && tmp2.length!=0) {
			String metaFileName = tmp2[0];
			String metaFolder = picUrlorName.substring(0, picUrlorName.length()-tmp[tmp.length-1].length());
			Log.d("vortex","metafilename: "+metaFileName+" metaFolder"+metaFolder);
			final ConfigurationModule meta = new AirPhotoMetaData(globalPh,ph,Source.file,
				Constants.VORTEX_ROOT_DIR+globalPh.get(PersistenceHelper.BUNDLE_NAME)+"/"+metaFolder,metaFileName,""); 
		new FileLoader(null, null, new FileLoadedCb(){
			@Override
			public void onFileLoaded(LoadResult res) {

				if (res.errCode==ErrorCode.frozen) {
					PhotoMeta pm = (PhotoMeta)meta.getEssence();
					Log.d("vortex","img N, W, S, E "+pm.N+","+pm.W+","+pm.S+","+pm.E);
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

	protected void setMetaData(PhotoMeta photoMeta) {
		
		photoMetaData=photoMeta;
	}



}


