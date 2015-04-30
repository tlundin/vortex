package com.teraim.vortex.loadermodule.configurations;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.blocks.Block;
import com.teraim.vortex.dynamic.types.CHash;
import com.teraim.vortex.dynamic.types.GisLayer;
import com.teraim.vortex.dynamic.types.SweLocation;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisObject;
import com.teraim.vortex.dynamic.workflow_realizations.gis.WF_Gis_Map;
import com.teraim.vortex.utils.DbHelper.DBColumnPicker;
import com.teraim.vortex.utils.DbHelper.Selection;
import com.teraim.vortex.utils.DbHelper.StoredVariableData;
import com.teraim.vortex.utils.Tools;

public class AddGisPointObjects extends Block {

	private String id, nName, label,
	target, coordType,  xVar,
	yVar, objContext,  imgSource,
	type;
	private boolean isVisible;

	public AddGisPointObjects(String id, String nName, String label,
			String target, String objContext,String coordType, String xVar, String yVar,
			String imgSource, String type, boolean isVisible) {
		super();
		this.id = id;
		this.nName = nName;
		this.label = label;
		this.target = target;
		this.coordType = coordType;
		this.xVar = xVar;
		this.yVar = yVar;
		this.objContext = objContext;
		this.imgSource = imgSource;
		this.isVisible = isVisible;
		this.type=type;
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
		//Need the key hash for the database.
		CHash myKeyChain = 
		GlobalState.getInstance().evaluateContext(this.objContext);
		if (myKeyChain==null) {
			Log.e("vortex","keychain  null!!");
			o.addRow("");
			o.addYellowText("Missing object context in AddGisPointObjects...not sure you want this.");
		}
		//Call to the database to get the objects.
		//Selection s = GlobalState.getInstance().getDb().createCoulmnSelection(myKeyChain.keyHash);
		Selection xVars = GlobalState.getInstance().getDb().createSelection(myKeyChain.keyHash, xVar);
		Log.d("vortex","selection: "+xVars.selection);
		Log.d("vortex","sel args: "+print(xVars.selectionArgs));
		Selection yVars = GlobalState.getInstance().getDb().createSelection(myKeyChain.keyHash, yVar);
		Log.d("vortex","selection: "+yVars.selection);
		Log.d("vortex","sel args: "+print(yVars.selectionArgs));
		//Static..we can generate a static GIS Point Object.
		Log.d("vortex","type is "+type);
		if (type!=null&&!type.equals("dynamic")) {
		
			Map<String, String> mapX,mapY;
			StoredVariableData varX,varY;
		
		DBColumnPicker pickerX = GlobalState.getInstance().getDb().getAllVariableInstances(xVars);
		DBColumnPicker pickerY = GlobalState.getInstance().getDb().getAllVariableInstances(yVars);
		if (pickerX !=null && pickerX.moveToFirst()&&pickerY!=null && pickerY.moveToFirst()) {
			
			Set<GisObject> myGisObjects = new HashSet<GisObject> ();
			int i=0;String testLabel;
			
			do {
				i++;
				testLabel=Integer.toString(i);
				varX = pickerX.getVariable();
				Log.d("vortex","Found "+varX.value+" for "+varX.name);
				mapX = pickerX.getKeyColumnValues();
				Log.d("vortex","Found columns "+mapX.toString()+" for "+varX.name);
				varY = pickerY.getVariable();
				Log.d("vortex","Found "+varY.value+" for "+varY.name);
				mapY = pickerY.getKeyColumnValues();
				Log.d("vortex","Found columns "+mapY.toString()+" for "+varY.name);
				if (!Tools.sameKeys(mapX, mapY)) {
					Log.e("vortex","key mismatch in db fetch: X key:"+mapX.toString()+"\nY key: "+mapY.toString());
				} else {
					//Add these variables to bag 
					myGisObjects.add(new GisObject(testLabel, new SweLocation(varY.value,varX.value)));
				}
					
			} while (pickerX.next()&&pickerY.next());
			
			if (target!=null&&target.length()>0) {
				//Add bag to layer.
				GisLayer myLayer=myContext.getCurrentGis().getGis().getLayer(target);
				if (myLayer!=null) {
					myLayer.addObjectBag(nName,myGisObjects);
					return;
				} 
			}
		} else
			Log.d("vortex","picker was null");
		}
		
	}

	private String print(String[] selectionArgs) {
		String res="";
		for (String s:selectionArgs)
			res+=s+",";
		res = res.substring(0, res.length()-1);
		return res;
	}




}
