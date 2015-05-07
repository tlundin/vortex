package com.teraim.vortex.dynamic.blocks;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.types.CHash;
import com.teraim.vortex.dynamic.types.GisLayer;
import com.teraim.vortex.dynamic.types.LatLong;
import com.teraim.vortex.dynamic.types.Location;
import com.teraim.vortex.dynamic.types.SweLocation;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;
import com.teraim.vortex.dynamic.workflow_realizations.gis.DynamicGisPoint;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisConstants;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisMultiPointObject;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisMultiPointObject.Type;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisObject;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisPointObject;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisPolygonObject;
import com.teraim.vortex.dynamic.workflow_realizations.gis.StaticGisPoint;
import com.teraim.vortex.dynamic.workflow_realizations.gis.WF_Gis_Map;
import com.teraim.vortex.utils.DbHelper.DBColumnPicker;
import com.teraim.vortex.utils.DbHelper.Selection;
import com.teraim.vortex.utils.DbHelper.StoredVariableData;
import com.teraim.vortex.utils.Tools;

public class AddGisPointObjects extends Block {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7979886099817953004L;
	private String id, nName, label,
	target, coordType,  locationVariables,
	objContext,  imgSource,
	refreshRate;
	private Bitmap bitmap=null;
	private boolean isVisible;
	private GisObjectType myType;
	public boolean loadDone=false;
	public enum GisObjectType {
		point,
		multipoint,
		polygon, linestring
	}
	private Set<GisObject> myGisObjects;
	private String radius;


	public AddGisPointObjects(String id, String nName, String label,
			String target, String objContext,String coordType, String locationVars, 
			String imgSource, String refreshRate, String radius, boolean isVisible, GisObjectType type) {
		super();
		this.id = id;
		this.nName = nName;
		this.label = label;
		this.target = target;
		this.coordType = coordType;
		this.locationVariables = locationVars;
		this.objContext = objContext;
		this.imgSource = imgSource;
		this.isVisible = isVisible;
		this.refreshRate=refreshRate;
		this.radius=radius;
		myType = type;
		if (this.coordType==null)
			this.coordType=GisConstants.SWEREF;
		Log.d("vortex","IMGURL: "+imgSource);


	}

	//Assumed that all blocks will deal with "current gis".

