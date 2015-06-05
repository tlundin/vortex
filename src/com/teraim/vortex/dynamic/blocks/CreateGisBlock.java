package com.teraim.vortex.dynamic.blocks;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import com.teraim.vortex.FileLoadedCb;
import com.teraim.vortex.GlobalState;
import com.teraim.vortex.R;
import com.teraim.vortex.dynamic.AsyncResumeExecutorI;
import com.teraim.vortex.dynamic.types.PhotoMeta;
import com.teraim.vortex.dynamic.types.Workflow.Unit;
import com.teraim.vortex.dynamic.workflow_abstracts.Container;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;
import com.teraim.vortex.dynamic.workflow_realizations.gis.WF_Gis_Map;
import com.teraim.vortex.loadermodule.ConfigurationModule;
import com.teraim.vortex.loadermodule.ConfigurationModule.Source;
import com.teraim.vortex.loadermodule.FileLoader;
import com.teraim.vortex.loadermodule.LoadResult;
import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;
import com.teraim.vortex.loadermodule.configurations.AirPhotoMetaData;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.PersistenceHelper;
import com.teraim.vortex.utils.Tools;

public class CreateGisBlock extends Block {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2013870148670474248L;
	private final String name,containerId,source,N,E,S,W;
	Unit unit;
	GlobalState gs;
	boolean isVisible = false,showHistorical;
	String format;

	
	
	private boolean menuUp = false;
	private WF_Context myContext;
	private LoggerI o;

	
	public CreateGisBlock(String id,String name, 
			String containerId,boolean isVisible,String source,String N,String E, String S,String W) {
		super();
		
		this.name = name;
		this.containerId=containerId;
		this.isVisible=isVisible;
		this.blockId=id;
		this.source=source;
		this.N=N;
		this.E=E;
		this.S=S;
		this.W=W;
	}

	
	

	//Callback after image has loaded.
	AsyncResumeExecutorI cb;
	
	public void create(WF_Context myContext,AsyncResumeExecutorI cb) {
		String picUrlorName=source;
		gs = GlobalState.getInstance();
		o = gs.getLogger();
		this.cb=cb;
		this.myContext = myContext;
		PersistenceHelper ph = gs.getPreferences();
		PersistenceHelper globalPh = gs.getGlobalPreferences();
		if(picUrlorName!=null) {
			if (!picUrlorName.startsWith("/"))
				picUrlorName="/"+picUrlorName;
				picUrlorName = Tools.parseString(picUrlorName);
				Log.d("vortex","ParseString result: "+picUrlorName);
				loadImageMetaData(ph,globalPh,picUrlorName);
		} else {
			Log.e("vortex","Pic url null! GisImageView will not load");
			o.addRow("");
			o.addRedText("Adding GisImageView failed. No picture defined!");
		}
	}
	
	public void createAfterLoad(PhotoMeta photoMetaData, String picUrlorName) {
		Container myContainer = myContext.getContainer(containerId);
		if (myContainer!=null && photoMetaData!=null) {
		LayoutInflater li = LayoutInflater.from(myContext.getContext());
		View mapView = li.inflate(R.layout.image_gis_layout, null);
		final View avstriktF = mapView.findViewById(R.id.avstriktF);
		WF_Gis_Map gis = new WF_Gis_Map(blockId, mapView, isVisible, picUrlorName,myContext,photoMetaData,avstriktF);
		myContainer.add(gis);
		myContext.addDrawable(name,gis);
		final View menuL = mapView.findViewById(R.id.menuL);
		
		menuL.setVisibility(View.INVISIBLE);
		avstriktF.setVisibility(View.INVISIBLE);
		final ImageButton menuB = (ImageButton)mapView.findViewById(R.id.menuB);
		
		menuB.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				menuL.setVisibility(menuUp?View.INVISIBLE:View.VISIBLE);
				menuUp = !menuUp;
			}
		});
		} else {
			o.addRow("");
			if (photoMetaData ==null) {
				Log.e("vortex","Photemetadata null! Cannot add GisImageView!");
				o.addRedText("Adding GisImageView to "+containerId+" failed. Photometadata missing (the boundaries of the image on the map)");

			}
			else {
				Log.e("vortex","Container null! Cannot add GisImageView!");
				o.addRedText("Adding GisImageView to "+containerId+" failed. Container cannot be found in template");
				cb.abortExecution("Missing container for GisImageView: "+containerId);
			}
		}
		cb.continueExecution();
	}

	private void loadImageMetaData(PersistenceHelper ph, PersistenceHelper globalPh, String picUrlorName) {		
		//Did we get image meta data in xml tags?
		//If not, try to load it from file.
		if (N==null||E==null||S==null||W==null||N.length()==0) 
			loadImageMetaFromFile(ph,globalPh,picUrlorName);
		else {
			Log.e("vortex","Found tags for photo meta");
			createAfterLoad(new PhotoMeta(N,E,S,W),picUrlorName);
		}
	}
	
	private void loadImageMetaFromFile(PersistenceHelper ph, final PersistenceHelper globalPh,final String fileName) {
		
		String tmp[] = fileName.split("/");
		if (tmp.length>0) {
			//figure out name of metadatafile. Assume to be same as picture name.
			String[]tmp2 = tmp[tmp.length-1].split("\\.");

		if (tmp2!=null && tmp2.length!=0) {
			final String metaFileName = tmp2[0];
			final String metaFolder = fileName.substring(0, fileName.length()-tmp[tmp.length-1].length());
			Log.d("vortex","metafilename: "+metaFileName+" metaFolder"+metaFolder);
			final ConfigurationModule meta = new AirPhotoMetaData(globalPh,ph,Source.file,
				Constants.VORTEX_ROOT_DIR+globalPh.get(PersistenceHelper.BUNDLE_NAME)+metaFolder,metaFileName,""); 
		new FileLoader(null, null, new FileLoadedCb(){
			@Override
			public void onFileLoaded(LoadResult res) {

				if (res.errCode==ErrorCode.frozen) {
					PhotoMeta pm = (PhotoMeta)meta.getEssence();
					Log.d("vortex","img N, W, S, E "+pm.N+","+pm.W+","+pm.S+","+pm.E);
					createAfterLoad(pm,fileName);
				}
				else {
					o.addRow("");
					o.addRedText("Could not find GIS image "+metaFileName);
					Log.e("vortex","Failed to parse image meta. Errorcode "+res.errCode.name());
					cb.abortExecution("Could not load GIS image or image meta file ["+metaFileName+"], errorcode: "+res.errCode.name());
				}
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





}


