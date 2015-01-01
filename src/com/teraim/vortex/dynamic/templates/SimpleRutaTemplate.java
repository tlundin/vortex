package com.teraim.vortex.dynamic.templates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.teraim.vortex.ParameterSafe;
import com.teraim.vortex.R;
import com.teraim.vortex.Start;
import com.teraim.vortex.dynamic.Executor;
import com.teraim.vortex.dynamic.blocks.CreateEntryFieldBlock;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Container;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.non_generics.NamedVariables;
import com.teraim.vortex.non_generics.StatusHandler;
import com.teraim.vortex.non_generics.StatusHandler.Kvot;
import com.teraim.vortex.ui.MenuActivity;
import com.teraim.vortex.utils.DbHelper;
import com.teraim.vortex.utils.DbHelper.JsonReport;
import com.teraim.vortex.utils.DbHelper.Selection;
import com.teraim.vortex.utils.PersistenceHelper;
import com.teraim.vortex.utils.Tools;



public class SimpleRutaTemplate extends Executor implements OnGesturePerformedListener {
	List<WF_Container> myLayouts;


	/* (non-Javadoc)
	 * @see android.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
	 */
	ViewGroup myContainer = null;
	private GestureLibrary gestureLib;
	private ParameterSafe ps;
	private List<Integer> rutor;
	private  ArrayAdapter<Integer> fieldListAdapter;

	private Variable rutaKlar;

	private TextView rutOutputValueField;
	private final static int MIN_UNSYNCED = 5;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		ps = gs.getSafe();
		al = gs.getVariableConfiguration();
		myContext.resetState();
		myLayouts = new ArrayList<WF_Container>();
		Log.d("nils","in onCreateView of ruta_template");
		myContainer = container;
		View v = inflater.inflate(R.layout.template_ruta_wf, container, false);	
		WF_Container root = new WF_Container("root", (LinearLayout)v.findViewById(R.id.root), null);
		ListView fieldList = (ListView)v.findViewById(R.id.fieldListL);
		ListView selectedList = (ListView)v.findViewById(R.id.SelectedL);
		LinearLayout aggregatePanel = (LinearLayout)v.findViewById(R.id.aggregates);
		myLayouts.add(root);
		myLayouts.add(new WF_Container("Field_List_panel_1", (LinearLayout)v.findViewById(R.id.fieldList), root));
		myLayouts.add(new WF_Container("Aggregation_panel_3", aggregatePanel, root));
		myLayouts.add(new WF_Container("Filter_panel_4", (LinearLayout)v.findViewById(R.id.filterPanel), root));
		myLayouts.add(new WF_Container("Field_List_panel_2", (LinearLayout)v.findViewById(R.id.Selected), root));
		myContext.addContainers(getContainers());


		LinearLayout rutorRemainingView = (LinearLayout)inflater.inflate(R.layout.display_value_textview, null);		
		TextView h = (TextView)rutorRemainingView.findViewById(R.id.header);
		h.setText("Rutor gjorda");

		rutOutputValueField = (TextView)rutorRemainingView.findViewById(R.id.outputValueField);

		
		aggregatePanel.addView(rutorRemainingView);


		Button exportB = new Button(getActivity());		
		exportB.setText("Exportera vald ruta");

