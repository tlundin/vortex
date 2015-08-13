package com.teraim.vortex.bluetooth;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.GlobalState.SyncStatus;
import com.teraim.vortex.log.LoggerI;

public abstract class MessageHandler {


	protected LoggerI o;
	protected int pingC = 0;
	protected GlobalState gs;
	protected BluetoothConnectionService bt;
	
	private static MessageHandler singleton;
	
	public MessageHandler() {
		gs = GlobalState.getInstance();
		bt = BluetoothConnectionService.getSingleton();
		o = gs.getLogger();
	
	}
	
	public static Handler getHandler() {
		if (singleton == null) {
			singleton = GlobalState.getInstance().isMaster()?new MasterMessageHandler():new SlaveMessageHandler();
		}
		return singleton.mHandler;	
	}
	
	private Handler mHandler = new Handler(Looper.getMainLooper()) {
		
	      
        @Override
		public void handleMessage(Message msg) {
        	Log.d("vortex","got message: "+msg.toString());
        	singleton.handleMessage(msg.obj);
			super.handleMessage(msg);
		}

		
	};


	public void handleMessage(Object message) {
		
		
		
			if (message instanceof EnvelopedMessage) {
				//Open envelope, and save in globalstate.
				gs.setSyncMessage(((EnvelopedMessage)message).getMessage());
				o.addRow("");
				o.addGreenText("[BT MESSAGE -->Received message: "+gs.getOriginalMessage().toString()+"]");
				gs.sendEvent(BluetoothConnectionService.BLUETOOTH_MESSAGE_RECEIVED);
			}

			else
				handleSpecialized(message);
		
	}



	public abstract void handleSpecialized(Object message);





}
