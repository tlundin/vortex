package com.teraim.vortex.dynamic.blocks;

import java.util.List;

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
import com.teraim.vortex.utils.RuleExecutor;
import com.teraim.vortex.utils.RuleExecutor.SubstiResult;
import com.teraim.vortex.utils.RuleExecutor.TokenizedItem;

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
	
	private boolean menuUp = false;
	private WF_Context myContext;
	private LoggerI o;
	
	public CreateGisBlock(String id,String name, 
			String containerId,boolean isVisible,String source,String N,String E, String S,String W) {
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
	}

	
	private String parseString(String varString) {

		RuleExecutor re = RuleExecutor.getInstance(GlobalState.getInstance().getContext());
		List<TokenizedItem> tokenized = re.findTokens(varString, null);
		SubstiResult x=null;
		if (tokenized!=null)
			x = re.substituteForValue(tokenized, varString, true);
		if (x!=null) {
			String res = x.result;
			res = res.replace("<", "");
			res = res.replace(">", "");
			return res;
		}
		else
			return varString;
	}

	//Callback after image has loaded.
	AsyncResumeExecutorI cb;
	
	public void create(WF_Context myContext,AsyncResumeExecutorI cb) {
		gs = GlobalState.getInstance();
		o = gs.getLogger();
		this.cb=cb;
		this.myContext = myContext;
		PersistenceHelper ph = gs.getPreferences();
		PersistenceHelper globalPh = gs.getGlobalPreferences();
		if(picUrlorName!=null) {
			if (!picUrlorName.startsWith("/"))
				picUrlorName="/"+picUrlorName;
				picUrlorName = parseString(picUrlorName);
				Log.d("vortex","ParseString result: "+picUrlorName);
				loadImageMetaData(ph,globalPh);
		} else {
			Log.e("vortex","Pic url null! GisImageView will not load");
			o.addRow("");
			o.addRedText("Adding GisImageView failed. No picture defined!");
		}
	}
	
	public void createAfterLoad(PhotoMeta photoMetaData) {
		Container myContainer = myContext.getContainer(containerId);
		if (myContainer!=null && photoMetaData!=null) {
		LayoutInflater li = LayoutInflater.from(myContext.getContext());
		View mapView = li.inflate(R.layout.image_gis_layout, null);
		WF_Gis_Map gis = new WF_Gis_Map(blockId, mapView, isVisible, picUrlorName,myContext,photoMetaData);
		myContainer.add(gis);
		myContext.addDrawable(id,gis);
		final View menuL = mapView.findViewById(R.id.menuL);
		menuL.setVisibility(View.INVISIBLE);
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

	private void loadImageMetaData(PersistenceHelper ph, PersistenceHelper globalPh) {		
		//Did we get image meta data in xml tags?
		//If not, try to load it from file.
		if (N==null||E==null||S==null||W==null||N.length()==0) 
			loadImageMetaFromFile(ph,globalPh);
		else {
			Log.e("vortex","Found tags for photo meta");
			createAfterLoad(new PhotoMeta(N,E,S,W));
		}
	}
	
	private void loadImageMetaFromFile(PersistenceHelper ph, final PersistenceHelper globalPh) {
		
		String tmp[] = picUrlorName.split("/");
		if (tmp.length>0) {
			//figure out name of metadatafile. Assume to be same as picture name.
			String[]tmp2 = tmp[tmp.length-1].split("\\.");

		if (tmp2!=null && tmp2.length!=0) {
			final String metaFileName = tmp2[0];
			final String metaFolder = picUrlorName.substring(0, picUrlorName.length()-tmp[tmp.length-1].length());
			Log.d("vortex","metafilename: "+metaFileName+" metaFolder"+metaFolder);
			final ConfigurationModule meta = new AirPhotoMetaData(globalPh,ph,Source.file,
				Constants.VORTEX_ROOT_DIR+globalPh.get(PersistenceHelper.BUNDLE_NAME)+metaFolder,metaFileName,""); 
		new FileLoader(null, null, new FileLoadedCb(){
			@Override
			public void onFileLoaded(LoadResult res) {

				if (res.errCode==ErrorCode.frozen) {
					PhotoMeta pm = (PhotoMeta)meta.getEssence();
					Log.d("vortex","img N, W, S, E "+pm.N+","+pm.W+","+pm.S+","+pm.E);
					createAfterLoad(pm);
				}
				else {
					o.addRow("");
					o.addRedText("Could not find GIS image "+metaFileName);
					Log.e("vortex","Failed to parse image location. Errorcode "+res.errCode.name());
					cb.abortExecution("Could not find GIS Image file ["+metaFileName+"], length "+metaFileName.length()+" PATH: "+Constants.VORTEX_ROOT_DIR+globalPh.get(PersistenceHelper.BUNDLE_NAME)+metaFolder);
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


