package com.teraim.vortex.log;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

public class FastLogger implements LoggerI {

	 Context myContext;
	 String loggerId;
	 TextView log = null;
	 StringBuilder myTxt;

	
	public FastLogger(Context c,String loggerId) {
		myContext = c;
		this.loggerId = loggerId;
		myTxt = new StringBuilder();
	}
	
	@Override
	public void setOutputView(TextView txt) {
		 log = txt;
	}

	@Override
	public void addRow(String text) {
		myTxt.append(text+"\n");
	}

	@Override
	public void addRedText(String text) {
		myTxt.append("[XXXXXX]"+text);
	}

	@Override
	public void addGreenText(String text) {
		myTxt.append(text);
	}

	@Override
	public void addYellowText(String text) {
		myTxt.append("**"+text);
	}

	@Override
	public void addText(String text) {
		myTxt.append(text);
	}

	@Override
	public CharSequence getLogText() {
		return myTxt;
	}

	@Override
	public void draw() {
		 if (log!=null) {
			 Log.d("nils","LOGGER ID: "+loggerId);
			 log.setText(myTxt);
		 }
		 else
			 Log.e("nils","LOG WAS NULL IN DRAW!!");
	 }

	@Override
	public void clear() {
		myTxt = new StringBuilder();
		if (log!=null) 
			log.setText(myTxt);
	}

	@Override
	public void addPurpleText(String text) {
		myTxt.append(text);
	}

	@Override
	public void ticky() {
		// TODO Auto-generated method stub
		
	}

}
