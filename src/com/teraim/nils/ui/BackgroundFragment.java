package com.teraim.nils.ui;

import com.teraim.nils.R;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class BackgroundFragment extends Fragment {

	 @Override
	  public View onCreateView(LayoutInflater inflater, ViewGroup container,
	      Bundle savedInstanceState) {
	    View view = inflater.inflate(R.layout.fragment_background,
	        container, false);
		    
	    return view;
	 }
	 
}


