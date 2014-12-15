package com.teraim.nils.ui;

import java.util.ArrayList;
import java.util.List;

import com.teraim.nils.R;
import com.teraim.nils.dynamic.VariableConfiguration;
import com.teraim.nils.dynamic.templates.ProvytaTemplate;
import com.teraim.nils.dynamic.templates.SimpleRutaTemplate;
import com.teraim.nils.dynamic.types.Workflow;
import com.teraim.nils.non_generics.Start;
import com.teraim.nils.ui.DrawerMenuAdapter.RowType;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class DrawerMenu {

	private Activity frameActivity;
	private DrawerMenuAdapter mAdapter;
	private List<DrawerMenuItem> items;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;

	public DrawerMenu(Activity a) {
	
		frameActivity=a;
	}
	
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		private boolean firstTimeClick=true;

		@Override
		public void onItemClick(AdapterView parent, View view, int position, long id) {
			if (firstTimeClick) {
				// Create new fragment and transaction
				Fragment newFragment = new BackgroundFragment();
				FragmentTransaction transaction = frameActivity.getFragmentManager().beginTransaction();
				// Replace whatever is in the fragment_container view with this fragment,
				// and add the transaction to the back stack
				transaction.replace(R.id.content_frame, newFragment);			
				// Commit the transaction
				transaction.commit();
				firstTimeClick=false;

			}

			if (items.get(position).getViewType()!=RowType.HEADER_ITEM.ordinal()) {				
				selectItem(position);
			} else {
				Log.d("nils","header selected. no action");
			}
		}
		
		/** Swaps fragments in the main content view */
		private void selectItem(int position) {

			// Highlight the selected item, update the title, and close the drawer
			mDrawerList.setItemChecked(position, true);
			//String wfId = mapItemsToName.get(position);

			//Workflow wf = gs.getWorkflowFromLabel(wfId);

			// Create a new fragment and specify the  to show based on position
			Fragment fragment=null;
			int p=1;
			if (wf!=null) 
				fragment = wf.createFragment();
			else
				fragment = new Fragment();

			Bundle args = new Bundle();
			args.putString("workflow_name", wf==null?wfId:wf.getName());
			fragment.setArguments(args);

			// Insert the fragment by replacing any existing fragment
			Start.singleton.changePage(fragment, wfId);
		}
	}
	
	public void createMenu(ArrayList<DrawerMenuItem> items) {
		//drawer items

		mAdapter = new DrawerMenuAdapter(frameActivity, items);

		mDrawerLayout = (DrawerLayout) frameActivity.findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) frameActivity.findViewById(R.id.left_drawer);

		// Set the adapter for the list view
		mDrawerList.setAdapter(mAdapter);
		// Set the list's click listener
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		mDrawerToggle = new ActionBarDrawerToggle(
				frameActivity,                  /* host Activity */
				mDrawerLayout,         /* DrawerLayout object */
				R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
				R.string.drawer_open,  /* "open drawer" description */
				R.string.drawer_close  /* "close drawer" description */
				) {

			/** Called when a drawer has settled in a completely closed state. */
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);

			}

			/** Called when a drawer has settled in a completely open state. */
			public void onDrawerOpened(View drawerView) {
				//createDrawerMenu(wfs);
				//mAdapter.notifyDataSetChanged();				
				super.onDrawerOpened(drawerView);

			}

		};

		// Set the drawer toggle as the DrawerListener
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		
		mAdapter.notifyDataSetChanged();				
		

	}
	
	public void closeDrawer() {
		mDrawerLayout.closeDrawers();
	}
	
	public void openDrawer() {			
		mDrawerLayout.openDrawer(Gravity.LEFT);	
	}
	/*
	private void createDrawerMenu(Workflow main) {
		mapItemsToName.clear();
		if (myState == State.POST_INIT) {
			VariableConfiguration al = gs.getArtLista();		
			items.clear();
			//The main workflow defines the menuitems.
			//xx
			//Add "static" headers to menu.
			items.add(new DrawerMenuHeader("Huvudmoment"));
			int c = 1;
			addItem(c++,"Ruta");
			String cr = al.getVariableValue(null,"Current_Ruta");
			if (cr!=null)
				addItem(c++,"Provyta/Linje");		
			items.add(new DrawerMenuHeader("Detalj"));
			c++;
			String cp = al.getVariableValue(null, "Current_Provyta");
			String dy = al.getVariableValue(null, "Current_Delyta");
			if (cp!=null && cr!=null && dy != null) {
				for (int i=0;i<wfs.length;i++) 
					addItem(c++,wfs[i]);
			}

		} else {
			items.add(new DrawerMenuHeader("Standby.."));
		}

	}
	*/

	public ActionBarDrawerToggle getDrawerToggle() {
		return mDrawerToggle;
	}
}

	