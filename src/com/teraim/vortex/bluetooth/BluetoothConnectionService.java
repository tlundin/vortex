package com.teraim.vortex.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.GlobalState.SyncStatus;
import com.teraim.vortex.exceptions.BluetoothDevicesNotPaired;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.PersistenceHelper;

public class BluetoothConnectionService  {



	private static BluetoothConnectionService singleton =null;
	private static BluetoothAdapter mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
	//final Activity mActivity;

	public static final String STATUS = "sync_status_message";

	public final static String SYNK_SERVICE_STOPPED = "com.teraim.vortex.synkstopped";
	
	public final static String SYNK_SERVICE_MESSAGE_RECEIVED = "com.teraim.vortex.message";
	public final static String SYNK_SERVICE_READ = "com.teraim.vortex.synk_read";
	public final static String SYNK_SERVICE_WRITE = "com.teraim.vortex.synk_write";
	public final static String SYNC_SERVICE_CONNECTION_ATTEMPT_FAILED = "com.teraim.vortex.synk_no_connect";
	public final static String SYNC_SERVICE_CONNECTION_BROKEN = "com.teraim.vortex.synk_attempt_fail";
//	public final static String SYNK_PING_MESSAGE_RECEIVED = "com.teraim.vortex.ping";
	public final static String SYNK_NO_BONDED_DEVICE = "com.teraim.vortex.binderror";
	public static final String SYNK_INITIATE = "com.teraim.vortex.synkinitiate";
	public static final String SAME_SAME_SYNDROME = "com.teraim.vortex.master_syndrome";
	public static final String SYNK_DATA_RECEIVED = "com.teraim.vortex.synk_data_received";
	public static final String BLUETOOTH_MESSAGE_RECEIVED = "com.teraim.vortex.templatemessage";
	public static final String RUTA_DONE = "com.teraim.vortex.ruta_done";
	public static final String MASTER_CHANGED_MY_CONFIG = "com.teraim.vortex.master_changed_my_config";
	public static final String PING_FROM_UPDATE = "com.teraim.vortex.ping_from_update";
	public static final String VERSION_MISMATCH = "com.teraim.vortex.version_mismatch";
	public static final String BLUETOOTH_RESTART_REQUIRED = "restart_required";



	//Ping_delay
	protected static final long PING_DELAY = 3000;
	
	private BroadcastReceiver brr=null;
	//Try pinging five times. Before giving up.
	private int pingC = 5;
	private GlobalState gs;
	private LoggerI o;
	private Context ctx;
	
