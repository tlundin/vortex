package com.teraim.vortex.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.Start;
import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.dynamic.types.Numerable.Type;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.types.Workflow.Unit;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.utils.DbHelper.Selection;
import com.teraim.vortex.utils.RuleExecutor.SubstiResult;
import com.teraim.vortex.utils.RuleExecutor.TokenizedItem;

public class Tools {



	/** Read the object from Base64 string. */
	public static byte[] serialize(Serializable s) { 
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		ObjectOutputStream oos = null; 
		try { 
			oos = new ObjectOutputStream(baos); 
			oos.writeObject(s); 
		} catch (IOException e) { 
			Log.e("nils", e.getMessage(), e); 
			return null; 
		} finally { 
			try { 
				oos.close(); 
			} catch (IOException e) {} 
		} 
		byte[] result = baos.toByteArray(); 
		Log.d("nils", "Object " + s.getClass().getSimpleName() + "written to byte[]: " + result.length); 
		return result; 
	} 

	public static Object deSerialize(byte[] in) { 
		Object result = null; 
		ByteArrayInputStream bais = new ByteArrayInputStream(in); 
		ObjectInputStream ois = null; 
		try { 
			ois = new ObjectInputStream(bais); 
			result = ois.readObject(); 
		} catch (Exception e) { 
			result = null; 
		} finally { 
			try { 
				ois.close(); 
			} catch (Throwable e) { 
			} 
		} 
		return result; 
	}

