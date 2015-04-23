package com.teraim.vortex.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.R;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.PersistenceHelper;
import com.teraim.vortex.utils.Tools;

public class ConfigMenu extends PreferenceActivity {

	public void onCreate(Bundle savedInstanceState) {			
		super.onCreate(savedInstanceState);
			
		// Display the fragment as the main content.
		getFragmentManager().beginTransaction()
		.replace(android.R.id.content, new SettingsFragment())
		.commit();
		setTitle("Change Configuration");
	}


	public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			this.getPreferenceManager().setSharedPreferencesName(Constants.GLOBAL_PREFS);
			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.myprefs);
			//Set default values for the prefs.
//			getPreferenceScreen().getSharedPreferences()
//			.registerOnSharedPreferenceChangeListener(this);
			this.getActivity().getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_PRIVATE)
			.registerOnSharedPreferenceChangeListener(this);
		
			EditTextPreference epref = (EditTextPreference) findPreference(PersistenceHelper.LAG_ID_KEY);
			epref.setSummary(epref.getText());

			ListPreference color = (ListPreference)findPreference(PersistenceHelper.DEVICE_COLOR_KEY);
			color.setSummary(color.getValue());

			epref = (EditTextPreference) findPreference(PersistenceHelper.USER_ID_KEY);
			epref.setSummary(epref.getText());
			

			epref = (EditTextPreference) findPreference(PersistenceHelper.SERVER_URL);
			epref.setSummary(epref.getText());
			
			
			epref = (EditTextPreference) findPreference(PersistenceHelper.BUNDLE_NAME);
			epref.setSummary(epref.getText());
			
			//CheckBoxPreference cpref = (CheckBoxPreference) findPreference(PersistenceHelper.DEVELOPER_SWITCH);
			
		}
		
	
		

		/* (non-Javadoc)
		 * @see android.app.Fragment#onPause()
		 */
		@Override
		public void onPause() {
			this.getActivity().getSharedPreferences("GlobalPrefs", Context.MODE_PRIVATE)
			.unregisterOnSharedPreferenceChangeListener(this);
			super.onPause();
		}




		/* (non-Javadoc)
		 * @see android.app.Fragment#onResume()
		 */
		@Override
		public void onResume() {
			//this.getPreferenceManager().setSharedPreferencesName(phone);
			this.getActivity().getSharedPreferences("GlobalPrefs", Context.MODE_PRIVATE)
			.registerOnSharedPreferenceChangeListener(this);			
			//getPreferenceScreen().getSharedPreferences()
			//.registerOnSharedPreferenceChangeListener(this);
			super.onResume();
		}




		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			Preference pref = findPreference(key);
			if (pref instanceof EditTextPreference) {
				EditTextPreference etp = (EditTextPreference) pref;
				pref.setSummary(etp.getText());
				if (key.equals(PersistenceHelper.BUNDLE_NAME)) {
					GlobalState gs = GlobalState.getInstance();
					if (gs != null) 
						//if a state exists, restart the app.
						Tools.restart(this.getActivity());
						
					
						
					
				}

					
			}
			else if (pref instanceof ListPreference) {
				ListPreference letp = (ListPreference) pref;
				pref.setSummary(letp.getValue());
				if (letp.getKey().equals(PersistenceHelper.DEVICE_COLOR_KEY)) {
					if (letp.getValue().equals("Master")) 
						Log.d("nils","Changed to MASTER");
						
					else if (letp.getValue().equals("Client")) 
						Log.d("nils","Changed to CLIENT");
					else if (letp.getValue().equals("Solo")) {
						//Turn off sync if on
						this.getActivity().getSharedPreferences(Constants.GLOBAL_PREFS,Context.MODE_PRIVATE).edit().putBoolean(PersistenceHelper.SYNC_FEATURE,false).apply();
						Log.d("nils","Changed to SOLO");
					}
					Tools.restart(this.getActivity());
					
				}
				
			}
			/*
			else if (pref instanceof CheckBoxPreference) {
				CheckBoxPreference cpref = (CheckBoxPreference)pref;
				if (key.equals(PersistenceHelper.DEVELOPER_SWITCH))
					if (cpref.isChecked()) {
						GlobalState.getInstance(getActivity()).createLogger();
						Log.d("NILS","CREATED LOGGER");
					}
					else {
						Log.d("NILS","UNCREATED LOGGER");
						GlobalState.getInstance(getActivity()).removeLogger();
					}
				}
*/
		}





	}

}




