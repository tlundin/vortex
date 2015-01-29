package com.teraim.vortex.loadermodule.configurations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.teraim.vortex.dynamic.types.SpinnerDefinition;
import com.teraim.vortex.dynamic.types.SpinnerDefinition.SpinnerElement;
import com.teraim.vortex.loadermodule.CSVConfigurationModule;
import com.teraim.vortex.loadermodule.LoadResult;
import com.teraim.vortex.loadermodule.ConfigurationModule.Source;
import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.utils.PersistenceHelper;
import com.teraim.vortex.utils.Tools;

public class SpinnerConfiguration extends CSVConfigurationModule {
	
	public final static String NAME = "Spinners";
	private final static int noOfRequiredColumns=5;			
	private final SpinnerDefinition sd=new SpinnerDefinition();
	private LoggerI o;

	public SpinnerConfiguration(PersistenceHelper globalPh,PersistenceHelper ph, String server, String bundle, LoggerI debugConsole) {
		super(globalPh, ph, Source.internet, server+bundle.toLowerCase()+"/", SpinnerConfiguration.NAME,"Spinner module        ");	 
		this.o = debugConsole;
		
	}

	@Override
	public String getFrozenVersion() {
		return (ph.get(PersistenceHelper.CURRENT_VERSION_OF_SPINNERS));
	}

	@Override
	protected void setFrozenVersion(String version) {
		ph.put(PersistenceHelper.CURRENT_VERSION_OF_SPINNERS,version);
		
	}

	@Override
	public boolean isRequired() {
		return false;
	}

	@Override
	public LoadResult parse(String row, Integer currentRow) throws IOException {
			List<SpinnerElement>sl = null;
			String curId=null;
			//Split into lines.			
			String[]  r = Tools.split(row);
			if (r.length<noOfRequiredColumns) {
				o.addRow("");
				o.addRedText("Too short row on line in spinnerdef file."+currentRow+" length: "+r.length);
			for (int i=0;i<r.length;i++) {
				o.addRow("R"+i+":"+r[i]);
			}
			String errMsg = "Spinnerdef file corrupt. Check Log for details";
			return new LoadResult(this,ErrorCode.parseError,errMsg);
			} else {
				String id = r[0];
				if (curId==null || !id.equals(curId)) {
					sl = new ArrayList<SpinnerElement>();
					sd.add(id, sl);
					curId = id;
				}
				sl.add(sd.new SpinnerElement(r[1],r[2],r[3],r[4]));
			}
			//good!
			return null;
			//mCallBack.onUpdate(pHelper.getCurrentRow(),this.getNumberOfLinesInRawData());
		}


	
	@Override
	protected LoadResult prepare() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setEssence() {
		essence = sd;
	}
		
		
		
	}

	

	

