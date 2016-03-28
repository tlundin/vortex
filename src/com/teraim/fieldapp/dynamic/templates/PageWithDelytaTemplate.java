package com.teraim.fieldapp.dynamic.templates;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.non_generics.DelyteManager;
import com.teraim.fieldapp.ui.ProvytaView;

public class PageWithDelytaTemplate extends Executor {

	@Override
	protected List<WF_Container> getContainers() {		
		myLayouts = new ArrayList<WF_Container>();
		WF_Container root = new WF_Container("root", (LinearLayout)v.findViewById(R.id.root), null);
		myLayouts.add(root);
		myLayouts.add(new WF_Container("Field_panel_1", (LinearLayout)v.findViewById(R.id.fieldList), root));
		myLayouts.add(new WF_Container("Aggregation_panel_3", (LinearLayout)v.findViewById(R.id.aggregates), root));
		myLayouts.add(new WF_Container("Description_panel_2", (LinearLayout)v.findViewById(R.id.Description), root));
		
		return myLayouts;
	}

	@Override
	public void execute(String function, String target) {
		// TODO Auto-generated method stub

	}

	List<WF_Container> myLayouts;
	ViewGroup myContainer = null;
	View v;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (!survivedCreate) {
			Log.d("vortex","hasnt survived create...exiting.");
			return null;
		}
		Log.d("nils","in onCreateView of Template PAGE with DELYTA");
		v = inflater.inflate(R.layout.template_page_with_agg_wf, container, false);	
		
		//myContext.onCreateView();
		myContext.addContainers(getContainers());
		
		ViewGroup provytaViewPanel = (LinearLayout)v.findViewById(R.id.Description);
		DelyteManager dym = DelyteManager.getInstance();

		//Marker man = new Marker(BitmapFactory.decodeResource(getResources(),R.drawable.icon_man));
		ProvytaView pyv = new ProvytaView(this.getActivity(), null,Constants.isAbo(dym.getPyID()));		
		
		if (wf!=null) {
			run();
		}	
		
		provytaViewPanel.addView(pyv);
		pyv.showDelytor(dym.getDelytor(),true);
		return v;
		
	}
}
