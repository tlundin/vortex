package com.teraim.fieldapp.dynamic.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.blocks.ButtonBlock;
import com.teraim.fieldapp.dynamic.blocks.OnclickExtra;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.types.Delyta;
import com.teraim.fieldapp.dynamic.types.Marker;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_DisplayValueField;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.non_generics.DelyteManager;
import com.teraim.fieldapp.non_generics.NamedVariables;
import com.teraim.fieldapp.non_generics.StatusHandler;
import com.teraim.fieldapp.non_generics.StatusHandler.Kvot;
import com.teraim.fieldapp.ui.MenuActivity;
import com.teraim.fieldapp.ui.ProvytaView;
import com.teraim.fieldapp.utils.PersistenceHelper;


public class ProvytaNivaTemplate extends Executor implements EventListener, OnGesturePerformedListener {


	private List<WF_Container> myLayouts;
	private View v;
	private DelyteManager dym;
	private ProvytaView pyv;
	private TextView delOutputValueField,smaOutputValueField,partnerOutputValueField;
	private GestureLibrary gestureLib;
	private ViewGroup fieldListPanel;
	private ButtonBlock fixPunkter;
	private ButtonBlock taBild;
	private ButtonBlock[] delyteKnappar = new ButtonBlock[DelyteManager.MAX_DELYTEID];
	private ButtonBlock smayteKnapp;
	private StatusHandler statusHandler;
	private Button tagSidaB;
	private boolean isAbo;
	//private ButtonBlock spillning;

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d("nils","in onCreateView of ProvytaNiv�Template");
		if (myContext==null) {
			Log.d("vortex","hasnt survived create...exiting.");
			return null;
		}
		v = inflater.inflate(R.layout.template_provyta_niva_wf, container, false);	
		//myContext.resetState();
		myContext.addContainers(getContainers());
		ViewGroup aggregatePanel = (LinearLayout)v.findViewById(R.id.aggregates);
		ViewGroup provytaViewPanel = (FrameLayout)v.findViewById(R.id.Description);

		fieldListPanel = (LinearLayout)v.findViewById(R.id.fieldList);

		WF_DisplayValueField rSel = new WF_DisplayValueField("whatevar", "[$Current_Ruta]",myContext, null, 
				"Vald Ruta", true,null,null,null);


		final String currPy = al.getCurrentProvyta();
		final String currRuta = al.getCurrentRuta();
		dym = DelyteManager.getInstance();
		
		dym.setSelected(-1);
		statusHandler = gs.getStatusHandler();

		isAbo = Constants.isAbo(dym.getPyID());

		

		LinearLayout delytorRemainingView = (LinearLayout)inflater.inflate(R.layout.display_value_textview, null);		
		LinearLayout smaRemainingView = (LinearLayout)inflater.inflate(R.layout.display_value_textview, null);		
		LinearLayout synkadMedView = (LinearLayout)inflater.inflate(R.layout.display_value_textview, null);		

		TextView h = (TextView)delytorRemainingView.findViewById(R.id.header);
		h.setText("Delytor gjorda");
		h = (TextView)smaRemainingView.findViewById(R.id.header);
		h.setText("Sm�ytor gjorda");
		h = (TextView)synkadMedView.findViewById(R.id.header);
		h.setText("Synkad mot: ");

		delOutputValueField = (TextView)delytorRemainingView.findViewById(R.id.outputValueField);
		smaOutputValueField= (TextView)smaRemainingView.findViewById(R.id.outputValueField);
		partnerOutputValueField= (TextView)synkadMedView.findViewById(R.id.outputValueField);

		smaOutputValueField.setText("");
		partnerOutputValueField.setText("");


		aggregatePanel.addView(rSel.getWidget());
		aggregatePanel.addView(delytorRemainingView);
		aggregatePanel.addView(smaRemainingView);
		aggregatePanel.addView(synkadMedView);

		//Marker man = new Marker(BitmapFactory.decodeResource(getResources(),R.drawable.icon_man));

		pyv = new ProvytaView(this.getActivity(), null, Constants.isAbo(dym.getPyID()));		

		provytaViewPanel.addView(pyv);

		GestureOverlayView gestureOverlayView = (GestureOverlayView)v.findViewById(R.id.gesture_overlay);

		gestureOverlayView.setGestureVisible(false);
		gestureOverlayView.addOnGesturePerformedListener(this);
		gestureLib = GestureLibraries.fromRawResource(this.getActivity(), R.raw.gestures);
		if (!gestureLib.load()) {      	
			Log.i("nils", "Load gesture libraries failed.");  
		}  



		myContext.addEventListener(this, EventType.onSave);
		
		tagSidaB = new Button(this.getActivity());
		
		tagSidaB.setText("Delningssk�rm");
		
		tagSidaB.setTextSize(30);
		
