package com.teraim.vortex.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
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

			//Create a filter that stops users from entering disallowed characters.
			InputFilter filter = new InputFilter() {
				@Override
				public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
					boolean keepOriginal = true;
					StringBuilder sb = new StringBuilder(end - start);
					for (int i = start; i < end; i++) {
						char c = source.charAt(i);
						if (isCharAllowed(c)) // put your condition here
							sb.append(c);
						else
							keepOriginal = false;
					}
					if (keepOriginal)
						return null;
					else {
						if (source instanceof Spanned) {
							SpannableString sp = new SpannableString(sb);
							TextUtils.copySpansFrom((Spanned) source, start, sb.length(), null, sp, 0);
							return sp;
						} else {
							return sb;
						}           
					}
				}

				private boolean isCharAllowed(char c) {
					return Character.isLetterOrDigit(c) || c=='.' || c=='_'||c=='/'|| c=='-';
				}
			};


			EditTextPreference epref = (EditTextPreference) findPreference(PersistenceHelper.LAG_ID_KEY);
			epref.setSummary(epref.getText());

			ListPreference color = (ListPreference)findPreference(PersistenceHelper.DEVICE_COLOR_KEY);
			color.setSummary(color.getValue());

			epref = (EditTextPreference) findPreference(PersistenceHelper.USER_ID_KEY);
			epref.setSummary(epref.getText());


			epref = (EditTextPreference) findPreference(PersistenceHelper.SERVER_URL);
			epref.setSummary(epref.getText());
			epref.getEditText().setFilters(new InputFilter[] {filter});

			epref = (EditTextPreference) findPreference(PersistenceHelper.BUNDLE_NAME);
			epref.setSummary(epref.getText());
			epref.getEditText().setFilters(new InputFilter[] {filter});
			
			epref = (EditTextPreference) findPreference(PersistenceHelper.BACKUP_LOCATION);
			if (epref.getText()==null||epref.getText().isEmpty()) {
				Log.e("vortex","gets here");
				epref.setText(Constants.DEFAULT_EXT_BACKUP_DIR);
				Log.d("vortex","TEXT: "+epref.getText());
			}
			Log.d("vortex","backup epref txt: "+epref.getText()+" le: "+epref.getText().length());
			epref.setSummary(epref.getText());

			final Preference button = (Preference)findPreference(getString(R.string.resetSyncButton));
			String bName = getActivity().getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_PRIVATE).getString(PersistenceHelper.BUNDLE_NAME,null);
			String syncPValue = getActivity().getSharedPreferences(bName,Context.MODE_PRIVATE).getString(PersistenceHelper.TIME_OF_LAST_SYNC,null);
			if (bName!=null && syncPValue!=null) {
				button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {   
						new AlertDialog.Builder(getActivity())
						.setTitle("Reset Sync")
						.setMessage("Pressing ok will rewind the synchronization pointer to zero. This will synchronize all values with partner device.") 
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setCancelable(false)
						.setPositiveButton(R.string.ok,new Dialog.OnClickListener() {				
							@Override
							public void onClick(DialogInterface dialog, int which) {
								String bName = getActivity().getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_PRIVATE).getString(PersistenceHelper.BUNDLE_NAME,null);
								String syncPValue = getActivity().getSharedPreferences(bName,Context.MODE_PRIVATE).getString(PersistenceHelper.TIME_OF_LAST_SYNC,null);
								Log.d("vortex","syncPValue is "+syncPValue);
								if (bName!=null && syncPValue!=null) {
									getActivity().getSharedPreferences(bName,Context.MODE_PRIVATE).edit().remove(PersistenceHelper.TIME_OF_LAST_SYNC).commit();
									Intent intent = new Intent();
									intent.setAction(MenuActivity.REDRAW);
									getActivity().sendBroadcast(intent);
									button.setEnabled(false);
								}


							}
						} )
						.setNegativeButton(R.string.cancel, new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {

							}
						})
						.show();
						return true;
					}
				});
			} else
				button.setEnabled(false);

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
			GlobalState gs = GlobalState.getInstance();

			if (pref instanceof EditTextPreference) {
				EditTextPreference etp = (EditTextPreference) pref;

				if (key.equals(PersistenceHelper.BUNDLE_NAME)) {
					if (etp.getText().length()!=0) {

						char[] strA = etp.getText().toCharArray();
						strA[0] = Character.toUpperCase(strA[0]);

						etp.setText(new String(strA));

						if (gs != null)  {
							new AlertDialog.Builder(getActivity())
							.setTitle("Restart")
							.setMessage("You need to restart for this change to take effect. Restart now?") 
							.setIcon(android.R.drawable.ic_dialog_alert)
							.setCancelable(false)
							.setPositiveButton(R.string.ok,new Dialog.OnClickListener() {				
								@Override
								public void onClick(DialogInterface dialog, int which) {
									Tools.restart(getActivity());
								}
								
							})
							.setNegativeButton(R.string.cancel, new Dialog.OnClickListener() {				
								@Override
								public void onClick(DialogInterface dialog, int which) {
								}
							})
							.show();
							
						}
					}
				}
				pref.setSummary(etp.getText());

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

			//force redraw of menuactivity.
			Intent intent = new Intent();
			intent.setAction(MenuActivity.REDRAW);
			getActivity().sendBroadcast(intent);
		}





	}

}




