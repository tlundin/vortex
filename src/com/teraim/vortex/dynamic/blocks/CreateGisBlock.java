package com.teraim.vortex.dynamic.blocks;

import java.util.List;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.android.gms.drive.internal.l;
import com.teraim.vortex.FileLoadedCb;
import com.teraim.vortex.GlobalState;
import com.teraim.vortex.R;
import com.teraim.vortex.dynamic.AsyncResumeExecutorI;
import com.teraim.vortex.dynamic.types.GisLayer;
import com.teraim.vortex.dynamic.types.Location;
import com.teraim.vortex.dynamic.types.PhotoMeta;
import com.teraim.vortex.dynamic.types.Workflow.Unit;
import com.teraim.vortex.dynamic.workflow_abstracts.Container;
import com.teraim.vortex.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Container;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;
import com.teraim.vortex.dynamic.workflow_realizations.gis.WF_Gis_Map;
import com.teraim.vortex.loadermodule.ConfigurationModule;
import com.teraim.vortex.loadermodule.ConfigurationModule.Source;
import com.teraim.vortex.loadermodule.LoadResult;
import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;
import com.teraim.vortex.loadermodule.WebLoader;
import com.teraim.vortex.loadermodule.configurations.AirPhotoMetaData;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.PersistenceHelper;
import com.teraim.vortex.utils.Tools;
import com.teraim.vortex.utils.Tools.WebLoaderCb;

public class CreateGisBlock extends Block {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2013870148670474254L;
	private final String name,containerId,source,N,E,S,W;
	Unit unit;
	GlobalState gs;
	boolean isVisible = false,showHistorical;
	String format;

	private Cutout cutOut=null;
	private boolean menuUp = false;
	private WF_Context myContext;
	private LoggerI o;
	private boolean hasSatNav;
	private WF_Gis_Map gis=null;

	public boolean hasCarNavigation() {
		return hasSatNav;
	}

	public CreateGisBlock(String id,String name, 
			String containerId,boolean isVisible,String source,String N,String E, String S,String W, boolean hasSatNav) {
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
		this.hasSatNav=hasSatNav;

	}




	//Callback after image has loaded.
	AsyncResumeExecutorI cb;



	/**
	 * 
	 * @param myContext
	 * @param cb
	 * @return true if loaded. False if executor should pause.
	 */

	public boolean create(WF_Context myContext,final AsyncResumeExecutorI cb) {
		
        
        Context ctx = myContext.getContext();
		gs = GlobalState.getInstance();
		o = gs.getLogger();
		this.cb=cb;
		this.myContext = myContext;
		PersistenceHelper ph = gs.getPreferences();
		PersistenceHelper globalPh = gs.getGlobalPreferences();

		final String serverFileRootDir = server(globalPh.get(PersistenceHelper.SERVER_URL))+globalPh.get(PersistenceHelper.BUNDLE_NAME).toLowerCase()+"/extras/";
		final String cacheFolder = Constants.VORTEX_ROOT_DIR+globalPh.get(PersistenceHelper.BUNDLE_NAME)+"/cache/";

		//Create pop dialog to display status.
		//PopupWindow pop = new PopupWindow(ctx);
		//LayoutInflater li = ((LayoutInflater)ctx.getSystemService(ctx.LAYOUT_INFLATER_SERVICE));
		//View popInner = li.inflate(R.layout.status_pop, null);
		//final TextView statusT = (TextView)popInner.findViewById(R.id.statusT);
		//pop.setContentView(popInner);
		//pop.showAtLocation(((WF_Container)myContext.getContainer("root")).getViewGroup(), Gravity.CENTER, 0, 0);
		
		
		
		if (source==null || source.length()==0) {
			Log.e("vortex","Pic url null! GisImageView will not load");
			o.addRow("");
			o.addRedText("GisImageView failed to load. No picture defined");
			//continue execution immediately.
			return true;
		}

		final String picName = Tools.parseString(source);
		Log.d("vortex","ParseString result: "+picName);
		//Load asynchronously. Put up a loadbar.


		Tools.onLoadCacheImage(serverFileRootDir, picName, cacheFolder, new WebLoaderCb() {

			@Override
			public void progress(int bytesRead) {
				Log.d("vortex","progress: "+bytesRead);
			}

			@Override
			public void loaded(Boolean result) {
				if (result)
					loadImageMetaData(picName,serverFileRootDir,cacheFolder);
				else {
					Log.e("vortex","Pic url null! GisImageView will not load");
					o.addRow("");
					o.addRedText("GisImageView failed to load. File not found: "+serverFileRootDir+picName);
					cb.abortExecution("GisImageView failed to load. File not found: "+serverFileRootDir+picName);

				}
			}
		});

		return false;
	}