		exportB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				final String currentRuta = gs.getVariableConfiguration().getVariableInstance(NamedVariables.CURRENT_RUTA).getValue();
				final String lagID = gs.getPersistence().get(PersistenceHelper.LAG_ID_KEY);
				if (currentRuta == null) {
					new AlertDialog.Builder(getActivity())
					.setTitle("Ingen ruta vald")
					.setMessage("Det verkar inte som om du ännu valt någon ruta.")
					.setPositiveButton("Jag ska göra det snart", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) { 
							// continue with delete
						}
					})			    
					.setIcon(android.R.drawable.ic_dialog_alert)
					.show();
				} else if (lagID==null || lagID.equals(PersistenceHelper.UNDEFINED) || lagID.length()==0) {
					new AlertDialog.Builder(getActivity())
					.setTitle("Inget LAG ID!")
					.setMessage("Du måste först ange ett lagID (skiftnyckeln) för att exportera.")
					.setPositiveButton("Okej - ska göra det!", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) { 
							// continue with delete
						}
					})			    
					.setIcon(android.R.drawable.ic_dialog_alert)
					.show();
				}
				else{
					int noOfSyncE = gs.getDb().getNumberOfUnsyncedEntries();
					new AlertDialog.Builder(getActivity())
					.setTitle("Export av Ruta "+currentRuta)
					.setMessage((noOfSyncE<MIN_UNSYNCED)?"Du ska till och exportera ruta "+currentRuta+".\n"+					
							"Vill du klarmarkera? ":"VARNING!! DU HAR "+noOfSyncE+" OSYNKADE INMATNINGAR!! Vill du verkligen klarmarkera?")
					.setPositiveButton("Ja - den är klar!", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) { 							
							rutaKlar = al.getVariableUsingKey(al.createRutaKeyMap(), NamedVariables.RUTA_KLAR_ANVÄNDARE);
							rutaKlar.setValue("1");
							export(true,currentRuta);
							al.getVariableInstance(NamedVariables.CURRENT_RUTA).setValue(null);
						}			
					})
					.setNeutralButton("Nej - men exportera ändå", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							export(false,currentRuta);
						}
					})
					.setNegativeButton("Avbryt! Ingen export", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) { 
							//do nothing.
						}
					}
					)		
					.setCancelable(false)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.show();
					
					
				}


			}
		});

		if (gs.isMaster()) {
			aggregatePanel.addView(exportB);
			createInvTypSelection();
			
		}

		GestureOverlayView gestureOverlayView = (GestureOverlayView)v.findViewById(R.id.gesture_overlay);

		gestureOverlayView.setGestureVisible(false);
		gestureOverlayView.addOnGesturePerformedListener(this);
		gestureLib = GestureLibraries.fromRawResource(this.getActivity(), R.raw.gestures);
		if (!gestureLib.load()) {      	
			Log.i("nils", "Load gesture libraries failed.");  
		}  


		final List<Integer> prevRutor = ps.getPrevRutor();

		DbHelper db = gs.getDb();
		rutor = ps.getRutor();
		
		if (rutor == null) {
			rutor = new ArrayList<Integer>();
			HashSet<Integer> temp = new HashSet<Integer>();
			List<String[]> values = db.getValues(new String[] {db.getColumnName("ruta")}, new Selection());
			for (String[] val:values)
				temp.add(Integer.parseInt(val[0]));
			rutor.addAll(temp);
			Collections.sort(rutor);
			gs.getSafe().setRutor(rutor);
		} 
		
		
		fieldListAdapter = new ArrayAdapter<Integer>(this.getActivity(),
		          android.R.layout.simple_list_item_1, android.R.id.text1, rutor);
		final ArrayAdapter selectedListA = new ArrayAdapter(this.getActivity(),android.R.layout.simple_list_item_1, android.R.id.text1, prevRutor);
		fieldList.setAdapter(fieldListAdapter);
		selectedList.setAdapter(selectedListA);
		fieldList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view,final int position,
					long arg3) {

				
				final Variable currentRuta = gs.getVariableConfiguration().getVariableInstance(NamedVariables.CURRENT_RUTA);
				final Integer rl = rutor.get(position);
				final String pi = rl.toString();
				final Map<String,String>rKeyChain = Tools.createKeyMap("år",Constants.CurrentYear,"ruta",pi);
				rutaKlar = al.getVariableUsingKey(rKeyChain, NamedVariables.RUTA_KLAR_ANVÄNDARE);
				String rutaKS = rutaKlar.getValue();
				final boolean rutaK = rutaKS!=null&&rutaKS.equals("1");
				String msg = null;
				if (rutaK)
					msg = "Vill du verkligen öppna ruta "+pi+"? Den är markerad klar!!!";
				else 
					msg = "Vill du "+(currentRuta.getValue()!=null&&currentRuta.getValue().equals(pi)?"fortsätta":"börja")+" insamling på ruta "+pi+"?";
				new AlertDialog.Builder(SimpleRutaTemplate.this.getActivity())
				.setTitle("Starta insamling")
				.setMessage(msg) 
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setCancelable(false)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which)  {		
						if (rutaK)
							rutaKlar.setValue("0");
						currentRuta.setValue(pi);	
						//Nullify currentprovyta and currentlinje
						gs.getVariableConfiguration().getVariableUsingKey(null, NamedVariables.CURRENT_PROVYTA).deleteValue();
						gs.getVariableConfiguration().getVariableUsingKey(null, NamedVariables.CURRENT_LINJE).deleteValue();
						gs.getVariableConfiguration().getVariableUsingKey(null, NamedVariables.CURRENT_DELYTA).deleteValue();
						gs.getVariableConfiguration().getVariableUsingKey(null, NamedVariables.CURRENT_SMAPROVYTA).deleteValue();
					
						//kill the current variable cache.
						gs.getVariableCache().invalidateAll();
						Variable stratum = gs.getVariableConfiguration().getVariableUsingKey(rKeyChain,NamedVariables.STRATUM);
						Variable hStratum = gs.getVariableConfiguration().getVariableUsingKey(rKeyChain,NamedVariables.STRATUM_HISTORICAL);
						String strH = stratum.getHistoricalValue();
						if (strH==null) {
							o.addRow("");
							o.addRedText("Stratum missing for ruta "+pi+" Will default to 1");
							strH="1";
						}
						hStratum.setValue(strH);
						Log.d("nils","HISTORICAL STRATUM SET TO "+hStratum.getValue());
						//copy
						prevRutor.add(0,rl);
						selectedListA.notifyDataSetChanged();
						gs.setKeyHash(al.createRutaKeyMap());
						Log.d("vortex","in simpleruta, after refreshkey, before menu redraw. PI: "+pi+" CurrentR_: "+currentRuta.getValue()+"\nkeyhash: "+gs.getCurrentKeyHash().toString());
						gs.sendEvent(MenuActivity.REDRAW);
						Log.d("vortex","in simpleruta, after refreshkey, after menu redraw!!");
						Start.singleton.changePage(new ProvytaTemplate(), "Provyta");					
					}

				})
				.setNegativeButton(android.R.string.no,new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {}} ) 
					.show();



			}



		});

		 myContext.getContainer("Aggregation_panel_3").draw();

		return v;

	}


	private void createInvTypSelection() {
		if (al.createRutaKeyMap()!=null) {
		gs.setKeyHash(al.createRutaKeyMap());
		CreateEntryFieldBlock x = new CreateEntryFieldBlock("typSpinner","RutaSorteringsTyp", "Aggregation_panel_3",true,"DDD",false,Constants.NO_DEFAULT_VALUE);
		x.create(myContext);
		}
	}


	@Override
	public void onResume() {


		StatusHandler sh = gs.getStatusHandler();
		sh.setStatusRuta();
		Kvot k = sh.getStatusRutor();
		rutOutputValueField.setText(k.toString());

		super.onResume();
	}
	@Override
	public void onPause() {
		super.onPause();
	}


	@Override
	protected List<WF_Container> getContainers() {
		return myLayouts;
	}






	@Override
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
		Log.d("nils","Number of gestures available: "+gestureLib.getGestureEntries().size());
		ArrayList<Prediction> predictions = gestureLib.recognize(gesture);
		Log.d("nils","Number of predictions: "+predictions.size());
		for (Prediction prediction : predictions) {
			if (prediction.score > .5) {
				Log.d("nils","MATCH!!");
				if (prediction.name.equals("left")) {
					final FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction(); 
					Fragment gs = new Fragment();  			
					ft.replace(R.id.content_frame, gs);
					ft.addToBackStack(null);
					ft.commit(); 
				} else 
					Toast.makeText(getActivity(), "Fel håll", Toast.LENGTH_SHORT).show();

			}
		}		
	}


	@Override
	public void execute(String function, String target) {
		// TODO Auto-generated method stub

	}
	
	
	private void export(boolean isKlar, String currentRuta) {
		String rutaSorteringsTyp = al.getVariableValue(al.createRutaKeyMap(), "RutaSorteringsTyp");
		JsonReport jRep = gs.getDb().export("ruta",currentRuta,isKlar,rutaSorteringsTyp);
		String msg, btnText;
		if (jRep == JsonReport.OK) {
			msg = "Filen har sparats och kommer att synkas automatiskt med Umeå.";
			btnText = "Ok, vad bra!";
		} else {
			msg = "Exporten fungerade inte. Orsak: "+jRep.name();
			btnText = "Ok";
		}
		new AlertDialog.Builder(getActivity())
		.setTitle("Export klar")
		.setMessage(msg)
		.setPositiveButton(btnText, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) { 
				// continue with delete
			}
		})			    
		.setIcon(android.R.drawable.ic_dialog_alert)
		.show();
	}











}