		tagSidaB.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				/*final FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction(); 
				Fragment gs = new TagTemplate();  			
				ft.replace(R.id.content_frame, gs);
				ft.addToBackStack(null);
				ft.commit(); */
				Start.singleton.changePage(new TagTemplate(), "Delningssk�rm");
			}
		});
		String xContext = "�r = [getCurrentYear()], ruta = [getColumnValue(ruta)], provyta = [getColumnValue(provyta)]";
		taBild = new ButtonBlock("_tabild","Foto","Start_Workflow", "fotobutton","Field_List_panel_1",NamedVariables.WF_FOTO,"action", NamedVariables.STATUS_FOTO,true,null,null,true,xContext,false);
		
		fixPunkter = new ButtonBlock("_","Fixpunkter","Start_Workflow", "fixpunktbutton","Field_List_panel_1",NamedVariables.WF_FIXPUNKTER,"action",NamedVariables.STATUS_FIXPUNKTER,true,null,null,true,xContext,false);

		//spillning = new ButtonBlock("_s","Spillning","Start_Workflow", "spillningbutton","Field_List_panel_1",NamedVariables.WF_SPILLNING,"action","status_spillning",true,null,null,true);

		for (int i=0; i<DelyteManager.MAX_DELYTEID;i++) {
			final int I = i;
			Map<String,String> buttonHash = al.createProvytaKeyMap();
			buttonHash.put("delyta", I+"");
			DB_Context buttonContext = new DB_Context(null,buttonHash);
			//Log.e("vortex","Creating knapp f�r delyta "+i);
			delyteKnappar[i]=new ButtonBlock("DelBl"+i,"Delyta "+i,"Start_Workflow", "Delyta "+i,"Field_List_panel_1",
					(isAbo?NamedVariables.WF_DELYTE_INMATNING_ABO:NamedVariables.WF_DELYTE_INMATNING),"action","STATUS:status_delyta",true,
					new OnclickExtra() {						
				@Override
				public void onClick() {
					varCache.getGlobalVariable(NamedVariables.CURRENT_DELYTA).setValue(I+"");
					Delyta dy = dym.getDelyta(I);
					dym.setSelected(I);
					Log.d("nils","Selected delyta set to "+dy.getId()+"");						
					gs.sendEvent(MenuActivity.REDRAW);
				} },buttonContext,-1);
		}
		 final String WF_ABO_SMA = "wf_sm�provytor_�bo";
		 final String WF_NILS_SMA = "wf_sm�provytor_Nils";
		Map<String,String> buttonHash = al.createProvytaKeyMap();
		//CHash buttonContext = new CHash(null,buttonHash);
		Log.d("vortex","context for status_smaprovytor: "+buttonHash.toString());
		smayteKnapp = new ButtonBlock("_smaprov","Sm�provytor","Start_Workflow", "Sm�provytor","Field_List_panel_1",(isAbo?WF_ABO_SMA:WF_NILS_SMA),"action", (isAbo?"STATUS:status_abo":"STATUS:status_smaprovytor"),true,null,null,true,"["+buttonHash.toString()+"]",false);
		
		//new ButtonBlock("_tabild","Foto","Start_Workflow", "fotobutton","Field_List_panel_1",NamedVariables.WF_FOTO,"action", "status_foto",true,null,null,true,xContext,false);
		/*
		for (int i=0; i<(isAbo?9:3);i++) {
			int j=i+1;
			final String I = j+"";
			Map<String,String> buttonHash = al.createProvytaKeyMap();
			String label ="";
			String wf="";
			buttonHash.put("smaprovyta", I);
			CHash buttonContext = new CHash(null,buttonHash);
			if (isAbo) {
				if (i>=3) {
					label = "�bo sm�prov yttre ";
					wf = NamedVariables.WF_SMAPROV_INMATNING_ABO_YTTRE;
				} else {
					label = "�bo sm�prov inre ";
					wf = NamedVariables.WF_SMAPROV_INMATNING_ABO_INRE;
				}
			} else {
				label = "Sm�provyta ";
				wf = NamedVariables.WF_SMAPROV_INMATNING;
			}


			smayteKnappar[i]=new ButtonBlock("SmaBl"+j,label+j,"Start_Workflow", "Sm�provyta "+j,"Field_List_panel_1",
					wf,"action","status_smaprovyta",true,
					new OnclickExtra() {						
				@Override
				public void onClick() {
					Log.d("nils","Setting current sm�prov to "+I);
					varCache.getVariable(NamedVariables.CURRENT_SMAPROVYTA).setValue(I);
				} },buttonContext,-1);
		}
		*/


		//Varna om provytecentrum flyttat eller saknas.
		Variable pyMarkTypV = varCache.getVariable(al.createProvytaKeyMap(),"ProvytacentrumMarkeringstyp");
		String pyMarkTyp = pyMarkTypV.getHistoricalValue();
		Log.d("nils","ProvytacentrumMarkeringstyp: "+pyMarkTyp);
		//Log.d("nils","FlyttatCentrumVarning"+currRuta+"_"+currPy+" equals "+gs.getPreferences().get("FlyttatCentrumVarning"+currRuta+"_"+currPy));
		if (pyMarkTyp!=null) { 
			if (pyMarkTyp.equals("2")&&gs.getPreferences().get("FlyttatCentrumVarning"+currRuta+"_"+currPy).equals(PersistenceHelper.UNDEFINED)) {
				//String avst = al.getVariableValue(pyKeyMap,"ProfilAvstand");
				//String rikt = al.getVariableValue(pyKeyMap,"ProfilRiktning");
				AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
				alert.setTitle("OBS!");
				alert.setMessage("Profil p� annat st�lle!");					
				alert.setCancelable(false);
				alert.setPositiveButton("Okej", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						gs.getPreferences().put("FlyttatCentrumVarning"+currRuta+"_"+currPy,"Marked");
					}


				});			
				alert.show();
			} else {
				if (pyMarkTyp.equals("3")&&gs.getPreferences().get("IngenProfilVarning"+currRuta+"_"+currPy).equals(PersistenceHelper.UNDEFINED)) {
					//String avst = al.getVariableValue(pyKeyMap,"ProfilAvstand");
					//String rikt = al.getVariableValue(pyKeyMap,"ProfilRiktning");
					AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
					alert.setTitle("OBS!");
					alert.setMessage("Profil saknas!");					
					alert.setCancelable(false);
					alert.setPositiveButton("Okej", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							gs.getPreferences().put("IngenProfilVarning"+currRuta+"_"+currPy,"Marked");
						}


					});			
					alert.show();

				}

			}
		}


		//Rita sidan
		refresh();
		
		if (gs.isMaster()||gs.isSolo())
			Toast.makeText(this.getActivity(),"<<<<<< Svep f�r Delningssidan!", Toast.LENGTH_SHORT).show();

		myContext.registerEvent(new WF_Event_OnSave("Template"));
		
		return v;

	}



	private void refresh() {
		Container myC = myContext.getContainer("Field_List_panel_1");
		statusHandler.setStatusProvyta();
		myContext.emptyContainer(myC);
		fieldListPanel.removeAllViews();
		Kvot k = statusHandler.getStatusDelytor();
		delOutputValueField.setText(k.toString());
		k = statusHandler.getStatusSmaProv();
		smaOutputValueField.setText(k.toString());
		partnerOutputValueField.setText(gs.getMyPartner());
		//if (dym.isObsolete())
		dym.init();

		pyv.showDelytor(dym.getDelytor(),false);

		
		//If master, show T�gsida. 
		//If solo or slave, show photo.
		if (gs.isSolo()||gs.isSlave())
			taBild.create(myContext);
		if (gs.isSolo()||gs.isMaster())
			fieldListPanel.addView(tagSidaB);
		fixPunkter.create(myContext);
		//if (!isAbo)
		//	spillning.create(myContext);

		Set<Integer> dys = new TreeSet<Integer>();
		//gs.setKeyHash(al.createDelytaKeyMap());
		Map<String, String> map;

		for (final Delyta d:dym.getDelytor())
			dys.add(d.getId());
		for (Integer id:dys) {	
			map = al.createProvytaKeyMap();
			map.put("delyta", id+"");
			gs.setDBContext(new DB_Context(null,map));
			delyteKnappar[id].create(myContext);	
			dys.add(id);			
		}
		smayteKnapp.create(myContext);
		
		gs.setDBContext(new DB_Context(null,al.createProvytaKeyMap()));		
		
		myContext.drawRecursively(myC);


	}




	@Override
	protected List<WF_Container> getContainers() {
		myLayouts = new ArrayList<WF_Container>();
		//myContainer = container;
		WF_Container root = new WF_Container("root", (LinearLayout)v.findViewById(R.id.root), null);
		myLayouts.add(root);
		myLayouts.add(new WF_Container("Field_List_panel_1", (LinearLayout)v.findViewById(R.id.fieldList), root));
		myLayouts.add(new WF_Container("Aggregation_panel_3", (LinearLayout)v.findViewById(R.id.aggregates), root));
		myLayouts.add(new WF_Container("Description_panel_1", (FrameLayout)v.findViewById(R.id.Description), root));
		return myLayouts;
	}




	@Override
	public void execute(String function, String target) {
		// TODO Auto-generated method stub

	}


	@Override
	public void onEvent(Event e) {
		if (e.getProvider().equals(Constants.SYNC_ID)) {
			
			refresh();
		}
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
					Fragment gs = new TagTemplate();  			
					ft.replace(R.id.content_frame, gs);
					ft.addToBackStack(null);
					ft.commit(); 
				} 
			}
		}		
	}












}