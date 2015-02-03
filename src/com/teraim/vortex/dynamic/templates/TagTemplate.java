package com.teraim.vortex.dynamic.templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.teraim.vortex.R;
import com.teraim.vortex.dynamic.Executor;
import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.dynamic.types.Delyta;
import com.teraim.vortex.dynamic.types.Marker;
import com.teraim.vortex.dynamic.types.Segment;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.workflow_abstracts.Event;
import com.teraim.vortex.dynamic.workflow_abstracts.EventListener;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Container;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.non_generics.DelyteManager;
import com.teraim.vortex.non_generics.DelyteManager.Coord;
import com.teraim.vortex.non_generics.NamedVariables;
import com.teraim.vortex.ui.ProvytaView;
import com.teraim.vortex.utils.Tools;




public class TagTemplate extends Executor implements EventListener, OnGesturePerformedListener {


	private static final int MAX_TÅG = 5,MAX_DELPUNKTER =6;

	private static final int COLS=MAX_TÅG+1,ROWS=MAX_DELPUNKTER+1;

	private GestureLibrary gestureLib;
	private GridLayout gl;
	private ProvytaView pyv;
	private LayoutInflater inflater;
	private DelyteManager dym;
	private VariableConfiguration al;
	private Button sparaB,nyUtlaggB,calculateB;
	private LinearLayout areaL;


