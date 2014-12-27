package com.teraim.vortex.ui;

import android.app.Fragment;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.teraim.vortex.R;




public class LoginConsoleFragment extends Fragment {

	TextView log;
	private TextView versionTxt;
	private ImageView bgImage;

	 @Override
	  public View onCreateView(LayoutInflater inflater, ViewGroup container,
	      Bundle savedInstanceState) {
	    View view = inflater.inflate(R.layout.fragment_login_console,
	        container, false);
	    
	    
		log = (TextView)view.findViewById(R.id.logger);
		versionTxt = (TextView)view.findViewById(R.id.versionTxt);
		bgImage = (ImageView)view.findViewById(R.id.bgImg);
		Typeface type=Typeface.createFromAsset(getActivity().getAssets(),
		        "clacon.ttf");
		log.setTypeface(type);
		log.setMovementMethod(new ScrollingMovementMethod());
	    return view;
	  }

	public TextView getTextWindow() {
		return log;
	}
	
	public TextView getVersionField() {
		return versionTxt;
	}


	 


}
