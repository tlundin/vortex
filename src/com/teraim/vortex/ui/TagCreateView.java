package com.teraim.vortex.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.teraim.vortex.dynamic.types.Delyta;
import com.teraim.vortex.dynamic.types.Point;
import com.teraim.vortex.dynamic.types.Segment;
import com.teraim.vortex.non_generics.DelyteManager.Coord;

/**
 * @author Terje
 * 
 * This class is used to draw a Provyta with all its parts (delytor)
 */
public class TagCreateView extends View {





	protected static final float DRAW_THRESHOLD = 75;

	protected static final float ON_R_DIFF = 2;

	private Paint p = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);

	private Paint px = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint pf = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint pl = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint p50 = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint p100 = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint dottedLinePaint = new Paint();

	private Paint pSma = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint pySelected = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);	

	private List<Delyta> delytor;

	private Paint       wCursorPaint,bCursorPaint,currCursorPaint;

	private boolean abo=false;
	private Handler handler;
	private int cursorX=200,cursorY=245;
	private float r,realRadiusinMeter=100;

	private boolean onCircle=true;
	private boolean drawActive=false;

	public TagCreateView(Context context, AttributeSet attrs) {
		super(context,attrs);

		pf.setColor(Color.BLACK);
		pf.setStyle(Style.STROKE);
		pf.setStrokeWidth(1);

		pySelected.setColor(Color.RED);
		pySelected.setStyle(Style.STROKE);
		pySelected.setStrokeWidth(3);

		dottedLinePaint.setARGB(255, 0, 0,0);
		dottedLinePaint.setStyle(Style.STROKE);
		dottedLinePaint.setStrokeWidth(4);
		dottedLinePaint.setPathEffect(new DashPathEffect(new float[] {10,10}, 5));
		dottedLinePaint.setAlpha(120);

		p.setColor(Color.BLACK);
		p.setStyle(Style.STROKE);
		p.setStrokeWidth(2);

		px.setColor(Color.DKGRAY);
		px.setTypeface(Typeface.SANS_SERIF);


		pl.setColor(Color.BLACK);
		pl.setStyle(Style.STROKE);
		pl.setTypeface(Typeface.DEFAULT_BOLD); 
		pl.setTextSize(25);

		p50.setColor(Color.BLUE);
		p50.setStrokeWidth(2);
		p50.setStyle(Style.STROKE);

		p50.setTypeface(Typeface.SANS_SERIF); 


		p100.setColor(Color.RED);
		p100.setStrokeWidth(3);
		p100.setStyle(Style.STROKE);
		p100.setTypeface(Typeface.SANS_SERIF); 

		pSma.setColor(Color.GREEN);
		pSma.setStyle(Style.FILL);



		wCursorPaint = new Paint();
		wCursorPaint.setColor(Color.WHITE);
		wCursorPaint.setStyle(Paint.Style.FILL);
		bCursorPaint = new Paint();
		bCursorPaint.setColor(Color.BLACK);
		bCursorPaint.setStyle(Paint.Style.FILL);
		currCursorPaint=wCursorPaint;
		final int interval = 1000; // 1 Second
		handler = new Handler();
		Runnable runnable = new Runnable(){
			public void run() {
				currCursorPaint = currCursorPaint.equals(wCursorPaint)?bCursorPaint:wCursorPaint;
				invalidate();
				handler.postDelayed(this, interval);

			}
		};

		this.setOnTouchListener(new View.OnTouchListener() {
			boolean moveCursor=false;
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				final float x = event.getX();
				final float y = event.getY();
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					if (event.getAction() == MotionEvent.ACTION_DOWN){
						Log.d("vortex","Touch coordinates : " +
								String.valueOf(event.getX()) + "x" + String.valueOf(event.getY()));
						float dx = x-cursorX;
						float dy = y-cursorY;
						float distance = (float)Math.sqrt((dx)*(dx) + (dy)*(dy));

						if (distance<=40)
							moveCursor = true;
					}
					return true;
				case MotionEvent.ACTION_UP:
					if (moveCursor) {
						moveCursor = false;
						if (drawActive) {
							addTagPoint(new float[]{cursorX,cursorY});
							if (Math.abs(distanceFromCenter(cursorX,cursorY)-r)<ON_R_DIFF) {
									onCircle=true;
									Log.d("vortex","on circle now true");
							}
							

						}

					}
					v.performClick();
					break;
				case MotionEvent.ACTION_MOVE:
					if (moveCursor) { 
						if (onCircle) {
							if (distanceFromCenter(x,y)<=(r-DRAW_THRESHOLD)) {
								float[] currentP = insideCircleXY(x,y,true);							
								addTagPoint(currentP);
								onCircle = false;
							}
						}
						float[] xy = insideCircleXY(x,y,onCircle);

						cursorX=(int)xy[0];
						cursorY=(int)xy[1];
						//Log.d("vortex","Distance from center: "+distanceFromCenter(cursorX,cursorY)+" r: "+r);
						currCursorPaint = wCursorPaint;
						invalidate();
					}
					break;
				default:
					break;
				}
				return true;
			}


		});

		handler.postDelayed(runnable, interval);
		drawActive=false;
	}


	private float[] insideCircleXY(float x, float y, boolean onCircle) {
		float distance = distanceFromCenter(x,y);
		if (distance<=r &&!onCircle)
			return new float[]{x,y};
		//not on circle. Return closest point on diameter.
		else {
			return closestPointOnRadius(x,y,distance);

		}
	}

	private float[] closestPointOnRadius(float x, float y,float distance) {
		float w = getWidth();
		float h = getHeight();
		float cx = w/2;
		float cy = h/2+20;
		float dx = x-cx;
		float dy = y-cy;
		float aX = cx + dx / distance * r;
		float aY = cy + dy / distance * r;
		return new float[]{aX,aY};
	}


	private float distanceFromCenter(float x,float y) {
		float w = getWidth();
		float h = getHeight();
		float cx = w/2;
		//float r=(float) ((w>=h)?((h/2)-h*.1):((w/2)-w*.1));
		float cy = h/2+20;
		float dx = x-cx;
		float dy = y-cy;
		float distance = (float)Math.sqrt((dx)*(dx) + (dy)*(dy));
		return distance;
	}

	float cy;
	float cx;
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);			
		float w = getWidth();
		float h = getHeight();



		boolean largeSize = (h>500);
		float margY = largeSize?20:0;
		if (largeSize) {
			r=(float) ((w>=h)?((h/2)-h*.1):((w/2)-w*.1));
			px.setTextSize(25);		
		}
		else {
			r=h/2;
			px.setTextSize(13);
		}
		cx = w/2;
		cy = (h/2+margY);//h/2;
		final float oScaleF = r/realRadiusinMeter;
		//A dot in the middle!
		canvas.drawPoint(cx, cy, p);

		float sr = 2.8f*oScaleF;
		Coord c;
		//6 meter ut på 0,120,240
		int sDists[] = new int[]{30,50,70};
		for (int i = 0; i<(abo?3:1);i++) {
			int sDist = sDists[i];
			c = calc_xy((int)(sDist*oScaleF),0);		
			canvas.drawCircle(cx,cy+c.y,sr,pSma);
			canvas.drawCircle(cx,cy+c.y,sr,pf);
			c = calc_xy((int)(sDist*oScaleF),120);
			canvas.drawCircle(cx+c.x,cy+c.y,sr,pSma);
			canvas.drawCircle(cx+c.x,cy+c.y,sr,pf);
			c = calc_xy((int)(sDist*oScaleF),240);
			canvas.drawCircle(cx+c.x,cy+c.y,sr,pSma);
			canvas.drawCircle(cx+c.x,cy+c.y,sr,pf);
		}
		drawDelytor(canvas,cx,cy,(float)oScaleF);
		canvas.drawText("100",(int)(cx+r)-15, cy, pf);
		if (largeSize)
			canvas.drawText("N",cx,(float)(h*.1), pl);

		if (drawActive) {
			//Log.d("vortex","oScaleF: "+oScaleF);
			drawTag(canvas,cx,cy,(float)oScaleF);
			float[] xy=tagPunkter.getLastXY();
			canvas.drawLine(xy[0],xy[1], cursorX, cursorY, dottedLinePaint);

		} else 
			initCursor(cx,1000);

		canvas.drawCircle(cursorX,cursorY, 20, currCursorPaint);	

	}


	//private List<Coord> scords=null;

	boolean cursorInitialized = false;
	public void initCursor(float x,float y) {
		if (!cursorInitialized) {
			float[] xy = insideCircleXY(x,y,true);
			cursorX=(int)xy[0];
			cursorY=(int)xy[1];
			cursorInitialized=true;
		}
	}
	//before delytor is calculated.
	private void drawTag(Canvas c, float cx, float cy, float oScaleF) {
		for (Segment s:tag) {
			drawSegment(s,c, cx, cy, oScaleF);			
		}
	}

	private void drawDelytor(Canvas c, float cx, float cy, float oScaleF) {
		if (delytor !=null&&!delytor.isEmpty()) {
			for (Delyta d:delytor) {
				Log.d("vortex","Found "+delytor.size()+" delytor!");
				for (Segment s:d.getSegments()) {
					Log.d("vortex","Has segment!");
					drawSegment(s,c, cx, cy, oScaleF);

				}
				Point numPos = d.getNumberPos();
				if (numPos!=null) {
					float nx = cx+(numPos.x*oScaleF);
					float ny = cy+(numPos.y*oScaleF);
					Log.d("nils","Drawing number at: "+nx+","+ny);
					p.setColor(Color.BLACK);
					c.drawText(d.getId()+"", nx, ny, px);
				}
			}
		} else {
			c.drawCircle(cx, cy, r, p);
		}

	}
	
	private void drawSegment(Segment s, Canvas c, float cx, float cy, float oScaleF) {
		float startX,startY,endX,endY;
		if (s.isArc) {
			//					if (!d.isSelected())
			//						continue;	
			//					else {
			RectF oval = new RectF(-r+cx, -r+cy, r+cx, r+cy);	
			Log.d("nils","start:end"+s.start.rikt+":"+s.end.rikt);
			float start = s.start.rikt;
			float end = Delyta.rDist((int)start,s.end.rikt);
			
			//start = start-90;

			Log.d("nils","Drawing arc from "+start+" with sweep "+end);
			//p.setColor(Color.BLUE);
			c.drawText("X", cx+s.start.x*oScaleF, cy+s.start.y*oScaleF, p50);
			
			double phi = 0.0174532925*(s.start.rikt);
			float nx = (float)(s.start.avst * Math.cos(phi));
			float ny = (float)(s.start.avst * Math.sin(phi));	
			c.drawText("X",cx+ nx*oScaleF,cy+ ny*oScaleF, p100);
			
			
			//c.drawArc(oval, start, end, false, p50);
			
			
			//c.drawArc(oval, start, end2, false, p100);
			//						c.drawArc(oval, start, end, false, pySelected);

			//					}
		} else {
			//float mirror = margY+r*2;							
			startX = cx+(s.start.x*oScaleF);
			startY = cy+(s.start.y*oScaleF);
			endX = cx+(s.end.x*oScaleF);
			endY = cy+(s.end.y*oScaleF);				
			//Log.d("nils","Drawing Start: "+startX+","+startY+" End: "+endX+","+endY);
			//p.setColor(Color.BLUE);
			//					c.drawLine(startX,startY,endX,endY, d.isSelected()?pySelected:p);	
			c.drawLine(startX,startY,endX,endY, p50);
		}

	}

	public void showDelytor(List<Delyta> delytor) {

		this.delytor = delytor;
		this.invalidate();
	}




	private Coord calc_xy(int avst, int rikt) {
		return new Coord(avst,rikt);
	}

	private Path mPath = null;
	
	
	List<Segment> tag ;

	private TagPunkter tagPunkter;
	
	
	
	private class TagPunkter {
		float[] raw = new float[200];
		int c=0;
		float scal ;
		
		public TagPunkter() {
			scal = realRadiusinMeter/r;
			
		}
		public Segment getArc(float[] xy) {		
			return createSegment(xy,true);
		}
		public Segment getLine(float[]xy) {
			return createSegment(xy,false);
		}
		
		private Segment createSegment(float[] xy,boolean isArc) {
			float ol[]= this.getLastXY();
			store(xy);
			float d1 = distanceFromCenter(xy[0],xy[1]);
			float d2 = distanceFromCenter(xy[0],xy[1]);
			float ox = (ol[0]-cx)*scal;
			float oy = (ol[1]-cy)*scal;
			float x = (xy[0]-cx)*scal;
			float y= (xy[1]-cy)*scal;
			Coord start = new Coord(ox,oy,(int)(d1*scal));
			Coord end = new Coord(x,y,(int)(d2*scal));
			return new Segment (start,end,isArc);
		}
				
		//		x1,y1,x2,y2,x3,y3
		//return x1,y1,x2,y2,x2,y2,x3,y3,x3,y3...
		//returns null if no values;
/*		public float[] getLines() {
			if (change&&c>=4) {
				int noOfPoints = 4+2*(c-4);
				ret = new float[noOfPoints];
				Log.d("vortex"," Length of array: "+noOfPoints);
				//n = number of lines.
				int j=0;
				for (int i=0;i<c-2;i+=2) {
					ret[j++]=raw[i];
					ret[j++]=raw[i+1];
					ret[j++]=raw[i+2];
					ret[j++]=raw[i+3];
				}
				change = false;
			}
			return ret;
		}
*/

		public void store(float xy[]) {
			raw[c++]=xy[0];
			raw[c++]=xy[1];
			
		}

		public int size() {
			return c;
		}

		
		public float[] getLastXY() {
			float[] retz = new float[2];
			if (c>=2) {
				retz[0]=raw[c-2];
				retz[1]=raw[c-1];
			}
			return retz;
		}

		public void clear() {
			c=0;
		}


	}

	
	public void addTagPoint(float[]xy) {
		if (!drawActive) {
			tag= new ArrayList<Segment>();
			tagPunkter = new TagPunkter();
			tagPunkter.store(xy);
			onCircle=false;
			drawActive=true;
		}  else {
			if (!onCircle)
				tag.add(tagPunkter.getLine(xy));
			else
				tag.add(tagPunkter.getArc(xy));
		}
		/*
		else {
			if (onCircle) {
				float w = getWidth();
				float h = getHeight();
				boolean largeSize = (h>500);
				float margY = largeSize?20:0;
				float cy;
				float cx;
				if (largeSize) {
					r=(float) ((w>=h)?((h/2)-h*.1):((w/2)-w*.1));
					px.setTextSize(25);		
				}
				else {
					r=h/2;
					px.setTextSize(13);
				}
				cx = w/2;
				cy = (h/2+margY);//h/2;
				RectF oval = new RectF(-r+cx, -r+cy, r+cx, r+cy);
				Coord s1 = new Coord(cursorX,cursorY);
				float start = prev.rikt;
				float end = Delyta.rDist((int)start,s1.rikt);
				start = start-90;
				if (start<0)
					start +=360;
				mPath.addArc(oval, start,end);
				prev = s1;
			} else
				mPath.lineTo(cursorX, cursorY);
			onCircle=!onCircle;	
			invalidate();
		}
		 */
	}

	public void resetTag() {
		tag.clear();
		onCircle=true;
	}


	public void reset() {
		resetTag();
		tagPunkter.clear();
		cursorInitialized=false;
		drawActive = false;
	}





}


