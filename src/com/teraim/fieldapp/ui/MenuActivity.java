package com.teraim.fieldapp.ui;

import java.util.Map;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.synchronization.DataSyncSessionManager;
import com.teraim.fieldapp.utils.PersistenceHelper;

/**
 * Parent class for Activities having a menu row.
 * @author Terje
 *
 */
public class MenuActivity extends Activity   {


	private BroadcastReceiver brr;
	private GlobalState gs;
	private PersistenceHelper globalPh;
	private boolean initdone=false,initfailed=false;

	private boolean syncIsRunning=false;
	private MenuActivity me;

	public final static String REDRAW = "com.teraim.fieldapp.menu_redraw";
	public static final String INITDONE = "com.teraim.fieldapp.init_done";
	public static final String INITSTARTS = "com.teraim.fieldapp.init_done";
	public static final String INITFAILED = "com.teraim.fieldapp.init_done_but_failed";	
	public static final String SYNC_REQUIRED = "com.teraim.fieldapp.sync_required";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		me = this;






		brr = new BroadcastReceiver() {
			@Override
			public void onReceive(Context ctx, Intent intent) {
				Log.d("nils", "received "+intent.getAction()+" in MenuActivity BroadcastReceiver");

				if (intent.getAction().equals(INITDONE))
					initdone=true;
				else if (intent.getAction().equals(INITSTARTS)) {
					initdone=false;
					initfailed=false;
				}
				else if (intent.getAction().equals(INITFAILED)) {
					initfailed=true;
				}
				else if (intent.getAction().equals(SYNC_REQUIRED))
					startSyncIfNotRunning();

				me.refreshStatusRow();
			}


		};
		//Listen for bluetooth events.
		IntentFilter filter = new IntentFilter();
		filter.addAction(INITDONE);		
		filter.addAction(INITSTARTS);	
		filter.addAction(REDRAW);
		filter.addAction(INITFAILED);

