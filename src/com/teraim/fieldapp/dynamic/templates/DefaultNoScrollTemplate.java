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

/**
 * 
 * @author Terje
 * Activity that runs a workflow that has a user interface.
 * Pressing Back button will return flow to parent workflow.
 */

public class DefaultNoScrollTemplate extends Executor {

	View view; 
	private LinearLayout my_root;

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("nils","In onCreate");

		
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d("nils","I'm in the onPause method");
	}
	
	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d("nils","I'm in the onCreateView method");
		if (view == null) {
		view = inflater.inflate(R.layout.template_wf_default_no_scroll, container, false);	
		
		
//		errorView = (TextView)v.findViewById(R.id.errortext);
		my_root = (LinearLayout)view.findViewById(R.id.myRoot);
//		my_pie = (LinearLayout)v.findViewById(R.id.pieRoot);
		if (myContext != null )
			myContext.addContainers(getContainers());
		
		
		if (wf!=null) {
			Log.d("vortex","Executing workflow!!");
			run();
			
		} else
			Log.d("vortex","No workflow found in oncreate default!!!!");
			
		} else {
			//If view exists, we are moving backwards in the stack. GIS objects need to drop their cached values.
			if (myContext!=null && myContext.getCurrentGis()!=null) {
				Log.d("vortex","Clearing gis cache in onCreateView");
				myContext.getCurrentGis().clearLayerCaches();
				myContext.getCurrentGis().getGis().initializeAndSiftGisObjects();
			}
				
		}
		return view;
	}
	

	@Override
	protected List<WF_Container> getContainers() {
		ArrayList<WF_Container> ret = new ArrayList<WF_Container>();
		ret.add(new WF_Container("root",my_root,null));
//		ret.add(new WF_Container("pie",my_pie,null));

		return ret;
	}
	@Override
	public void execute(String function, String target) {
		
	}
	
	

	@Override
	public void onStart() {
		Log.d("nils","I'm in the onStart method");
		super.onStart();


	}





}