	public void createAfterLoad(PhotoMeta photoMetaData, String cachedImgFilePath) {

		Container myContainer = myContext.getContainer(containerId);
		if (myContainer!=null && photoMetaData!=null) {
			LayoutInflater li = LayoutInflater.from(myContext.getContext());
			FrameLayout mapView = (FrameLayout)li.inflate(R.layout.image_gis_layout, null);
			final View avstRL = mapView.findViewById(R.id.avstRL);
			final View createMenuL = mapView.findViewById(R.id.createMenuL);

			Rect r=null;

			if (cutOut==null) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(cachedImgFilePath, options);
				int imageHeight = options.outHeight;
				int imageWidth = options.outWidth;
				Log.d("vortex","image rect h w is "+imageHeight+","+imageWidth);

				r = new Rect(0,0,imageWidth,imageHeight);

			} else {
				//This is a cutout. pop the correct slice specification from the stack. 
				r = cutOut.r;
				Location topC = cutOut.geoR.get(0);
				Location botC = cutOut.geoR.get(1);
				photoMetaData = new PhotoMeta(topC.getY(),botC.getX(),botC.getY(),topC.getX());
				cutOut=null;
			}
			gis = new WF_Gis_Map(this,r,blockId, mapView, isVisible, cachedImgFilePath,myContext,photoMetaData,avstRL,createMenuL,myLayers);
			//need to throw away the reference to myLayers.
			myLayers=null;
			myContainer.add(gis);
			myContext.addGis(gis.getId(),gis);
			myContext.addEventListener(gis, EventType.onSave);
			myContext.addDrawable(name,gis);
			final View menuL = mapView.findViewById(R.id.menuL);

			menuL.setVisibility(View.INVISIBLE);
			avstRL.setVisibility(View.INVISIBLE);
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


	private void loadImageMetaData(String picName,String serverFileRootDir,String cacheFolder) {		

		if (N==null||E==null||S==null||W==null||N.length()==0) {
			//Parse and cache the metafile.
			loadImageMetaFromFile(picName, serverFileRootDir,cacheFolder);
		}
		else {
			Log.e("vortex","Found tags for photo meta");
			createAfterLoad(new PhotoMeta(N,E,S,W),cacheFolder+picName);
		}
	}

	//User parser to parse and cache xml.

	private void loadImageMetaFromFile(final String fileName,String serverFileRootDir,final String cacheFolder) {

		//cut the .jpg type ending.
		String[]tmp = fileName.split("\\.");

		if (tmp!=null && tmp.length!=0) {
			final String metaFileName = tmp[0];			
			Log.d("vortex","metafilename: "+metaFileName);
			final ConfigurationModule meta = new AirPhotoMetaData(GlobalState.getInstance().getGlobalPreferences(),
					GlobalState.getInstance().getPreferences(),Source.internet,
					serverFileRootDir,metaFileName,""); 
			if (meta.thaw().errCode!=ErrorCode.thawed) {
				Log.d("vortex","no frozen metadata. will try to download.");
				new WebLoader(null, null, new FileLoadedCb(){
					@Override
					public void onFileLoaded(LoadResult res) {

						if (res.errCode==ErrorCode.frozen) {
							PhotoMeta pm = (PhotoMeta)meta.getEssence();
							Log.d("vortex","img N, W, S, E "+pm.N+","+pm.W+","+pm.S+","+pm.E);
							createAfterLoad(pm,cacheFolder+fileName);
						}
						else {
							o.addRow("");
							o.addRedText("Could not find GIS image "+metaFileName);
							Log.e("vortex","Failed to parse image meta. Errorcode "+res.errCode.name());
							cb.abortExecution("Could not load GIS image meta file ["+metaFileName+"]. Probably the file is missing under the 'extras' folder");
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
			} else {
				Log.d("vortex","Found frozen metadata. Will use it");
				PhotoMeta pm = (PhotoMeta)meta.getEssence();
				Log.d("vortex","img N, W, S, E "+pm.N+","+pm.W+","+pm.S+","+pm.E);
				createAfterLoad(pm,cacheFolder+fileName);
			}
		}
	}


	private class Cutout {
		Rect r;
		List<Location> geoR;
	}


	//Reloads current flow with a new viewport.
	//Cache for layers.
	List<GisLayer> myLayers=null;

	public void setCutOut(Rect r, List<Location> geoR, List<GisLayer> myLayers) {
		cutOut = new Cutout();
		cutOut.r = r;
		cutOut.geoR = geoR;
		this.myLayers = myLayers;
	}

	public String server(String serverUrl) {
		if (!serverUrl.endsWith("/"))
			serverUrl+="/";
		if (!serverUrl.startsWith("http://")) 
			serverUrl = "http://"+serverUrl;
		return serverUrl;
	}


}