	public void create(WF_Context myContext) {
		o = GlobalState.getInstance().getLogger();

		WF_Gis_Map gisB = myContext.getCurrentGis();
		if (gisB==null) {
			Log.e("vortex","gisB null!!");
			o.addRow("");
			o.addRedText("Cannot add objects to GIS Layer...GIS has not been initialized. Likely missing or erroneus block_add_gis_image_view");
			return;
		}

		if (imgSource!=null&&imgSource.length()>0 ) {
			String protocol="http://";
			if (!imgSource.toLowerCase().startsWith(protocol))
				imgSource = protocol+imgSource;
			Log.d("vortex","IMGURL: "+imgSource);
			new DownloadImageTask(gisB)
			.execute(imgSource);
		}		




		//Need the key hash for the database.
		CHash myKeyChain = 
				GlobalState.getInstance().evaluateContext(this.objContext);
		if (myKeyChain==null) {
			Log.e("vortex","keychain  null!!");
			o.addRow("");
			o.addYellowText("Missing object context in AddGisPointObjects. Potential error if variables have keychain");
		}
		if (locationVariables==null || locationVariables.isEmpty()) {
			Log.e("vortex","Missing GPS Location variable(s)!");
			o.addRow("");
			o.addRedText("Missing GPS Location variable(s). Cannot continue..aborting");
			return;
		}
		String kc="null";
		if (myKeyChain.keyHash!= null)
			kc = myKeyChain.keyHash.toString();
		Log.d("vortex","In onCreate for AddGisP for type: "+myType+"with keychain: "+kc);
		//Call to the database to get the objects.

		//Either one or two location variables. 
		//If One, separate X,Y by comma
		//If two, One for X & one for Y.
		Log.d("vortex","Location variables: "+locationVariables);
		//Try split.
		String[] locationVarArray = locationVariables.split(",");
		if (locationVarArray.length>2){
			Log.e("vortex","Too many GPS Location variables! Found "+locationVarArray.length+" variables.");
			o.addRow("");
			o.addRedText("Too many GPS Location variables! Found "+locationVarArray.length+" variables. You can only have either one with format GPS_X,GPS_Y or one for X and one for Y!");
			return;
		}
		boolean twoVars = (locationVarArray.length==2);
		
		if(twoVars && myType.equals(GisObjectType.multipoint)) {
			Log.e("vortex","Multivar on multipoint!");
			o.addRow("");
			o.addRedText("Multipoint can only have one Location variable with comma separated values, eg. GPSX_1,GPSY_1,...GPSX_n,GPSY_n");
			return;
		}
		Selection coordVar1S,coordVar2S=null;
		DBColumnPicker pickerLocation1,pickerLocation2=null;  
		coordVar1S = GlobalState.getInstance().getDb().createSelection(myKeyChain.keyHash, locationVarArray[0]);
		Log.d("vortex","selection: "+coordVar1S.selection);
		Log.d("vortex","sel args: "+print(coordVar1S.selectionArgs));
		pickerLocation1 = GlobalState.getInstance().getDb().getAllVariableInstances(coordVar1S);
		if (twoVars) {
			coordVar2S = GlobalState.getInstance().getDb().createSelection(myKeyChain.keyHash, locationVarArray[1]);
			Log.d("vortex","selection: "+coordVar2S.selection);
			Log.d("vortex","sel args: "+print(coordVar2S.selectionArgs));
			pickerLocation2 = GlobalState.getInstance().getDb().getAllVariableInstances(coordVar2S);
		}
		//Static..we can generate a static GIS Point Object.
		Log.d("vortex","refreshrate is "+refreshRate);
		boolean dynamic = false;
		if (refreshRate!=null&&refreshRate.equalsIgnoreCase("dynamic")) {
			Log.d("vortex","Setting type to dynamic for "+nName);
			dynamic = true;
		}
		Map<String, String> map1,map2;
		StoredVariableData storedVar1,storedVar2;



		if (pickerLocation1 !=null && pickerLocation1.moveToFirst()) {

			if (pickerLocation2!=null && !pickerLocation2.moveToFirst()) {
				Log.e("vortex","Missing Y Variables!");
				o.addRow("");
				o.addRedText("Cannot find any instances of "+locationVarArray[1]);
				return;
			}

			myGisObjects = new HashSet<GisObject> ();
			int i=0;

			do {
				i++;
				
				storedVar1 = pickerLocation1.getVariable();
				Log.d("vortex","Found "+storedVar1.value+" for "+storedVar1.name);
				map1 = pickerLocation1.getKeyColumnValues();
				Log.d("vortex","Found columns "+map1.toString()+" for "+storedVar1.name);
				Log.d("vortex","bitmap null? "+(bitmap==null));
				Variable v1=null,v2=null;
				if (dynamic) 
					v1 = GlobalState.getInstance().getVariableConfiguration().getVariableUsingKey(pickerLocation1.getKeyColumnValues(),storedVar1.name);
				if (twoVars) {
					storedVar2 = pickerLocation2.getVariable();
					Log.d("vortex","Found "+storedVar2.value+" for "+storedVar2.name);
					map2 = pickerLocation1.getKeyColumnValues();
					Log.d("vortex","Found columns "+map2.toString()+" for "+storedVar2.name);
					if (!Tools.sameKeys(map1, map2)) {
						Log.e("vortex","key mismatch in db fetch: X key:"+map1.toString()+"\nY key: "+map2.toString());
					} else {
						if (!dynamic)
							myGisObjects.add(new StaticGisPoint(storedVar2.name, new SweLocation(storedVar1.value,storedVar2.value)));
						else {
							v2 = GlobalState.getInstance().getVariableConfiguration().getVariableUsingKey(pickerLocation1.getKeyColumnValues(),storedVar2.name);
							if (v1!=null && v2!=null) 
								myGisObjects.add(new DynamicGisPoint(v1.getId()+"_"+v2.getId(), v1,v2));
						}
					}
					if (!pickerLocation2.next())
						break;
				} else {
					if (myType.equals(GisObjectType.point)) {
						if (!dynamic)
							myGisObjects.add(new StaticGisPoint(storedVar1.name, new SweLocation(storedVar1.value)));
						else
							myGisObjects.add(new DynamicGisPoint(v1.getId(),v1));
					}
					else if (myType.equals(GisObjectType.multipoint))
						myGisObjects.add(new GisMultiPointObject(map1,GisObject.createListOfLocations(storedVar1.value,coordType),Type.MULTIPOINT));
					else if (myType.equals(GisObjectType.linestring)) {
						myGisObjects.add(new GisMultiPointObject(map1,GisObject.createListOfLocations(storedVar1.value,coordType),Type.LINESTRING));
					}
					else if (myType.equals(GisObjectType.polygon))
						myGisObjects.add(new GisPolygonObject(map1,storedVar1.value,coordType));
				}
				//Add these variables to bag 
				
			} while (pickerLocation1.next());
			Log.d("vortex","Added "+myGisObjects.size()+" objects");
			if (target!=null&&target.length()>0) {
				//Add bag to layer.
				GisLayer myLayer=myContext.getCurrentGis().getGis().getLayer(target);
				//Apply radius to all point objects, if any.
				if (myLayer!=null) {
					if (radius!=null) {
						for (GisObject go:myGisObjects) {
							if (go instanceof GisPointObject) 
								((GisPointObject)go).setRadius(radius);
						}
					}

					myLayer.addObjectBag(nName,myGisObjects,dynamic);
					loadDone=true;
					return;
				} else
					Log.e("vortex","Layer was null! "+target);
			}
		} else
			Log.e("vortex","picker was null");


	}



	private String print(String[] selectionArgs) {
		String res="";
		for (String s:selectionArgs)
			res+=s+",";
		res = res.substring(0, res.length()-1);
		return res;
	}


	private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

		WF_Gis_Map gisB;
		public DownloadImageTask(WF_Gis_Map gisB) {
			this.gisB=gisB;
		}

		protected Bitmap doInBackground(String... urls) {
			String urldisplay = urls[0];
			Bitmap mIcon11 = null;
			try {
				Log.d("vortex","Trying to load bitmap");
				InputStream in = new java.net.URL(urldisplay).openStream();
				mIcon11 = BitmapFactory.decodeStream(in);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return mIcon11;
		}

		protected void onPostExecute(Bitmap result) {
			if(!loadDone) 
				Log.e("vortex","not done loading objects!!");
			else {
				Log.e("vortex","seting bitmaps!!");
				for(GisObject g:myGisObjects) {
					if (g instanceof GisPointObject)
						((GisPointObject)g).setIcon(result);
				}
				if(gisB!=null)
					gisB.getGis().invalidate();
			}
		}
	}


}
