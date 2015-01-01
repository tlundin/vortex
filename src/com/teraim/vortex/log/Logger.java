package com.teraim.vortex.log;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.widget.TextView;

import com.teraim.vortex.R;

public class Logger implements LoggerI {

	 //CharSequence myTxt = new SpannableString("");
	 SpannableStringBuilder myTxt = new SpannableStringBuilder();
	 TextView log = null;
	 Context myContext;
	 String loggerId;
	 int ticky=0;
	 	
	public Logger(Context c,String loggerId) {
		myContext = c;
		this.loggerId = loggerId;
	}
	 
	 public void setOutputView(TextView txt) {
		 log = txt;
	 }
	 
	 public void addRow(String text) {
			 myTxt.append("\n"+text);
		
	 }
	 public void addRedText(String text) {

		 SpannableString s = new SpannableString(text);
		 s.setSpan(new TextAppearanceSpan(myContext, R.style.RedStyle),0,s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		 myTxt.append(s);	 
		 //if (log!=null) log.setText(myTxt);
	 }	 
	 public void addGreenText(String text) {
		 SpannableString s = new SpannableString(text);
		 s.setSpan(new TextAppearanceSpan(myContext, R.style.GreenStyle),0,s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		 myTxt.append(s);	
		 //if (log!=null) log.setText(myTxt);
	 }
	 public void addYellowText(String text) {
		 SpannableString s = new SpannableString(text);
		 s.setSpan(new TextAppearanceSpan(myContext, R.style.YellowStyle),0,s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		 myTxt.append(s);	
		 //if (log!=null) log.setText(myTxt);
	 }
	 public void addText(String text) {
		 myTxt.append(text);
		 //if (log!=null) log.setText(myTxt);
	 }
	 
	 
	 
	 public CharSequence getLogText() {
		 return myTxt;
	 }

	 public void draw() {
		 if (log!=null) {
			 log.setText(myTxt);
		 }
		 else
			 Log.e("nils","LOG WAS NULL IN DRAW!!");
	 }


	public void clear() {
		myTxt.clear();
		if (log!=null) log.setText(myTxt);
	}

	@Override
	public void addPurpleText(String text) {
		 SpannableString s = new SpannableString(text);
		 s.setSpan(new TextAppearanceSpan(myContext, R.style.PurpleStyle),0,s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		 myTxt.append(s);
	}


	@Override
	public void ticky() {
		myTxt.append(".");
		draw();
	}
}
