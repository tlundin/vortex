package com.teraim.vortex.dynamic.templates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableLayout.LayoutParams;
import android.widget.TextView;

import com.teraim.vortex.R;
import com.teraim.vortex.Start;
import com.teraim.vortex.bluetooth.EnvelopedMessage;
import com.teraim.vortex.bluetooth.LinjeDone;
import com.teraim.vortex.bluetooth.LinjeStarted;
import com.teraim.vortex.dynamic.Executor;
import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.dynamic.types.CHash;
import com.teraim.vortex.dynamic.types.ColumnDescriptor;
import com.teraim.vortex.dynamic.types.SweLocation;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.types.Workflow;
import com.teraim.vortex.dynamic.workflow_abstracts.Event;
import com.teraim.vortex.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.vortex.dynamic.workflow_abstracts.EventListener;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Container;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Event_OnBluetoothMessageReceived;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Linje_Meter_List;
import com.teraim.vortex.dynamic.workflow_realizations.WF_TimeOrder_Sorter;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.non_generics.NamedVariables;
import com.teraim.vortex.ui.Linje;
import com.teraim.vortex.utils.DbHelper;
import com.teraim.vortex.utils.DbHelper.Selection;
import com.teraim.vortex.utils.Geomatte;
import com.teraim.vortex.utils.InputFilterMinMax;
import com.teraim.vortex.utils.Tools;


public class LinjePortalTemplate extends Executor implements LocationListener, EventListener {
	List<WF_Container> myLayouts;
	VariableConfiguration al;
	DbHelper db;

	private SweLocation myL=null;
	EditText meterEd,meterEnEd;
	String currentLinje,currentYear;
	private LinearLayout aggregatePanel,fieldList,selectedPanel,numTmp,fieldListB;
	private String stratum;
	private Linje linje;
	private RelativeLayout intervallL;
	private Button startB,stopB;
	private FrameLayout linjeF;
	private TextView gpsView;
	private WF_Container root;
	private Spinner avgrSp;
	private double[] startPunkt;
	private Map<String, String> linjeKey;

	private final static String LinjePortalId = "LinjePortalTemplate";

	//private SweLocation center = new SweLocation(6564201.573, 517925.98);

	private LocationManager lm;
	private Variable linjeStatus,linjeStartEast,linjeStartNorth;

	private final int d = 25;
	private final double[][] startDistFromCenter = {{0,-d},{0,-d},{0,-d},{d,0},{d,0},{d,0},{0,d},{0,d},{0,d},{-d,0},{-d,0},{-d,0}};
	private SweLocation center;
	private String histNorr;
	private String histOst;
	
	private String[] avgrValueA;
	
	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d("nils","in onCreateView of LinjePortalTemplate");
		if (myContext == null) {
			Log.e("vortex","No context, exit");
			return null;
		}
		myContext.resetState();
		//Listen to LinjeStarted and LinjeDone events.
		myContext.addEventListener(this, EventType.onBluetoothMessageReceived);
		View v = inflater.inflate(R.layout.template_linje_portal_wf, container, false);	
		root = new WF_Container("root", (LinearLayout)v.findViewById(R.id.root), null);
		aggregatePanel = (LinearLayout)v.findViewById(R.id.aggregates);
		fieldList = (LinearLayout)v.findViewById(R.id.fieldList);
		fieldListB = (LinearLayout)fieldList.findViewById(R.id.fieldListB);
		//ListView selectedList = (ListView)v.findViewById(R.id.SelectedL);
		selectedPanel = (LinearLayout)v.findViewById(R.id.selected);

		lm = (LocationManager)this.getActivity().getSystemService(Context.LOCATION_SERVICE);

		stopB = (Button)new Button(this.getActivity());
		startB = (Button)fieldList.findViewById(R.id.startB);

		al = gs.getVariableConfiguration();
		db = gs.getDb();
		currentYear = al.getVariableValue(null,"Current_Year");
		currentLinje = al.getVariableValue(null,"Current_Linje");

