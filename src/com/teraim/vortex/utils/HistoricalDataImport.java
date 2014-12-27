package com.teraim.vortex.utils;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.teraim.vortex.FileLoadedCb;
import com.teraim.vortex.GlobalState;
import com.teraim.vortex.FileLoadedCb.ErrorCode;
import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.non_generics.Constants;

public class HistoricalDataImport extends AsyncTask<GlobalState ,Integer,LoadResult>{



	private static final String DELNINGS_P = "Delningspunkt";
	ProgressBar pb;
	TextView tv;
	FileLoadedCb cb;
	LoggerI myLog;
	String vNo ="";
	
	public HistoricalDataImport(ProgressBar pb, TextView tv,LoggerI log,FileLoadedCb cb) {
		this.pb=pb;
		this.tv=tv;
		this.cb=cb;
		this.myLog=log;

	}



	/* Reads input files with historical data and inserts into database. */
	@Override
	protected void onPostExecute(LoadResult res) {
		cb.onFileLoaded(res.errCode,res.version);
	}


	LoggerI o;
	GlobalState gs;
	VariableConfiguration al;
	DbHelper myDb;
	private Set<String> varIds = new HashSet<String>();
	@Override
	protected LoadResult doInBackground(GlobalState... params) {

		Log.d("nils","In scanData");
		gs = params[0];
		al = gs.getArtLista();
		o = gs.getLogger();
		myDb=gs.getDb();
		PersistenceHelper ph = gs.getPersistence();

		String serverUrl = ph.get(PersistenceHelper.SERVER_URL);
		if (serverUrl ==null || serverUrl.equals(PersistenceHelper.UNDEFINED) || serverUrl.length()==0)
			return new LoadResult(ErrorCode.configurationError,null);
		//Add / if missing.
		if (!serverUrl.endsWith("/")) {
			serverUrl+="/";
		}
		if (!serverUrl.startsWith("http://")) {
			serverUrl = "http://"+serverUrl;
			o.addRow("server url name missing http header...adding");		
		}
		String bundle = ph.get(PersistenceHelper.BUNDLE_NAME);
		if (bundle == null) {
			Log.d("vortex","missing bundle name...returning");
			return null;
		}
		bundle = bundle.toLowerCase();
		LoadResult ec=load(serverUrl+"/"+bundle+"/"+Constants.PY_HISTORICAL_FILE_NAME);

		if (ec.errCode==ErrorCode.HistoricalLoaded) {
			int histCounter = ph.getI(PersistenceHelper.HIST_LOAD_COUNTER+vNo);
			histCounter = histCounter < 0 ? 0:histCounter;
			Log.d("nils","Historical file scanned. Inserting to DB.");
			Log.d("nils","histCounter is "+histCounter+" name: "+PersistenceHelper.HIST_LOAD_COUNTER+vNo);

			/*
			StringTokenizer st = new StringTokenizer(result);
			int j = 0;
			int c=0;
			while (st.hasMoreTokens()) {
				System.out.println(st.nextToken());
				j++;
				if (j>100)
					break;
			}
			 */

			try {
				JSONObject jObject = new JSONObject(result);

				JSONArray jArray = jObject.getJSONArray("source");
				if (histCounter == jArray.length()-1)
					return ec;
				//Erase old history if this is load from start.
				if(histCounter == 0) {
					Log.e("nils","HISTORIA RENSAS!!");
					myDb.deleteHistory();
				}
				myDb.fastPrep();
				pb.setMax(jArray.length());
				
				for (int i=histCounter; i < jArray.length(); i++)
				{
					try {
						
						if(isCancelled()) {
							ec.errCode = ErrorCode.Aborted;
							break;
						}
						JSONObject keyObj = jArray.getJSONObject(i);
						// Pulling items from the array


						/*Log.d("nils","Ruta: ["+
						keyObj.getString("ruta")+
						"] provyta: ["+keyObj.getString("provyta")+
						"] delyta: ["+keyObj.getString("delyta")+
						"] smaprovyta ["+keyObj.getString("smaprovyta"));
						 */
						//Vars:
						JSONArray varArray = keyObj.getJSONArray("Vars");
						JSONObject varObj;
						String varId,value;
						for (int a=0; a < varArray.length(); a++) {
							varObj = varArray.getJSONObject(a);
							varId = varObj.getString("name");
							value = varObj.getString("value");
							myDb.fastHistoricalInsert(keyObj.getString("ruta"),
									keyObj.getString("provyta"),
									keyObj.getString("delyta"),
									keyObj.getString("smaprovyta"),
									varId,value);						
							varIds.add(varId);
						}


					} catch (JSONException e) {
						e.printStackTrace();
						return new LoadResult(ErrorCode.parseError,null);
					}
					this.publishProgress(i,jArray.length());
					ph.put(PersistenceHelper.HIST_LOAD_COUNTER+vNo, i);
				}
				//Scan all variables to see if any of them do not exist in VariabelDef.
				
				List<String> row;
				for (String varId:varIds) {
					row = al.getCompleteVariableDefinition(varId);
					if (row==null) {
						myLog.addRow("");
						myLog.addRedText("Variable "+varId+" defined in Historical Import not found in the VariabelDefinition.");
						Log.d("nils","Missing: ["+varId+"]");
					}
				}

			} catch(Exception e) { e.printStackTrace();return new LoadResult(ErrorCode.parseError,null);}


		} 
	
		//gs.getPersistence().put(PersistenceHelper.CURRENT_VERSION_OF_HISTORY_FILE,vNo);		
		return ec;

	}


