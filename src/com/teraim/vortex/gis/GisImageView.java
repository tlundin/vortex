package com.teraim.vortex.gis;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;

import com.teraim.vortex.dynamic.types.PhotoMeta;
import com.teraim.vortex.dynamic.types.Point;
import com.teraim.vortex.loadermodule.configurations.GisPolygonConfiguration;
import com.teraim.vortex.loadermodule.configurations.GisPolygonConfiguration.GisBlock;
import com.teraim.vortex.loadermodule.configurations.GisPolygonConfiguration.SweRefCoordinate;
import com.teraim.vortex.non_generics.Constants;

public class GisImageView extends GestureImageView {

	private Paint       wCursorPaint,bCursorPaint,markerPaint ;
	//private Path    	mPath;
	private float mX, mY;
	private static final float TOUCH_TOLERANCE = 4;
	private List<Point> myPoints;
	private List<Poly> myPaths; 
	private Calendar calendar = Calendar.getInstance();
	private Paint currCursorPaint,mPaint,txtPaint;
	private Handler handler;
	private boolean drawActive = false;
	private int rNum =1;
	private Paint borderPaint;

	public GisImageView(Context context) {
		super(context);
		init();
	}

	public GisImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public GisImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		//used for cursor blink.
		calendar.setTime(new Date());


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
		txtPaint.setTextSize(25);
		txtPaint.setColor(Color.WHITE);
		txtPaint.setStyle(Paint.Style.STROKE);
		txtPaint.setTextAlign(Paint.Align.CENTER);

		borderPaint = new Paint();
		borderPaint.setColor(Color.WHITE);
		borderPaint.setStyle(Paint.Style.STROKE);
		borderPaint.setStrokeWidth(3);
		
		currCursorPaint = wCursorPaint;

		myPoints = new ArrayList<Point>();
		myPaths = Collections.synchronizedList(new ArrayList<Poly>());

		fixScale = scale * scaleAdjust;
		Log.d("vortex","Fixscale is "+fixScale);

		//make sure cursor blinks.
		final int interval = 1000; // 1 Second
		handler = new Handler();
		Runnable runnable = new Runnable(){
			public void run() {
				currCursorPaint = currCursorPaint.equals(wCursorPaint)?bCursorPaint:wCursorPaint;
				invalidate();
				handler.postDelayed(this, interval);

			}
		};

		handler.postDelayed(runnable, interval);

	}

	private Paint createNewPaint() {
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setColor(nextColor());
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(10); 
		return mPaint;
	}

	float fixScale;
	private float fixedX;
	private float fixedY;
	private float cX;
	private float cY;
	PhotoMeta gisImage;
	//difference in % between ruta and image size.
	private float rXRatio,rYRatio;
	
	
	
	public void setGisData(PhotoMeta gd, String rutaId) {
		GisPolygonConfiguration gp = GisPolygonConfiguration.getSingleton();
		if (gp == null) {
			Log.e("vortex","missing gis polygon blocks");
			return;
		}
		List<GisBlock> mBlocks = gp.getBlocks(rutaId);
		gisImage = gd;
		//calculate ratio between grid and ruta size.
		rXRatio = (Constants.RUTA_SIZE-gd.getWidth())/gd.getWidth();
		rYRatio = (Constants.RUTA_SIZE-gd.getHeight())/gd.getHeight();
		Log.d("vortex","Calling.... "+rXRatio+","+rYRatio);
		
		//diff between real size and pixel size.
		float pXR = this.getWidth()/gd.getWidth();
		float pYR = this.getHeight()/gd.getHeight();
		
		//Translate into a list of Path objects.
		if (mBlocks!=null) {
			for (GisBlock bl:mBlocks) {
				List<List<SweRefCoordinate>> pl = bl.polygons;
				for (List<SweRefCoordinate> sl:pl) {
					Path mPath = new Path();
					SweRefCoordinate swe;
					Poly p=null;
					for (int i=0;i<sl.size();i++) {
						swe = sl.get(i);	
						Log.d("vortex","Coord x: Pic Left:"+swe.E+","+gd.left);
						float x = (swe.E-gd.left)*pXR;
						float y = (swe.N-gd.top)*pYR;
						if (i==0) 
							p = new Poly(mPath,x,y);
						else
							mPath.lineTo(x, y);
						Log.d("vortex","l�gger till x,y "+x+","+y);
					}
					if (p!=null) {
						p.save();
						//myPaths.add(new Poly(mPath,x,y));
					}
				}
			}
		}
	
	}
	
	public void startPoly() {
		fixScale = scale * scaleAdjust;
		Path mPath = new Path();
		mPath.reset();
		//Calculate the dots on the map
		cX = calcX(polyVertexX);
		cY = calcY(polyVertexY);
		mPath.moveTo(cX, cY);		
		mX = polyVertexX;
		mY = polyVertexY;
		Point p = new Point(cX,cY);
		//myPoints = new ArrayList<Point>();
		myPoints = new ArrayList<Point>();
		myPoints.add(p);
		myPaths.add(new Poly(mPath,cX,cY));
		drawActive=true;
		invalidate();
	}


	public String checkForTargets() {
		//draws cursor at current location.
		fixedX = x;
		fixedY = y;
		fixScale = scale * scaleAdjust;
		
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


	public void addVertex() {
		float dx = Math.abs(polyVertexX - mX);
		float dy = Math.abs(polyVertexY - mY);
		//if difference between two presses is small, discard it.
		if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
			cX = calcX(polyVertexX);
			cY = calcY(polyVertexY);
			//Save these for diff calculation.
			mX = polyVertexX;
			mY = polyVertexY;
			myPaths.get(myPaths.size()-1).getPath().lineTo(cX, cY);
			//mPath.quadTo(mX, mY, (polyVertexX + mX)/2, (polyVertexY + mY)/2);
			Point p = new Point(cX,cY);
			myPoints.add(p);
			invalidate();
		} else
			Log.d("vortex"," failed on diff");

	}


	int colorCount = 0;
	int[] myColors = {Color.BLUE,Color.YELLOW,Color.RED,Color.GREEN,Color.WHITE,Color.CYAN};
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



	@Override
	protected void dispatchDraw(Canvas canvas) {
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
		return (mx-x)*1/fixScale;
	}
	private float calcY(float my) {
		return (my-y)*1/fixScale;
	}

	private float fCalcX(float mx) {
		return (mx-fixedX)*1/fixScale;
	}
	private float fCalcY(float my) {
		return (my-fixedY)*1/fixScale;
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
			myPaint = createNewPaint();
			bounds = new Rect();
			setLabel("Poly "+rNum++);

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
		}
		
		public boolean isComplete() {
			return isReady;
		}

		private Rect getRect() {
			return bounds;
		}
	}

}
