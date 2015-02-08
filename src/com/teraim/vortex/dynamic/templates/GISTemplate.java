package com.teraim.vortex.dynamic.templates;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.teraim.vortex.FileLoadedCb;
import com.teraim.vortex.GlobalState;
import com.teraim.vortex.R;
import com.teraim.vortex.Start;
import com.teraim.vortex.dynamic.Executor;
import com.teraim.vortex.dynamic.types.PhotoMeta;
import com.teraim.vortex.dynamic.types.SweLocation;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Container;
import com.teraim.vortex.gis.GisImageView;
import com.teraim.vortex.loadermodule.ConfigurationModule;
import com.teraim.vortex.loadermodule.ConfigurationModule.Source;
import com.teraim.vortex.loadermodule.FileLoader;
import com.teraim.vortex.loadermodule.LoadResult;
import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;
import com.teraim.vortex.loadermodule.configurations.AirPhotoMetaData;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.Geomatte;
import com.teraim.vortex.utils.PersistenceHelper;
import com.teraim.vortex.utils.Tools;
/**
 * 
 * @author Terje
 * Template used to generate GIS user interface.
 */
public class GISTemplate extends Executor implements LocationListener {

	private LinearLayout my_root;
	private GisImageView gi;
	private ActionMode mActionMode;
	private boolean drawActive=false;
	private LocationManager lm;
	private Context ctx;
	private String gisDir;
	private PersistenceHelper globalPh,ph;
	private String myRuta;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		ctx = this.getActivity();
		View v = inflater.inflate(R.layout.template_gis, container, false);	
		my_root = (LinearLayout)v.findViewById(R.id.myRoot);
		gi = (GisImageView)my_root.findViewById(R.id.gis_image);
		globalPh = GlobalState.getInstance(ctx).getGlobalPreferences();
		ph = GlobalState.getInstance(ctx).getPreferences();
		gisDir = Constants.VORTEX_ROOT_DIR+globalPh.get(PersistenceHelper.BUNDLE_NAME)+"/flygdata/";
		myRuta = "207";//GlobalState.getInstance(ctx).getVariableConfiguration().getCurrentRuta();
		myContext.addContainers(getContainers());
		//gi.setI

		if (wf!=null) {
			Log.d("vortex","Executing workflow!!");
			run();
		} else
			Log.d("vortex","No workflow found in oncreate default!!!!");

		String picName = "207.jpg";
		loadImageMetaData(picName);
		Bitmap bmp = Tools.getScaledImage(ctx,gisDir+picName);
		gi.setImageBitmap(bmp);
		if (bmp==null) {
			new AlertDialog.Builder(this.getActivity()).setTitle("Missing picture")
			.setMessage("Cannot start. Did not find background GIS image in GIS folder ")
			.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					getFragmentManager().popBackStackImmediate();
				}})
				.setCancelable(false)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.show();	

		} else 	{		
			Log.d("nils","bmp w h"+bmp.getWidth()+" "+bmp.getHeight());
			gi.setOnLongClickListener(new View.OnLongClickListener() {

				// Called when the user long-clicks on someView
				public boolean onLongClick(View view) {
					String target= gi.checkForTargets();
					if (target!=null) {
						executeWf(target);
						return true;
					}
					else {	
						if (drawActive)
							gi.addVertex();
						else {
							drawActive = true;
							gi.startPoly();
						}

						if (mActionMode != null) {
							return false;
						}

						// Start the CAB using the ActionMode.Callback defined above
						mActionMode = getActivity().startActionMode(mActionModeCallback);
						view.setSelected(true);
						return true;
					}
				}


			});

			gi.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String target= gi.checkForTargets();
					if (target!=null) {
						executeWf(target);
					}				
				}
			});

			lm = (LocationManager)this.getActivity().getSystemService(Context.LOCATION_SERVICE);

		}
		return v;

	}


	private void executeWf(String target) {
		//Execute a new workflow named target?
		Start.singleton.changePage(gs.getWorkflow("wf_simplePage"), null);
	}

	@Override
	protected List<WF_Container> getContainers() {
		ArrayList<WF_Container> ret = new ArrayList<WF_Container>();
		ret.add(new WF_Container("root",my_root,null));
		return ret;
	}

	@Override
	public void execute(String function, String target) {

	}

	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {


		// Called when the action mode is created; startActionMode() was called
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Inflate a menu resource providing context menu items
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.gismenu, menu);
			return true;
		}

		// Called each time the action mode is shown. Always called after onCreateActionMode, but
		// may be called multiple times if the mode is invalidated.
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
		/*
			MenuItem drawToogle = menu.getItem(0);
			MenuItem erase = menu.getItem(1);

			if (drawActive) {
				drawToogle.setTitle("Save");
				erase.setVisible(true);
			}
			else {
				drawToogle.setTitle("Start drawing");
				erase.setVisible(false);
			}
			return true;
		}
		 */
		// Called when the user selects a contextual menu item
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case R.id.menu_save:
				drawActive = false;
				gi.savePoly();
				mode.finish(); // Action picked, so close the CAB
				return true;
			case R.id.menu_erase:
				gi.erasePoly();         		
				drawActive = false;
				mode.finish();
				return true;

			default:
				Log.d("vortex","default called");
				return false;
			}
		}

		// Called when the user exits the action mode
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
		}
	};
	private SweLocation myL;
	private double centerNorth;
	private double centerEast;


	@Override
	public void onPause() {
		if (lm!=null) {
		lm.removeUpdates(this);
		if (mActionMode != null) {
			mActionMode.finish();
		}
		}
		super.onPause();
	}




	@Override
	public void onStart() {
		if (lm!=null) {
			if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
				startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), 0);
		}
		super.onStart();
	}

	@Override
	public void onResume() {
		if (lm!=null) {
			lm.requestLocationUpdates(
		
				LocationManager.GPS_PROVIDER,
				0,
				1,
				this);


		}
		super.onResume();
	}

	@Override
	public void onLocationChanged(Location location) {
		myL = Geomatte.convertToSweRef(location.getLatitude(), location.getLongitude());

		//double distance = Geomatte.sweDist(myL.east, myL.north, center.east, center.north);
		//gpsView.setText(((int)distance)+"");

		double distCenterN = myL.north - centerNorth; //negative = on top of.
		double distCenterE = myL.east - centerEast;

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}


	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}


	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

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
					gi.setGisData(pm, myRuta);
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




}
