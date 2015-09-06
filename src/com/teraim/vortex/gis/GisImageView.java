package com.teraim.vortex.gis;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.FillType;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.Start;
import com.teraim.vortex.dynamic.types.CHash;
import com.teraim.vortex.dynamic.types.GisLayer;
import com.teraim.vortex.dynamic.types.Location;
import com.teraim.vortex.dynamic.types.PhotoMeta;
import com.teraim.vortex.dynamic.types.SweLocation;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.types.Workflow;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.vortex.dynamic.workflow_realizations.gis.DynamicGisPoint;
import com.teraim.vortex.dynamic.workflow_realizations.gis.FullGisObjectConfiguration;
import com.teraim.vortex.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.GisObjectType;
import com.teraim.vortex.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.PolyType;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisConstants;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisFilter;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisMultiPointObject;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisObject;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisPathObject;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisPointObject;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisPolygonObject;
import com.teraim.vortex.dynamic.workflow_realizations.gis.StaticGisPoint;
import com.teraim.vortex.dynamic.workflow_realizations.gis.WF_Gis_Map;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.non_generics.NamedVariables;
import com.teraim.vortex.utils.Geomatte;
import com.teraim.vortex.utils.RuleExecutor;
import com.teraim.vortex.utils.RuleExecutor.SubstiResult;
import com.teraim.vortex.utils.RuleExecutor.TokenType;
import com.teraim.vortex.utils.RuleExecutor.TokenizedItem;

public class GisImageView extends GestureImageView implements TrackerListener {

	private final static String Deg = "\u00b0";

	//Various paints.
	private Paint txtPaint;
	private Paint polyPaint;
	private Paint rCursorPaint;
	private Paint blCursorPaint;
	private Paint btnTxt,vtnTxt;
	private Paint grCursorPaint;
	private Paint selectedPaint;
	private Paint borderPaint;
	private Paint fgPaintSel;
	private Paint  wCursorPaint,bCursorPaint,markerPaint;
	private Paint paintBlur;
	private Paint paintSimple;

	private Handler handler;
	private Context ctx;
	private Calendar calendar = Calendar.getInstance();

	//Photometadata for the current view.
	private PhotoMeta photoMetaData;
	private Variable myX,myY;
	private static final Map<String,String>YearKeyHash = new HashMap<String,String>();

	private static final int LabelOffset = 5;

	//Long click or short click?
	private boolean clickWasShort=false;

	//To avoid creating a new rect in onDraw.
	private Rect rectReuse = new Rect();

	//The user Gis Point Object
	private GisPointObject userGop;

	//The bag and layer that contains the object currently clicked.
	private Set<GisObject> touchedBag;
	private GisLayer touchedLayer;
	private GisObject touchedGop = null;


	private boolean showLabelForAWhile=true;

	private Set<GisObject> candidates = new TreeSet<GisObject>(new Comparator<GisObject>() {
		@Override
		public int compare(GisObject lhs, GisObject rhs) {
			return (int)(lhs.getDistanceToClick()-rhs.getDistanceToClick());
		}
	});



	public GisImageView(Context context) {
		super(context);
		init(context);
	}

	public GisImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public GisImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	private void init(Context ctx) {



		YearKeyHash.clear();
		YearKeyHash.put("�r", Constants.getYear());
		this.ctx=ctx;
		//used for cursor blink.
		calendar.setTime(new Date());
		grCursorPaint = new Paint();
		grCursorPaint.setColor(Color.GRAY);
		grCursorPaint.setStyle(Paint.Style.FILL);
		blCursorPaint = new Paint();
		blCursorPaint.setColor(Color.BLUE);
		blCursorPaint.setStyle(Paint.Style.FILL);
		rCursorPaint = new Paint();
		rCursorPaint.setColor(Color.RED);
		rCursorPaint.setStyle(Paint.Style.FILL);
		wCursorPaint = new Paint();
		wCursorPaint.setColor(Color.WHITE);
		wCursorPaint.setStyle(Paint.Style.FILL);
		bCursorPaint = new Paint();
		bCursorPaint.setColor(Color.BLACK);
		bCursorPaint.setStyle(Paint.Style.FILL);
		markerPaint = new Paint();
		markerPaint .setColor(Color.YELLOW);
		markerPaint .setStyle(Paint.Style.FILL);		
		txtPaint = new Paint();
		txtPaint.setTextSize(8);
		txtPaint.setColor(Color.WHITE);
		txtPaint.setStyle(Paint.Style.STROKE);
		txtPaint.setTextAlign(Paint.Align.CENTER);
		selectedPaint = new Paint();
		selectedPaint.setTextSize(8);
		selectedPaint.setColor(Color.BLACK);
		selectedPaint.setStyle(Paint.Style.STROKE);
		selectedPaint.setTextAlign(Paint.Align.CENTER);
		btnTxt = new Paint();
		btnTxt.setTextSize(8);
		btnTxt.setColor(Color.WHITE);
		btnTxt.setStyle(Paint.Style.STROKE);
		btnTxt.setTextAlign(Paint.Align.CENTER);
		vtnTxt = new Paint();
		vtnTxt.setTextSize(8);
		vtnTxt.setColor(Color.WHITE);
		vtnTxt.setStyle(Paint.Style.STROKE);
		vtnTxt.setTextAlign(Paint.Align.CENTER);
		borderPaint = new Paint();
		borderPaint.setColor(Color.WHITE);
		borderPaint.setStyle(Paint.Style.STROKE);
		borderPaint.setStrokeWidth(3);
		polyPaint = new Paint();
		polyPaint.setColor(Color.WHITE);
		polyPaint.setStyle(Paint.Style.STROKE);
		polyPaint.setStrokeWidth(0);

		fgPaintSel = new Paint();
		fgPaintSel.setColor(Color.YELLOW);
		fgPaintSel.setStyle(Paint.Style.STROKE);
		fgPaintSel.setStrokeWidth(2);

		paintSimple = new Paint();
		paintSimple.setAntiAlias(true);
		paintSimple.setDither(true);
		paintSimple.setColor(Color.argb(248, 255, 255, 255));
		paintSimple.setStrokeWidth(2f);
		paintSimple.setStyle(Paint.Style.STROKE);
		paintSimple.setStrokeJoin(Paint.Join.ROUND);
		paintSimple.setStrokeCap(Paint.Cap.ROUND);

		paintBlur = new Paint();
		paintBlur.set(paintSimple);
		paintBlur.setColor(Color.argb(235, 74, 138, 255));
		paintBlur.setStrokeWidth(5f);
		paintBlur.setMaskFilter(new BlurMaskFilter(15, BlurMaskFilter.Blur.NORMAL)); 


		if (GlobalState.getInstance()!=null&&GlobalState.getInstance().getTracker()!=null)
			GlobalState.getInstance().getTracker().registerListener(this);


	}
	double pXR,pYR;
	private WF_Gis_Map myMap;
	private boolean allowZoom;
	private double[] stateToSave = new double[4];


