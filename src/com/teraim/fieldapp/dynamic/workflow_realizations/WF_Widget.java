package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.util.Log;
import android.view.View;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Drawable;

public class WF_Widget extends WF_Thing implements Drawable {

	private View myView;
	private boolean isVisible;
	protected VariableConfiguration al;
	
	public WF_Widget(String id,View v,boolean isVisible,WF_Context myContext) {
		super(id);
		al = GlobalState.getInstance().getVariableConfiguration();
		myView = v;
		if (!isVisible)
			hide();
		this.isVisible = isVisible;
		myContext.addDrawable(id, this);
		
	}


	@Override
	public View getWidget() {
		return myView;
	}


	@Override 
	public boolean isVisible() {
		return isVisible;
	}
	
	@Override
	public void show() {
		Log.d("nils","Showing view ");
		myView.setVisibility(View.VISIBLE);
		isVisible = true;
	}


	@Override
	public void hide() {
		Log.d("nils","Hiding view ");
		myView.setVisibility(View.GONE);
		isVisible = false;
	}



};