	public static boolean writeToFile(String filename,String text) {
		PrintWriter out;
		try {
			out = new PrintWriter(filename);
			out.println(text);
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static void witeObjectToFile(Object object, String filename) throws IOException {

		Log.d("nils","Writing frozen object to file "+filename);
		ObjectOutputStream objectOut = null;
		try {
			FileOutputStream fileOut = new FileOutputStream(filename);
			objectOut = new ObjectOutputStream(fileOut);
			objectOut.writeObject(object);
			fileOut.getFD().sync();


		} finally {
			if (objectOut != null) {
				try {
					objectOut.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}


	/**
	 * 
	 * @param context
	 * @param filename
	 * @return
	 * @throws IOException 
	 * @throws StreamCorruptedException 
	 * @throws ClassNotFoundException 
	 */
	public static Object readObjectFromFile(String filename) throws StreamCorruptedException, IOException, ClassNotFoundException {
		ObjectInputStream objectIn = null;
		Object object = null;
		try {
		FileInputStream fileIn = new FileInputStream(filename);
		objectIn = new ObjectInputStream(fileIn);
		object = objectIn.readObject();

		}
		finally {
			if (objectIn != null) {
				try {
					objectIn.close();
				} catch (IOException e) {
					// do nowt
				}
			}
		}

		return object;
	}



	//This cannot be part of Variable, since Variable is an interface.

	public static Type convertToType(String text) {
		Type[] types = Type.values();	
		//Special cases
		if (text.equals("number"))
			return Type.NUMERIC;
		for (int i =0;i<types.length;i++) {
			if (text.equalsIgnoreCase(types[i].name()))
				return types[i];

		}
		return null;
	}

	public static Unit convertToUnit(String unit) {
		Log.d("unit","unit is "+unit+" with length "+unit.length());
		if (unit == null ||unit.length()==0) {
			Log.d("unit","translates to undefined");
			return Unit.nd;
		}
		Unit[] units = Unit.values();
		if (unit.equals("%"))
			return Unit.percentage;
		for (int i =0;i<units.length;i++) {
			if (unit.equalsIgnoreCase(units[i].name()))
				return units[i];
		}
		return Unit.nd;				
	}

	public static void createFoldersIfMissing(File file) {
		final File parent_directory = file.getParentFile();

		if (null != parent_directory)
		{
			parent_directory.mkdirs();
		}
	}

	//Scales an image to a size that can be displayed.

	public static Bitmap getScaledImage(Context ctx,String fileName) {

		//Try to load pic from disk, if any.
		//To avoid memory issues, we need to figure out how big bitmap to allocate, approximately
		//Picture is in landscape & should be approx half the screen width, and 1/5th of the height.
		//First get the ration between h and w of the pic.
		final BitmapFactory.Options options = new BitmapFactory.Options();
		if (fileName == null) {			
			return null;
		}
		options.inJustDecodeBounds=true;
		BitmapFactory.decodeFile(fileName,options);		

		//there is a picture..
		int realW = options.outWidth;
		int realH = options.outHeight;


		//check if file exists
		if (realW>0) {
			double ratio = realH/realW;
			//Height should not be higher than width.
			Log.d("nils", "realW realH"+realW+" "+realH);

			//Find out screen size.

			DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
			int sWidth = metrics.widthPixels;

			//Target width should be about half the screen width.

			double tWidth = sWidth;
			//height is then the ratio times this..
			int tHeight = (int) (tWidth*ratio);

			//use target values to calculate the correct inSampleSize
			options.inSampleSize = Tools.calculateInSampleSize(options, (int)tWidth, tHeight);

			Log.d("nils"," Calculated insamplesize "+options.inSampleSize);
			//now create real bitmap using insampleSize

			options.inJustDecodeBounds = false;
			Log.d("nils","Filename: "+fileName);
			return BitmapFactory.decodeFile(fileName,options);


		}
		else {
			Log.e("Vortex","Did not find picture "+fileName);
			//need to set the width equal to the height...
			return null;
		}
	}
	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
			int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(res, resId, options);
	}


	public static int calculateInSampleSize(
			BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			// Calculate ratios of height and width to requested height and width
			final int heightRatio = Math.round((float) height / (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);

			// Choose the smallest ratio as inSampleSize value, this will guarantee
			// a final image with both dimensions larger than or equal to the
			// requested height and width.
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}

		return inSampleSize;
	}




	/*********************************************************
	 * 
	 * File Data Parsers.
	 */

	public static Map<String,String> createKeyMapCopy(Map<String,String> orig) {
		Map<String,String> ret = new HashMap<String,String>();
		for (String k:orig.keySet()) {
			String v = orig.get(k);
			if (k != null && v!=null && ! v.equals("null"))
				ret.put(k,v);
		}
		return ret;
	};
	
	
	public static Map<String,String> createKeyMap(String ...parameters) {

		if ((parameters.length & 1) != 0 ) {
			Log.e("nils","createKeyMap needs an even number of arguments");
			return null;
		}
		String colName;
		String colValue;
		Map<String,String> ret = new HashMap<String,String>();
		for (int i=0;i<parameters.length;i+=2) {
			colName = parameters[i];
			colValue = parameters[i+1];	
			if (colName != null && colValue!=null && !colValue.equals("null"))
				ret.put(colName,colValue);

		}
		return ret;
	}

	public static String getPrintedUnit(Unit unit) {
		if (unit == Unit.percentage)
			return "%";
		if (unit == Unit.nd || unit == null)
			return "";
		else
			return unit.name();
	}

	public static boolean doesFileExist(String folder, String fileName) {
		Log.d("vortex","Looking for file "+fileName+ "in folder "+folder);
		File f = new File(folder,fileName);
		return (f.exists() && !f.isDirectory());
	}
	
	public static boolean isNetworkAvailable(Context ctx) {
		ConnectivityManager connectivityManager 
		= (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
	}
	

	public static boolean isNumeric(String str)
	{	
		if (str==null||str.length()==0)
			return false;
		for (char c : str.toCharArray())
		{
			if (!Character.isDigit(c)) return false;
		}
		return true;
	}

	public static boolean isVersionNumber(String str)
	{	
		if (str==null||str.length()==0)
			return false;
		for (char c : str.toCharArray())
		{
			if (!Character.isDigit(c) && c!='.')
				return false;
		}
		return true;
	}
	//Create a map of references to variables. 



	public static Bitmap drawableToBitmap (Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable)drawable).getBitmap();
		}

		Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap); 
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);

		return bitmap;
	}

