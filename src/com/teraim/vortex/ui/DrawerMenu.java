package com.teraim.vortex.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.teraim.vortex.R;
import com.teraim.vortex.dynamic.types.Workflow;
import com.teraim.vortex.non_generics.Start;
import com.teraim.vortex.ui.DrawerMenuAdapter.RowType;

public class DrawerMenu {

	private Activity frameActivity;
	private DrawerMenuAdapter mAdapter;
	private List<DrawerMenuItem> items;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private int currentIndex=0;
	private Map<Integer,Integer> index; 
	private List<Workflow> workflowsL;
	
	public DrawerMenu(Activity a) {	
		frameActivity=a;
		createMenu();
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
			Integer pos = index.get(position);
			if (pos!=null) {
				Workflow wf = workflowsL.get(pos);
				//Workflow wf = gs.getWorkflowFromLabel(wfId);
				if (wf == null)
					Log.e("vortex","ups!!! Got null when looking for workflow ");
				else {
					// Create a new fragment and specify the  to show based on position
					Fragment fragment=wf.createFragment();
					Bundle args = new Bundle();
					args.putString("workflow_name", wf.getName());
					fragment.setArguments(args);
					DrawerMenu.this.closeDrawer();
					// Insert the fragment by replacing any existing fragment
					Start.singleton.changePage(fragment, wf.getLabel());
				}
			} else
				Log.e("vortex","Could not find any entry for menu position "+position);
		}
	}
		public void createMenu() {
			//drawer items
			//different if already created?
			items = new ArrayList<DrawerMenuItem>(); 
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

			workflowsL = new ArrayList<Workflow>();
			index = new HashMap<Integer,Integer>();
			currentIndex=0;
			
		}

		public void addHeader(String label, String bgColor,String textColor) {
			items.add(new DrawerMenuHeader(label,bgColor,textColor));
		}

		public void addItem(String label, Workflow wf,String bgColor,String textColor) {
			//add the workflow reference to a list
			workflowsL.add(wf);
			//keep track of the location.
			index.put(items.size(),currentIndex++);
			items.add(new DrawerMenuSelectable(label,bgColor,textColor));

		}

		public void closeDrawer() {
			mDrawerLayout.closeDrawers();
		}

		public void openDrawer() {	
			mAdapter.notifyDataSetChanged();				
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
		// TODO Auto-generated method stub
		return mDrawerToggle;
	}

	

}