	/**
	 *  
	 * @param wf_Gis_Map 	The map object
	 * @param pm			The geo coordinates for the image corners
	 * @param zoom			If extreme zoom is enabled
	 * @return 
	 */
	public void initialize(WF_Gis_Map wf_Gis_Map, PhotoMeta pm,boolean allowZoom) {


		mapLocationForClick=null;
		this.photoMetaData=pm;
		pXR = this.getImageWidth()/pm.getWidth();
		pYR = this.getImageHeight()/pm.getHeight();

		//Filer away all objects not visible and create cached values for all gisobjects on this map and zoom level.
		myMap = wf_Gis_Map;



		imgHReal = pm.N-pm.S;
		imgWReal = pm.E-pm.W;
		myX = GlobalState.getInstance().getVariableConfiguration().getVariableUsingKey(YearKeyHash, NamedVariables.MY_GPS_LAT);
		myY = GlobalState.getInstance().getVariableConfiguration().getVariableUsingKey(YearKeyHash, NamedVariables.MY_GPS_LONG);
		this.allowZoom = allowZoom;
		stateToSave[0]=pm.N;
		stateToSave[1]=pm.E;
		stateToSave[2]=pm.S;
		stateToSave[3]=pm.W;


		setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d("vortex","Gets short!");
				if (clickXY!=null)
					return;
				//if (doubleTap())
				//	return;
				calculateMapLocationForClick(polyVertexX,polyVertexY);


				if (gisTypeToCreate!=null) {
					//GisObject newP = StaticGisPoint(gisTypeToCreate, Map<String, String> keyChain,Location myLocation, Variable statusVar)

					List<Location> myDots;

					if (newGisObj ==null) {						
						Set<GisObject> bag;
						for (GisLayer l:myMap.getLayers()) { 

							bag = l.getBagOfType(gisTypeToCreate.getName());
							if (bag!= null) {
								Log.d("vortex","found correct bag!");
								newGisObj = createNewGisObject(gisTypeToCreate,bag);
								if (gisTypeToCreate.getGisPolyType()==GisObjectType.Point)
									myMap.setVisibleCreate(true);
							} 
						}
					}
					if (newGisObj !=null) {
						myDots=null;
						if (gisTypeToCreate.getGisPolyType()==GisObjectType.Linestring) {
							myDots = newGisObj.getCoordinates();
							myDots.add(mapLocationForClick);
						} else if (gisTypeToCreate.getGisPolyType()==GisObjectType.Polygon) {
							//Todo: Change this. currently assumed poly is under 1.
							myDots = ((GisPolygonObject)newGisObj).getPolygons().get("Poly 1");
							myDots.add(mapLocationForClick);
						}
						//Show menu and length only after at least 2 points defined.
						if (myDots!=null && myDots.size()>1) {
							myMap.setVisibleCreate(true);
							myMap.showLength(Geomatte.lengthOfPath(myDots));
						}
					} else 
						Log.e("vortex","New GisObj is null!");
				} else
					Log.e("vortex","gistype null!");
				clickWasShort = true;
				invalidate();

			}
		});



		setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				Log.d("vortex","Gets long!");
				if (clickXY!=null)
					return false;
				calculateMapLocationForClick(polyVertexX,polyVertexY);
				clickWasShort = false;
				invalidate();
				return true;
			}


		});

		//Remove any gis objects outside current viewport.
		initializeAndSiftGisObjects();

	}


	/**
	 * 
	 * @param layers
	 * @return copy only the gis objects in the layers that are visible in this view.
	 */
	public void initializeAndSiftGisObjects() {
		for (GisLayer layer :myMap.getLayers()) {

			filterLayer(layer);

		}

	}

	/**
	 * 
	 * @param layer
	 * 
	 * Will go through a layer and check if the gisobjects are inside the map. 
	 * If inside, the object is marked as useful.
	 * As a sideeffect, calcualte all the local coordinates.
	 */

	public void filterLayer(GisLayer layer) {
		Log.d("vortex","In filterAndCopy, layer has "+layer.getGisBags().size()+" bags");
		Map<String, Set<GisObject>> gops = layer.getGisBags();

		for (String key:gops.keySet()) {
			Set<GisObject> bag = gops.get(key);

			Iterator<GisObject> iterator = bag.iterator();
			int[] xy;
			boolean hasDynamic = false;
			while (iterator.hasNext()) {
				GisObject go = iterator.next();
				//All dynamic objects are always in the map potentially.
				if (go instanceof DynamicGisPoint) {
					hasDynamic=true;
					//Dynamic objects are always useful and do not have a cached value.
					go.markAsUseful();
				}
				else if (go instanceof GisPointObject) {
					GisPointObject gop = (GisPointObject)go;
					xy = new int[2];
					boolean inside = translateMapToRealCoordinates(gop.getLocation(),xy);
					if (inside) {
						go.markAsUseful();
						gop.setTranslatedLocation(xy);
					}
					continue;
				} 
				else if (go instanceof GisPathObject) {
					GisPathObject gpo = (GisPathObject)go;
					boolean hasAtleastOneCornerInside = false;
					List<int[]> corners = new ArrayList<int[]>();
					for (Location location:go.getCoordinates()) {
						xy = new int[2];
						if(translateMapToRealCoordinates(location,xy))
							hasAtleastOneCornerInside = true;
						corners.add(xy);
					}
					if (hasAtleastOneCornerInside) {
						go.markAsUseful();
						Path p = new Path();
						boolean first =true;
						for (int[] corner:corners) {
							if (first) {
								first=false;
								p.moveTo(corner[0], corner[1]);
							}
							else
								p.lineTo(corner[0], corner[1]);
						}
						if (go instanceof GisPolygonObject)
							p.close();
						gpo.setPath(p);
					}
				} 
				else
					Log.d("vortex","Gisobject "+go.getLabel()+" was not added");

			}
			Log.d("vortex","Bag: "+key+" size: "+bag.size());

		}
	}




	private float fixedX=-1;
	private float fixedY;

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (fixedX==-1) {
			fixedY=y;
			fixedX=x;
			Log.d("vortex","fixed xy"+fixedX+","+fixedY);
		}
	}


	PhotoMeta gisImage;
	//difference in % between ruta and image size.

	private Location mapLocationForClick=null;
	private float[] clickXY;
	private double imgHReal;
	private double imgWReal;

	private GisObject newGisObj;

	private Set<GisObject> currentCreateBag;

	private float[] translateToReal(float mx,float my) {
		float fixScale = scale * scaleAdjust;
		Log.d("vortex","fixscale: "+fixScale);
		mx = (mx-x)/fixScale;
		my = (my-y)/fixScale;
		return new float[]{mx,my};
	}


	private Location translateRealCoordinatestoMap(float[] xy) {

		float rX = this.getImageWidth()/2+((float)xy[0]);
		float rY = this.getImageHeight()/2+((float)xy[1]);
		pXR = photoMetaData.getWidth()/this.getImageWidth();
		pYR = photoMetaData.getHeight()/this.getImageHeight();
		double mapDistX = rX*pXR;
		double mapDistY = rY*pYR;
		return new SweLocation(mapDistX + photoMetaData.W,photoMetaData.N-mapDistY);

	}

	/** 
	 * 
	 * @param l	- the location to examine
	 * @param xy - the translated x and y into screen coordinates.
	 * @return true if the coordinate is inside the current map.
	 */
	private boolean translateMapToRealCoordinates(Location l, int[] xy) {
		//Assume it is inside
		boolean isInside = true;
		double mapDistX = l.getX()-photoMetaData.W;
		if (mapDistX <=imgWReal && mapDistX>=0);
		//Log.d("vortex","Distance X in meter: "+mapDistX+" [inside]");
		else {

			//Log.e("vortex","Distance X in meter: "+mapDistX+" [outside!]");
			//Log.d("vortex","w h of gis image. w h of image ("+photoMetaData.getWidth()+","+photoMetaData.getHeight()+") ("+this.getScaledWidth()+","+this.getScaledHeight()+")");
			//Log.d("vortex","photo (X) "+photoMetaData.W+"-"+photoMetaData.E);
			//Log.d("vortex","photo (Y) "+photoMetaData.S+"-"+photoMetaData.N);
			//Log.d("vortex","object X,Y: "+l.getX()+","+l.getY());

			//No, it is outside.
			isInside = false;
		}
		double mapDistY = l.getY()-photoMetaData.S;
		if (mapDistY <=imgHReal && mapDistY>=0)
			;//Log.d("vortex","Distance Y in meter: "+mapDistY+" [inside]");
		else {
			//Log.e("vortex","Distance Y in meter: "+mapDistY+" [outside!]");
			//Log.d("vortex","w h of gis image. w h of image ("+photoMetaData.getWidth()+","+photoMetaData.getHeight()+") ("+this.getScaledWidth()+","+this.getScaledHeight()+")");
			//Log.d("vortex","photo (X) "+photoMetaData.W+"-"+photoMetaData.E);
			//Log.d("vortex","photo (Y) "+photoMetaData.S+"-"+photoMetaData.N);
			//Log.d("vortex","object X,Y: "+l.getX()+","+l.getY());
			isInside = false;
		}
		pXR = this.getImageWidth()/photoMetaData.getWidth();
		pYR = this.getImageHeight()/photoMetaData.getHeight();

		//		Log.d("vortex","px, py"+pXR+","+pYR);
		double pixDX = mapDistX*pXR;
		double pixDY = mapDistY*pYR;
		//Log.d("vortex","distance on map (in pixel no scale): x,y "+pixDX+","+pixDY);
		float rX = ((float)pixDX)-this.getImageWidth()/2;
		float rY = this.getImageHeight()/2-((float)pixDY);

		//		Log.d("vortex","X: Y:"+x+","+y);
		//		Log.d("vortex","fixScale: "+fixScale);
		//		Log.d("vortex","after calc(x), calc(y) "+rX+","+rY);
		//		Log.d("vortex","fX: fY:"+fixedX+","+fixedY);
		//		Log.d("vortex","after fcalc(x), fcalc(y) "+this.fCalcX(rX)+","+fCalcY(rY));
		xy[0]=(int)rX; 
		xy[1]=(int)rY;

		return isInside;

	}

	public Location calculateMapLocationForClick(float x, float y) {
		//Figure out geo coords from pic coords.
		clickXY=translateToReal(x,y);
		mapLocationForClick = translateRealCoordinatestoMap(clickXY);
		Log.d("vortex","click at "+x+","+y);
		Log.d("vortex","click at "+mapLocationForClick.getX()+","+mapLocationForClick.getY());
		return mapLocationForClick;
	}

	private GisObject createNewGisObject(
			FullGisObjectConfiguration gisTypeToCreate, Set<GisObject> bag) {
		//create object or part of object.
		CHash keyH = gisTypeToCreate.getObjectKeyHash();
		GisObject ret=null;

		//break if no keyhash.
		if (keyH!=null && keyH.keyHash!=null) {
			String uid = UUID.randomUUID().toString();
			Log.d("vortex","HACK: Adding uid: "+uid);
			keyH.keyHash.put("uid", uid);
			Log.d("vortex","keyhash for new obj is: "+keyH.keyHash.toString());
			List<Location> myDots;

			switch (gisTypeToCreate.getGisPolyType()) {

			case Point:
				ret = new StaticGisPoint(gisTypeToCreate,keyH.keyHash,mapLocationForClick, null);
				int[] xy = new int[2];
				translateMapToRealCoordinates(mapLocationForClick,xy);
				((StaticGisPoint)ret).setTranslatedLocation(xy);
				break;

			case Linestring:
				myDots = new ArrayList<Location>();
				ret = new GisMultiPointObject(gisTypeToCreate, keyH.keyHash,myDots);	
				break;
			case Polygon:
				ret = new GisPolygonObject(gisTypeToCreate, keyH.keyHash,
						"",GisConstants.SWEREF,null);
				break;

			}
			//Log.d("vortex","Adding "+ret.toString()+" to bag "+bag.toString());
			ret.markAsUseful();
			bag.add(ret);	

			//save layer and object for undo.
			currentCreateBag = bag;

		} else 
			Log.e("vortex","Cannot create, object keyhash is null!!!");
		return ret;
	}

	//returns true if there is no more backing up possible.
	public boolean goBack() {
		if (newGisObj!=null) {
			if (newGisObj instanceof StaticGisPoint) {
				currentCreateBag.remove(newGisObj);
				newGisObj=null;


			} else if (newGisObj instanceof GisMultiPointObject ||
					newGisObj instanceof GisPolygonObject) {
				List<Location> myDots = newGisObj.getCoordinates();
				if (myDots==null ||myDots.isEmpty()) {					
					currentCreateBag.remove(newGisObj);
					newGisObj=null;
				}
				else
					myDots.remove(myDots.size()-1);				
			}
			this.invalidate();
			return false;
		} else {
			gisTypeToCreate=null;
			return true;
		}
	}

	public void createOk() {
		//if this is a path, close it.
		if (newGisObj instanceof GisPolygonObject) 
			((GisPathObject)newGisObj).getPath().close();

		GlobalState.getInstance().getDb().insertGisObject(newGisObj);

		gisTypeToCreate=null;
		touchedBag = currentCreateBag;
		currentCreateBag=null;
		touchedGop = newGisObj;
		newGisObj=null;
		myMap.setVisibleAvstRikt(true,touchedGop);

		this.redraw();

	}



	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		canvas.save();
		//scale and adjust.
		try {
			float adjustedScale = scale * scaleAdjust;
			if (allowZoom && adjustedScale>2.0f)
				myMap.setZoomButtonVisible(true);
			else
				myMap.setZoomButtonVisible(false);
			canvas.translate(x, y);
			if(adjustedScale != 1.0f) {
				canvas.scale(adjustedScale, adjustedScale);
			}

			candidates.clear();

			for (GisLayer layerO:myMap.getLayers()) {
				String layerId = layerO.getId();
				//Log.d("vortex","drawing layer "+layerId);
				//get all objects that should be drawn on this layer.
				if (!layerO.isVisible()) {
					Log.d("vortex","layer not visible...skipping "+layerId);
					continue;
				}
				//if (layerO.hasDynamic()) {
				//Log.d("vortex","dynamic obj found in "+layer);
				//	this.startDynamicRedraw();
				//}
				Map<String, Set<GisObject>> bags = layerO.getGisBags();
				Map<String, Set<GisFilter>> filterMap = layerO.getFilters();
				//only allow clicks if something is not already touched.
				pXR = this.getImageWidth()/photoMetaData.getWidth();
				pYR = this.getImageHeight()/photoMetaData.getHeight();

				if (!bags.isEmpty()) {

					for (String key:bags.keySet()) {
						Set<GisFilter> filters = filterMap!=null?filterMap.get(key):null;
						Set<GisObject> bagOfObjects = bags.get(key);
						//Log.d("vortex","Found "+gisObjects.size()+" objects");
						Iterator<GisObject> iterator = bagOfObjects.iterator();

						while (iterator.hasNext()) {
							GisObject go = iterator.next();
							//If not inside map, or if touched, skip.
							if (!go.isUseful() || (touchedGop!=null&&go.equals(touchedGop)))
								continue;
							if (go instanceof GisPointObject) {
								GisPointObject gop = (GisPointObject)go;

								if (gop.isDynamic()) {
									int[] xy = new int[2];
									boolean inside = translateMapToRealCoordinates(go.getLocation(),xy);
									if (!inside) {
										if (gop.equals(userGop)) {
											myMap.showCenterButton(false);
											userGop=null;
										} 
										//This object should not be drawn.
										continue;
									} else {
										if (gop.isUser()) {
											userGop = gop;
											myMap.showCenterButton(true);
										}
										gop.setTranslatedLocation(xy);
									}


								}  
								Bitmap bitmap = gop.getIcon();
								float radius = gop.getRadius();
								String color = gop.getColor();
								Style style = gop.getStyle();
								PolyType polyType=gop.getShape();

								String statusColor = colorShiftOnStatus(gop.getStatusVariable());
								if (statusColor!=null)
									color = statusColor;

								if (filters!=null&&!filters.isEmpty()) {

									//Log.d("vortex","has filter!");
									RuleExecutor ruleExecutor = GlobalState.getInstance().getRuleExecutor();
									for (GisFilter filter:filters) {	
										if (filter.isActive()) {
											//Log.d("vortex","Filter active!");
											if (!filter.hasCachedFilterResult()) 
												filter.setTokens(ruleExecutor.findTokens(filter.getExpression(),null, gop.getKeyHash()));
											Log.d("vortex","EXpr: "+filter.getExpression()+" tokens null? "+filter.getTokens());
											if (!gop.hasCachedFilterResult(filter)) {
												List<TokenizedItem> myTokens = filter.getTokens();
												for (TokenizedItem t:myTokens) {
													if (t.getType()==TokenType.variable)
														t.setVariable(GlobalState.getInstance().getVariableConfiguration().getVariableUsingKey(gop.getKeyHash(), t.getVariable().getId()));
												}
												SubstiResult substR = ruleExecutor.substituteForValue(myTokens, filter.getExpression(),false);
												String result = ruleExecutor.parseExpression(filter.getExpression(),substR.result);
												if (result!=null&&Integer.parseInt(result)==0) 
													gop.setCachedFilterResult(filter,true);
												else
													gop.setCachedFilterResult(filter,false);
											}
											if ( gop.getCachedFilterResult(filter)) {
												Log.d("vortex","FILTER MATCH FOR FILTER: "+filter.getLabel());
												bitmap = filter.getBitmap();
												radius = filter.getRadius();
												color = filter.getColor();
												style = filter.getStyle();
												polyType = filter.getShape();
											}
										} else
											Log.d("vortex","Filter turned off!");
									}
								}
								int[] xy = gop.getTranslatedLocation();
								drawPoint(canvas,bitmap,radius,color,style,polyType,xy,adjustedScale);

								if (layerO.showLabels()) {

									drawGopLabel(canvas,xy,gop.getLabel(),LabelOffset,bCursorPaint,txtPaint);
								}


							} else if (go instanceof GisPathObject) {
								if (go instanceof GisMultiPointObject) {
									if (!((GisMultiPointObject)go).isLineString()) {
										Log.d("vortex","This is a multipoint. Path is useless.");
									}
								}

								drawGop(canvas,layerO,go,false);
							}

							//Check if an object has been clicked in this layer.
							if (touchedGop==null && gisTypeToCreate == null && mapLocationForClick!=null && go.isTouchedByClick(mapLocationForClick,pXR,pYR) && !go.equals(userGop))
								candidates.add(go);
						}
					}
				}
			}

			//Special rendering of touched gop.
			if (!candidates.isEmpty()) {
				//Candidates are sorted in a set. Return first. 
				String candidatesS="";
				for (GisObject go:candidates) {
					candidatesS+=go.getLabel()+" Distance: "+go.getDistanceToClick()+"\n";
				}
				Log.d("vortex","DEBUG: Members of candidateS: \n"+candidatesS);
				touchedGop = candidates.iterator().next();
				showLabelForAWhile=true;
				startShowLabelTimer();

				//Find the layer and bag touched. 
				for (GisLayer layer:myMap.getLayers()) {
					touchedBag = layer.getBagContainingGo(touchedGop);
					if (touchedBag!=null) 
						touchedLayer = layer;
				}
				//if longclick, open the actionbar menu.
				if (!clickWasShort)
					myMap.startActionModeCb();
				else {
					myMap.setVisibleAvstRikt(true,touchedGop);

					displayDistanceAndDirection();
					if (mostRecentGPSValueTimeStamp!=-1 && myX!=null&&myY!=null&&myX.getValue()!=null && myY.getValue()!=null) {
						//Create a line from user to object.
						double mX = Double.parseDouble(myX.getValue());
						double mY = Double.parseDouble(myY.getValue());
						riktLinjeStart = new int[2];riktLinjeEnd = new int[2];
						translateMapToRealCoordinates(new SweLocation(mX,mY),riktLinjeStart);
						translateMapToRealCoordinates(touchedGop.getLocation(),riktLinjeEnd);
					}

				}
				candidates.clear();

			} 

			if (touchedGop!=null) {
				drawGop(canvas,touchedLayer,touchedGop,true);

			} 
		} catch(Exception e) {
			if (GlobalState.getInstance()!=null) {
				LoggerI o = GlobalState.getInstance().getLogger();
				if (o!=null) {
					o.addRow("");

					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);		
					o.addRedText(sw.toString());
					e.printStackTrace();
				}
			}
		}
		canvas.restore();
		//Reset any click done. 
		mapLocationForClick=null;
		clickXY=null;

	}


	private void startShowLabelTimer() {
		final int interval = 1000; // 1 Second
		Handler handler = new Handler();
		Runnable runnable = new Runnable(){
			public void run() {
				showLabelForAWhile=false;
				postInvalidate();
			}
		};

		handler.postDelayed(runnable, interval);
	}

	private String colorShiftOnStatus(Variable statusVar) {
		String color=null;
		if (statusVar!=null ) {
			String value = statusVar.getValue();
			if (value.equals("1")) {
				color = "yellow";
			}
			else if (value.equals("2")) {
				color = "red";
			}
			else if	(value.equals("3")) {
				color = "green";
			}
		} 
		return color;
	}

	private void drawGop(Canvas canvas, GisLayer layerO, GisObject go, boolean selected) {

		boolean beingDrawn = false;
		if (newGisObj !=null) {
			beingDrawn = go.equals(newGisObj);
			Log.d("vortex","beingdrawn is "+beingDrawn);
		}
		//will only be called from here if selected.
		GisPointObject gop=null;
		//Only gets her for Gispoint, if it is selected.
		if (go instanceof GisPointObject) {
			Log.d("vortex","gets here! Beingdrawn is "+beingDrawn);
			gop = (GisPointObject)go;
			drawPoint(canvas, null,gop.getRadius(), "red", Style.FILL, gop.getShape(), gop.getTranslatedLocation(),1);
		}

		else if (go instanceof GisPathObject) {

			GisPathObject gpo = (GisPathObject)go;
			//Add glow effect if it is currently being drawn.
			
			boolean onlyOne =false;

			Path p = gpo.getPath();

			//objects being drawn cannot be cached.
			if (p==null || beingDrawn) {
				p = new Path();
				List<Location> ll = go.getCoordinates();
				onlyOne = ll.size()==1;
				Log.d("vortex","Only one is "+onlyOne);
				boolean first = true;
				if (ll!=null) {
					int[] xy = new int[2];
					for (Location l:ll) {
						translateMapToRealCoordinates(l,xy);
						if (onlyOne) {
							drawPoint(canvas, null,2, "white", Style.STROKE, PolyType.circle, xy,1);
							break;
						} else {
							if (first) {
								p.moveTo(xy[0],xy[1]);
								first =false;
							} else
								p.lineTo(xy[0],xy[1]);
						}
					}
				}

			}
			if (!onlyOne) {
				gpo.setPath(p);


				if (selected) {
					canvas.drawPath(p, paintBlur);
					canvas.drawPath(p, paintSimple);

				}
				else if (beingDrawn) {
					canvas.drawPath(p, polyPaint);
					//myMap.showLenthOfPath(new PathMeasure(p,false));

				} else {
					String color = colorShiftOnStatus(go.getStatusVariable());
					if (color==null) 
						color = go.getColor();
					canvas.drawPath(p, createPaint(color,Paint.Style.STROKE,0));
				}
			}
		}

		//Check if label should be drawn.
		int[]xy;
		if (layerO.showLabels()) {
			if (gop==null) {
				xy = new int[2];
				translateMapToRealCoordinates(go.getLocation(),xy);
			}
			else
				xy = gop.getTranslatedLocation();

			drawGopLabel(canvas,xy,go.getLabel(),LabelOffset,selected?wCursorPaint:bCursorPaint,selected?selectedPaint:vtnTxt);
		}
		//Check if directional line should be drawn.
		if (riktLinjeStart!=null)
			canvas.drawLine(riktLinjeStart[0], riktLinjeStart[1], riktLinjeEnd[0],riktLinjeEnd[1],fgPaintSel);//fgPaintSel

	}




	/**
	 * Draws a Label above the object location at the distance given by offSet
	 * @param canvas
	 * @param xy
	 * @param mLabel
	 * @param offSet
	 * @param bgPaint
	 * @param txtPaint
	 */
	private void drawGopLabel(Canvas canvas, int[] xy, String mLabel, float offSet, Paint bgPaint, Paint txtPaint) {
		Rect bounds = new Rect();
		txtPaint.getTextBounds(mLabel, 0, mLabel.length(), bounds);
		bounds.set(bounds.left-2,bounds.top-2,bounds.right+2,bounds.bottom+2);
		bounds.offset((int)xy[0]-bounds.width()/2,(int)xy[1]-(bounds.height()/2+(int)offSet));
		canvas.drawRect(bounds, bgPaint);
		canvas.drawText(mLabel, xy[0], (int)xy[1]-(bounds.height()/2+(int)offSet),txtPaint);								

	}

	private void drawPoint(Canvas canvas, Bitmap bitmap, float radius, String color, Style style, PolyType type, int[] xy, float adjustedScale) {
		Rect r = new Rect();
		//pXR = this.getImageWidth()/photoMetaData.getWidth();		
		//radius = Math.max(5, (int)(radius*pXR));
		//Log.d("vortex","PXR: Radius:"+pXR+","+radius);
		if (bitmap!=null) {
			//Log.d("vortex","bitmap! "+gop.getLabel());
			r.set(xy[0]-32, xy[1]-32, xy[0], xy[1]);
			canvas.drawBitmap(bitmap, null, r, null);
		} //circular?

		else if (type == PolyType.circle) {
			//Log.d("vortex","x,y,r"+xy[0]+","+xy[1]+","+radius);
			canvas.drawCircle(xy[0], xy[1],radius, createPaint(color,style));
		} 
		//no...square.
		else if (type == PolyType.rect) {
			//Log.d("vortex","rect!");
			int diam = (int)(radius/2);
			r.set(xy[0]-diam, xy[1]-diam, xy[0]+diam, xy[1]+diam);
			canvas.drawRect(r, createPaint(color,style));
		}
		else if (type == PolyType.triangle) {
			drawTriangle(canvas,color,style,radius,xy[0], xy[1]);
		}
	}

	//0 = distance, 1=riktning.


	public void drawTriangle(Canvas canvas, String color, Style style,
			float radius, int x, int y) {
		Paint paint = this.createPaint(color, style);
		Path path = new Path();
		path.setFillType(FillType.EVEN_ODD);

		path.moveTo(x,y-radius);
		path.lineTo(x+radius, y+radius);
		path.lineTo(x-radius, y+radius);
		path.close();

		canvas.drawPath(path, paint);
	}

	public void unSelectGop() {
		touchedGop=null;
		showLabelForAWhile=true;
		myMap.setVisibleAvstRikt(false,null);
		//Remove directional line.
		riktLinjeStart=null;
		invalidate();
	}



	//save the position where the user pressed start. 
	private int[] riktLinjeStart,riktLinjeEnd;
	final static int TimeOut = 30;

	private void displayDistanceAndDirection() {
		final int interval = 250; 

		if (handler==null) {
			handler = new Handler();
			Runnable runnable = new Runnable(){
				public void run() {
					displayDistanceAndDirectionL();
					if (handler!=null)
						handler.postDelayed(this, interval);
				}
			};

			handler.postDelayed(runnable, interval);

		}

	}

	private void displayDistanceAndDirectionL() {


		//Check preconditions for GPS to work

		if (myX==null||myY==null||GlobalState.getInstance()==null) {
			myMap.setAvstTxt("C_Error");
			handler=null;
			return;
		}
		if (touchedGop==null) {
			Log.d("vortex","terminated display dist");
			handler=null;
			return;
		}

		if (!GlobalState.getInstance().getTracker().isGPSEnabled) {
			myMap.setAvstTxt("GPS OFF");
			//myMap.setRiktTxt(spinAnim());
		}
		//Start a redrawtimer if not already started that redraws this window independent of the redraw cycle of the gops.

		//Check  timediff. Returns null in case no value exists.

		//long timeDiff;
		//long ct;
		if (mostRecentGPSValueTimeStamp!=-1) {
			//ct = System.currentTimeMillis();
			//timeDiff = (ct-mostRecentGPSValueTimeStamp)/1000;			
		} else {
			myMap.setAvstTxt("No Value");
			//myMap.setRiktTxt(spinAnim());
			return;
		}
		//boolean old = timeDiff>TimeOut; 
		/*if (old) {
			Log.d("vortex","Time of insert: "+mostRecentGPSValueTimeStamp);
			Log.d("vortex","Current time: "+ct);
			Log.d("vortex","TimeDiff: "+timeDiff);
		}
		 */
		double mX = Double.parseDouble(myX.getValue());
		double mY = Double.parseDouble(myY.getValue());
		double gX = touchedGop.getLocation().getX();
		double gY = touchedGop.getLocation().getY();
		int dist = (int)Geomatte.sweDist(mY,mX,gY,gX);
		int rikt = (int)(Geomatte.getRikt2(mY, mX, gY, gX)*57.2957795);
		myMap.setAvstTxt(dist>9999?(dist/1000+"km"):(dist+"m"));
		myMap.setRiktTxt(rikt+Deg);




	}



	private Map<String,Paint> paintCache = new HashMap<String,Paint>();

	public Paint createPaint(String color, Paint.Style style) {
		return createPaint(color,style,2);
	}

	public Paint createPaint(String color, Paint.Style style, int strokeWidth) {
		String key = style==null?color:color+style.name();
		Paint p = paintCache.get(key);
		if (p!=null) {
			//Log.d("vortex","returns cached paint for "+key);
			return p;
		}
		//If no cached object, create.
		p = new Paint();
		p.setColor(color!=null?Color.parseColor(color):Color.YELLOW);
		p.setStyle(style!=null?style:Paint.Style.FILL);
		p.setStrokeWidth(strokeWidth);
		paintCache.put(key, p);
		return p;
	}








	private boolean isStarted=false;


	private FullGisObjectConfiguration gisTypeToCreate;



	public void runSelectedWf() {
		//update image to close polygon.
		invalidate();
		if (touchedGop!=null)
			runSelectedWf(touchedGop);
		unSelectGop();
	}
	public void runSelectedWf(GisObject gop) {
		GlobalState.getInstance().setKeyHash(gop.getKeyHash());

		Log.d("vortex","Setting current keyhash to "+gop.getKeyHash());
		String target = gop.getWorkflow();
		Workflow wf = GlobalState.getInstance().getWorkflow(target);
		if (wf ==null) {
			Log.e("vortex","missing click target workflow");
			new AlertDialog.Builder(ctx)
			.setTitle("Missing workflow")
			.setMessage("No workflow associated with the GIS object or workflow not found: ["+target+"]. Check your XML.") 
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setCancelable(false)
			.setNeutralButton("Ok",new Dialog.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {

				}
			} )
			.show();
		} else {

			Variable statusVariable = gop.getStatusVariable();
			String statusVarS=null;
			if (statusVariable!=null) {
				statusVarS = statusVariable.getId();
				String valS = statusVariable.getValue();
				if (valS==null || valS.equals("0")) {
					Log.d("vortex","Setting status variable to 1");
					statusVariable.setValue("1");
				} else
					Log.d("vortex","NOT Setting status variable to 1...current val: "+statusVariable.getValue());
				myMap.registerEvent(new WF_Event_OnSave("Gis"));

			} 

			Start.singleton.changePage(wf,statusVarS);

		}
	}





	final static float ScaleTo = 4.0f;
	public void centerOnUser() {
		if (userGop!=null) {
			int[] xy = new int[2];
			boolean inside = translateMapToRealCoordinates(userGop.getLocation(),xy);
			if (inside) {
				//float[] rxy = translateToReal((float)xy[0],(float)xy[1]);
				float scaleDiff = ScaleTo-scaleAdjust;
				//moveBy(rxy[0],rxy[1]);
				setPosition(fixedX-xy[0]*ScaleTo,fixedY-xy[1]*ScaleTo); //300 450
				redraw();
				Log.d("vortex","X Y USER "+xy[0]+","+xy[1]);
				//Log.d("vortex","RX RY USER "+rxy[0]+","+rxy[1]);
				Log.d("vortex","X Y SCALE: SCALEADJ:"+x+","+y+","+scale+","+scaleAdjust);
				//this.invalidate();
				//float newScale = 4.0f-scaleAdjust;
				startZoom(x, y,scaleDiff);
			}
		}/* else {
			new AlertDialog.Builder(ctx)
			.setTitle("Context problem")
			.setMessage("You are either outside map or have no valid GPS location.") 
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setCancelable(true)
			.setNeutralButton("Ok",new Dialog.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {

				}
			} )
			.show();
		}*/
	}

	enum CreateState {
		initial,
		inBetween,
		readyToGo
	}

	public void cancelGisObjectCreation() {
		if (gisTypeToCreate!=null) {
			gisTypeToCreate=null;
			if (newGisObj!=null) {
				currentCreateBag.remove(newGisObj);
				currentCreateBag =null;	
				newGisObj=null;		
			}
		}
		invalidate();
	}


	public void startGisObjectCreation(FullGisObjectConfiguration fop) {
		//unselect if selected
		this.unSelectGop();
		gisTypeToCreate=fop;
	}

	public void deleteSelectedGop() {
		if (touchedGop!=null)
			GlobalState.getInstance().getDb().deleteAllVariablesUsingKey(touchedGop.getKeyHash());
		touchedBag.remove(touchedGop);
		//Dont need to keep track of the bag anymore.
		touchedBag=null;
		invalidate();
	}

	public GisObject getSelectedGop() {
		return touchedGop;
	}

	public void describeSelectedGop() {
		String hash = "*null*";
		if (touchedGop.getKeyHash()!=null)
			hash = touchedGop.getKeyHash().toString();
		new AlertDialog.Builder(ctx)
		.setTitle("GIS OBJECT DESCRIPTION")
		.setMessage("Type: "+touchedGop.getId()+"\nLabel: "+touchedGop.getLabel()+
				"\nSweref: "+touchedGop.getLocation().getX()+","+touchedGop.getLocation().getY()+
				"\nAttached workflow: "+touchedGop.getWorkflow()+
				"\nKeyHash: "+hash+
				"\nPolygon type: "+touchedGop.getGisPolyType().name())
				.setIcon(android.R.drawable.ic_menu_info_details)
				.setCancelable(true)
				.setNeutralButton("Ok",new Dialog.OnClickListener() {				
					@Override
					public void onClick(DialogInterface dialog, int which) {

					}
				} )
				.show();
	}

	long mostRecentGPSValueTimeStamp=-1;
	@Override
	public void gpsStateChanged(GPS_State newState) {
		Log.d("vortex","Got GPS STATECHANGE");
		if (newState==GPS_State.newValueReceived)
			mostRecentGPSValueTimeStamp = System.currentTimeMillis();

		this.postInvalidate();


	}

	//Starts a redraw every 3rd second if at least one object is dynamic.
	private void startDynamicRedraw() {
		if (!isStarted) {
			final int interval = 1000; // 1 Second
			handler = new Handler();
			Runnable runnable = new Runnable(){
				public void run() {
					gpsStateChanged(GPS_State.ping);
					handler.postDelayed(this, interval);
				}
			};

			handler.postDelayed(runnable, interval);
			isStarted=true;
		}
	}

	public List<Location> getRectGeoCoordinates(Rect r) {

		//Left top right bottom!
		List<Location> ret = new ArrayList<Location> ();

		Location topCorner = calculateMapLocationForClick(0,0);
		Location bottomCorner = calculateMapLocationForClick(this.displayWidth,this.displayHeight);

		ret.add(topCorner);
		ret.add(bottomCorner);

		return ret;

	}

	public Rect getCurrentViewSize(float fileImageWidth,float fileImageHeight) {
		//float Scales = scale * scaleAdjust;


		//int top = (int)(rX-this.getImageWidth()/2);

		final float Xs = (this.getScaledWidth()/2)-x;
		final float Ys = (this.getScaledHeight()/2)-y;
		final float Xe = Xs+this.displayWidth;
		final float Ye = Ys+this.displayHeight;


		final float scaleFx = fileImageWidth/this.getScaledWidth();
		final float scaleFy = fileImageHeight/this.getScaledHeight();


		final int left  = (int)(Xs * scaleFx);
		final int top  = (int)(Ys * scaleFy);

		final int right = (int)(Xe * scaleFx);
		final int bottom =(int)(Ye * scaleFy);

		Rect r = new Rect(left,top,right,bottom);

		Log.d("vortex","top bottom left right "+top+","+bottom+","+left+","+right);
		return r;
	}





}