	public static void initialize(Context context) {
		kill();
		singleton =
			new  BluetoothConnectionService(context);
		
	}
	public static void kill() {
		if (singleton != null)
			singleton.stop();
	}
	public BluetoothConnectionService(Context ctx) {
		this.ctx=ctx;
		gs = GlobalState.getInstance();	
		if (gs!=null) {
		o = gs.getLogger();
		Log.d("NILS","Bluetooth on create");
		o.addRow("BlueTooth service starting up");
		
		
		

		//showNotification();
		brr = new BroadcastReceiver() {
			@Override
			public void onReceive(final Context ctx, Intent intent) {
				final String action = intent.getAction();
				//If message fails, try to ping until sister replies.
				if (action.equals(BluetoothConnectionService.SYNC_SERVICE_CONNECTION_BROKEN)) {
					stop();
				}
				else if (action.equals(BluetoothConnectionService.SAME_SAME_SYNDROME)) {
					pingC=0;
					stop();
				}
				
				else if (action.equals(BluetoothConnectionService.SYNC_SERVICE_CONNECTION_ATTEMPT_FAILED)) {
					//Try to ping again in a while if still running.
					new Handler().postDelayed(new Runnable() {
						public void run() {
							if (pingC>0) {
								pingC--;
								if(gs.getSyncStatus()==SyncStatus.searching) {
									Toast.makeText(ctx, "Connection attempts left: "+pingC, Toast.LENGTH_LONG).show();
									try {
										startClient();					
									} catch (Exception e) {
										o.addRow("");
										o.addRedText("Bluetooth Connection failed in prepare");
										stop();
									}
								}
							} else
								//after five attempts, stop the sync, and shutdown bluetooth.
								stop();
						}
					}, PING_DELAY);
				}

				
				else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
					final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
							BluetoothAdapter.ERROR);
					switch (state) {
					case BluetoothAdapter.STATE_OFF:
						Log.d("BT","Bluetooth off");
						pingC=0;
						stop();
						break;
					case BluetoothAdapter.STATE_TURNING_OFF:
						Log.d("BT","Bluetooth turning off");
						break;
					case BluetoothAdapter.STATE_ON:
						Log.d("BT","Bluetooth on...starting server");
						start();
						break;
					case BluetoothAdapter.STATE_TURNING_ON:
						Log.d("BT","Bluetooth turning on");
						break;
					}
				}

			}};
			//Listen for the bluetooth to start if not started.
			//When bluetooth runs, call start server. Then try to ping the server.
			IntentFilter ifi = new IntentFilter();
			ifi.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
			ifi.addAction(BluetoothConnectionService.SYNC_SERVICE_CONNECTION_ATTEMPT_FAILED);
			ifi.addAction(BluetoothConnectionService.SYNC_SERVICE_CONNECTION_BROKEN);
			ifi.addAction(BluetoothConnectionService.SYNK_SERVICE_STOPPED);
			ifi.addAction(BluetoothConnectionService.SAME_SAME_SYNDROME);
			//ifi.addAction(BluetoothConnectionService.SYNK_SERVICE_STARTED);
			ctx.registerReceiver(brr, ifi);
			
			
			BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
			if (ba==null) {
				Log.e("NILS","bt adaptor null in bluetoothremotedevice service CREATE");
				stop();
			} 
			if (!ba.isEnabled())	{
				Log.d("vortex","Enabling bluetooth");
				ba.enable();
			}
			else {
				Log.d("NILS","No socket...trying to aquire..");
				start();
				
			}
			
		}
	}

	private void start() {
		if (client!=null && server!=null) {
			Log.d("vortex","client and server already running");
			return;
		} 
		
		try {
			startClient();
			startServer();

		} catch (BluetoothDevicesNotPaired e) {
			Toast.makeText(ctx,"No bounded (paired) device found",Toast.LENGTH_LONG).show();
			Intent intent = new Intent();
			intent.setAction(BluetoothConnectionService.SYNK_NO_BONDED_DEVICE);
			ctx.sendBroadcast(intent);
			stop();		
		}  catch (Exception e) {
			e.printStackTrace();
		}
	}

	void ping() {
		//Send a ping to see if we can connect straight away.
		Log.d("NILS","Sending ping");
		String myName, myLag;
		String bundleVersion,softwareVersion;
		myName = gs.getGlobalPreferences().get(PersistenceHelper.USER_ID_KEY);
		myLag = gs.getGlobalPreferences().get(PersistenceHelper.LAG_ID_KEY);
		bundleVersion = gs.getPreferences().get(PersistenceHelper.CURRENT_VERSION_OF_WF_BUNDLE);
		softwareVersion = Float.toString(gs.getGlobalPreferences().getF(PersistenceHelper.CURRENT_VERSION_OF_PROGRAM));
		boolean requestAll = gs.getPreferences().get(PersistenceHelper.TIME_OF_LAST_SYNC).equals(PersistenceHelper.UNDEFINED);

		boolean succ = send(gs.isMaster()?new MasterPing(myName,myLag,bundleVersion,softwareVersion,requestAll):new SlavePing(myName,myLag,bundleVersion,softwareVersion,requestAll));
		if (!succ) {
			Log.e("vortex","ping failed, no connection!");
			stop();
		} 
		else
			gs.setSyncStatus(SyncStatus.waiting_for_ping);

	}


	
	
	

	
	


	ClientConnectThread client=null;
	AcceptThread server=null;
	ConnectedThread connected_T = null;


	//Start a Client thread for communication.
	//Create an instance of this singleton if not already existing.
	public void startClient() throws BluetoothDevicesNotPaired {

		
		//check if there is a bonded device.
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		BluetoothDevice pair;
		if (pairedDevices.isEmpty()) {
			Log.e("NILS","Didn't find any paired device.");
			
			throw new BluetoothDevicesNotPaired();
		}
		else {
			pair = pairedDevices.iterator().next();
			Log.d("NILS","FIRST PAIRED DEVICE IS: "+pair.getName());
			client = new ClientConnectThread(ctx,pair);
			client.start();
			if (pairedDevices.size()>1)
				Log.e("NILS","Error: More than one bonded device");
			
		}
		//caller need to wait for socket.

	}


	private void startServer() {
		//Create server thread
		Log.d("NILS","Trying to start server.");
		server = new AcceptThread();
		server.start();
		//Tell the world.
		

	}

	private class AcceptThread extends Thread {
		private final BluetoothServerSocket mmServerSocket;

		public AcceptThread() {
			// Use a temporary object that is later assigned to mmServerSocket,
			// because mmServerSocket is final
			BluetoothServerSocket tmp = null;
			try {
				// MY_UUID is the app's UUID string, also used by the client code
				tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("NILS", Constants.getmyUUID());
			} catch (IOException e) { }
			mmServerSocket = tmp;
			
		}

		public void run() {
			BluetoothSocket socket = null;
			// Keep listening until exception occurs or a socket is returned
			Log.d("NILS","Server is started");
			while (true) {
				try {
					socket = mmServerSocket.accept();
					Log.d("NILS","SERVER got a SOCKET");

				} catch (IOException e) {
					Log.e("NILS","Exception in AcceptThread");
					e.printStackTrace();
					break;
				}
				// If a connection was accepted
				if (socket != null) {
					// Do work to manage the connection (in a separate thread)
					Log.d("NILS","calling managesocket");
					manageConnectedSocket(socket);
					try {
						mmServerSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				}
			}
		}



		/** Will cancel the listening socket, and cause the thread to finish */
		public void cancel() {
			try {
				mmServerSocket.close();
			} catch (IOException e) { }
		}
	}





	private void manageConnectedSocket(BluetoothSocket socket) {  	
		connected_T = new ConnectedThread(gs,socket);
		connected_T.start();
		Log.d("NILS","connected thread started.");
		//wait for ping
		
		//send ping
		ping();
	}

	

	final static int MESSAGE_READ = 1;


	private class ClientConnectThread extends Thread {

		private final BluetoothSocket mmSocket;
		private Context mContext;
		public ClientConnectThread(Context ctx,BluetoothDevice device) {
			// Use a temporary object that is later assigned to mmSocket,
			// because mmSocket is final
			BluetoothSocket tmp = null;

			// Get a BluetoothSocket to connect with the given BluetoothDevice
			try {
				// MY_UUID is the app's UUID string, also used by the server code
				tmp = device.createRfcommSocketToServiceRecord(Constants.getmyUUID());
			} catch (IOException e) { }
			mmSocket = tmp;
			mContext = ctx;
		}

		public void run() {
			// Cancel discovery because it will slow down the connection
			mBluetoothAdapter.cancelDiscovery();

			try {
				// Connect the device through the socket. This will block
				// until it succeeds or throws an exception
				Log.d("NILS","Trying to connect to sister device..");
				mmSocket.connect();
			} catch (IOException connectException) {
				// Unable to connect; close the socket and get out
				Log.d("NILS","Failed to connect to sister");
				connectException.printStackTrace();
				//Tell the world.
				Intent intent = new Intent();
				intent.setAction(SYNC_SERVICE_CONNECTION_ATTEMPT_FAILED);
				mContext.sendBroadcast(intent);
				try {
					mmSocket.close();
				} catch (IOException closeException) { }
				return;
			}

			// Do work to manage the connection (in a separate thread)
			Log.d("NILS","connected to sister device..");

			manageConnectedSocket(mmSocket);
		}

		/** Will cancel an in-progress connection, and close the socket */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) { }
		}

	}	



	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		//private final InputStream mmInStream;
		//private final OutputStream mmOutStream;
		private final ObjectOutputStream obj_out;
		private final ObjectInputStream obj_in;
		private GlobalState gs;

		public ConnectedThread(GlobalState gs, BluetoothSocket socket) {
			this.gs=gs;
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			ObjectOutputStream tmp_obj_out=null;
			ObjectInputStream tmp_obj_in=null;


			// Get the input and output streams, using temp objects because
			// member streams are final
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
				//stream for sending objects.
				tmp_obj_out = new
						ObjectOutputStream (tmpOut);
				tmp_obj_in = 
						new ObjectInputStream (tmpIn);

			} catch (IOException e) { }

			obj_out = tmp_obj_out;
			obj_in = tmp_obj_in;
			//mmInStream = tmpIn;
			//mmOutStream = tmpOut;

		}

		public void run() {
			//byte[] buffer = new byte[1024];  // buffer store for the stream
			//int bytes; // bytes returned from read()

			// Keep listening to the InputStream until an exception occurs
			Object o=null;
			MessageHandler mHandler = MessageHandler.getHandler();
			
			while (true) {

					// Read from the InputStream
					try {
						
						o = obj_in.readObject();
						mHandler.handleMessage(o);
					} catch (ClassNotFoundException e) {
						Log.e("NILS","CLASS NOT FOUND IN Stream");
						e.printStackTrace();
					} catch (Exception e) {
						Log.e("NILS","got strange exception in Inputstream");
						
						if (e instanceof StreamCorruptedException) {
							gs.sendEvent(BLUETOOTH_RESTART_REQUIRED);
							singleton.stop();
						}
						else
							gs.sendEvent(SYNC_SERVICE_CONNECTION_BROKEN);
						
						break;
					}
					// Send the obtained bytes to the UI activity
					//mHandler.obtainMessage(MESSAGE_READ, -1, -1, o).sendToTarget();
			}
		}

		// Write object with ObjectOutputStream
		public void write(Object o) {
			try {
				
				obj_out.writeObject(o);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}



		/* Call this from the main activity to shutdown the connection */

		public void cancel() {
			try {
				obj_out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			try {
				obj_in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (mmSocket.isConnected()) {
				Log.d("nils","ISCONNECTED CALLING SOCKET CLOSE!!!");
				//Is this always called anyway??
				try {
					mmSocket.close();
				} catch (IOException e) { e.printStackTrace();}
			}
			
			
		}
	}


	Queue<Object>msgBuf = new LinkedList<Object>();



	
	
	public boolean send(Object o) {
		Log.d("NILS","Sending: "+o.toString());
		
		if(connected_T!=null) {
			connected_T.write(o);
			return true;
		}
		
		else {
			Log.e("vortex","Write failed, no connection");
			return false;
		}
	}


	public static BluetoothConnectionService getSingleton() {
		return singleton;
	}

	public void stop() {
		gs.setSyncStatus(SyncStatus.stopped);
		Toast.makeText(ctx, "Sync stopped", Toast.LENGTH_SHORT).show();
		stopp();
	}
	
	public void success() {
		gs.setSyncStatus(SyncStatus.stopped);
		Toast.makeText(ctx, "Sync successful", Toast.LENGTH_SHORT).show();
		stopp();
	}
	
	private void stopp() {
		//drop reference to this object.
		Log.e("vortex","STOP CALLED ON BLUETOOTH. Syncstatus: "+gs.getSyncStatus().name());
		singleton = null;
		if (client!=null)
			client.cancel();
		if (server!=null)
			server.cancel();
		if (connected_T !=null)
			connected_T.cancel();
		connected_T=null;
		if(BluetoothAdapter.getDefaultAdapter().isEnabled())
			BluetoothAdapter.getDefaultAdapter().disable();
		
		

		try {
			ctx.unregisterReceiver(brr);
		} catch (IllegalArgumentException e) {
			Log.d("nils","unregisterReceiver - dropping exception");
		}
		
	}
	


	


}