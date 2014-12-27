package com.teraim.vortex.ui;

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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.GlobalState.ErrorCode;
import com.teraim.vortex.R;
import com.teraim.vortex.bluetooth.BluetoothConnectionService;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.utils.PersistenceHelper;

/**
 * Parent class for Activities having a menu row.
 * @author Terje
 *
 */
public class MenuActivity extends Activity {


	private BroadcastReceiver brr;
	private GlobalState gs;
	private PersistenceHelper ph;
	private AlertDialog x;
	
	public final static String REDRAW = "com.teraim.vortex.menu_redraw";
	public static final String INITDONE = "com.teraim.vortex.init_done";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		final MenuActivity me = this;

		gs = GlobalState.getInstance(this);
		ph = gs.getPersistence();
		
		x =  new AlertDialog.Builder(MenuActivity.this)
		.setTitle("Lång synk - mycket data")
		.setMessage("Skriver in data") 
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setCancelable(false)
				.setPositiveButton("Continue in background", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which)  {
					}
				}).create();
		
		
		
		brr = new BroadcastReceiver() {
			boolean inSameSame=false;
			private int currentBlockCount;
			@Override
			public void onReceive(Context ctx, Intent intent) {
				Log.d("nils", "received "+intent.getAction()+" in MenuActivity BroadcastReceiver");


				if (intent.getAction().equals(BluetoothConnectionService.SYNK_SERVICE_CONNECTED)||
						intent.getAction().equals(BluetoothConnectionService.SYNK_SERVICE_STOPPED)) {

				}
				else if (intent.getAction().equals(BluetoothConnectionService.SYNK_NO_BONDED_DEVICE)) {
					new AlertDialog.Builder(MenuActivity.this)
					.setTitle("Blåtandsproblem")
					.setMessage("För att synkroniseringen ska fungera måste dosorna bindas via blåtandsmenyn. Vill du göra det nu?") 
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setCancelable(false)
					.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which)  {
							Intent intentBluetooth = new Intent();
							intentBluetooth.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
							startActivity(intentBluetooth); 
						}

					})
					.setNegativeButton(android.R.string.no,new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {}} ) 
						.show();

				}
				else if (intent.getAction().equals(BluetoothConnectionService.SAME_SAME_SYNDROME)) {

					if (!inSameSame) {
						inSameSame=true;
						new AlertDialog.Builder(MenuActivity.this)
						.setTitle("Båda samma")
						.setMessage("Båda dosorna är konfigurerade likadant. En måste byta till mästare/slav under skiftnyckels-menyn.") 
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setCancelable(false)
						.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which)  {
								inSameSame = false;
							}

						})
						.show();
					}
				}
				else if (intent.getAction().equals(BluetoothConnectionService.SYNK_INITIATE)) {
					
					/*AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MenuActivity.this);
					// set title
					alertDialogBuilder.setTitle("Synkning av data pågår");
					// set dialog message
					alertDialogBuilder.setMessage("Vänligen vänta lite så att inget går snett.").setCancelable(false);

					// create alert dialog
					AlertDialog alertDialog = alertDialogBuilder.create();

					// show it
					alertDialog.show();
					 */
				}
				else if (intent.getAction().equals(INITDONE))
					initdone=true;
				else if (intent.getAction().equals(BluetoothConnectionService.SYNK_BLOCK_UI)) {
					currentBlockCount = 0;
					x.setMessage("Skriver in data: 0");
					x.show();
				}
				else if (intent.getAction().equals(BluetoothConnectionService.SYNK_UNBLOCK_UI)) 
					x.cancel();
				else if (intent.getAction().equals(BluetoothConnectionService.PING_FROM_UPDATE)) {
					currentBlockCount+=10;
					x.setMessage("Skriver in data: "+currentBlockCount);
				}
					
				btInitDone();
				me.refreshStatusRow();
			}


		};
		//Listen for bluetooth events.
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothConnectionService.SYNK_SERVICE_STARTED);
		filter.addAction(BluetoothConnectionService.SYNK_SERVICE_STOPPED);
		filter.addAction(BluetoothConnectionService.SYNK_SERVICE_CONNECTED);
		filter.addAction(BluetoothConnectionService.SYNK_NO_BONDED_DEVICE);
		filter.addAction(BluetoothConnectionService.SYNK_INITIATE);
		filter.addAction(BluetoothConnectionService.SYNK_DATA_RECEIVED);
		filter.addAction(BluetoothConnectionService.SAME_SAME_SYNDROME);
		filter.addAction(BluetoothConnectionService.SYNK_DATA_TRANSFER_DONE);
		filter.addAction(BluetoothConnectionService.MASTER_CHANGED_MY_CONFIG);
		filter.addAction(BluetoothConnectionService.SYNK_BLOCK_UI);
		filter.addAction(BluetoothConnectionService.SYNK_UNBLOCK_UI);
		filter.addAction(BluetoothConnectionService.PING_FROM_UPDATE);
		filter.addAction(INITDONE);		
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
			mnu[c].setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);	

		}
		mnu[mnu.length-1]=menu.add(0,mnu.length-1,mnu.length-1,"");
		mnu[mnu.length-1].setIcon(android.R.drawable.ic_menu_preferences);
		mnu[mnu.length-1].setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		//mnu5.setIcon(android.R.drawable.ic_menu_preferences);
		//mnu5.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		//refreshStatusRow();
	}

	protected void refreshStatusRow() {
		Log.d("NILS","Refreshing status row ");
		if (!initdone) {
			Log.d("nils","no status before init is done");
			return;
		}
		int c=0;
		String r,p,l,d;
		p= gs.getArtLista().getVariableValue(null,"Current_Provyta");
		r= gs.getArtLista().getVariableValue(null,"Current_Ruta");
		l= gs.getArtLista().getVariableValue(null, "Current_Linje");
		d= gs.getArtLista().getVariableValue(null, "Current_Delyta");
		String pid = p==null?"?":p;
		String rid = r==null?"?":r;
		String lid = l==null?"?":l;
		String did = d==null?"?":d;
		mnu[c++].setTitle("Osynkat: "+gs.getDb().getNumberOfUnsyncedEntries());
		mnu[c++].setTitle("R:"+rid+" P:"+pid+" L:"+lid+" D:"+did);
		mnu[c++].setTitle("LOG");
		mnu[c++].setTitle("SYNK "+gs.getSyncStatusS());
		//mnu[c++].setTitle("Användare: "+gs.getPersistence().get(PersistenceHelper.USER_ID_KEY));
		//mnu[c++].setTitle("Typ: "+gs.getDeviceType());
		if(!ph.getB(PersistenceHelper.DEVELOPER_SWITCH)) {
			Log.d("nils","devswitch off");
			mnu[2].setVisible(false);
		}
		else {
			Log.d("nils","devswitch on");
			mnu[2].setVisible(true);
		}


	}

	private boolean BTInProgress=false;
	private void btInitDone() {
		BTInProgress=false;
	}

	private boolean MenuChoice(MenuItem item) {


		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
				case DialogInterface.BUTTON_POSITIVE:
					//Turn off bluetooth if running
					//This will also turn off the server as a side effect.
					//Intent intent = new Intent();
					//intent.setAction(BluetoothAdapter.ACTION_STATE_CHANGED);
					Intent intent = new Intent(getBaseContext(),BluetoothConnectionService.class);
					if (gs.getSyncStatus()==BluetoothConnectionService.SYNK_STOPPED) {
						//Check that synk can start.
						ErrorCode err = gs.checkSyncPreconditions();
						if (err == ErrorCode.ok) {
							Log.d("nils","Trying to start bt-service");
							startService(intent);
						}
						else {							
							new AlertDialog.Builder(MenuActivity.this)
							.setTitle("Synkning kan inte genomföras just nu.")
							.setMessage("Felkod: "+err.name()) 
							.setIcon(android.R.drawable.ic_dialog_alert)
							.setCancelable(false)
							.setPositiveButton(R.string.iunderstand, new DialogInterface.OnClickListener() {						
								@Override
								public void onClick(DialogInterface dialog, int which)  {
									btInitDone();
								}

							})				 
							.show();							
						}
						//Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
						//startActivity(enableBtIntent);
						//intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
					}

					else {
						stopService(intent);
						//intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON);
					}
					//getBaseContext().sendBroadcast(intent);
					break;

				case DialogInterface.BUTTON_NEGATIVE:
					btInitDone();
					break;
				}
			}
		};

		switch (item.getItemId()) {
		case 0:
		case 1:
			break;

		case 2:
			final Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.log_dialog_popup);
			dialog.setTitle("Session Log");
			final TextView tv=(TextView)dialog.findViewById(R.id.logger);
			Typeface type=Typeface.createFromAsset(getAssets(),
					"clacon.ttf");
			tv.setTypeface(type);
			final LoggerI log = gs.getLogger();
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

			break;
		case 3:
			if (!BTInProgress) {
				BTInProgress=true;
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Synkronisering")
				.setMessage("Vill du "+(gs.getSyncStatus()==BluetoothConnectionService.SYNK_STOPPED?"slå på ":"stänga av ")+"synkroniseringen?").setPositiveButton("Ja", dialogClickListener)
				.setNegativeButton("Nej", dialogClickListener).show()
				.setCanceledOnTouchOutside(false);
			}
			break;
		case 4:
			Intent intent = new Intent(getBaseContext(),ConfigMenu.class);
			startActivity(intent);
			return true;
		}
		return false;
	}


}
