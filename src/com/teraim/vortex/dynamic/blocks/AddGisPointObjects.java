package com.teraim.vortex.dynamic.blocks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.AsyncTask;
import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.R;
import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.dynamic.types.CHash;
import com.teraim.vortex.dynamic.types.GisLayer;
import com.teraim.vortex.dynamic.types.SweLocation;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.types.Variable.DataType;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;
import com.teraim.vortex.dynamic.workflow_realizations.gis.DynamicGisPoint;
import com.teraim.vortex.dynamic.workflow_realizations.gis.FullGisObjectConfiguration;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisConstants;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisMultiPointObject;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisObject;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisPolygonObject;
import com.teraim.vortex.dynamic.workflow_realizations.gis.StaticGisPoint;
import com.teraim.vortex.dynamic.workflow_realizations.gis.WF_Gis_Map;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.DbHelper.DBColumnPicker;
import com.teraim.vortex.utils.DbHelper.Selection;
import com.teraim.vortex.utils.DbHelper.StoredVariableData;
import com.teraim.vortex.utils.Expressor;
import com.teraim.vortex.utils.PersistenceHelper;
import com.teraim.vortex.utils.Tools;

public class AddGisPointObjects extends Block implements FullGisObjectConfiguration {


	private static final long serialVersionUID = 7979886099817953005L;
	private String nName, label,
	target, coordType,locationVariables,imgSource,refreshRate;
	private List<Expressor.EvalExpr> objContext;
	private Bitmap icon=null;
	private boolean isVisible;
	private GisObjectType myType;
	public boolean loadDone=false;
	private Set<GisObject> myGisObjects;
	private float radius;
	private String color;
	private Paint.Style fillType;
	private PolyType polyType;
	private String onClick;
	private String statusVariable;
	private CHash objKeyHash;
	private boolean isUser;
	private boolean createAllowed;
	private GisLayer myLayer;
	private boolean dynamic;


	public AddGisPointObjects(String id, String nName, String label,
			String target, String objContext,String coordType, String locationVars, 
			String imgSource, String refreshRate, String radius, boolean isVisible, 
			GisObjectType type, String color, String polyType, String fillType, 
			String onClick, String statusVariable, boolean isUser, boolean createAllowed, LoggerI o) {
		super();
		this.blockId = id;
		this.nName = nName;
		this.label = label;
		this.target = target;
		this.coordType = coordType;
		this.locationVariables = locationVars;
		this.objContext = Expressor.preCompileExpression(objContext);
		this.imgSource = imgSource;
		this.isVisible = isVisible;
		this.refreshRate=refreshRate;
		this.onClick = onClick;
		this.color=color;
		this.statusVariable=statusVariable;
		this.isUser=isUser;
		this.createAllowed=createAllowed;
		myType = type;

		if (coordType==null)
			this.coordType=GisConstants.SWEREF;

		setRadius(radius);

		this.fillType=Paint.Style.FILL;
		if  (fillType !=null) {
			if (fillType.equalsIgnoreCase("STROKE"))
				this.fillType = Paint.Style.STROKE;
			else if (fillType.equalsIgnoreCase("FILL_AND_STROKE"))
				this.fillType = Paint.Style.FILL_AND_STROKE;
		}
		this.polyType=PolyType.circle;
		this.radius=10;
		if (polyType!=null) {
			try {
			this.polyType=PolyType.valueOf(polyType);
			} catch (IllegalArgumentException e) {
				if (polyType.toUpperCase().equals("SQUARE")||polyType.toUpperCase().equals("RECT")||polyType.toUpperCase().equals("RECTANGLE"))
					this.polyType=PolyType.rect;
				else if (polyType.toUpperCase().equals("TRIANGLE"))
					this.polyType=PolyType.triangle;
				else {
					o.addRow("");
					o.addRedText("Unknown polytype: ["+polyType+"]. Will default to circle");
				}
			}
		}

		if (Tools.isNumeric(radius)) {
			this.radius=Float.parseFloat(radius);

		}

		//Set default icons for different kind of objects.
	}

	//Assumed that all blocks will deal with "current gis".