	public static String[] generateList(GlobalState gs, Variable variable) {
		String[] opt=null;
		VariableConfiguration al = gs.getVariableConfiguration();
		LoggerI o = gs.getLogger();
		List<String >listValues = al.getListElements(variable.getBackingDataSet());
		Log.d("nils","Found dynamic list definition for variable "+variable.getId());

		if (listValues!=null&&listValues.size()>0) {
			String [] columnSelector = listValues.get(0).split("=");
			String[] column=null;
			boolean error = false;
			if (columnSelector[0].equalsIgnoreCase("@col")) {
				Log.d("nils","found column selector");
				//Column to select.
				String dbColName = gs.getDb().getColumnName(columnSelector[1]);
				if (dbColName!=null) {
					Log.d("nils","Real Column name for "+columnSelector[1]+" is "+dbColName);
					column = new String[1];
					column[0]=dbColName;
				} else {
					Log.d("nils","Column referenced in List definition for variable "+variable.getLabel()+" not found: "+columnSelector[1]);
					o.addRow("");
					o.addRedText("Column referenced in List definition for variable "+variable.getLabel()+" not found: "+columnSelector[1]);
					error = true;
				}
				if (!error) {
					//Any other columns part of key?
					Map<String,String>keySet = new HashMap<String,String>();
					if (listValues.size()>1) {
						//yes..include these in search
						Log.d("nils","found additional keys...");
						String[] keyPair;							
						for (int i=1;i<listValues.size();i++) {
							keyPair = listValues.get(i).split("=");
							if (keyPair!=null && keyPair.length==2) {
								String valx=al.getVariableValue(null,keyPair[1]);
								if (valx!=null) 										
									keySet.put(keyPair[0], valx);
								else {
									Log.e("nils","The variable used for dynamic list "+variable.getLabel()+" is not returning a value");
									o.addRow("");
									o.addRedText("The variable used for dynamic list "+variable.getLabel()+" is not returning a value");
								}
							} else {
								Log.d("nils","Keypair error: "+keyPair);
								o.addRow("");
								o.addRedText("Keypair referenced in List definition for variable "+variable.getLabel()+" cannot be read: "+keyPair);
							}
						}

					} else 
						Log.d("nils","no additional keys..only column");
					Selection s = gs.getDb().createCoulmnSelection(keySet);
					List<String[]> values = gs.getDb().getValues(column, s);
					if (values !=null) {
						Log.d("nils","Got "+values.size()+" results");
						//Remove duplicates and sort.
						SortedSet<String> ss = new TreeSet<String>(new Comparator<String>(){
							public int compare(String a, String b){
								return Integer.parseInt(a)-Integer.parseInt(b);
							}}						                         
								);
						String S;
						for (int i = 0; i<values.size();i++) {
							S = values.get(i)[0];
							if (Tools.isNumeric(S))
									ss.add(S);
							else
								Log.e("vortex","NonNumeric value found: ["+S+"]");
						}
						opt = new String[ss.size()];
						int i = 0; 
						Iterator<String> it = ss.iterator();
						while (it.hasNext()) {
							opt[i++]=it.next();
						}
					}
				} else
					opt=new String[]{"Config Error...please check your list definitions for variable "+variable.getLabel()};


			} else
				Log.e("nils","List "+variable.getId()+" has too few parameters: "+listValues.toString());
		} else
			Log.e("nils","List "+variable.getId()+" has strange parameters: "+listValues.toString());
		return opt;
	}











