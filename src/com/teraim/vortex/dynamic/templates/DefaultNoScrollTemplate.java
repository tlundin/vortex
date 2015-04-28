package com.teraim.vortex.dynamic.templates;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.teraim.vortex.R;
import com.teraim.vortex.dynamic.Executor;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Container;

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
		
		View v = inflater.inflate(R.layout.template_wf_default_no_scroll, container, false);	
//		errorView = (TextView)v.findViewById(R.id.errortext);
		my_root = (LinearLayout)v.findViewById(R.id.myRoot);
//		my_pie = (LinearLayout)v.findViewById(R.id.pieRoot);
		myContext.addContainers(getContainers());

		if (wf!=null) {
			Log.d("vortex","Executing workflow!!");
			run();
		} else
			Log.d("vortex","No workflow found in oncreate default!!!!");
			
		
		return v;
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