		this.registerReceiver(brr, filter);
		//Listen for Service started/stopped event.



	}




	@Override
	public void onDestroy()
	{
		Log.d("NILS", "In the onDestroy() event");

		//Stop listening for bluetooth events.
		this.unregisterReceiver(brr);
		super.onDestroy();

	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		CreateMenu(menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		refreshStatusRow();
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		return MenuChoice(item);
	}

	private final static int NO_OF_MENU_ITEMS = 5;

	MenuItem mnu[] = new MenuItem[NO_OF_MENU_ITEMS];

	private void CreateMenu(Menu menu)
	{

		for(int c=0;c<mnu.length-1;c++) {
			mnu[c]=menu.add(0,c,c,"");
			mnu[c].setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);	

		}
		//mnu[1].setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);		
		//mnu[1].setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM|MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		mnu[mnu.length-1]=menu.add(0,mnu.length-1,mnu.length-1,"");
		mnu[mnu.length-1].setIcon(android.R.drawable.ic_menu_preferences);
		mnu[mnu.length-1].setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		mnu[mnu.length-1].setVisible(false);
		//mnu5.setIcon(android.R.drawable.ic_menu_preferences);
		//mnu5.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		//refreshStatusRow();
	}

	protected void refreshStatusRow() {
		Log.d("NILS","Refreshing status row ");
		mnu[2].setTitle("LOG");
		mnu[2].setVisible(true);
		gs = GlobalState.getInstance();
		if (initfailed) {
			mnu[mnu.length-1].setVisible(true);
			return;
		}
		if (gs==null || !initdone) 
			return;

		globalPh = gs.getGlobalPreferences();
		//Log.d("vortex","Global prefs: "+gs.getGlobalPreferences()+" isdev "+globalPh.getB(PersistenceHelper.DEVELOPER_SWITCH));
		if (!syncIsRunning)
			mnu[0].setTitle("Osynkat: "+gs.getDb().getNumberOfUnsyncedEntries());
		else 
			mnu[0].setTitle("Synkar..");
		mnu[1].setTitle("Context");

		mnu[0].setVisible(!globalPh.get(PersistenceHelper.SYNC_METHOD).equals("NONE")&&!gs.isSolo());	
		mnu[1].setVisible(globalPh.getB(PersistenceHelper.SHOW_CONTEXT));		
		mnu[2].setVisible(globalPh.getB(PersistenceHelper.DEVELOPER_SWITCH));		
		mnu[3].setVisible(true);
		mnu[mnu.length-1].setVisible(true);

		if (globalPh.getB(PersistenceHelper.SYNC_VIA_INTERNET)&&
				globalPh.get(PersistenceHelper.SYNC_METHOD).equals("Internet"))
			mnu[3].setIcon(R.drawable.syncon);
		else if (!globalPh.getB(PersistenceHelper.SYNC_VIA_INTERNET)) {
			if (globalPh.get(PersistenceHelper.SYNC_METHOD).equals("Internet"))
				mnu[3].setIcon(R.drawable.syncoff);
			else
				mnu[3].setIcon(null);
		}

	}



	private boolean MenuChoice(MenuItem item) {

		switch (item.getItemId()) {
		case 0:
			if (globalPh.get(PersistenceHelper.SYNC_METHOD).equals("Bluetooth"))
				startSyncIfNotRunning();
			break;
		case 1:
			Map<String, String> hash = gs.getCurrentKeyMap();
			String dialogText="";
			if (hash!=null)
				dialogText = hash.toString();
			new AlertDialog.Builder(this)
			.setTitle("Context")
			.setMessage(dialogText) 
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setCancelable(false)
			.setNeutralButton("Ok",new Dialog.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {

				}
			} )
			.show();
			break;

		case 2:
			final Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.log_dialog_popup);
			dialog.setTitle("Session Log");
			final TextView tv=(TextView)dialog.findViewById(R.id.logger);
			final ScrollView sv=(ScrollView)dialog.findViewById(R.id.logScroll);
			Typeface type=Typeface.createFromAsset(getAssets(),
					"clacon.ttf");
			tv.setTypeface(type);
			final LoggerI log = Start.singleton.getLogger();
			log.setOutputView(tv);
			//trigger redraw.
			log.draw();
			Button close=(Button)dialog.findViewById(R.id.log_close);
			dialog.show();
			close.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
				}
			});
			Button clear = (Button)dialog.findViewById(R.id.log_clear);
			clear.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					log.clear();
				}
			});
			Button scrollD = (Button)dialog.findViewById(R.id.scrollDown);
			scrollD.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					sv.post(new Runnable() {
						@Override
						public void run() {
							sv.fullScroll(ScrollView.FOCUS_DOWN);
						}
					});
				}
			});
			Button print = (Button)dialog.findViewById(R.id.printdb);
			print.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					gs.getBackupManager().backupDatabase("dump");
				}
			});

			break;
		case 3:
			Account mAccount = Start.CreateSyncAccount(getApplicationContext());
			if (!globalPh.getB(PersistenceHelper.SYNC_VIA_INTERNET)) {
				ContentResolver.addPeriodicSync(
						mAccount,
						Start.AUTHORITY,
						Bundle.EMPTY,
						Start.SYNC_INTERVAL);
				ContentResolver.setSyncAutomatically(mAccount, Start.AUTHORITY, true);
				globalPh.put(PersistenceHelper.SYNC_VIA_INTERNET,true);
				Log.d("vortex", "Internet sync started");
			} else {
				ContentResolver.setSyncAutomatically(mAccount, Start.AUTHORITY, false);
				globalPh.put(PersistenceHelper.SYNC_VIA_INTERNET,false);
			}
			this.refreshStatusRow();
			break;
		case 4:
			//close drawer menu if open
			if (Start.singleton.getDrawerMenu()!=null)
				Start.singleton.getDrawerMenu().closeDrawer();
			Intent intent = new Intent(getBaseContext(),ConfigMenu.class);
			startActivity(intent);
			return true;
		}
		return false;
	}


	private void startSyncIfNotRunning() {
		DataSyncSessionManager.start(MenuActivity.this, new UIProvider(this) {
			@Override
			public void onClose() {
				me.onCloseSync();

			};
		});

	}




	protected void onCloseSync() {
		Log.d("vortex","IN on close SYNC!!");
		refreshStatusRow();
		DataSyncSessionManager.stop();
	}


	/**
	 * 
	 * @author Terje
	 *
	 * Helper class that allows other threads to interact with the UI main thread. 
	 * Caller must override onClose for specific actions to happen when aborting sync.
	 */

	public class UIProvider {

		public static final int LOCK =1, UNLOCK=2, ALERT = 3, UPDATE_SUB = 4, CONFIRM = 5, UPDATE = 6; 
		private String row1="",row2="";
		private AlertDialog uiBlockerWindow=null;  
		public static final int Destroy_Sync = 1;

		Handler mHandler= new Handler(Looper.getMainLooper()) {
			boolean twoButton=false;

			private void oneButton() {
				dismiss();
				uiBlockerWindow =  new AlertDialog.Builder(mContext)
				.setTitle("Synchronizing")
				.setMessage("Receiving data..standby") 
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setCancelable(false)
				.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						onClose();
					}
				})
				.show();
				twoButton=false;
			}

			private void twoButton(final ConfirmCallBack cb) {
				dismiss();
				uiBlockerWindow =  new AlertDialog.Builder(mContext)
				.setTitle("Synchronizing")
				.setMessage("Receiving data..standby") 
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setCancelable(false)
				.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						//onClose must be overridden. 
						onClose();				
					}
				})
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						cb.confirm();

					}
				})
				.show();
				twoButton=true;
			}


			private void dismiss() {
				if (uiBlockerWindow!=null)
					uiBlockerWindow.dismiss();
			}

			@Override
			public void handleMessage(Message msg) {

				switch (msg.what) {
				case LOCK:
					oneButton();
					Log.d("vortex","I get here?");
					break;
				case UNLOCK:
					uiBlockerWindow.cancel();
					break;
				case ALERT:
					if (twoButton)
						oneButton();
					row1 = (String)msg.obj;
					row2="";
					uiBlockerWindow.setMessage(row1+"\n"+row2);



					break;
				case UPDATE:
					row1 = (String)msg.obj;
					uiBlockerWindow.setMessage(row1+"\n"+row2);
					break;
				case UPDATE_SUB:
					row2 = (String)msg.obj;
					uiBlockerWindow.setMessage(row1+"\n"+row2);
					break;

				case CONFIRM:
					if (!twoButton)
						twoButton((ConfirmCallBack)msg.obj);

					break;

				}


			}


		};
		private Context mContext;

		public UIProvider(Context context) {

			mContext = context;
		}





		public void lock() {
			Log.d("vortex","Lock called");
			mHandler.obtainMessage(LOCK).sendToTarget();

		}

		/**
		 * 
		 * @param msg
		 * Shows message and switched to one button dialog.
		 */
		public void alert(String msg) {
			mHandler.obtainMessage(ALERT,msg).sendToTarget();

		}

		public void syncTry(String msg) {
			mHandler.obtainMessage(ALERT,msg).sendToTarget();

		}

		public void open() {
			mHandler.obtainMessage(UNLOCK).sendToTarget();
		}

		public void setInfo(String msg) {
			mHandler.obtainMessage(UPDATE_SUB,msg).sendToTarget();

		}


		public void onClose() {

		}

		/**
		 * 
		 * @param msg
		 * Shows message and switched to two button dialog. Callback if positive ok is pressed. Otherwise onClose.
		 */
		public void confirm(String msg, final ConfirmCallBack cb) {
			mHandler.obtainMessage(CONFIRM,cb).sendToTarget();
			mHandler.obtainMessage(UPDATE,msg).sendToTarget();
		}

		/**
		 * 
		 * @param msg
		 * Does not change dialog type.
		 */
		public void update(String msg) {
			mHandler.obtainMessage(UPDATE,msg).sendToTarget();			
		}





		


		/**if (globalPh.getB(PersistenceHelper.SYNC_VIA_INTERNET)) {
						Account mAccount = Start.CreateSyncAccount(getActivity().getApplicationContext());
						Log.d("vortex", "periodic sync starts!");
						ContentResolver.addPeriodicSync(
								mAccount,
								Start.AUTHORITY,
								Bundle.EMPTY,
								Start.SYNC_INTERVAL);
						ContentResolver.setSyncAutomatically(mAccount, Start.AUTHORITY, true);
					}
		 */


	}





}