	/*
	public static SpinnerDefinition thawSpinners(Context myC) {		
		SpinnerDefinition sd=null;
		Log.d("nils","NO NETWORK. Loading file spinner def");
		sd = (SpinnerDefinition)Tools.readObjectFromFile(myC,Constants.CONFIG_FILES_DIR+Constants.WF_FROZEN_SPINNER_ID);		
		if (sd==null) 
			Log.d("vortex","No frozen Spinner definition");
		else
			Log.d("nils","Thawspinners called. Returned "+sd.size()+" spinners");
		return sd;

	}
	 */
	//Check if two keys are equal
	public static boolean sameKeys(Map<String, String> m1,
			Map<String, String> m2) {
		if (m1.size() != m2.size())
			return false;
		for (String key: m1.keySet()) {
			Log.d("nils","Key:"+key+" m1: "+(m1==null?"null":m1.toString())+" m2: "+(m2==null?"null":m2.toString()));
			if (m1.get(key)==null&&m2.get(key)==null)
				continue;
			if ((m1.get(key)==null || m2.get(key)==null)||!m1.get(key).equals(m2.get(key)))
				return false;
		}
		Log.d("nils","keys equal..no header");
		return true;
	}

	public static String[] split(String input) {
		List<String> result = new ArrayList<String>();
		int start = 0;
		boolean inQuotes = false;
		for (int current = 0; current < input.length(); current++) {
			if (input.charAt(current) == '\"') inQuotes = !inQuotes; // toggle state
			boolean atLastChar = (current == input.length() - 1);
			if(atLastChar) {
				if (input.charAt(current) == ',') {
					if (start==current)
						result.add("");
					else
						result.add(input.substring(start,current));
					result.add("");
				} else {
					//Log.d("nils","Last char: "+input.charAt(current));
					result.add(input.substring(start));
				}
			}
			else if (input.charAt(current) == ',' && !inQuotes) {
				String toAdd = input.substring(start, current);
				//Log.d("Adding",toAdd);

				result.add(toAdd);
				start = current + 1;
			}
		}
		if (result.size()==0)
			return new String[]{input};
		else
			return result.toArray(new String[0]);

	}

