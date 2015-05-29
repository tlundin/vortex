package com.teraim.vortex.dynamic.workflow_realizations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.os.Handler;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TableLayout.LayoutParams;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.Executor;
import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.workflow_abstracts.Filter;
import com.teraim.vortex.dynamic.workflow_abstracts.Filterable;
import com.teraim.vortex.dynamic.workflow_abstracts.Listable;
import com.teraim.vortex.dynamic.workflow_abstracts.Sortable;
import com.teraim.vortex.dynamic.workflow_abstracts.Sorter;

public abstract class WF_List extends WF_Widget implements Sortable,Filterable {

	protected final List<Listable> list = new  ArrayList<Listable>(); //Instantiated in constructor
	protected final List<Filter> myFilters=new ArrayList<Filter>();
	protected final List<Sorter> mySorters=new ArrayList<Sorter>();
	protected List<? extends Listable> filteredList;

	protected WF_Context myContext;
	protected GlobalState gs;
	protected VariableConfiguration al;

	//How about using the Container's panel?? TODO
	public WF_List(String id,boolean isVisible,WF_Context ctx) {
		super(id,new LinearLayout(ctx.getContext()),isVisible,ctx);	
		myWidget = (LinearLayout)getWidget();
		myWidget.setOrientation(LinearLayout.VERTICAL);
		myWidget.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		myContext = ctx;
		gs = GlobalState.getInstance();
		al = gs.getVariableConfiguration();
	}
	
	@Override
	public void addSorter(Sorter s) {
		mySorters.add(s);
	}
	@Override
	public void removeSorter(Sorter s) {
		mySorters.remove(s);
	}
	@Override
	public void addFilter(Filter f) {
		myFilters.add(f);
	}

	@Override
	public void removeFilter(Filter f) {
		myFilters.remove(f);
	}
	
	public List<Listable> getList() {
		return list;
	}
	

	int intC=0;
	boolean drawActive = false;
	public void draw() {
		Log.d("draw","DRAW CALLED "+ (++intC)+" times from list"+this.getId());
		//Log.d("nils","DrawActive "+drawActive);
		if (!drawActive) {
			//Log.d("nils","Settingdrawactive to true from list"+this.getId());
			drawActive = true;
			new Handler().postDelayed(new Runnable() {
				public void run() {
					filteredList = list;
					if (myFilters != null) {			
						List<Listable> listx = new ArrayList<Listable>(list);
						for (Filter f:myFilters) {
							f.filter(listx);
						}
						filteredList = listx;
					}
					//Log.d("nils","before sorter: "+System.currentTimeMillis());
					if (mySorters != null) {
						for (Sorter s:mySorters) {
							filteredList = s.sort(filteredList);
						}
					}
					//Log.d("nils","After sorter: "+System.currentTimeMillis());

					//Log.d("nils","in redraw...");
					myWidget.removeAllViews();
					for (Listable l:filteredList) {
						//l.refreshInputFields();
						l.refresh();						
						//Everything is WF_Widgets, so this is safe!					
						myWidget.addView(((WF_Widget)l).getWidget());
					} 
					//Log.d("nils","Settingdrawactive to false");
					drawActive = false;
				}
			}, 0);
		} else
			Log.d("nils","DISCARDED DRAW CALL");


	}
	


}
