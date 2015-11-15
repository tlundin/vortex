package com.teraim.vortex.ui;

import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.R;
import com.teraim.vortex.Start;
import com.teraim.vortex.bluetooth.DataSyncSessionManager;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.utils.PersistenceHelper;

/**
 * Parent class for Activities having a menu row.
 * @author Terje
 *
 */
public class MenuActivity extends Activity   {


	private BroadcastReceiver brr;
	private GlobalState gs;
	private PersistenceHelper globalPh;

	private boolean syncIsRunning=false;
	private MenuActivity me;

	public final static String REDRAW = "com.teraim.vortex.menu_redraw";
	public static final String INITDONE = "com.teraim.vortex.init_done";
	public static final String INITSTARTS = "com.teraim.vortex.init_done";
	public static final String SYNC_REQUIRED = "com.teraim.vortex.sync_required";

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
				else if (intent.getAction().equals(INITSTARTS))
					initdone=false;
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
	private boolean initdone=false;
	private void CreateMenu(Menu menu)
	{

		for(int c=0;c<mnu.length-1;c++) {
			mnu[c]=menu.add(0,c,c,"");
			mnu[c].setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM|MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);	

		}
		mnu[1].setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		//mnu[1].setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM|MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		mnu[mnu.length-1]=menu.add(0,mnu.length-1,mnu.length-1,"");
		mnu[mnu.length-1].setIcon(android.R.drawable.ic_menu_preferences);
		mnu[mnu.length-1].setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		//mnu5.setIcon(android.R.drawable.ic_menu_preferences);
		//mnu5.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		//refreshStatusRow();
	}

	protected void refreshStatusRow() {
		Log.d("NILS","Refreshing status row ");
		mnu[2].setTitle("LOG");
		mnu[2].setVisible(true);
		gs = GlobalState.getInstance();
		if (gs==null || !initdone) {
			Log.d("nils","no status before init is done");
			return;
		}
		globalPh = gs.getGlobalPreferences();
		//Log.d("vortex","Global prefs: "+gs.getGlobalPreferences()+" isdev "+globalPh.getB(PersistenceHelper.DEVELOPER_SWITCH));
		if (!syncIsRunning)
			mnu[0].setTitle("Osynkat: "+gs.getDb().getNumberOfUnsyncedEntries());
		else 
			mnu[0].setTitle("Synkar..");
		String mContextH = "Context: []";
		Map<String, String> hash = gs.getCurrentKeyHash();
		if (hash!=null)
			mContextH = hash.toString();
		mnu[1].setTitle(mContextH);
		//mnu[c++].setTitle("Användare: "+gs.getPersistence().get(PersistenceHelper.USER_ID_KEY));
		//mnu[c++].setTitle("Typ: "+gs.getDeviceType());
		boolean hasSynk = globalPh.getB(PersistenceHelper.SYNC_FEATURE)&&!gs.isSolo();
		mnu[0].setVisible(hasSynk);	
		//If (title is empty, don't show r-p-d-l status
		mnu[1].setVisible(globalPh.getB(PersistenceHelper.SHOW_CONTEXT));		
		mnu[2].setVisible(globalPh.getB(PersistenceHelper.DEVELOPER_SWITCH));	

	}

	DataSyncSessionManager syncMgr=null;

	private boolean MenuChoice(MenuItem item) {

		switch (item.getItemId()) {
		case 0:
			startSyncIfNotRunning();
			break;
		case 1:
			Map<String, String> hash = gs.getCurrentKeyHash();
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
					gs.getDb().printAllVariables();
				}
			});

			break;
		case 3:

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
		if (syncMgr==null) 
			syncMgr = new DataSyncSessionManager(MenuActivity.this, new UIProvider(this) {
				@Override
				public void onClose() {
					me.onCloseSync();
					
				};
			});
		else
			Log.d("vortex","Discarded...syncmgr is not null");

	}




	protected void onCloseSync() {
		Log.d("vortex","IN on close SYNC!!");
		if (syncMgr!=null) {
			Log.d("vortex","syncmgr not null!");
			syncMgr.destroy();
			syncMgr=null;
			refreshStatusRow();
		}
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
		public void open() {
			mHandler.obtainMessage(UNLOCK).sendToTarget();
		}

		public void setCounter(String msg) {
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





		public void resynchAtClose() {
			// TODO Auto-generated method stub
			
		}





	}





}
