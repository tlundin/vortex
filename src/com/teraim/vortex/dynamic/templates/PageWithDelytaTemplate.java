package com.teraim.vortex.dynamic.templates;

import java.util.ArrayList;
import java.util.List;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.teraim.vortex.R;
import com.teraim.vortex.dynamic.Executor;
import com.teraim.vortex.dynamic.types.Marker;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Container;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.non_generics.DelyteManager;
import com.teraim.vortex.ui.ProvytaView;

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
		if (myContext!=null) {
		Log.d("nils","in onCreateView of Template PAGE with DELYTA");
		v = inflater.inflate(R.layout.template_page_with_agg_wf, container, false);	
		
		myContext.onCreateView();
		myContext.addContainers(getContainers());
		
		ViewGroup provytaViewPanel = (LinearLayout)v.findViewById(R.id.Description);
		DelyteManager dym = DelyteManager.getInstance();
		if (dym==null)
			dym = DelyteManager.create(gs,Integer.parseInt(al.getCurrentProvyta()));
		//Marker man = new Marker(BitmapFactory.decodeResource(getResources(),R.drawable.icon_man));
		ProvytaView pyv = new ProvytaView(activity, null, null,Constants.isAbo(dym.getPyID()));		
		
		if (wf!=null) {
			run();
		}	
		
		provytaViewPanel.addView(pyv);
		pyv.showDelytor(dym.getDelytor(),true);
		return v;
		}
		return super.onCreateView(inflater, container, savedInstanceState);
	}
}
