package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.util.Map;

import android.graphics.Bitmap;
import android.util.Log;

import com.teraim.vortex.dynamic.types.Location;
import com.teraim.vortex.dynamic.types.SweLocation;
import com.teraim.vortex.dynamic.types.Variable;


public class GisPointObject extends GisObject {
	Bitmap icon;
	boolean dynamic = false;
	boolean multivar = false;
	Variable myXVar,myYVar,myXYVar;

	public GisPointObject(String myLabel, Location myLocation) {
		super();
		this.myLabel = myLabel;
		myCoordinates.add(myLocation);
		dynamic = false;
	}

	public GisPointObject(Map<String, String> keyChain, Location myLocation) {
		this.keyChain = keyChain;
		myCoordinates.add(myLocation);
		dynamic = false;
	}
	public GisPointObject(String testLabel, Variable x, Variable y) {
		super();
		this.myLabel = testLabel;
		dynamic = true;
		multivar=true;
		myXVar=x;
		myYVar=y;
	}
	public GisPointObject(String testLabel, Variable v1) {
		super();
		this.myLabel = testLabel;
		dynamic = true;
		multivar=false;
		myXYVar=v1;
	}
	public Location getLocation() {
		if (!dynamic) {
			if (myCoordinates.isEmpty())
				return null;
			return myCoordinates.get(0);
		}
		if (multivar) {
			if (myXVar==null || myYVar == null) {
				Log.e("vortex","(At least) one variable missing in GisPObject: "+myXVar+" "+myYVar);
				return null;
			}
			String x = myXVar.getValue();
			String y = myYVar.getValue();
			if (x== null || y == null) {
				Log.d("vortex","(At least) one variable have no value in GisPObject: "+myXVar+" "+myYVar);
				return null;
			} else
				return new SweLocation(x,y);
		} else {
			if (myXYVar == null) {
				Log.e("vortex","Variable missing in GisPObject getLocation(!multivar). Other vars are: X,Y: "+myXVar+","+myYVar);
				return null;
			}
			String xy = myXYVar.getValue();
			if (xy==null) {
				Log.d("vortex","Variable has no value in GisPObject: "+myXYVar);
				return null;
			}
			String[] xys = xy.split(",");
			if (xys.length<2) {
				Log.e("vortex","Strange value for GPS coord variable: "+xy);
				return null;
			} else
				return new SweLocation(xys[0],xys[1]);
				
		}
		
	}

	@Override
	public String toString() {
		String res="";
		res+=" \nDynamic: "+(dynamic?"yes":"no");
		res+="\nMultivar: "+(multivar?"yes":"no");
		res+="\nLabel: "+myLabel;
		if (dynamic) {
			
			res+="\nVariable values: xy, x, y";
			if (myXYVar==null)
				res+="null, ";
				else 
					res+=myXYVar.getValue()+", ";
			if (myXVar==null)
				res+="null, ";
			else
				res+=myXVar.getValue()+", ";
			if (myYVar ==null)
				res+="null, ";
			else
				res+=myYVar.getValue();
		}
		return res;
	}

	public Bitmap getIcon() {
		return icon;
	}

	public void setIcon(Bitmap mb) {
		this.icon=mb;
	}



}