	@Override
	protected List<WF_Container> getContainers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void execute(String function, String target) {
		// TODO Auto-generated method stub

	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View v = inflater.inflate(R.layout.template_tag, container, false);	

		final FrameLayout py = (FrameLayout)v.findViewById(R.id.circle);
		gl = (GridLayout)v.findViewById(R.id.gridLayout);

		areaL = (LinearLayout)v.findViewById(R.id.areaL);

		Marker man = new Marker(BitmapFactory.decodeResource(getResources(),R.drawable.icon_man));

		dym = DelyteManager.getInstance();

		pyv = new ProvytaView(activity, null, man, Constants.isAbo(dym.getPyID()));		

		py.addView(pyv);

		al = gs.getVariableConfiguration();
		//Get all variables from group "delningstag".

		//Delyta equals variable name..eg. Tåg1 = Delyta 1 asf.

		//tableElements[i][index]=ar+","+s;
		this.inflater = inflater;



		drawEmptyTable();

		fillTable();

		pyv.showDelytor(dym.getDelytor(),false);



		//					final EditText avst = (EditText)l.findViewById(R.id.avst);
		//					final EditText rikt = (EditText)l.findViewById(R.id.rikt);	

		/*
		int[][] t = { 

				{100,233,0,360,64,322,100,47},
				{100,288,100,48,100,120,100,263},
				{100,48,100,288},
				{100,233,57,180,100,143},
				{100,29,75,336,100,320},
				{100,261,100,36,100,98,100,200},
				{100,219,100,116},
				{100,116,100,30},
				{100,30,100,270}	

		};
		 */
		/*	
		Button[] button = new Button[5];
		for (int i=0;i<5;i++) {
			button[i]=(Button)v.findViewById(buttonIds[i]);
			final int c = i;
			button[i].setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					pyv.showDelytor(dy.get(c));
				}
			});
		}

		 */

		calculateB = (Button)v.findViewById(R.id.redraw);
		calculateB.setEnabled(false);
		nyUtlaggB = (Button)v.findViewById(R.id.rensa);
		nyUtlaggB.setEnabled(gs.isMaster());
		sparaB = (Button)v.findViewById(R.id.spara);
		sparaB.setEnabled(false);
		gl.setEnabled(false);

		calculateB.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				createDelytorFromTable();
				dym.analyze();
				TagTemplate.this.updateAreaField();
				pyv.showDelytor(dym.getDelytor(),false);
				sparaB.setEnabled(true);
			}


		});

		nyUtlaggB.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {

				final String rutaId = al.getVariableValue(null, "Current_Ruta");
				final String provytaId = al.getVariableValue(null, "Current_Provyta");
				new AlertDialog.Builder(TagTemplate.this.getActivity())
				.setTitle("Nyutlägg - Varning")
				.setMessage("Alla delytor under ruta/provyta ["+rutaId+"/"+provytaId+"] kommer suddas. Är du säker?")
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) { 
						drawEmptyTable();
						gs.getDb().eraseDelytor(gs,rutaId,provytaId,true);	


						//Mark as nyutlägg.
						Variable nyUtlagg = gs.getVariableConfiguration().getVariableUsingKey(gs.getVariableConfiguration().createProvytaKeyMap(), NamedVariables.NYUTLAGG);
						nyUtlagg.setValue("1");
						//init.
						dym.init();
						//show default
						pyv.showDelytor(dym.getDelytor(),false);
						nyUtlaggB.setEnabled(false);
						calculateB.setEnabled(true);
						gl.setEnabled(true);
					}
				})
				.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) { 
						// do nothing
					}
				})
				.setIcon(android.R.drawable.ic_dialog_alert)
				.show();

			}
		});
		sparaB.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				new AlertDialog.Builder(TagTemplate.this.getActivity())
				.setTitle("Spara")
				.setMessage("Det här sparar alla förändringar permanent. Är du säker?")
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) { 
						if(save()) {
							sparaB.setEnabled(false);
							calculateB.setEnabled(false);
							nyUtlaggB.setEnabled(true);
							gs.triggerTransfer();
						}
					}
				})
				.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) { 
						// do nothing
					}
				})
				.setIcon(android.R.drawable.ic_dialog_alert)
				.show();
			}
		});


		GestureOverlayView gestureOverlayView = (GestureOverlayView)v.findViewById(R.id.gesture_overlay);

		gestureOverlayView.setGestureVisible(false);
		gestureOverlayView.addOnGesturePerformedListener(this);
		gestureLib = GestureLibraries.fromRawResource(this.getActivity(), R.raw.gestures);
		if (!gestureLib.load()) {      	
			Log.i("nils", "Load gesture libraries failed.");  
		}  


		//Display area if any delytor.
		if (dym.getDelytor().size()>0) {
			updateAreaField();
		} else
			Log.d("nils","NO DELYTOR WHEN TIME TO DRAW AREA");


		return v;

	}



	/*
	private final OnTouchListener hav = new OnTouchListener() {
	    @Override
	    public boolean onTouch(View v, MotionEvent event) {
	        if(MotionEvent.ACTION_UP == event.getAction())
				if (!Tools.isNumeric(((EditText)v).getText().toString()))
					((EditText)v).setText("");
	        return false;
	    }
	};
	 */

	private void updateAreaField() {

		areaL.removeAllViews();
		Map<Integer,Float> aMap = new HashMap<Integer,Float>();
		float a;
		for (Delyta d:dym.getDelytor()) {			
			a=d.getArea();
			Log.d("nils","Area for dy "+d.getId()+" is "+a);
			if (a==0) 
				Log.e("nils","AREA WAS 0 in updateAreaField for DY "+d.getId());
			else {
				Float area = aMap.get(d.getId());
				if (area==null)
					area = a;
				else 
					area +=a;
				aMap.put(d.getId(), area);
			}
		}
		Set<Integer> keys = aMap.keySet();
		LinearLayout el;
		TextView h,tv;

		for (Integer id:keys) {				
			el = (LinearLayout)inflater.inflate(R.layout.area_elem, null);
			h =(TextView)el.findViewById(R.id.header);
			tv=(TextView)el.findViewById(R.id.value);
			h.setText("DY "+id+":");
			tv.setText((String.format("%.2f", (aMap.get(id)/100f)))+"\u33A1");
			Log.d("nils","AREA added for delyta "+id+": "+aMap.get(id));
			areaL.addView(el);
		}

		Log.d("nils","exiting area calc");

	}



	private final OnFocusChangeListener hav = new OnFocusChangeListener() {
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			if(hasFocus)
				if (!Tools.isNumeric(((EditText)v).getText().toString()))
					((EditText)v).setText("");
		}
	};



	private void drawEmptyTable() {
		Log.d("nils","In drawEmptyTable");
		gl.removeAllViews();
		//Create table for Tåg input.
		//Add empty top corner at index 0.
		gl.addView(new TextView(gs.getContext()),0);
		//Add headers.
		TextView h; LinearLayout l;
		for (int i=1;i<COLS;i++)	{			
			h = (TextView)inflater.inflate(R.layout.header_tag_textview, null);
			h.setText("TÅG"+i);
			gl.addView(h,i);
		}
		//while there are still some tåg with values, continue.
		for (int j=1;j<ROWS;j++) {	
			for (int i=0;i<COLS;i++)	{
				final int ii = i;final int jj = j;
				if (i==0) {
					//Add row header.
					h = (TextView)inflater.inflate(R.layout.header_tag_textview, null);
					h.setText(j+"");
					gl.addView(h,j*COLS);
				} else {
					l = (LinearLayout)inflater.inflate(R.layout.edit_fields_tag, null);
					gl.addView(l,i+j*COLS);
					final EditText avst = (EditText)l.findViewById(R.id.avst);
					final EditText rikt = (EditText)l.findViewById(R.id.rikt);
					avst.setText("avs");
					rikt.setText("rik");
					final LinearLayout l2 = (LinearLayout)(gl.getChildAt(ii+(jj%COLS+1)*COLS));
					avst.setOnFocusChangeListener(hav);
					rikt.setOnFocusChangeListener(hav);
					avst.setOnLongClickListener(new OnLongClickListener() {

						@Override
						public boolean onLongClick(View v) {
							avst.setText("avs");
							return true;
						}
					});
					rikt.setOnLongClickListener(new OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							rikt.setText("rik");
							return true;
						}
					});	
					/*
					rikt.setOnKeyListener(new OnKeyListener() {
						@Override
						public boolean onKey(View v, int keyCode, KeyEvent event) {
							if (keyCode == KeyEvent.KEYCODE_TAB || keyCode == KeyEvent.KEYCODE_ENTER) {
								Log.d("nils","GETS A TAB OR ENTER");
								LinearLayout l2 = (LinearLayout)(gl.getChildAt(ii+(jj%COLS+1)*COLS));
								EditText avst = ((EditText)(l2).findViewById(R.id.avst));
								if(!Tools.isNumeric(avst.getText().toString()))
									avst.setText("");
								return true;
							}
							return false;
						}
					});
					 */


				}					
			}

		}
		gl.getChildAt(COLS+1).requestFocus();

	}


	private void fillTable() {
		Log.d("nils","In fillTable");
		List<Segment> tag;
		int row =1; int col=1;
		for (Delyta dy:dym.getDelytor()) {	
			if (!dy.isBackground()) {
				tag = dy.getSegments();
				for (Segment s:tag) {
					setElement((LinearLayout)(gl.getChildAt(col+row*COLS)),s.start);
					row++;
					if (row>=ROWS) {
						Log.e("nils","Overflow in table. Too many rows");
						break;
					}
				}
				row=1;
				if (col<COLS) 
					col++;
				else {
					Log.e("nils","Overflow in table. Too many delytor!");
					break;
				}
			}
		}
	}


	private void setElement(LinearLayout ll,Coord c) {
		EditText avst,rikt;
		avst = ((EditText)(ll).findViewById(R.id.avst));
		rikt = ((EditText)(ll).findViewById(R.id.rikt));
		avst.setText(c.avst+"");
		rikt.setText(c.rikt+"");
	}

	private Coord getElement(LinearLayout ll) {
		EditText avst,rikt;
		avst = ((EditText)(ll).findViewById(R.id.avst));
		rikt = ((EditText)(ll).findViewById(R.id.rikt));
		String avstS = avst.getText().toString();
		String riktS = rikt.getText().toString();
		if (empty(avstS)||empty(riktS)||!Tools.isNumeric(avstS)||!Tools.isNumeric(riktS))
			return null;
		int avstI = Integer.parseInt(avstS);
		int riktI = Integer.parseInt(riktS);
		return new Coord(avstI,riktI);
	}