	public void create(WF_Context myContext) {
		setDefaultBitmaps(myContext);
		o = GlobalState.getInstance().getLogger();
		GlobalState gs = GlobalState.getInstance();
		WF_Gis_Map gisB = myContext.getCurrentGis();
		if (gisB==null) {
			Log.e("vortex","gisB null!!");
			return;
		} else {
			if (createAllowed) {
				Log.d("vortex","Adding type to create menu for "+nName);
				gisB.addGisObjectType(this);
			}
			if (gisB.isZoomLevel()) {
				Log.d("vortex","Zoom level!..use existing gis objects!");
				return;
			}
		}


		if (imgSource!=null&&imgSource.length()>0 ) {
			File cached = gs.getCachedFileFromUrl(imgSource);
			if (cached==null) {
				Log.d("vortex","no cached image...trying live.");
				String fullPicURL = Constants.VORTEX_ROOT_DIR+GlobalState.getInstance().getGlobalPreferences().get(PersistenceHelper.BUNDLE_NAME)+"/extras/"+imgSource;
				Log.d("vortex","IMGURL: "+imgSource);
				new DownloadImageTask()
				.execute(fullPicURL);
			} else {
				try {
					icon = BitmapFactory.decodeStream(new FileInputStream(cached));
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}		




		//Need the key hash for the database.
		objKeyHash = 
				GlobalState.getInstance().evaluateContext(this.getObjectContext());
		Log.d("vortex","OBJ KEYHASH "+objKeyHash.keyHash.toString());
		//Use current year for statusvar.
		Map<String, String> currYearH = Tools.copyKeyHash(objKeyHash.keyHash);
		currYearH.put("�r",Constants.getYear());
		Log.d("vortex","Curryear HASH "+currYearH.toString());

		if (objKeyHash==null) {
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
		if (objKeyHash.keyHash!= null)
			kc = objKeyHash.keyHash.toString();
		Log.d("vortex","In onCreate for AddGisP for ["+nName+"], Obj_type: "+myType+" with keychain: "+kc);
		//Call to the database to get the objects.

		//Either one or two location variables. 
		//If One, separate X,Y by comma
		//If two, One for X & one for Y.
		//Log.d("vortex","Location variables: "+locationVariables);
		//Try split.
		String[] locationVarArray = locationVariables.split(",");
		if (locationVarArray.length>2){
			Log.e("vortex","Too many GPS Location variables! Found "+locationVarArray.length+" variables.");
			o.addRow("");
			o.addRedText("Too many GPS Location variables! Found "+locationVarArray.length+" variables. You can only have either one with format GPS_X,GPS_Y or one for X and one for Y!");
			return;
		}
		String locationVar1=locationVarArray[0].trim();
		String locationVar2=null;

		boolean twoVars = (locationVarArray.length==2);
		if (twoVars)
			locationVar2 = locationVarArray[1].trim();
		Log.d("vortex","Twovars is "+twoVars+" gisvars are: "+(twoVars?" ["+locationVarArray[0]+","+locationVarArray[1]:locationVarArray[0])+"]");
		if(twoVars && myType.equals(GisObjectType.Multipoint)) {
			Log.e("vortex","Multivar on multipoint!");
			o.addRow("");
			o.addRedText("Multipoint can only have one Location variable with comma separated values, eg. GPSX_1,GPSY_1,...GPSX_n,GPSY_n");
			return;
		}
		VariableConfiguration al = GlobalState.getInstance().getVariableConfiguration();



		Selection coordVar1S=null,coordVar2S=null,statusVarS = null;
		DBColumnPicker pickerLocation1,pickerLocation2=null,pickerStatusVars=null;  
		if (this.getStatusVariable()!=null) {
			statusVarS = GlobalState.getInstance().getDb().createSelection(currYearH, this.getStatusVariable().trim());
			pickerStatusVars = GlobalState.getInstance().getDb().getAllVariableInstances(statusVarS);
		}


		coordVar1S = GlobalState.getInstance().getDb().createSelection(objKeyHash.keyHash, locationVar1);
		Log.d("vortex","selection: "+coordVar1S.selection);
		Log.d("vortex","sel args: "+print(coordVar1S.selectionArgs));
		List<String> row = al.getCompleteVariableDefinition(locationVar1);
		if (row==null) {
			Log.e("vortex","Variable not found!");
			o.addRow("");
			o.addRedText("Variable "+locationVar1+" was not found. Check Variables.CSV and Groups.CSV!");
			return;
		}
		DataType t1 = al.getnumType(row);
		if (t1==DataType.array) {
			pickerLocation1 = GlobalState.getInstance().getDb().getLastVariableInstance(coordVar1S);
			Log.e("vortex","called getLast.");
		}
		else
			pickerLocation1 = GlobalState.getInstance().getDb().getAllVariableInstances(coordVar1S);

		if (twoVars) {

			DataType t2 = al.getnumType(al.getCompleteVariableDefinition(locationVar2));
			coordVar2S = GlobalState.getInstance().getDb().createSelection(objKeyHash.keyHash, locationVar2);
			Log.d("vortex","selection: "+coordVar2S.selection);
			Log.d("vortex","sel args: "+print(coordVar2S.selectionArgs));
			if (t2==DataType.array)
				pickerLocation2 = GlobalState.getInstance().getDb().getLastVariableInstance(coordVar2S);
			else
				pickerLocation2 = GlobalState.getInstance().getDb().getAllVariableInstances(coordVar2S);
		}
		//Static..we can generate a static GIS Point Object.
		Log.d("vortex","refreshrate is "+refreshRate);
		dynamic = false;
		if (refreshRate!=null&&refreshRate.equalsIgnoreCase("dynamic")) {
			Log.d("vortex","Setting type to dynamic for "+nName);
			dynamic = true;
		}
		Map<String, String> map1,map2;
		StoredVariableData storedVar1,storedVar2;



		if (pickerLocation1 !=null ) {
			
			myGisObjects = new HashSet<GisObject> ();
			boolean hasValues = pickerLocation1.moveToFirst();
			//No values! A dynamic variable can create new ones, so create object anyway.
			if ((!hasValues&&dynamic) || (hasValues&&dynamic&&twoVars&&!pickerLocation2.moveToFirst())) {
				Log.e("vortex","no X,Y instances found for keychain..creating empty");
				Variable v1 = GlobalState.getInstance().getVariableConfiguration().getVariableUsingKey(objKeyHash.keyHash, locationVar1);
				if (v1==null){
					Log.e("vortex", locationVar1+" does not exist. Check your configuration!");
					o.addRow("");
					o.addRedText("Cannot find variable "+locationVar1);
					return;
				}
				if (twoVars) {
					Variable v2 = GlobalState.getInstance().getVariableConfiguration().getVariableUsingKey(objKeyHash.keyHash, locationVar2);
					if (v2==null){
						Log.e("vortex", locationVar2+" does not exist. Check your configuration!");
						o.addRow("");
						o.addRedText("Cannot find variable "+locationVar2);
						return;
					}
					myGisObjects.add(new DynamicGisPoint(this,objKeyHash.keyHash, v1,v2,null));
				} else
					myGisObjects.add(new DynamicGisPoint(this,objKeyHash.keyHash, v1,null));
			} else {


				if (hasValues&&twoVars&&pickerLocation2!=null&&!pickerLocation2.moveToFirst()) {
					Log.e("vortex","Missing values!!!");
					o.addRow("");
					o.addRedText("Cannot find any instances of secondary loc variable "+locationVar2);
					return;
				}

				if (!hasValues && !dynamic) {
					Log.d("vortex","No values in database for static GisPObject with name "+nName);
					o.addRow("No values in database for static GisPObject with name "+nName);
				} else {
					Map <String,Variable> statusVarM=null;
					boolean foundStatusVar = false;
					if (pickerStatusVars!=null) 
						foundStatusVar=pickerStatusVars.moveToFirst();


					while (foundStatusVar) {
						String value  = pickerStatusVars.getVariable().value;
						Variable statusVar = GlobalState.getInstance().getVariableConfiguration().getCheckedVariable(pickerStatusVars.getKeyColumnValues(),pickerStatusVars.getVariable().name,value,true);
						if (statusVarM == null) 
							statusVarM = new HashMap<String,Variable>();
						statusVarM.put(statusVar.getKeyChain().get("uid"), statusVar);
						Log.d("vortex","added statusvar");
						foundStatusVar = pickerStatusVars.next();
					} 



					do {

						storedVar1 = pickerLocation1.getVariable();
						//Log.d("vortex","Found "+storedVar1.value+" for "+storedVar1.name);
						map1 = pickerLocation1.getKeyColumnValues();
						//Log.d("vortex","Found columns "+map1.toString()+" for "+storedVar1.name);
						//Log.d("vortex","bitmap null? "+(icon==null));
						Variable v1=null,v2=null,statusVar=null;
						//If status variable has a value in database, use it. 
						if (statusVarM!=null) 
							statusVar = statusVarM.get(map1.get("uid"));
						//if there is a statusvariable defined, but no value found, create a new empty variable.
						if (statusVariable !=null && statusVar == null) {
							currYearH.put("uid",map1.get("uid"));
							statusVar = GlobalState.getInstance().getVariableConfiguration().getCheckedVariable(currYearH,statusVariable,"0",true);
						}
						if (dynamic) {
							String value = pickerLocation1.getVariable().value;
							v1 = GlobalState.getInstance().getVariableConfiguration().getCheckedVariable(pickerLocation1.getKeyColumnValues(),storedVar1.name,value,true);
						}
						if (twoVars) {
							storedVar2 = pickerLocation2.getVariable();
							Log.d("vortex","Found "+storedVar2.value+" for "+storedVar2.name);
							map2 = pickerLocation1.getKeyColumnValues();
							Log.d("vortex","Found columns "+map2.toString()+" for "+storedVar2.name);
							if (!Tools.sameKeys(map1, map2)) {
								Log.e("vortex","key mismatch in db fetch: X key:"+map1.toString()+"\nY key: "+map2.toString());
							} else {
								if (!dynamic) {
									myGisObjects.add(new StaticGisPoint(this,map1, new SweLocation(storedVar1.value,storedVar2.value),statusVar));
								}
								else {
									String value = pickerLocation2.getVariable().value;
									v2 = GlobalState.getInstance().getVariableConfiguration().getCheckedVariable(pickerLocation1.getKeyColumnValues(),storedVar2.name,value,true);
									if (v1!=null && v2!=null) 
										myGisObjects.add(new DynamicGisPoint(this,map1, v1,v2,statusVar));
									else {
										Log.e("vortex","cannot create dyna 2 gis obj. One or both vars is null: "+v1+","+v2);
										continue;
									}
								}
							}
							if (!pickerLocation2.next())
								break;
						} else {
							if (myType.equals(GisObjectType.Point)) {
								if (!dynamic)
									myGisObjects.add(new StaticGisPoint(this,map1, new SweLocation(storedVar1.value),statusVar));
								else
									myGisObjects.add(new DynamicGisPoint(this,map1,v1,statusVar));
							}
							else if (myType.equals(GisObjectType.Multipoint)||myType.equals(GisObjectType.Linestring))
								myGisObjects.add(new GisMultiPointObject(this,map1,GisObject.createListOfLocations(storedVar1.value,coordType)));

							else if (myType.equals(GisObjectType.Polygon)) {
								Log.d("vortex","Adding polygon");
								myGisObjects.add(new GisPolygonObject(this,map1,storedVar1.value,coordType,statusVar));
							}
						}
						//Add these variables to bag 

					} while (pickerLocation1.next());
					Log.d("vortex","Added "+myGisObjects.size()+" objects");

				}
			}
		} else
			Log.e("vortex","picker was null");
		//Add type to layer. Add even if empty.
		if (target!=null) {//&&target.length()>0 && myGisObjects!=null && !myGisObjects.isEmpty()) {
			//Add bag to layer.
			myLayer=myContext.getCurrentGis().getLayer(target);
			if (myLayer!=null) {
				myLayer.addObjectBag(nName,myGisObjects,dynamic);

			} else {
				Log.e("vortex","Could not find layer ["+target+"] for type "+nName);
				o.addRow("");
				o.addRedText("Could not find layer ["+target+"]. This means that type "+nName+" was not added");
			}
		}
		loadDone=true;

	}

	
	public String getObjectContext() {
			return Expressor.analyze(objContext);		
	}

	private void setDefaultBitmaps(WF_Context myContext) {
		if (icon==null) {
			
			if (myType == GisObjectType.Polygon) {
				
				icon = BitmapFactory.decodeResource(myContext.getContext().getResources(), R.drawable.poly);
				
				
			}

		}
	}

	private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

		
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

		protected void onPostExecute(final Bitmap result) {
			if(!loadDone)
				Log.e("vortex","In load bitmap postexecute, but load failed or not done");
			icon=result;
			/*
			if(!loadDone&&tries-->0) {
				Log.e("vortex","not done loading objects!!");
				final Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						onPostExecute(result);
					}
				}, 1000);
			}
			else {
				Log.e("vortex","setting bitmaps or giving up");
				icon=result;
				if(gisB!=null)
					gisB.getGis().invalidate();
			}*/
		}
	}



	public String getLabel() {
		return label;

	}

	public boolean isVisible() {
		return isVisible;
	}

	public float getRadius() {
		return radius;
	}

	public String getColor() {
		return color;
	}

	public Bitmap getIcon() {
		return icon;
	}

	@Override
	public GisObjectType getGisPolyType() {
		return myType;
	}


	public void setRadius(String radius) {
		if (!Tools.isNumeric(radius))
			return;
		this.radius = Float.parseFloat(radius);
	}

	private String print(String[] selectionArgs) {
		String res="";
		for (String s:selectionArgs)
			res+=s+",";
		res = res.substring(0, res.length()-1);
		return res;
	}

	



	@Override
	public Style getStyle() {
		return fillType;
	}

	@Override
	public PolyType getShape() {
		return polyType;
	}

	@Override
	public String getClickFlow() {
		return onClick;
	}

	@Override
	public CHash getObjectKeyHash() {
		return objKeyHash;
	}

	public String getStatusVariable() {
		return statusVariable;
	}

	public boolean isUser() {
		return isUser;
	}

	@Override
	public String getName() {
		return nName;
	}










}



