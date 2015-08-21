package com.teraim.vortex.gis;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
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
import android.graphics.Rect;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
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
import com.teraim.vortex.dynamic.workflow_realizations.gis.FullGisObjectConfiguration;
import com.teraim.vortex.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.GisObjectType;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisConstants;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisFilter;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisMultiPointObject;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisObject;
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
	private Calendar calendar = Calendar.getInstance();
	private Paint txtPaint;
	private Handler handler;
	private Context ctx;
	private Paint polyPaint;
	private PhotoMeta photoMetaData;
	private Paint rCursorPaint;
	private Paint blCursorPaint;
	private Paint btnTxt,vtnTxt;
	private Paint grCursorPaint;
	private Paint selectedPaint;
	private Paint borderPaint;
	private Paint fgPaintSel;
	private Paint  wCursorPaint,bCursorPaint,markerPaint;
	private Variable myX,myY;
	private GisObject touchedGop = null;
	private static final Map<String,String>YearKeyHash = new HashMap<String,String>();
	private Paint paintBlur;
	private Paint paintSimple;
	private boolean clickWasShort=false;

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
		YearKeyHash.put("år", Constants.getYear());
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
						for (GisLayer l:myLayers) { 

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
						//Set menu visible only after at least 2 points defined.
						if (myDots!=null && myDots.size()>1)
							myMap.setVisibleCreate(true);

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


		if (GlobalState.getInstance().getTracker()!=null)
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
	 */
	public void initialize(WF_Gis_Map wf_Gis_Map, PhotoMeta pm, boolean zoom) {
		mapLocationForClick=null;
		this.photoMetaData=pm;
		pXR = this.getImageWidth()/pm.getWidth();
		pYR = this.getImageHeight()/pm.getHeight();
		myMap = wf_Gis_Map;
		imgHReal = pm.N-pm.S;
		imgWReal = pm.E-pm.W;
		myX = GlobalState.getInstance().getVariableConfiguration().getVariableUsingKey(YearKeyHash, NamedVariables.MY_GPS_LAT);
		myY = GlobalState.getInstance().getVariableConfiguration().getVariableUsingKey(YearKeyHash, NamedVariables.MY_GPS_LONG);
		this.allowZoom = zoom;
		stateToSave[0]=pm.N;
		stateToSave[1]=pm.E;
		stateToSave[2]=pm.S;
		stateToSave[3]=pm.W;
		
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
	private float rXRatio,rYRatio;
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


	private int[] translateMapToRealCoordinates(Location l) {

		double mapDistX = l.getX()-photoMetaData.W;
		if (mapDistX <=imgWReal && mapDistX>=0);
		//Log.d("vortex","Distance X in meter: "+mapDistX+" [inside]");
		else {

			//Log.e("vortex","Distance X in meter: "+mapDistX+" [outside!]");
			//Log.d("vortex","w h of gis image. w h of image ("+photoMetaData.getWidth()+","+photoMetaData.getHeight()+") ("+this.getScaledWidth()+","+this.getScaledHeight()+")");
			//Log.d("vortex","photo (X) "+photoMetaData.W+"-"+photoMetaData.E);
			//Log.d("vortex","photo (Y) "+photoMetaData.S+"-"+photoMetaData.N);
			//Log.d("vortex","object X,Y: "+l.getX()+","+l.getY());

			return null;
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
			return null;
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
		return new int[]{(int)rX,(int)rY};

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
		GlobalState.getInstance().getDb().insertGisObject(newGisObj);
		gisTypeToCreate=null;
		touchedBag = currentCreateBag;
		currentCreateBag=null;
		touchedGop = newGisObj;
		newGisObj=null;
		myMap.setSelectedObjectText(touchedGop.getLabel());
		myMap.setVisibleAvstRikt(true);

		this.redraw();
		
	}

	private GisPointObject userGop;

	private Set<GisObject> touchedBag;

	private boolean showLabelForAWhile=true;

	private Set<GisObject> candidates = new TreeSet<GisObject>(new Comparator<GisObject>() {
		@Override
		public int compare(GisObject lhs, GisObject rhs) {
			return (int)(lhs.getDistanceToClick()-rhs.getDistanceToClick());
		}
	});

	private GisLayer touchedLayer;

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

			for (GisLayer layerO:myLayers) {
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

				if (!bags.isEmpty()) {
					for (String key:bags.keySet()) {
						Set<GisFilter> filters = filterMap.get(key);
						Set<GisObject> bagOfObjects = bags.get(key);
						//Log.d("vortex","Found "+gisObjects.size()+" objects");
						Iterator<GisObject> iterator = bagOfObjects.iterator();
						while (iterator.hasNext()) {
							GisObject go = iterator.next();
							if (go instanceof GisPointObject) {
								GisPointObject gop = (GisPointObject)go;
								//Skip the the touched
								if (touchedGop!=null&&gop.equals(touchedGop))
									continue;
								int[] xy = null;
								//Use buffered value if available.
								xy = gop.getTranslatedLocation();
								if (xy==null) { 
									Location l = gop.getLocation();
									if (l==null) {
										Log.e("vortex","Gop is missing geo location!");
										iterator.remove();
										continue;
									} 
									xy = translateMapToRealCoordinates(l);
									if (xy==null) {
										//If not on map, remove it.
										if (!gop.isDynamic())
											iterator.remove();
										else {
											//if user is outside map, remove usergop to prevent centering outside map image.
											if (gop.equals(userGop)) {
												myMap.showCenterButton(false);
												userGop=null;
											}
										}
										continue;
									} else {
										if (gop.isUser()) {
											userGop = gop;
											myMap.showCenterButton(true);
										}
										gop.setTranslatedLocation(xy);
									}
								}
								//only allow clicks if something is not already touched.
								pXR = this.getImageWidth()/photoMetaData.getWidth();
								pYR = this.getImageHeight()/photoMetaData.getHeight();


								Bitmap bitmap = gop.getIcon();
								float radius = gop.getRadius();
								String color = gop.getColor();
								Style style = gop.getStyle();
								boolean isCircle = gop.isCircle();
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
												isCircle = filter.isCircle();
											}
										} else
											Log.d("vortex","Filter turned off!");
									}
								} 
								drawGop(canvas,bitmap,radius,color,style,isCircle,xy,adjustedScale);

								if (layerO.showLabels()) {
									drawGopLabel(canvas,xy,gop.getLabel(),gop.getRadius(),bCursorPaint,txtPaint);
								}

							} else if (go instanceof GisMultiPointObject) {
								//Log.d("vortex","Drawing multipoint!!");
								GisMultiPointObject gop = (GisMultiPointObject)go;
								List<Location> ll = go.getCoordinates();
								if (ll!=null) {
									if (gop.isLineString()) {
										//Log.d("vortex","Drawing linestring!!");
										int[] xy = new int[2];
										if (ll.size()==1) {
											xy = translateMapToRealCoordinates(ll.get(0));
											if (xy==null)
												continue;
											else
												canvas.drawCircle((float)xy[0],(float)xy[1],2,paintSimple);
										} else {
											drawGop(canvas,layerO,go,false);
										}
									} else {
										//Multipoints are not selectable. Only draw the dots.
										for (Location l:ll) {
											int[] xy = translateMapToRealCoordinates(l);
											if (xy==null)
												break;
											Rect r = new Rect();
											r.set(xy[0]-10, xy[1]-10, xy[0]+10, xy[1]+10);
											canvas.drawRect(r, blCursorPaint);
										}
									}
								}

							} else if (go instanceof GisPolygonObject) {
								//Log.d("vortex","Drawing Polygons!!");
								GisPolygonObject gop = (GisPolygonObject)go;
								Map<String, List<Location>> polys = gop.getPolygons();

								for (List<Location> poly:polys.values()) {
									int[] xy;
									if (poly.size()==1) {
										xy = translateMapToRealCoordinates(poly.get(0));
										if (xy==null)
											continue;
										else
											canvas.drawCircle((float)xy[0],(float)xy[1],5,paintSimple);
									} else {								

										drawGop(canvas,layerO,go,false);


									}
									//canvas.drawPath(p, createPaint(gop.getColor(),gop.getStyle()));
								}
							}
							
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
				for (GisLayer layer:myLayers) {
					touchedBag = layer.getBagContainingGo(touchedGop);
					if (touchedBag!=null) 
						touchedLayer = layer;
				}
				//if longclick, open the actionbar menu.
				if (!clickWasShort)
					myMap.startActionModeCb();
				else {
					myMap.setVisibleAvstRikt(true);
					myMap.setSelectedObjectText(touchedGop.getLabel());
					displayDistanceAndDirection();
					if (riktLinjeStart!=null)
						canvas.drawLine(riktLinjeStart[0], riktLinjeStart[1], riktLinjeEnd[0],riktLinjeEnd[1],fgPaintSel);//fgPaintSel
				}
				candidates.clear();

			} 

			if (touchedGop!=null) {
				drawGop(canvas,touchedLayer,touchedGop,true);

			} 
		} catch(Exception e) {
			LoggerI o = GlobalState.getInstance().getLogger();
			o.addRow("");
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);		
			o.addRedText(sw.toString());
			e.printStackTrace();
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
		int[] xy;
		//will only be called from here if selected.
		if (go instanceof GisPointObject) {
			GisPointObject gop = (GisPointObject)go;
			xy = translateMapToRealCoordinates(go.getLocation());
			drawGop(canvas, null,gop.getRadius(), "red", Style.FILL, gop.isCircle(), xy,1);
			if (layerO.showLabels()||showLabelForAWhile)
				drawGopLabel(canvas,xy,gop.getLabel(),gop.getRadius(),wCursorPaint,selectedPaint);

			//Other objects might or might not be selected.
		} else {
			List<Location> ll = go.getCoordinates();
			boolean first = true;
			//String path="";
			Path p = new Path();
			if (ll!=null) {
				for (Location l:ll) {
					xy = translateMapToRealCoordinates(l);
					if (xy==null)
						continue;
					if (first) {
						p.moveTo(xy[0],xy[1]);
						first =false;
					} else
						p.lineTo(xy[0],xy[1]);
				}
			}

			//Add glow effect if it is currently being drawn.
			boolean beingDrawn = go.equals(newGisObj);

			if (go instanceof GisPolygonObject && !beingDrawn)
				p.close();
			if (selected) {
				canvas.drawPath(p, paintBlur);
				canvas.drawPath(p, paintSimple);
				if (layerO.showLabels()||showLabelForAWhile) {
					xy = translateMapToRealCoordinates(go.getLocation());
					drawGopLabel(canvas,xy,go.getLabel(),0,wCursorPaint,selectedPaint);
				}
			}
			else if (beingDrawn) {
				canvas.drawPath(p, polyPaint);
				
			} else {
				String color = colorShiftOnStatus(go.getStatusVariable());
				if (color==null) 
					color = go.getColor();
				canvas.drawPath(p, createPaint(color,Paint.Style.STROKE,0));
				if (layerO.showLabels()) {
					xy = translateMapToRealCoordinates(go.getLocation());
					if (xy!=null)
						drawGopLabel(canvas,xy,go.getLabel(),0,bCursorPaint,txtPaint);
				}
				
			}
		}


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

	private void drawGop(Canvas canvas, Bitmap bitmap, float radius, String color, Style style, boolean isCircle, int[] xy, float adjustedScale) {
		Rect r = new Rect();
		//pXR = this.getImageWidth()/photoMetaData.getWidth();		
		//radius = Math.max(5, (int)(radius*pXR));
		//Log.d("vortex","PXR: Radius:"+pXR+","+radius);
		if (bitmap!=null) {
			//Log.d("vortex","bitmap! "+gop.getLabel());
			r.set(xy[0]-32, xy[1]-32, xy[0], xy[1]);
			canvas.drawBitmap(bitmap, null, r, null);
		} //circular?

		else if(isCircle) {

			//Log.d("vortex","x,y,r"+xy[0]+","+xy[1]+","+radius);
			canvas.drawCircle(xy[0], xy[1],radius, createPaint(color,style));
		} 
		//no...square.
		else {
			//Log.d("vortex","rect!");
			int diam = (int)(radius/2);
			r.set(xy[0]-diam, xy[1]-diam, xy[0]+diam, xy[1]+diam);
			canvas.drawRect(r, createPaint(color,style));
		}
	}

	//0 = distance, 1=riktning.


	public void unSelectGop() {
		touchedGop=null;
		showLabelForAWhile=true;
		myMap.setVisibleAvstRikt(false);
		invalidate();
	}

	boolean drawLine=true;


	//save the position where the user pressed start. 
	int[] riktLinjeStart,riktLinjeEnd;
	final static int TimeOut = 30;

	private void displayDistanceAndDirection() {
		final int interval = 250; 

		if (handler==null) {
			drawLine=true;
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

		long timeDiff;
		long ct;
		if (mostRecentGPSValueTimeStamp!=-1) {
			ct = System.currentTimeMillis();
			timeDiff = (ct-mostRecentGPSValueTimeStamp)/1000;			
		} else {
			myMap.setAvstTxt("No Value");
			//myMap.setRiktTxt(spinAnim());
			return;
		}
		boolean old = timeDiff>TimeOut; 
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
		myMap.setAvstTxt((old?timeDiff+"s:":"")+(dist>9999?(dist/1000+"km"):(dist+"m")));
		myMap.setRiktTxt(rikt+Deg);
		if (drawLine) {
			riktLinjeStart  = translateMapToRealCoordinates(new SweLocation(mX,mY));
			riktLinjeEnd  = translateMapToRealCoordinates(touchedGop.getLocation());
			if (riktLinjeStart!=null) {
				drawLine=false;
				invalidate();
			}
		}



	}

	private int spinState=0;
	private String spinAnim() {
		final String[] spinSymbols = new String[] {"|","/","-","\\"};
		String r = spinSymbols[spinState];
		spinState++;
		if (spinState == spinSymbols.length)
			spinState=0;
		return r;
	}

	private Paint newTextPaint(int height) {
		Paint p = new Paint();
		p.setColor(Color.WHITE);
		p.setStyle(Style.STROKE);
		p.setTextSize(height);
		return p;
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


	List<GisLayer> myLayers=new ArrayList<GisLayer>();

	public void addLayer(GisLayer layer) {
		if(layer!=null) {
			Log.d("vortex","Succesfully added layer");
			myLayers.add(layer);
		}

	}

	public GisLayer getLayer(String identifier) {
		if (myLayers==null||myLayers.isEmpty()||identifier==null)
			return null;
		for (GisLayer gl:myLayers) {
			if (gl.getId().equals(identifier))
				return gl;
		}
		return null;
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
					// TODO Auto-generated method stub

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
			int[] xy = translateMapToRealCoordinates(userGop.getLocation());
			if (xy!=null) {
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
		float Scales = scale * scaleAdjust;


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
	
	/*
	public Rect getCurrentViewSizeP(float wf,float hf) {
		final float w = 600;
		final float h = 920;

		float ws = this.getScaledWidth();
		float hs = this.getScaledHeight();

		float sx  = ws/wf;
		float sy  = hs/hf;

		float xf = x/sx;
		float yf = y/sy;

		//float w = this.getImageWidth();
		//float h = this.getImageHeight();

		float adjustedScale = scale * scaleAdjust;
		Log.d("vortex","scale: "+scaleAdjust);

		Log.d("vortex","wf hf ws hs "+wf+","+hf+","+ws+","+hs);
		Log.d("vortex","x y fixedX*scale fixedY*scale"+x+","+y+","+fixedX*adjustedScale+","+fixedY*adjustedScale);
		//float x0 = x-fixedX*scale;
		//float y0 = y-fixedY*scale;
		Log.d("vortex","sx sy xf yf w h "+sx+","+sy+","+xf+","+yf+","+w+","+h);

		int top    = (int)Math.max( 0, hf / 2 - (h / 2 / sy) + yf );
		int bottom = (int)Math.min( hf,hf / 2 + (h / 2 / sy) + yf );
		int left   = (int)Math.max( 0, wf / 2 - (w / 2 / sx) - xf );
		int right  = (int)Math.min( wf,wf / 2 + (w / 2 / sx) - xf );

		Log.d("vortex","top bottom left right "+top+","+bottom+","+left+","+right);

		Rect r = new Rect(left,top,right,bottom);

		return r;
	}

	public Rect getCurrentViewSizeOld(float fullW,float fullH) {
		Rect r = new Rect();
		Log.d("vortex","scalew scaleh imgw imgh"+ this.getScaledWidth()+","+this.getScaledHeight()+","+this.getImageWidth()+","+this.getImageHeight());
		float[] topCorner = translateToReal(0, 0);
		float[] bottomCorner = translateToReal(this.getImageWidth(), this.getImageHeight());
		Log.d("vortex","TopX "+ topCorner[0]+", TopY "+topCorner[1]);//+","+this.getImageWidth()+","+this.getImageHeight());

		topCorner[0] = this.getImageWidth()/2+((float)topCorner[0]);
		topCorner[1] = this.getImageHeight()/2+((float)topCorner[1]);
		bottomCorner[0] = this.getImageWidth()/2+((float)bottomCorner[0]);
		bottomCorner[1] = this.getImageHeight()/2+((float)bottomCorner[1]);

		float left,right,top,bottom;

		left = topCorner[0];
		right = bottomCorner[0];
		top = topCorner[1];
		bottom = bottomCorner[1];
		double N,E,S,W;
		Location topCornerL = this.translateRealCoordinatestoMap(topCorner);
		Location bottomCornerL = this.translateRealCoordinatestoMap(bottomCorner);
		N = topCornerL.getY();
		W = topCornerL.getX();
		E = bottomCornerL.getX();
		S = bottomCornerL.getY();
		Log.d("vortex","Real coordinates for this slice (NESW) : "+N+","+E+","+S+","+W);
		PhotoMeta pm = new PhotoMeta(N,E,S,W);
		//Translate to coordinates in large image. 
		float Px = fullW/this.getScaledWidth();
		float Py = fullH/this.getScaledHeight();
		r.left=(int)(left*Px);
		r.right= (int)(right*Px);
		r.top=(int)(top*Py);
		r.bottom=(int)(bottom*Py);
		Log.d("vortex","fullw fullh rl rr rt rb px py"+fullW+","+fullH+","+r.left+","+r.right+","+r.top+","+r.bottom+","+Px+","+Py);


		return r;
	}
*/

	

	


}