	String result = null;
//	int rowC=0;
	private LoadResult load(String fileUrl) {

		Log.d("nils","File url: "+fileUrl);
		InputStream in=null;

			try {	
				URL url = new URL(fileUrl);
				Log.d("nils","Fetching historical data bundle: "+fileUrl);

				/* Open a connection to that URL. */
				URLConnection ucon = url.openConnection();
				if (Build.VERSION.SDK != null && Build.VERSION.SDK_INT > 13) { ucon.setRequestProperty("Connection", "close"); }
				in = ucon.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
				StringBuilder sb = new StringBuilder();
				String line = null;
				String header = reader.readLine();
				if (header == null) {
					Log.e("nils","Header was null in history import file!");
					o.addRow("");
					o.addRedText("Header empty in history import");
					return new LoadResult(ErrorCode.configurationError,null);
				} else {
					if (gs.getPersistence().getB(PersistenceHelper.VERSION_CONTROL_SWITCH_OFF)) {
						o.addRow("Version control is switched off.");
						Log.d("nils","Version control is switched off.");
					} else {
							String[] h = header.split(",");
							if (h==null||h.length!=2) {
								o.addRow("");
								o.addRedText("History file lacks version number. Will abort scan");
								Log.e("nils","History file lacks version number. Will abort scan");
								return new LoadResult(ErrorCode.configurationError,null);
							} else {
							vNo=h[1].trim();
							o.addRow("Historical data version: "+vNo);
							//check with current version.
							String currentHistoryVersion = gs.getSafe().getHistoricalFileVersion();
//							String currentHistoryVersion = gs.getPersistence().get(PersistenceHelper.CURRENT_VERSION_OF_HISTORY_FILE);
							if (currentHistoryVersion!=null&&currentHistoryVersion.equals(vNo)) {
								Log.d("nils","Version equal");
								if (gs.getPersistence().getI(PersistenceHelper.HIST_LOAD_COUNTER+vNo)!=-1) {
									Log.d("nils","Loadcounter not zero..will not load. Countervalue: "+
											gs.getPersistence().getI(PersistenceHelper.HIST_LOAD_COUNTER+vNo));
									return new LoadResult(ErrorCode.sameold,vNo);
								}
							}  else
								Log.d("nils","Found historical file version: "+vNo+" Not equal to: "+currentHistoryVersion);
							
							o.addRow("");
							o.addYellowText("Loading historical data. Current version: "+currentHistoryVersion+" New version: "+vNo);
							Log.d("nils","Loading historical data. Current version: "+currentHistoryVersion+" New version: "+vNo);
						}
					}
				}
				//sb.append(header);

				while ((line = reader.readLine()) != null)
				{
					sb.append(line + "\n");
					if(isCancelled()) {
						reader.close();
						return new LoadResult(ErrorCode.Aborted,vNo);
					}
				}

				result = sb.toString();
//				if (result!=null)
//					rowC = (int)(result.length()/77);
//				Log.d("nils","rowC is "+rowC);
				reader.close();
			} catch (EOFException e) {
				e.printStackTrace();
				return new LoadResult(ErrorCode.ioError,null);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return new LoadResult(ErrorCode.notFound,null);			
			} catch (IOException e) {			
				e.printStackTrace();
				return new LoadResult(ErrorCode.ioError,null);
			}
			finally {
				try {if (in!=null)in.close();}catch (Exception e){};
			}
			return new LoadResult(ErrorCode.HistoricalLoaded,vNo);
	}



	/*
		try {
			InputStreamReader is = new InputStreamReader(gs.getContext().getResources().openRawResource(R.raw.delningspunkter_short));
			BufferedReader br = new BufferedReader(is);
			String row;
			String header = br.readLine();
			Log.d("NILS",header);
			//Find rutId etc
			while((row = br.readLine())!=null) {
				lineC++;
				String  r[] = row.split(",");
				String rutaID,provytaID,delytaID,year;
				if (r!=null&&r.length>5) {	
					year = "2014";
					rutaID = r[0];
					provytaID = r[2];
					delytaID = r[3];
					String tag="";
					for (int i=4;i<r.length;i++) {

						if (r[i]!=null && !r[i].equalsIgnoreCase("NULL") && !r[i].startsWith("-")) {
							tag+=r[i];
							if (i<r.length-1)
								tag+="|";
						}
					}
					//Insert to database.
					if(tag.length()>0) {

						String varId = "TAG";

						Variable v=null; 
						Map<String,String>keys = Tools.createKeyMap(VariableConfiguration.KEY_YEAR,year,"ruta",rutaID,"provyta",provytaID,"delyta",delytaID);
						gs.setKeyHash(keys);
						if (keys!=null  && provytaID !=null && rutaID !=null && delytaID != null) {						
							v = gs.getArtLista().getVariableInstance(varId);
							if (v!=null)
								v.setValue(tag);
							else
								Log.e("nils","Variable null in TagfileParser");
							Log.d("nils","Tåg:"+tag+"[R:"+rutaID+" P:"+provytaID+" D:"+delytaID+"]");

							publishProgress(lineC);
						} else {
							Log.e("nils","Null error on line "+lineC+" rutaID: "+
									rutaID+" pyID: "+provytaID+" delytaID: "+delytaID+" Variable: "+v+" keys: "+keys.toString());
							br.close();
							return ErrorCode.configurationError;
						}

					}

				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
			return ErrorCode.ioError;
		}
		return ErrorCode.tagLoaded;
	}
	 */
	/* (non-Javadoc)
	 * @see android.os.AsyncTask#onProgressUpdate(java.lang.Object[])
	 */
	@Override
	protected void onProgressUpdate(Integer... values) {
		tv.setText("Loaded: "+values[0]+"/"+values[1]);
		pb.setProgress(values[0]);
		
	}

}


