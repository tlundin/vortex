package com.teraim.vortex.ui;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.R;
import com.teraim.vortex.Start;
import com.teraim.vortex.bluetooth.BluetoothConnectionService;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.PersistenceHelper;

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
					if (gs != null) {
						//if a state exists, restart the app.
						Log.d("vortex","restarting...bundle name now "+gs.getGlobalPreferences().get(PersistenceHelper.BUNDLE_NAME));
						Activity context = this.getActivity();
						android.app.FragmentManager fm = context.getFragmentManager();
						for(int i = 0; i < fm.getBackStackEntryCount(); ++i) {    
						    fm.popBackStack();
						}
						Intent intent = new Intent(context, Start.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
						context.finish();
						/*
						int mPendingIntentId = 123456;
						PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
						AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
						mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
						System.exit(0);
						*/
						
					}
				}

					
			}
			else if (pref instanceof ListPreference) {
				ListPreference letp = (ListPreference) pref;
				pref.setSummary(letp.getValue());
				if (letp.getKey().equals(PersistenceHelper.DEVICE_COLOR_KEY)) {
					if (letp.getValue().equals("Master")) 
						Log.d("nils","Changed to MASTER");
						
					else 
						Log.d("nils","Changed to CLIENT");
					Intent intent = new Intent(this.getActivity().getBaseContext(),BluetoothConnectionService.class);
					this.getActivity().stopService(intent);
					GlobalState.getInstance().resetHandler();
					
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