	public static String removeStartingZeroes(String value) {
		if (value == null || value.length()<=1 || !value.startsWith("0"))
			return value;
		return removeStartingZeroes(value.substring(1));
	}		
	
	
	public static void restart(Activity context) {
		Log.d("vortex","restarting...");			
		android.app.FragmentManager fm = context.getFragmentManager();
		for(int i = 0; i < fm.getBackStackEntryCount(); ++i) {    
		    fm.popBackStack();
		}
		Intent intent = new Intent(context, Start.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
		context.finish();		
	}

	public static Map<String,String> findKeyDifferences(Map<String, String> longer,
			Map<String, String> shorter) {
		Map<String,String> res = new HashMap<String,String>();
		//if (shorter!=null && longer!=null){
		//	Log.d("vortex","Longer: "+longer.toString());
		//	Log.d("vortex","shorter: "+shorter.toString());
		//}
		if (longer == null) 
			return null;
		if (shorter == null && !longer.isEmpty())
			return copyKeyHash(longer);
		Set<String> sK = shorter.keySet();
		for (String key:longer.keySet()) {
			if (!sK.contains(key)) {
				//Log.d("vortex","KEY DIFFERENT:"+key);
				res.put(key, longer.get(key));
			}
		}
		if (res.isEmpty())
			return null;
		return res;
	}

	public static Map<String,String> copyKeyHash(Map<String,String> orig) {
		if (orig==null)
			return null;
		Map<String,String> res = new HashMap<String,String>();
		for (String key:orig.keySet()) {
			res.put(key, orig.get(key));		
		}
		return res;
	}
	
	public static boolean isURL(String source) {
		if (source==null)
			return false;
		if (!source.startsWith("http")||source.startsWith("www"))
			return false;
		return true;
	}
	
	public static String parseString(String varString) {
		return parseString(varString, GlobalState.getInstance().getCurrentKeyHash());
	}
	
	public static String parseString(String varString, Map<String,String> keyHash) {

		if (varString == null||varString.isEmpty())
			return "";
		if (!varString.contains("["))
			return varString;
		if (keyHash!=null)
			Log.d("vortex","Keyhash: "+keyHash.toString());
		int cIndex=0;
		String res="";
		do {
			int hakeS = varString.indexOf('[', cIndex)+1;
			if (hakeS==0)
				break;
			int hakeE = varString.indexOf(']', hakeS);
			if (hakeE==-1) {
				Log.e("vortex","missing end bracket in parseString: "+varString);
				GlobalState.getInstance().getLogger().addRow("");
				GlobalState.getInstance().getLogger().addRedText("missing end bracket in parseString: "+varString);
				break;
			}
			Log.e("vortex","PARSESTRING: "+varString.substring(hakeS, hakeE));
			String interPS = interpret(varString.substring(hakeS, hakeE),keyHash);
			String preS = varString.substring(cIndex, hakeS-1);
			res = preS+interPS;
			cIndex=hakeE+1;
		} while(cIndex<varString.length());
		String postS = varString.substring(cIndex,varString.length());
		return res+postS;
	}
	private static String interpret(String varString, Map<String,String> keyHash) {
		final RuleExecutor re = GlobalState.getInstance().getRuleExecutor();
		List<TokenizedItem> tokenized = re.findTokens(varString, null, keyHash);
		SubstiResult x=null;
		if (tokenized!=null)
			x = re.substituteForValue(tokenized, varString, true);
		if (x!=null) {
			String res = x.result;
			return res;
		}
		else
			return varString;
	}

	public static void cacheImage(final String url, final String folder) {
		
		(new DownloadTask(new WebLoaderCb(){@Override
		public void loaded(Boolean result) {
			if (result)
				Log.d("vortex","Cached "+url);
		}})).execute(url,url,folder); 

		
	}
	
	private interface WebLoaderCb {

		public void loaded(Boolean result);
	}
	
	
	private static class DownloadTask extends AsyncTask<String, Void, Boolean> {
		WebLoaderCb cb;

		public DownloadTask(WebLoaderCb cb) {
			this.cb=cb;
		}

		protected Boolean doInBackground(String... fileNameAndUrl) {
			final String protoH = "http://";	
			if (fileNameAndUrl==null||fileNameAndUrl.length<3) {
				Log.e("vortex","filename or url name corrupt in downloadtask");
				return false;
			}
			String fileName = fileNameAndUrl[0];
			String url = fileNameAndUrl[1];
			String folder = fileNameAndUrl[2];
			if (!url.startsWith(protoH))
				url=protoH+url;
			//If url is used as name, remove protocol.
			if (fileName.startsWith(protoH))
				fileName = fileName.replace(protoH, "");
			if (fileName.contains("|")) {
				Log.e("vortex","Illegal filename, cannot cache: "+fileName);
				return false;
			}
				
			fileName = fileName.replace("/", "|");
			fileName = folder+fileName;
			File file = new File(fileName);
			
			//File already cached
			if(file.exists()) {
				Log.d("vortex","NO cache - file exists");
				return true;
			}
				
			Tools.createFoldersIfMissing(new File(fileName));
			
				
			try {
				saveUrl(fileName,url);
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}

		protected void onPostExecute(Boolean result) {
			
			cb.loaded(result);
		}
	}

	
	public static void saveUrl(final String filename, final String urlString)
	        throws MalformedURLException, IOException {
	    BufferedInputStream in = null;
	    FileOutputStream fout = null;
	    try {
	        in = new BufferedInputStream(new URL(urlString).openStream());
	        fout = new FileOutputStream(filename);

	        final byte data[] = new byte[1024];
	        int count;
	        while ((count = in.read(data, 0, 1024)) != -1) {
	            fout.write(data, 0, count);
	        }
	    } finally {
	        if (in != null) {
	            in.close();
	        }
	        if (fout != null) {
	            fout.close();
	        }
	    }
	}
	
	public static File getCachedFile(String fileName, String folder) {
		final String protoH = "http://";	
		fileName = fileName.replace("protoH", "");
		fileName = fileName.replace("|", "/");
		fileName = folder+fileName;
		File f = new File(fileName);
		Log.d("vortex", "getCached: "+fileName);
		if (f.exists()) {
			
			return f;
		}
		else 
			return null;
	}

}