//	100|179|100|295|100|20|43|77|100|133
/*
	private void createDelytorFromTable() {
		dym.clear();
		List<Coord> tc = new ArrayList<Coord>();
		tc.add(new Coord(100,179));
		tc.add(new Coord(100,295));
		tc.add(new Coord(100,20));
		tc.add(new Coord(43,77));
		tc.add(new Coord(100,133));
		dym.addUnknownTag(tc);


	}
*/	 

	private void createDelytorFromTable() {
		//Empty existing. 
		dym.clear();
		List<Coord> tagCoordinateList = new ArrayList<Coord>();
		Coord c;
		LinearLayout ll;
		for(int col=1;col<COLS;col++) {
			for (int row=1;row<ROWS;row++) {
				ll = (LinearLayout)(gl.getChildAt(col+row*COLS));
				c = getElement(ll);
				if (c==null) 
					break;
				else
					tagCoordinateList.add(c);
			}
			if (tagCoordinateList.size()>1) {
				DelyteManager.ErrCode ec = dym.addUnknownTag(tagCoordinateList);
				if (ec == null||ec!=DelyteManager.ErrCode.ok)
					Toast.makeText(getActivity(), "Tåg i kolumn "+col+" är vajsing!", Toast.LENGTH_LONG).show();;
			} 
			tagCoordinateList.clear();
		}
		//Check if no delyta. In that case, add default.
		if (dym.getDelytor().isEmpty()) {
			Log.d("nils","Adding default in createdelytorfromtable");
			dym.addDefault();
		}
	}


	/*
private void drawTrains() {
	List<Delyta> delytor=new ArrayList<Delyta>();
	//Create train array.
	for (int i=1;i<numberOfColumns+1;i++) {
		int index = 0;
		List<Coord> coords = new ArrayList<Coord>();
		for (int j=1;j<MAX_ROWS+1;j++) {
			LinearLayout ll = (LinearLayout)(gl.getChildAt(j*(numberOfColumns+1)+i));
			EditText avst = ((EditText)(ll).findViewById(R.id.avst));
			EditText rikt = ((EditText)(ll).findViewById(R.id.rikt));
			String avS = avst.getText().toString();
			String riS = rikt.getText().toString();
			//If one of the values are empty - tag ends.
			if (empty(avS)||empty(riS)) {
				if (coords.size()>1) {
					Delyta delyta = new Delyta();
					ErrCode e = delyta.create(coords);
					if (e == ErrCode.ok) {
						delytor.add(delyta);

					} else 
						Log.e("nils","Couldnt create delyta: "+e.name());
				}
				break;
			}
			else {	
				coords.add(new Coord(Integer.parseInt(avS),Integer.parseInt(riS)));
				//t[i-1][index++] = Integer.parseInt(avS);
				//t[i-1][index++] = Integer.parseInt(riS);
			}
		}
	}


	//Draw.





}
	 */
	private boolean empty(String s) {
		return s==null||s.length()==0;
	}



	/*

		• Första och sista punkten måste ligga på cirkelprovytans periferi.	 
		• Delningspunkterna måste beskrivas medurs. 
		• Första linjen i tåget får ej vara en cirkelbåge. 
		• Om två delningspunkter mellan första och sista brytpunkt ligger på periferin måste 
		  linjen mellan dem vara en cirkelbåge. I annat fall måste en av punkterna flyttas in 
		  mot centrum 1 dm, så att avståndet till punkten ej är lika med ytradien. 
		• Antalet delningspunkter får vara högst 6 per delningståg. 
		• Provytan får delas i högst 5 delar
	 */

	@Override
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
		Log.d("nils","Number of gestures available: "+gestureLib.getGestureEntries().size());
		ArrayList<Prediction> predictions = gestureLib.recognize(gesture);
		Log.d("nils","Number of predictions: "+predictions.size());
		for (Prediction prediction : predictions) {
			if (prediction.score > .5 && prediction.name.equals("right")) {
				if (dym.hasUnsavedChanges()) {
					new AlertDialog.Builder(TagTemplate.this.getActivity())
					.setTitle("Vill du spara?")
					.setMessage("Du har gjort ändringar till tåget. Vill du spara dessa före du går iväg?")
					.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) { 
							if (save())
								getFragmentManager().popBackStackImmediate();
						}
					})
					.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) { 
							getFragmentManager().popBackStackImmediate();
						}
					})
					.setIcon(android.R.drawable.ic_dialog_alert)
					.show();
				} else
					getFragmentManager().popBackStackImmediate();
			}
		}
	}

	@Override
	public void onEvent(Event e) {
		// TODO Auto-generated method stub

	}

	private boolean save() {
		if (!dym.save()) {
			new AlertDialog.Builder(TagTemplate.this.getActivity())
			.setTitle("Fel")
			.setMessage("En småprovyta tycks ligga utanför. Kanske är ditt tåg lite konstigt? (det ska gå MEDURS!). Om du känner dig tvärsäker att du visst har rätt: rapportera bugg!")
			.setPositiveButton("Ok, jag flyttar!", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) { 

				}
			})			
			.setIcon(android.R.drawable.ic_dialog_alert)
			.show();


			return false;
		}
		return true;
	}




}
