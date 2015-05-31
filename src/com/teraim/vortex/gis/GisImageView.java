package com.teraim.vortex.gis;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.Start;
import com.teraim.vortex.dynamic.types.GisLayer;
import com.teraim.vortex.dynamic.types.Location;
import com.teraim.vortex.dynamic.types.PhotoMeta;
import com.teraim.vortex.dynamic.types.Point;
import com.teraim.vortex.dynamic.types.SweLocation;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.types.Workflow;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisFilter;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisMultiPointObject;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisObject;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisPointObject;
import com.teraim.vortex.dynamic.workflow_realizations.gis.GisPolygonObject;
import com.teraim.vortex.dynamic.workflow_realizations.gis.WF_Gis_Map;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.non_generics.NamedVariables;
import com.teraim.vortex.utils.Geomatte;
import com.teraim.vortex.utils.RuleExecutor;
import com.teraim.vortex.utils.RuleExecutor.SubstiResult;
import com.teraim.vortex.utils.RuleExecutor.TokenType;
import com.teraim.vortex.utils.RuleExecutor.TokenizedItem;

public class GisImageView extends GestureImageView {

	private Paint       wCursorPaint,bCursorPaint,markerPaint ;
	//private Path    	mPath;
	private float mX, mY;
	private static final float TOUCH_TOLERANCE = 4;
	private List<Point> myPoints;
	//private List<Poly> myPaths; 
	private Calendar calendar = Calendar.getInstance();
	private Paint currCursorPaint,mPaint,txtPaint;
	private Handler handler;
	private boolean drawActive = false;
	private int rNum =1;
	private Paint borderPaint;
	private Context ctx;
	private Paint polyPaint;

	private int colorCount = 0;
	private int[] myColors = {Color.BLUE,Color.YELLOW,Color.RED,Color.GREEN,Color.WHITE,Color.CYAN};
	//private List<WF_MapLayer> mapLayers;
	private PhotoMeta photoMetaData;
	private Paint rCursorPaint;
	private Paint blCursorPaint;
	private Paint btnTxt,vtnTxt;
	private Paint grCursorPaint;
	private final Map<String,String>YearKeyHash = new HashMap<String,String>();
	

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
		polyPaint.setStrokeWidth(1);
		currCursorPaint = wCursorPaint;
		myPoints = new ArrayList<Point>();
		//myPaths = Collections.synchronizedList(new ArrayList<Poly>());
		fixScale = scale * scaleAdjust;
		Log.d("vortex","Fixscale is "+fixScale);

		setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				checkForTargets(polyVertexX,polyVertexY);
			}
		});



		//make sure cursor blinks.
		/*final int interval = 1000; // 1 Second
		handler = new Handler();
		Runnable runnable = new Runnable(){
			public void run() {
				currCursorPaint = currCursorPaint.equals(wCursorPaint)?bCursorPaint:wCursorPaint;
				invalidate();
				handler.postDelayed(this, interval);

			}
		};

		handler.postDelayed(runnable, interval);
		 */
	}
	double pXR,pYR;
	private WF_Gis_Map myMap;
	public void initialize(WF_Gis_Map wf_Gis_Map, PhotoMeta pm) {
		mapLocationForClick=null;
		fixScale = scale * scaleAdjust;
		this.photoMetaData=pm;
		pXR = this.getImageWidth()/pm.getWidth();
		pYR = this.getImageHeight()/pm.getHeight();
		fixedX = x;
		fixedY = y;
		myMap = wf_Gis_Map;
		imgHReal = pm.N-pm.S;
		imgWReal = pm.E-pm.W;
	}
	/*
	private Paint createNewPaint() {
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setColor(nextColor());
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(1); 
		return mPaint;
	}
	 */
	float fixScale;
	private float fixedX;
	private float fixedY;

	PhotoMeta gisImage;
	//difference in % between ruta and image size.
	private float rXRatio,rYRatio;
	private Location mapLocationForClick=null;
	private float[] clickXY;
	private double imgHReal;
	private double imgWReal;
	/*
	private int[] translateMapToRealCoordinates(float scale, Location l) {
		Log.d("vortex","w h of gis image. w h of image ("+photoMetaData.getWidth()+","+photoMetaData.getHeight()+") ("+this.getImageWidth()+","+this.getImageHeight()+")");
		final float pXR = this.getImageWidth()/photoMetaData.getWidth()*fixScale;
		final float pYR = this.getImageHeight()/photoMetaData.getHeight()*fixScale;
		Log.d("vortex","px, py"+pXR+","+pYR);
		int x = ((int)calcX((float)((l.getX()-photoMetaData.left)*pXR)));

		int y = ((int)calcY((float)((photoMetaData.top-l.getY())*pYR)));
		Log.d("vortex","lägger till x,y "+x+","+y);

		return new int[]{x,y};
	}
	 */
	private float[] translateToReal(float mx,float my) {
		float fixScale = scale * scaleAdjust;
		//Log.d("vortex","MX, MY "+mx+","+my);
		Log.d("vortex","W, H "+this.getImageWidth()+","+this.getImageHeight());
		Log.d("vortex","fixscale: "+fixScale);
		mx = (mx-x)/fixScale;//+(this.getImageWidth()/2)/fixScale;
		my = (my-y)/fixScale;//+(this.getImageHeight()/2)/fixScale;
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


	private int[] translateMapToRealCoordinates(float scale, Location l) {

		double mapDistX = l.getX()-photoMetaData.W;
		if (mapDistX <=imgWReal && mapDistX>=0)
			;//Log.d("vortex","Distance X in meter: "+mapDistX+" [inside]");
		else {
			
			Log.e("vortex","Distance X in meter: "+mapDistX+" [outside!]");
			Log.d("vortex","w h of gis image. w h of image ("+photoMetaData.getWidth()+","+photoMetaData.getHeight()+") ("+this.getScaledWidth()+","+this.getScaledHeight()+")");
			Log.d("vortex","photo (X) "+photoMetaData.W+"-"+photoMetaData.E);
			Log.d("vortex","photo (Y) "+photoMetaData.S+"-"+photoMetaData.N);
			Log.d("vortex","object X,Y: "+l.getX()+","+l.getY());
			 
			return null;
		}
		double mapDistY = l.getY()-photoMetaData.S;
		if (mapDistY <=imgHReal && mapDistY>=0)
			;//Log.d("vortex","Distance Y in meter: "+mapDistY+" [inside]");
		else {
			Log.e("vortex","Distance Y in meter: "+mapDistY+" [outside!]");
			Log.d("vortex","w h of gis image. w h of image ("+photoMetaData.getWidth()+","+photoMetaData.getHeight()+") ("+this.getScaledWidth()+","+this.getScaledHeight()+")");
			Log.d("vortex","photo (X) "+photoMetaData.W+"-"+photoMetaData.E);
			Log.d("vortex","photo (Y) "+photoMetaData.S+"-"+photoMetaData.N);
			Log.d("vortex","object X,Y: "+l.getX()+","+l.getY());
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
		//float rX = calcX(sX);
		//float rY = calcY(sY);
		//		Log.d("vortex","X: Y:"+x+","+y);
		//		Log.d("vortex","fixScale: "+fixScale);
		//		Log.d("vortex","after calc(x), calc(y) "+rX+","+rY);
		//		Log.d("vortex","fX: fY:"+fixedX+","+fixedY);
		//		Log.d("vortex","after fcalc(x), fcalc(y) "+this.fCalcX(rX)+","+fCalcY(rY));
		return new int[]{(int)rX,(int)rY};

	}
	/*
	public void setGisData(PhotoMeta gd, String rutaId) {
		GisPolygonConfiguration gp = GisPolygonConfiguration.getSingleton();
		if (gp == null) {
			Log.e("vortex","missing gis polygon blocks");
			return;
		}
		fixScale = scale * scaleAdjust;
		float margin = 0;
		List<GisBlock> mBlocks = gp.getBlocks(rutaId);
		gisImage = gd;
		//calculate ratio between grid and ruta size.
		Log.d("vortex","Calling.... "+rXRatio+","+rYRatio);

		//diff between real size and pixel size.
		Log.d("vortex","w h of gis image. w h of image ("+gd.getWidth()+","+gd.getHeight()+") ("+this.getImageWidth()+","+this.getImageHeight()+")");

		float mpXR = (float)pXR*fixScale;
		float mpYR = (float)pYR*fixScale;

		//Translate into a list of Path objects.
		if (mBlocks!=null) {
			for (GisBlock bl:mBlocks) {
				List<List<SweRefCoordinate>> pl = bl.polygons;
				for (List<SweRefCoordinate> sl:pl) {
					SweRefCoordinate swe;
					for (int i=0;i<sl.size();i++) {
						swe = sl.get(i);	
						float x =(float)( (swe.E-gd.W)*pXR);
						float y =(float)( (gd.S-swe.N)*pYR+margin);
						if (i==0) {
							startPoly(x,y);
						}
						else {
							this.addVertex(x, y);
							//Log.d("vortex","lägger till x,y "+x+","+y);
						}
					}
					this.savePoly();
				}
			}
		}

	}

	 */
	/*
	public void startPoly() {
		startPoly(polyVertexX,polyVertexY);
	}

	private void startPoly(float px,float py) {
		fixScale = scale * scaleAdjust;
		Path mPath = new Path();
		mPath.reset();
		//Calculate the dots on the map
		float cX = calcX(px);
		float cY = calcY(py);
		mPath.moveTo(cX, cY);		
		mX = px;
		mY = py;
		Point p = new Point(cX,cY);
		//myPoints = new ArrayList<Point>();
		myPoints = new ArrayList<Point>();
		myPoints.add(p);
		myPaths.add(new Poly(mPath,cX,cY));
		drawActive=true;
		invalidate();
	}
	 */

	//Determine if something clicked. If so, open a something dialog.


	public void checkForTargets(float x, float y) {
		//Figure out geo coords from pic coords.
		clickXY=translateToReal(x,y);
		mapLocationForClick = translateRealCoordinatestoMap(clickXY);
		Log.d("vortex","click at "+mapLocationForClick.getX()+","+mapLocationForClick.getY());
		//Check if button is up & clicked.
		if (wfButtonClicked()) {
			GlobalState.getInstance().setKeyHash(getClickedObject().getKeyHash());

			Log.d("vortex","Setting current keyhash to "+getClickedObject().getKeyHash());
			String target = getClickedObject().getWorkflow();
			if (target ==null) {
				GlobalState.getInstance().getLogger().addRow("");
				GlobalState.getInstance().getLogger().addRedText("Target workflow is missing for clicked object");
				Log.e("vortex","missing click target workflow");
			} else {
				Workflow wf = GlobalState.getInstance().getWorkflow(target);
				Variable statusVariable = getClickedObject().getStatusVariable();
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
					
				} else
					Log.e("vortex","statusvariable missing!!");
				
				Start.singleton.changePage(wf,statusVarS);

			}
		}




		this.invalidate();
	}

	//List<float[]> plicked=null;
	/*
	public void checkForTargets(float x, float y) {
		//Figure out geo coords from pic coords.

		if (plicked==null)
			plicked = new ArrayList<float[]>();
		plicked.add(this.translate(x, y));
		this.invalidate();
	}*/
	/*	public String checkForTargetsOld() {
		//draws cursor at current location.
		fixedX = x;
		fixedY = y;
		fixScale = scale * scaleAdjust;
		(polyVertexY-x)/fixScale;
		(polyVertexY-y)/fixScale;
	private float calcX(polyVertexX) {
		return (polyVertexY-x)/fixScale;
	}
	private float calcY(polyVertexY) {
		return (polyVertexY-y)/fixScale;
	}

		//check if any current Poly Label is within click.

		for (Poly p:myPaths) {
			if (!p.isComplete())
				continue;
			Rect r = new Rect(p.getRect());
			//size it up a bit to make it easier to hit.
			r.set(r.left-25, r.top-25, r.right+25, r.bottom+25);
			//r.offset((int)x,(int)y);
			Log.d("vertex","px "+polyVertexX+" py: "+polyVertexY);
			if (r.contains((int)calcX(polyVertexX), (int)calcY(polyVertexY))) {
				Log.d("vortex","inside "+p.getLabel());
				return p.getLabel();
			}
			else
				Log.d("vortex","outside l: "+r.left+"top: "+r.top+" right "+r.right+" bottom "+r.bottom);
		}
		Log.d("vortex","exiting checkfortargets");
		invalidate();
		return null;
	}
	 */
	/*
	public void addVertex() {
		addVertex(polyVertexX,polyVertexY);
	}
	public void addVertex(float px,float py) {
		float dx = Math.abs(px - mX);
		float dy = Math.abs(py - mY);
		//if difference between two presses is small, discard it.
		//if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
		float cX = calcX(px);
		float cY = calcY(py);
		//Save these for diff calculation.
		mX = px;
		mY = py;
		myPaths.get(myPaths.size()-1).getPath().lineTo(cX, cY);
		//mPath.quadTo(mX, mY, (polyVertexX + mX)/2, (polyVertexY + mY)/2);
		Point p = new Point(cX,cY);
		myPoints.add(p);
		invalidate();
		//} else
		//	Log.d("vortex"," failed on diff");

	}


	private int nextColor() {
		colorCount++;
		if (colorCount==myColors.length)
			colorCount=0;
		return myColors[colorCount];
	}

	public void erasePoly() {
		int c = myPaths.size();
		if (c>0) {
			synchronized(myPaths) {
				myPaths.remove(c-1);
			}
		}
		drawActive=false;

	}

	public void savePoly() {
		myPoints = new ArrayList<Point>();
		myPaths.get(myPaths.size()-1).save();
		drawActive=false;
	}
	 */


	private boolean wfButtonClicked() {
		if (bRect!=null) {			
			if (bRect.contains((int)clickXY[0], (int)clickXY[1])) {
				Log.d("vortex","Button clicked!!");
				return true;
			}
		}
		return false;
	}

	private Rect bRect = null;
	private GisPointObject clickedGisObject=null;

	private void setButtonLocation(Rect bRect,GisPointObject iWasClicked) {
		this.bRect = bRect;
		this.clickedGisObject = iWasClicked;
	}

	private GisPointObject getClickedObject() {
		return this.clickedGisObject;
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		canvas.save();
		//scale and adjust.

		float adjustedScale = scale * scaleAdjust;
		canvas.translate(x, y);
		if(adjustedScale != 1.0f) {
			canvas.scale(adjustedScale, adjustedScale);
		}
		for (String layer:myLayers.keySet()) {
			Log.d("vortex","drawing layer "+layer);
			//get all objects that should be drawn on this layer.
			GisLayer layerO = myLayers.get(layer);
			if (!layerO.isVisible()) {
				Log.d("vortex","layer not visible...skipping "+layer);
				continue;
			}
			if (layerO.hasDynamic()) {
				Log.d("vortex","dynamic obj found in "+layer);
				this.startDynamicRedraw();
			}
			Map<String, Set<GisObject>> bags = layerO.getGisBags();
			Map<String, Set<GisFilter>> filterMap = layerO.getFilters();
			if (!bags.isEmpty()) {
				GisPointObject touchedGop = null;
				for (String key:bags.keySet()) {
					Set<GisFilter> filters = filterMap.get(key);
					Set<GisObject> gisObjects = bags.get(key);
					Log.d("vortex","Found "+gisObjects.size()+" objects");
					for (GisObject go:gisObjects) {
						boolean isTouched=false;


						if (go instanceof GisPointObject) {
							GisPointObject gop = (GisPointObject)go;
							Location l = gop.getLocation();
							if (l!=null) {
								int[] xy = translateMapToRealCoordinates(adjustedScale,l);
								if (xy==null) {
									continue;
								}
								if (mapLocationForClick!=null && touchedGop ==null && gop.isTouchedByClick(mapLocationForClick,pXR,pYR)) {
									touchedGop = gop;
									isTouched=true;
								}
								Bitmap bitmap = gop.getIcon();
								float radius = gop.getRadius();
								String color = gop.getColor();
								Style style = gop.getStyle();
								boolean isCircle = gop.isCircle();
								
								Variable statusVar = gop.getStatusVariable();
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
												if (result!=null&&!result.equals("0")) 
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
								Rect r = new Rect();

								if (bitmap!=null) {
									Log.d("vortex","bitmap!");
									r.set(xy[0]-32, xy[1]-32, xy[0], xy[1]);
									canvas.drawBitmap(bitmap, null, r, null);
								} //circular?
								else if(isCircle) {
									//Log.d("vortex","x,y,r"+xy[0]+","+xy[1]+","+radius);
									canvas.drawCircle(xy[0], xy[1], radius, !isTouched?createPaint(color,style):rCursorPaint);
								} //no...square.
								else {
									//Log.d("vortex","rect!");
									int diam = (int)radius;
									r.set(xy[0]-diam, xy[1]-diam, xy[0]+diam, xy[1]+diam);
									canvas.drawRect(r, createPaint(color,style));
								}
								if (layerO.showLabels()&&!isTouched) {
									String mLabel = gop.getLabel();
									Rect bounds = new Rect();
									txtPaint.getTextBounds(mLabel, 0, mLabel.length(), bounds);
									bounds.set(bounds.left-2,bounds.top-2,bounds.right+2,bounds.bottom+2);
									bounds.offset((int)xy[0]-bounds.width()/2,(int)xy[1]-(bounds.height()/2+(int)radius));
									canvas.drawRect(bounds, bCursorPaint);
									canvas.drawText(mLabel, xy[0], (int)xy[1]-(bounds.height()/2+(int)radius),txtPaint);								
								}

								isTouched=false;

							}
						} else if (go instanceof GisMultiPointObject) {
							//Log.d("vortex","Drawing multipoint!!");
							GisMultiPointObject gop = (GisMultiPointObject)go;
							List<Location> ll = go.getCoordinates();
							if (ll!=null) {
								if (gop.isLineString()) {
									Log.d("vortex","Drawing linestring!!");
									boolean first=true;
									Path p = new Path();
									for (Location l:ll) {
										int[] xy = translateMapToRealCoordinates(adjustedScale,l);
										if (xy==null)
											continue;
										else
											Log.d("vortex","not outside!!");
										if (first) {
											p.moveTo(xy[0],xy[1]);
											first =false;
										} else
											p.lineTo(xy[0],xy[1]);
									}
									canvas.drawPath(p, rCursorPaint);

								} else {

									for (Location l:ll) {
										int[] xy = translateMapToRealCoordinates(adjustedScale,l);
										if (xy==null)
											break;
										Rect r = new Rect();
										r.set(xy[0]-10, xy[1]-10, xy[0]+10, xy[1]+10);
										canvas.drawRect(r, blCursorPaint);
									}
								}
							}

						} else if (go instanceof GisPolygonObject) {
							Log.d("vortex","Drawing Polygons!!");
							GisPolygonObject gop = (GisPolygonObject)go;
							Map<String, List<Location>> polys = gop.getPolygons();

							for (List<Location> poly:polys.values()) {
								String path="";
								Path p = new Path();
								int[] xy;
								boolean first=true;
								for (Location l:poly) {
									xy = translateMapToRealCoordinates(adjustedScale,l);
									if (xy==null)
										break;;
										path+="{"+xy[0]+","+xy[1]+"}";
										if (first) {
											p.moveTo(xy[0],xy[1]);
											first =false;
										} else
											p.lineTo(xy[0],xy[1]);	
								}
								//p.close();
								Log.d("vortex","PATH: "+path);
								canvas.drawPath(p, createPaint(gop.getColor(),gop.getStyle()));
							}
						}
					}
				}
				if (touchedGop!=null) {
					String mLabel = touchedGop.getLabel();
					Location l = touchedGop.getLocation();
					int[] xy = translateMapToRealCoordinates(adjustedScale,l);
					String btnT = "Kör flöde >>";
					Rect boundsR = new Rect(),distR = null,riktR=null,bothR;
					btnTxt.getTextBounds(mLabel, 0, mLabel.length(), boundsR);
					
					int[] dr = getDistanceAndDirectionToUser(touchedGop);
					if (dr!=null) {
						int distance = dr[0];
						int rikt = dr[1];
						String distS="Distans: "+Integer.toString(distance);
						String riktS="Riktning: "+Integer.toString(rikt);
						distR = new Rect();
						riktR = new Rect();
						
						vtnTxt.getTextBounds(riktS, 0, riktS.length(), riktR);
						btnTxt.getTextBounds(distS, 0, distS.length(), distR);
						

						int left=riktR.left,right=riktR.right;
						if (distR.width()>riktR.width()) {
							left=distR.left;
							right=distR.right;
						}
						bothR = new Rect(left, riktR.top, right, riktR.bottom*2+10);

						bothR.set(bothR.left-2,bothR.top-2,bothR.right+2,bothR.bottom+2);
						bothR.offset((int)xy[0],(int)xy[1]-(int)(bothR.height()+touchedGop.getRadius()));
						canvas.drawRect(bothR, blCursorPaint);
						canvas.drawText(riktS, bothR.centerX(), xy[1]-(int)(bothR.height()+touchedGop.getRadius()), vtnTxt);
						canvas.drawText(distS, bothR.centerX(), xy[1]-(int)(bothR.height()/2+touchedGop.getRadius()), btnTxt);
					}
					
					boundsR.offset((int)xy[0]-boundsR.width()/2,(int)xy[1]);
					//canvas.drawRect(bounds, blCursorPaint);
					canvas.drawText(mLabel, boundsR.left, boundsR.top, txtPaint);

					Rect bbounds = new Rect();
					btnTxt.getTextBounds(btnT, 0, btnT.length(), bbounds);
					bbounds.offset((int)xy[0]-bbounds.width()/2,(int)(xy[1]+touchedGop.getRadius()*3));
					bbounds.set(bbounds.left-2,bbounds.top-2, bbounds.right+2,bbounds.bottom+2);
					this.setButtonLocation(bbounds,touchedGop);
					canvas.drawRect(bbounds, blCursorPaint);
					canvas.drawText(btnT, xy[0], xy[1]+touchedGop.getRadius()*3, btnTxt);

				}
			}
		}

		canvas.restore();
	}
	//0 = distance, 1=riktning.
	


	private int[] getDistanceAndDirectionToUser(GisPointObject go) {
		
		GlobalState gs = GlobalState.getInstance();
		final LoggerI o = gs.getLogger();
		Variable myX = GlobalState.getInstance().getVariableConfiguration().getVariableUsingKey(YearKeyHash, NamedVariables.MY_GPS_LAT);
		Variable myY = GlobalState.getInstance().getVariableConfiguration().getVariableUsingKey(YearKeyHash, NamedVariables.MY_GPS_LONG);
		if (myX==null||myY==null) {
			Log.e("vortex","location variables for user does not exist.");			
			return null;
		} 
		if (myX.getValue()==null ||myY.getValue()==null) {
			Log.e("vortex","myX or myY saknar värde");
			o.addRow("myX or myY saknar värde");
			return null;
		}
		double mX = Double.parseDouble(myX.getValue());
		double mY = Double.parseDouble(myY.getValue());
		double gX = go.getX();
		double gY = go.getY();
		
		int dist = (int)Geomatte.sweDist(mY,mX,gY,gX);
		int rikt = (int)(Geomatte.getRikt2(mY, mX, gY, gX)*57.2957795);
		return new int[]{dist,rikt};
	}

	private Paint newTextPaint(int height) {
		Paint p = new Paint();
		p.setColor(Color.WHITE);
		p.setStyle(Style.STROKE);
		p.setTextSize(height);
		return p;
	}

	private Paint createPaint(String color, Paint.Style style) {

		Paint p = new Paint();
		p.setColor(color!=null?Color.parseColor(color):Color.YELLOW);
		p.setStyle(style!=null?style:Paint.Style.FILL);
		return p;
	}

	//@Override
	/*
	protected void dispatchDraw2(Canvas canvas) {
		super.dispatchDraw(canvas);
		//		canvas.drawCircle(0, 0, 12, bCursorPaint);
		//		canvas.drawCircle(x, y, 12, mCursorPaint);


		canvas.save();

		//scale and adjust.
		float adjustedScale = scale * scaleAdjust;
		canvas.translate(x, y);
		if(adjustedScale != 1.0f) {
			canvas.scale(adjustedScale, adjustedScale);
		}


		//Draw a small circle at the head of the current polygon being drawn.

		if (drawActive && myPoints!=null && myPoints.size()>0) {
			int s = myPoints.size()-1;
			canvas.drawCircle(myPoints.get(s).x, myPoints.get(s).y,15, markerPaint);
		} 


		if (myPaths!=null && myPaths.size()>0){

			for (Poly p:myPaths) {
				canvas.drawPath( p.getPath(),  p.getPaint());
				if (p.isComplete()) {

					canvas.drawRect(p.getRect(), bCursorPaint);
					//					if (showLabels)
					canvas.drawText(p.getLabel(),p.labelX,p.labelY, txtPaint);
				}
			}
		}

		//Draw a blinking square cursor at current location if nothing else is happening
		canvas.drawCircle((polyVertexX-fixedX)*1/fixScale,(polyVertexY-fixedY)*1/fixScale, 10, currCursorPaint);

		//Draw a square around edge of picture
		float w =(this.getImageWidth()+this.getImageWidth()*rXRatio)/2.0f;
		float h =(this.getImageHeight()+this.getImageHeight()*rXRatio)/2.0f;
		canvas.drawRect(fCalcX(-w), fCalcY(-h), fCalcX(w), fCalcY(h), borderPaint);

		//Draw the polygons for all partaking blocks, if any.

		//If person visible, draw a little figure at location.

		canvas.restore();
	}




	private float calcX(float mx) {
		return (mx-x)/fixScale;
	}
	private float calcY(float my) {
		return (my-y)/fixScale;
	}

	private float fCalcX(float mx) {
		return (mx-fixedX)/fixScale;
	}
	private float fCalcY(float my) {
		return (my-fixedY)/fixScale;
	}


	private class Poly {

		Path mPath;
		String mLabel;
		float labelX,labelY;
		Paint myPaint;
		boolean isReady =false;
		Rect bounds;
		int picW,picH;


		public Poly(Path p, float lx, float ly) {
			mPath = p;
			labelX=lx;
			labelY=ly;
			myPaint = polyPaint;
			bounds = new Rect();
			setLabel("Poly "+rNum++);

		}

		public Poly(Path p) {
			mPath = p;
			myPaint = polyPaint;
			bounds = new Rect();
		}

		private void setLabel(String label) {
			mLabel = label;
			txtPaint.getTextBounds(mLabel, 0, mLabel.length(), bounds);
			bounds.offset((int)labelX-bounds.width()/2,(int)labelY);

		}
		private Path getPath() {
			return mPath;
		}
		private String getLabel() {
			return mLabel;
		}
		private Paint getPaint() {
			return myPaint;
		}

		public void save() {
			isReady=true;
			mPath.close();
		}

		public boolean isComplete() {
			return isReady;
		}

		private Rect getRect() {
			return bounds;
		}
	}
	 */
	Map<String,GisLayer> myLayers=new HashMap<String,GisLayer>();

	public void addLayer(GisLayer layer,String identifier) {
		if(layer!=null) {
			Log.d("vortex","Succesfully added layer");
			myLayers.put(identifier,layer);
		}

	}

	public GisLayer getLayer(String identifier) {
		return myLayers.get(identifier);
	}

	private boolean isStarted=false;
	//Starts a redraw every 3rd second if at least one object is dynamic.
	private void startDynamicRedraw() {
		if (!isStarted) {
			final int interval = 3000; // 1 Second
			handler = new Handler();
			Runnable runnable = new Runnable(){
				public void run() {
					invalidate();
					handler.postDelayed(this, interval);
				}
			};

			handler.postDelayed(runnable, interval);
			isStarted=true;
		}
	}


}
