package com.teraim.vortex.ui;

import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.location.Location;
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



	private Paint p = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);

	private Paint px = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint pf = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint pl = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint p50 = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint p100 = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);

	private Paint pSma = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint pySelected = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);	

	private List<Delyta> delytor;
	
	private Paint       wCursorPaint,bCursorPaint,currCursorPaint;

	private boolean abo=false;
	private Handler handler;
	private int cursorX=200,cursorY=245;

	private boolean onCircle=true;

	public TagCreateView(Context context, AttributeSet attrs) {
		super(context,attrs);

		pf.setColor(Color.BLACK);
		pf.setStyle(Style.STROKE);
		pf.setStrokeWidth(1);

		pySelected.setColor(Color.RED);
		pySelected.setStyle(Style.STROKE);
		pySelected.setStrokeWidth(3);


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
	            	moveCursor = false;
	                v.performClick();
	                break;
	            case MotionEvent.ACTION_MOVE:
	            	if (moveCursor) { float[] xy = insideCircleXY(x,y,onCircle);
	            	       	cursorX=(int)xy[0];
		                	cursorY=(int)xy[1];
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

		
	}
	
	
	private float[] insideCircleXY(float x, float y, boolean onCircle) {
		float w = getWidth();
		float h = getHeight();

		float cx = w/2;
		float r=(float) ((w>=h)?((h/2)-h*.1):((w/2)-w*.1));
		float cy = h/2+20;
		float dx = x-cx;
		float dy = y-cy;
		float distance = (float)Math.sqrt((dx)*(dx) + (dy)*(dy));
		if (distance<=r &&!onCircle)
			return new float[]{x,y};
		//outside circle. Return closest point on diameter.
		else {
			float aX = cx + dx / distance * r;
			float aY = cy + dy / distance * r;
			return new float[]{aX,aY};

		}
	}

	float r,realRadiusinMeter=10;

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);			
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
		canvas.drawText("10",(int)(cx+r)-15, cy, pf);
		if (largeSize)
			canvas.drawText("N",cx,(float)(h*.1), pl);
		//Draw a blinking square cursor at current location if nothing else is happening
		canvas.drawCircle(cursorX,cursorY, 20, currCursorPaint);

	}


	List<Coord> scords=null;

	
	public void init() {
		float[] xy = insideCircleXY(0,0,true);
       	cursorX=(int)xy[0];
    	cursorY=(int)xy[1];
    	invalidate();
	}

	private void drawDelytor(Canvas c, float cx, float cy, float oScaleF) {
		float startX,startY,endX,endY;
		if (delytor !=null&&!delytor.isEmpty()) {
			for (Delyta d:delytor) {
				Log.d("vortex","Found "+delytor.size()+" delytor!");
				for (Segment s:d.getSegments()) {	
					Log.d("vortex","Has segment!");

					if (s.isArc) {
						//					if (!d.isSelected())
						//						continue;	
						//					else {
						RectF oval = new RectF(-r+cx, -r+cy, r+cx, r+cy);	
						Log.d("nils","start - end"+s.start.rikt+"-"+s.end.rikt);
						float start = s.start.rikt;
						float end = Delyta.rDist((int)start,s.end.rikt);
						Log.d("nils","Drawing arc from "+start+" with sweep "+end);
						start = start-90;
						if (start<0)
							start +=360;

						p.setColor(Color.LTGRAY);
						c.drawArc(oval, start, end, false, p);
						//						c.drawArc(oval, start, end, false, pySelected);

						//					}
					} else {
						//float mirror = margY+r*2;							
						startX = cx+(s.start.x*oScaleF);
						startY = cy+(s.start.y*oScaleF);
						endX = cx+(s.end.x*oScaleF);
						endY = cy+(s.end.y*oScaleF);				
						Log.d("nils","Drawing Start: "+startX+","+startY+" End: "+endX+","+endY);
						p.setColor(Color.LTGRAY);
						//					c.drawLine(startX,startY,endX,endY, d.isSelected()?pySelected:p);	
						c.drawLine(startX,startY,endX,endY, p);
					}

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
			Log.d("vortex","Drawing circle!");
			c.drawCircle(cx, cy, r, p);
		}
		if (mPath!=null)
			c.drawPath(mPath, p50);
		
	}

	public void showDelytor(List<Delyta> delytor) {

		this.delytor = delytor;
		this.invalidate();
	}




	private Coord calc_xy(int avst, int rikt) {
		return new Coord(avst,rikt);
	}
	
	Path mPath = null;
	Coord prev = null;
	
	public void addTagPoint() {
		if (mPath==null) {
			mPath = new Path();
			mPath.moveTo(cursorX,cursorY);
			onCircle=false;
			prev = new Coord(cursorX,cursorY);
		} else {
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
	}

	public void resetTag() {
		mPath=null;
		onCircle=true;
	}





}