		if (currentLinje == null) {
			AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
			alert.setTitle("Ingen linje angiven!");
			alert.setMessage("Den h�r menyn g�r inte att k�ra utan att en linje valts under ProvytaMenyn.");
			alert.setPositiveButton("Jag f�rst�r", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			});
			alert.setIcon(android.R.drawable.ic_dialog_alert);
			alert.show();
		}
		else {
			Log.d("nils","Current Linje is "+currentLinje);
			startPunkt = startDistFromCenter[Integer.parseInt(currentLinje)-1];
			northW = startPunkt[1]<0;
			southW = startPunkt[1]>0;
			if (!northW && !southW) {
				eastW = startPunkt[0]>0;
				westW = startPunkt[0]<0;
			}

			Log.e("nils","eastW: "+eastW+" westW: "+westW+" southW:"+southW+" northW: "+northW);
			Map<String,String>pyKeyMap = al.createLinjeKeyMap();
			linjeKey = Tools.createKeyMap(VariableConfiguration.KEY_YEAR,currentYear,"ruta",al.getVariableValue(null,"Current_Ruta"),"linje",currentLinje);
			linjeStatus = al.getVariableUsingKey(linjeKey, NamedVariables.STATUS_LINJE);
			Variable pyCentrumNorr = al.getVariableUsingKey(pyKeyMap, "CentrumGPSNS");
			Variable pyCentrumOst = al.getVariableUsingKey(pyKeyMap, "CentrumGPSEW");
			histNorr = pyCentrumNorr.getHistoricalValue();
			histOst = pyCentrumOst.getHistoricalValue();
			Log.d("nils","pyKEyMap: "+pyKeyMap.toString());
			Log.d("nils","Historical norr ost: "+histNorr+" "+histOst);



			stratum = al.getVariableValue(al.createRutaKeyMap(),NamedVariables.STRATUM_HISTORICAL);

			Log.d("nils","STRATUM: "+stratum);
			//			status = Active.INITIAL;
			stopB.setText("KLAR");

			startB.setText("STARTA");
			fieldListB.setVisibility(View.INVISIBLE);
			linjeStartEast = al.getVariableUsingKey(linjeKey, "!linjestartEast");
			linjeStartNorth = al.getVariableUsingKey(linjeKey, "!linjestartNorth");

			if (linjeStatus.getValue()!=null) {
				if (linjeStatus.getValue().equals(Constants.STATUS_STARTAD_MEN_INTE_KLAR)) {
					if (linjeStartEast.getValue()!=null && linjeStartNorth!=null) {
						double lStartE = Double.parseDouble(linjeStartEast.getValue());
						double lStartN = Double.parseDouble(linjeStartNorth.getValue());

						setStart(lStartE,lStartN);
						Log.d("nils","Linjestatus was STATUS_STARTAD_MEN_INTE_KLAR");
					} else {
						Log.d("nils","Status changed back to initial, because of missing values for startE,startN");
						linjeStatus.setValue(Constants.STATUS_INITIAL);	
						center = null;
					}
				}
				else if (linjeStatus.getValue().equals(Constants.STATUS_AVSLUTAD_OK)){
					Log.d("nils","Linjestatus is Avslutad");						
				} 
			} else {
				Log.e("nils","Linjestatus was null");
				linjeStatus.setValue(Constants.STATUS_INITIAL);
				center = null;
			}

			/*
			//Set statpunkt to theoretical start.
			if (status == Active.INITIAL) {
				if (histNorr !=null && histOst !=null) {
					double teoriNorr = Double.parseDouble(histNorr)+startPunkt[1];
					double teoriOst = Double.parseDouble(histOst)+startPunkt[0];
					setStart(teoriOst,teoriNorr);		
				}

			}
			 */


			LinearLayout filterPanel = (LinearLayout)v.findViewById(R.id.filterPanel);
			myLayouts = new ArrayList<WF_Container>();
			myLayouts.add(root);
			myLayouts.add(new WF_Container("Field_List_panel_1", fieldList, root));
			myLayouts.add(new WF_Container("Aggregation_panel_3", aggregatePanel, root));
			myLayouts.add(new WF_Container("Filter_panel_4", filterPanel, root));
			myLayouts.add(new WF_Container("Field_List_panel_2", selectedPanel, root));
			myContext.addContainers(getContainers());

			gpsView = (TextView)aggregatePanel.findViewById(R.id.gpsView);
			gpsView.setText("S�ker...");

			intervallL = (RelativeLayout) inflater.inflate(R.layout.intervall_popup, null);
			numTmp  = (LinearLayout)inflater.inflate(R.layout.edit_field_numeric, null);

			avgrSp = (Spinner) intervallL.findViewById(R.id.avgrTyp);

//			List<String>avgrTyper = Arrays.asList(new String[] {"�kermark","Sl�ttervall","Vatten","Otillg�nglig v�tmark","Otillg�nglig brant","Rasrisk","Tomt/Bebyggelse","On�bar biotop�","Betr�dnadsf�rbud"});			
			
			List<String>avgrTyperRaw = al.getListElements(al.getCompleteVariableDefinition(NamedVariables.AVGRTYP));
			String[] tmp;
			String[] avgrTyper = new String[avgrTyperRaw.size()];
			avgrValueA=new String[avgrTyperRaw.size()];
			int c=0;
			for (String s:avgrTyperRaw) {
				s=s.replace("{", "");
				s=s.replace("}", "");								
				tmp = s.split("=");
				if (tmp==null||tmp.length!=2) {
					Log.e("nils","found corrupt element: "+s);
					o.addRow("");
					o.addRedText("Variabeln Avgr�nsning:AvgrTyp saknar v�rden.");					
					avgrValueA[c]="null";
					avgrTyper[c]="****";
				} else {
					avgrValueA[c]=tmp[1];
					avgrTyper[c]=tmp[0];
				}
				c++;
			}
			ArrayAdapter<String> sara=new ArrayAdapter<String>(getActivity(),android.R.layout.simple_list_item_1,avgrTyper);
			avgrSp.setAdapter(sara);

			linjeF = (FrameLayout) filterPanel.findViewById(R.id.linje);

			linje = new Linje(getActivity(),(eastW?"E":(westW?"W":(northW?"N":(southW?"S":"?")))));

			linjeF.addView(linje);

			List<String>startAlt = (histOst != null && histNorr != null)?Arrays.asList(new String[] {"S�tt startpunkt h�r","Anv�nd ber�knad startpunkt","Starten m�ste kartinventeras","Hela linjen i karta"}):
				Arrays.asList(new String[] {"S�tt startpunkt h�r"});			

			final int startIHar = startAlt.indexOf("S�tt startpunkt h�r");
			final int startIKart = startAlt.indexOf("Starten m�ste kartinventeras");
			final int helaIKart = startAlt.indexOf("Hela linjen i karta");
			final int anvandTeoriStart = startAlt.indexOf("Anv�nd ber�knad startpunkt");
			final LinearLayout startSpinnerL = (LinearLayout)inflater.inflate(R.layout.edit_field_spinner, null);
			final Spinner startSp= (Spinner)startSpinnerL.findViewById(R.id.spinner);
			ArrayAdapter<String> ara=new ArrayAdapter<String>(getActivity(),android.R.layout.simple_list_item_1,startAlt);
			startSp.setAdapter(ara);


			stopB.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {

					AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
					alert.setTitle("Stopp!");
					alert.setMessage("Vill du s�kert avsluta och klarmarkera?");					
					alert.setCancelable(false);
					alert.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							setEnded();
							//gs.sendMessage(new LinjeDone(currentLinje));
							getFragmentManager().popBackStackImmediate();
						}


					});
					alert.setNegativeButton("Nej", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

						}
					});
					alert.show();
				}});

			startB.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (((ViewGroup)startSpinnerL.getParent())!=null)
						((ViewGroup)startSpinnerL.getParent()).removeView(startSpinnerL);
					if (!linjeStatus.getValue().equals(Constants.STATUS_STARTAD_MEN_INTE_KLAR)) {
						if (linjeStatus.getValue().equals(Constants.STATUS_AVSLUTAD_OK)) {
							new AlertDialog.Builder(v.getContext()).setTitle("Linjen markerad avslutad!")
							.setMessage("Vill du g�ra om linjen?")
							.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) { 												
									linjeStatus.setValue(Constants.STATUS_INITIAL);
									startB.performClick();
									gpsView.setText("");
								}})
								.setNegativeButton("Nej",new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) { 
										// continue with delete
									}})
									.setCancelable(false)
									.setIcon(android.R.drawable.ic_dialog_alert)
									.show();								
						}
						if (linjeStatus.getValue().equals(Constants.STATUS_INITIAL)) {
							AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
							alert.setTitle("Start");
							alert.setMessage("Hur vill du starta linjen?");
							alert.setView(startSpinnerL);
							alert.setCancelable(false);

							alert.setPositiveButton("K�r ig�ng", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									if (startSp.getSelectedItemPosition()==startIHar&&myL==null) {									
										new AlertDialog.Builder(LinjePortalTemplate.this.getActivity())
										.setTitle("Din position �r ok�nd!")
										.setMessage("Eftersom GPSen �nnu inte lyckats pricka in din position, s� kan du inte anv�nda det h�r alternativet")
										.setPositiveButton("Jag f�rst�r!", new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int which) { 
												// continue with delete
											}
										})
										.setCancelable(false)
										.setIcon(android.R.drawable.ic_dialog_alert)
										.show();
									} else {

										//should intervall be opened?
										if (startSp.getSelectedItemPosition()==startIKart||
												startSp.getSelectedItemPosition()==helaIKart) {
											int start=0,end=-1;

											if (startSp.getSelectedItemPosition()==helaIKart) 										
												end = 200;
											openInterVallPopup(start,end);
											double teoriNorr = Double.parseDouble(histNorr)+startPunkt[1];
											double teoriOst = Double.parseDouble(histOst)+startPunkt[0];
											setStart(teoriOst,teoriNorr);
										}  else if (startSp.getSelectedItemPosition()==startIHar) {

											setStart(myL.east,myL.north);

										} else if (startSp.getSelectedItemPosition()==anvandTeoriStart) {	
											Log.d("nils","I should en up here.");
											double teoriNorr = Double.parseDouble(histNorr)+startPunkt[1];
											double teoriOst = Double.parseDouble(histOst)+startPunkt[0];
											setStart(teoriOst,teoriNorr);	
										} else 
											Log.d("nils","startSPinner: "+startSp.getSelectedItemPosition());
										/*
										if (gs.syncIsAllowed()) {
											gs.triggerTransfer();
											//Send a LinjeStarted Message in an envelope.
											gs.sendMessage(new EnvelopedMessage(new LinjeStarted(currentLinje)));
										}
										*/

									}
								}



							});
							alert.setNegativeButton("Avbryt", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {

								}});
							alert.show();
						}
					}

				}});

			Log.d("nils","year: "+currentYear+" Ruta: "+al.getVariableValue(null,"Current_Ruta")+" Linje: "+currentLinje);

			Map<String,String> keySet = Tools.createKeyMap(VariableConfiguration.KEY_YEAR,currentYear,"ruta",al.getVariableValue(null,"Current_Ruta"),"linje",currentLinje);

			Selection selection = db.createSelection(keySet,"!linjeobjekt");

			List<ColumnDescriptor> columns = new ArrayList<ColumnDescriptor>();
			columns.add(new ColumnDescriptor("meter",true,false,true));
			columns.add(new ColumnDescriptor("value",false,true,false));
			WF_Linje_Meter_List selectedList = new WF_Linje_Meter_List("selected_list", true, myContext,columns, selection,"!linjeobjekt",keySet,linje,avgrValueA,avgrTyper);

			selectedList.addSorter(new WF_TimeOrder_Sorter());

			selectedPanel.addView(selectedList.getWidget());

			//Trigger null event for redraw.
			selectedList.onEvent(null);

			//Variable linjeObj = al.getVariableInstance(NamedVariables.LINJEOBJEKT);
			List<String> lobjT = al.getCompleteVariableDefinition(NamedVariables.LINJEOBJEKT);
			List<String>objTypes = al.getListElements(lobjT);
			if (objTypes!=null)
				Log.d("nils","Found objTypes! "+objTypes.toString());

			//Generate buttons.
			TextView spc = new TextView(this.getActivity());
			spc.setWidth(20);

			Button b;			
			for (final String linjeObjLabel:objTypes) {
				if (stratum !=null && linjeObjLabel.equals(NamedVariables.RENSTIG)&&
						!stratum.equals("10"))
					continue;
				b = new Button(this.getActivity());
				LayoutParams params = new LayoutParams();
				params.width = LayoutParams.MATCH_PARENT;
				params.height = LayoutParams.WRAP_CONTENT;
				b.setLayoutParams(params);
				b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
				if (linjeObjLabel.equals(NamedVariables.RENSTIG))
					b.setText("Liten fj�llstig");
				else
					b.setText(linjeObjLabel);
				b.setOnClickListener(new OnClickListener() {			
					@Override
					public void onClick(View v) {

						if (linjeObjLabel.equals("Avgr�nsning"))
							openInterVallPopup(-1,-1);
						else
							openInterVallPopup(linjeObjLabel);
					}


				});
				fieldListB.addView(b);
			}
			spc = new TextView(this.getActivity());
			spc.setWidth(20);
			fieldListB.addView(spc);
			fieldListB.addView(stopB);

			//WF_ClickableField_Selection aggNo = new WF_ClickableField_Selection_OnSave("Avslutade Rutor:", "De rutor ni avslutat",
			//		myContext, "AvslRutor",true);
			//aggregatePanel.addView(aggNo.getWidget());
		}
		mHandler = new Handler();
		startRepeatingTask();

		return v;

	}




	private void setEnded() {
		startB.setBackgroundResource(android.R.drawable.btn_default);
		startB.setText("STARTA");
		linjeStatus.setValue(Constants.STATUS_AVSLUTAD_OK);						
		fieldListB.setVisibility(View.INVISIBLE);	
		myContext.registerEvent(new WF_Event_OnSave(LinjePortalId));
	}


	@Override
	public void onStart() {
		if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
			startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), 0);

		super.onStart();
	}


	@Override
	public void onResume() {
		myL=null;
		lm.requestLocationUpdates(
				LocationManager.GPS_PROVIDER,
				0,
				1,
				this);



		super.onResume();
	}

	@Override
	public void onPause() {
		myL=null;
		stopRepeatingTask();
		super.onPause();
	}






	@Override
	public void onDestroy() {
		Log.d("Vortex","On destroy called");
		lm.removeUpdates(this);
		super.onDestroy();
	}


	@Override
	protected List<WF_Container> getContainers() {
		return myLayouts;
	}

	public void execute(String name, String target) {

	}



	@Override
	public void onLocationChanged(Location location) {
		if (tickerRunning)
			this.stopRepeatingTask();
		myL = Geomatte.convertToSweRef(location.getLatitude(), location.getLongitude());
		String info=refreshGPSInfo();
		if (info!=null)
			gpsView.setText(info);
		else
			gpsView.setText("GPS �r ig�ng");
	}

	private void setStart(double east, double north) {
		linjeStartEast.setValue(east+"");
		linjeStartNorth.setValue(north+"");
		center = new SweLocation(east,north);
		startB.setBackgroundColor(Color.GREEN);
		stopB.setBackgroundColor(Color.RED);
		fieldListB.setVisibility(View.VISIBLE);
		startB.setText("STARTAD");
		linjeStatus.setValue(Constants.STATUS_STARTAD_MEN_INTE_KLAR);
		myContext.registerEvent(new WF_Event_OnSave(LinjePortalId));
		//Initialize LinjeView
		String info=refreshGPSInfo();
		if (info!=null)
			gpsView.setText(info);

	}
	
	boolean eastW,southW,northW,westW = false;
	private String refreshGPSInfo() {

		double x=0,y=0;
		String ret=null;
		if (center!=null&&myL!=null) {
			if (northW) {
				x = myL.east-center.east;
				y = myL.north-center.north;  //the more north, the lower the value.
			}
			else if (southW) {
				x = center.east-myL.east;
				y = center.north-myL.north;
			}
			else if (eastW) {
				x = myL.north-center.north;
				y = myL.east-center.east;
			}
			else if (westW) {
				x = center.north-myL.north;
				y = center.east-myL.east;
			}

			ret = "FiS: "+((int)x)+" As:"+(int)y;
			//X should now be deviation from Line. Y is distance from Start.
			linje.setUserPos((float)x,(float)y);
			linje.invalidate();
		} 
		return ret;	
	}


	@Override
	public void onProviderDisabled(String provider) {
		gpsView.setText("GPS AV");
	}



	@Override
	public void onProviderEnabled(String provider) {
		gpsView.setText("GPS P�");
	}



	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		if (status == LocationProvider.AVAILABLE) {
			if (gpsView.getText().equals("S�ker.."))
				gpsView.setText("GPS Ig�ng!");
		} else if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
			gpsView.setText("S�ker..");


		} else if (status == LocationProvider.OUT_OF_SERVICE) {
			gpsView.setText("S�ker..");
		}
	}


	enum Linjetyp {
		PUNKT,
		INTERVALL
	}

	private void openInterVallPopup(String linjeObjLabel) {	 
		meterEd = (EditText)numTmp.findViewById(R.id.edit);
		meterEd.setFilters(new InputFilter[]{ new InputFilterMinMax("0", "200")});
		openInterVallPopup(Linjetyp.PUNKT,linjeObjLabel,null);
	}



	private void openInterVallPopup(int start, int end) {		
		meterEd = (EditText)intervallL.findViewById(R.id.avgrStart);
		if (start!=-1)
			meterEd.setText(start+"");
		meterEd.setFilters(new InputFilter[]{ new InputFilterMinMax("0", "199")});
		meterEnEd = (EditText)intervallL.findViewById(R.id.avgrSlut);
		if (end!=-1)
			meterEnEd.setText(end+"");
		meterEnEd.setFilters(new InputFilter[]{ new InputFilterMinMax("1", "200")});		
		openInterVallPopup(Linjetyp.INTERVALL,"Avgr�nsning",intervallL);
	}

	Dialog complexD=null;
	
	private void openInterVallPopup(final Linjetyp typ,final String linjeObjLabel, ViewGroup myView) {
		complexD = null;
		boolean skipToEnd=false;

		AlertDialog.Builder alert = new AlertDialog.Builder(this.getActivity());
		
		
		
		alert.setMessage("Ange metertal f�r linjeobjekt");
		//If punkt, determine what kind of view to present. If existing objects, show selection.
		if (typ==Linjetyp.PUNKT && myView==null) {
			Set<Map<String, String>> existingLinjeObjects = getExistingObjects(linjeObjLabel);
			if (existingLinjeObjects!=null) {
				Log.d("vortex","Got this back: "+existingLinjeObjects.toString());
				Iterator<Map<String,String>> it = existingLinjeObjects.iterator();
				//Create a button array with a button for each meter.
				List<Button> buttonArray = new ArrayList<Button>();
				while (it.hasNext()) {
					Map<String,String> currentObj = it.next();
					final String meterV = currentObj.get("meter");
					Button b = new Button(this.getActivity());
					b.setText(meterV+" meter");
					b.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							if (complexD==null)
								Log.e("vortex","complexD null..should not happen");
							else
								complexD.dismiss();
							jumpToWorkFlow(meterV, null,linjeObjLabel,typ);
						}
					});
					buttonArray.add(b);
				}
				//Skapa en knapp f�r fallet nytt objekt.
				Button b = new Button(this.getActivity());
				b.setText("Skapa nytt objekt p� annat metertal");
				b.setOnClickListener(new OnClickListener() {					
					@Override
					public void onClick(View v) {
						complexD.dismiss();
						openInterVallPopup(typ,linjeObjLabel,numTmp);
					}
				});
				buttonArray.add(b);
				myView = new LinearLayout(this.getActivity());
				((LinearLayout)myView).setOrientation(LinearLayout.VERTICAL);
				for (Button bb:buttonArray) {
					myView.addView(bb);
				}
				alert.setMessage("V�lj existerande objekt eller skapa nytt:");
				skipToEnd=true;


			} else {
				Log.d("vortex","Did not find any existing objects of type "+linjeObjLabel);
				myView = numTmp;	
			}

		}

		if (((ViewGroup)myView.getParent())!=null)
			((ViewGroup)myView.getParent()).removeView(myView);

		alert.setTitle(linjeObjLabel);

		if (!skipToEnd) {

			alert.setPositiveButton("Spara", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Editable metA=null;
					Editable metS=meterEd.getText();
					boolean error = false;
					if (typ==Linjetyp.INTERVALL) {
						metA = meterEnEd.getText();
						
						if (metA.length()==0||metS.length()==0) {
							o.addRow("");
							o.addRedText("Avst�nd meter tom");
							error = true;
							new AlertDialog.Builder(LinjePortalTemplate.this.getActivity())
							.setTitle("FEL: Slut v�rde fattas")
							.setMessage("Du m�ste ange b�de start och slut p� avgr�nsningen!") 
							.setIcon(android.R.drawable.ic_dialog_alert)
							.setCancelable(false)
							.setNeutralButton("Ok",new Dialog.OnClickListener() {				
								@Override
								public void onClick(DialogInterface dialog, int which) {
									
								}
							} )
							.show();
						} else if (Integer.parseInt(metS.toString())>Integer.parseInt(metA.toString())) {
							o.addRow("");
							o.addRedText("Start l�ngre bort �n Slut");
							error = true;
							new AlertDialog.Builder(LinjePortalTemplate.this.getActivity())
							.setTitle("Fel v�rden")
							.setMessage("Start m�ste vara l�gre �n slut!") 
							.setIcon(android.R.drawable.ic_dialog_alert)
							.setCancelable(false)
							.setNeutralButton("Ok",new Dialog.OnClickListener() {				
								@Override
								public void onClick(DialogInterface dialog, int which) {
									
								}
							} )
							.show();
						}
					}
					if (metS!=null && metS.length()>0 && !error) {
						Log.d("nils","Got meters: "+meterEd.getText());

						//Create new !linjeobjekt with the meters.
						String meter = (meterEd.getText().toString());
						//peel away zeros from beginning.
						meter = meter.replaceFirst("^0+(?!$)", "");
						Log.d("nils","meter is now: "+meter);
						jumpToWorkFlow(meter, metA!=null?metA.toString():null,linjeObjLabel,typ);
					}
				}

			});
			alert.setNegativeButton("Avbryt", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

				}
			});	
		}
		complexD = alert.setView(myView).create();
		complexD.setCancelable(false);
		complexD.show();

	}

	private void jumpToWorkFlow(String start, String end, String linjeObjLabel,Linjetyp typ) {
		Variable currentMeter = al.getVariableInstance(NamedVariables.CURRENT_METER);
		if (currentYear==null||al.getVariableValue(null,"Current_Ruta")==null||currentLinje==null||currentMeter==null) {
			o.addRow("");
			o.addRedText("Could not start workflow "+linjeObjLabel+
					"_wf, since no value exist for one of [Current_year, Current_ruta, Current_Linje, Current_Meter]");
		} else {
			currentMeter.setValue(start);
			//check if the variable exist. If so - no deal.
			Map<String,String> key = Tools.createKeyMap(VariableConfiguration.KEY_YEAR,currentYear,"ruta",al.getVariableValue(null,"Current_Ruta"),"linje",currentLinje,"meter",start,"value",linjeObjLabel);
			Map<String,String> keyI = new HashMap<String,String>(key);
			keyI.remove("value");
			if (typ==Linjetyp.INTERVALL) {							
				Log.d("nils","S�tter intervall variabler");
				gs.setKeyHash(keyI);
				Variable v = al.getVariableInstance(NamedVariables.AVGRANSSLUT);
				v.setValue(end);
				v= al.getVariableInstance(NamedVariables.AVGRTYP);
				Log.d("nils","Setting avgrtyp to "+((String)avgrSp.getSelectedItem()));
				v.setValue(avgrValueA[avgrSp.getSelectedItemPosition()]);
			}
			gs.setKeyHash(key);
			Variable v = al.getVariableUsingKey(key, NamedVariables.LINJEOBJEKT);
			//Variable v = al.getVariableInstance();

			if (v.setValue(linjeObjLabel)) {
				Log.d("nils","Stored "+linjeObjLabel+" under meter "+start);
				myContext.registerEvent(new WF_Event_OnSave("Template"));
			} else 
				Log.e("nils","Variable "+v.getId()+" Obj:"+v+" already has value "+v.getValue()+" for keychain "+key.toString());

			if (typ == Linjetyp.PUNKT) {
				if (linjeObjLabel.equals(NamedVariables.RENSTIG)) {								
					al.getVariableUsingKey(keyI, NamedVariables.TransportledTyp).setValue("2");
				} else {
					//Start workflow here.
					Log.d("nils","Trying to start workflow "+"wf_"+linjeObjLabel);
					Workflow wf = gs.getWorkflow("wf_"+linjeObjLabel);
					if (wf!=null) {
						Fragment f = wf.createFragment(wf.getTemplate());
						if (f == null) {
							o.addRow("");
							o.addRedText("Couldn't create new fragment...Workflow was named"+wf.getName());
							Log.e("nils","Couldn't create new fragment...Workflow was named"+wf.getName());
						}
						Bundle b = new Bundle();
						b.putString("workflow_name", "wf_"+linjeObjLabel); //Your id
						f.setArguments(b); //Put your id to your next Intent
						//save all changes�
						CHash r = gs.evaluateContext(wf.getContext());
						if (r.err==null) {
							gs.setKeyHash(r.keyHash);
							//Keep this if the hash values change.
							gs.setRawHash(r.rawHash);

							Start.singleton.changePage(f,linjeObjLabel);
							Log.d("nils","Should have started "+"wf_"+linjeObjLabel);
						} else {
							o.addRow("");
							o.addRedText("Error in context when trying to start : "+"wf_"+linjeObjLabel+". Error: "+r.err);
						}
					} else {
						o.addRow("");
						o.addRedText("Couldn't find workflow named "+"wf_"+linjeObjLabel);
						Log.e("nils","Couldn't find workflow named"+"wf_"+linjeObjLabel);
					}
				}
			}
		}
	}


	private Set<Map<String, String>> getExistingObjects(String linjeObjLabel) {
		Map<String,String> objChain = Tools.createKeyMap(VariableConfiguration.KEY_YEAR,currentYear,"ruta",al.getVariableValue(null,"Current_Ruta"),"linje",currentLinje,"value",linjeObjLabel);
		return db.getKeyChainsForAllVariableInstances(NamedVariables.LINJEOBJEKT, objChain, "meter");
	}


	@Override
	public void onEvent(Event e) {
		//Py ruta changed. Force reload of page with new settings.


		if (e instanceof WF_Event_OnBluetoothMessageReceived) { 
			if ((gs.getOriginalMessage() instanceof LinjeStarted) && !isRunning()) {
				String linjeId = ((LinjeStarted)gs.getOriginalMessage()).linjeId;
				if (linjeId!=null && linjeId.equals(currentLinje)) {
					if (linjeStartEast.getValue()!=null && linjeStartNorth!=null) {
						double lStartE = Double.parseDouble(linjeStartEast.getValue());
						double lStartN = Double.parseDouble(linjeStartNorth.getValue());										
						setStart(lStartE,lStartN);
					}
				}
			}
			else if (gs.getOriginalMessage() instanceof LinjeDone && isRunning()) {
				String linjeId = ((LinjeDone)gs.getOriginalMessage()).linjeId;
				if (linjeId!=null && linjeId.equals(currentLinje)) 
					setEnded();

			}
		}
	}

	public boolean isRunning() {
		return linjeStatus==null?false:linjeStatus.getValue().equals(Constants.STATUS_STARTAD_MEN_INTE_KLAR);
	}


	private static String ticker = "       ...V�ntar p� GPS...        ";


	private int tStrLen = 10, curPos=0;
	private void updateStatus() {
		int end = curPos+tStrLen;
		if (end>ticker.length())
			end = ticker.length();
		if (curPos==ticker.length()) {
			curPos=0;
			end = tStrLen;
		}
		String cString = ticker.substring(curPos, end);
		curPos++;
		gpsView.setText(cString);		
	}

	boolean tickerRunning=false;
	void startRepeatingTask() {
		curPos = 0;
		tickerRunning = true;
		mStatusChecker.run(); 
	}

	void stopRepeatingTask() {
		mHandler.removeCallbacks(mStatusChecker);
		tickerRunning = false;

	}
	private Handler mHandler;
	private int mInterval = 250;
	Runnable mStatusChecker = new Runnable() {
		@Override 
		public void run() {
			updateStatus(); //this function can change value of mInterval.
			mHandler.postDelayed(mStatusChecker, mInterval);
		}
	};

}