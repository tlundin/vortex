package com.teraim.vortex.bluetooth;

import android.content.Intent;
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
		if(Looper.myLooper() == Looper.getMainLooper())
			Log.d("vortex","IN UI THREAD!!");
		else
			Log.d("vortex","NOT IN UI THREAD!!");
		//gs.setHandler(mHandler);
		
	}
	
	public static MessageHandler getHandler() {
		
		singleton = GlobalState.getInstance().isMaster()?new MasterMessageHandler():new SlaveMessageHandler();
		
		return singleton;	
	}
	
	


	public void handleMessage(Object message) {
		
		
		
			if (message instanceof EnvelopedMessage) {
				//Open envelope, and save in globalstate.
				gs.setSyncMessage(((EnvelopedMessage)message).getMessage());
				o.addRow("");
				o.addGreenText("[BT MESSAGE -->Received message: "+gs.getOriginalMessage().toString()+"]");
				gs.sendEvent(BluetoothConnectionService.BLUETOOTH_MESSAGE_RECEIVED);
			}

			else  if (message instanceof SyncRestartRequest) {
				Log.d("vortex","Restart request received...pinging!");
				bt.ping();
			}
		
			else
				handleSpecialized(message);
		
	}



	public abstract void handleSpecialized(Object message);


	


}
