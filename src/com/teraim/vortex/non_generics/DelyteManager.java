package com.teraim.vortex.non_generics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import android.util.Log;
import android.widget.Toast;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.dynamic.types.Delyta;
import com.teraim.vortex.dynamic.types.Segment;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.utils.DbHelper;
import com.teraim.vortex.utils.Tools;


//SLU specific class.



public class DelyteManager {

	private static DelyteManager instance;
	boolean hasUnsaved = false;
	GlobalState gs;
	VariableConfiguration al;
	private List<Delyta> myDelytor = new ArrayList<Delyta>();
	private int myPyID;

	public static final int MAX_DELYTEID = 5;



	//helper classes
	public static class Coord {
		public float x;
		public float y;	
		public final int avst,rikt;
		public Coord(int avst,int rikt) {
			int riktC = rikt-90;
			if (riktC<0)
				riktC +=360;
			double phi = 0.0174532925*(riktC);
			x = (float)(avst * Math.cos(phi));
			y = (float)(avst * Math.sin(phi));	
			//
			this.avst=avst;
			this.rikt=rikt;
		}
	}



	public enum ErrCode {
		TooShort,
		EndOrStartNotOnRadius,
		ok
	}


	public void analyze() {
		//Find the background
		calcRemainingYta();		
		//Store the number to database
		saveNoOfDelytor();
		//Sort south - west and give each a delyteID
		if (!assignDelyteId()) {
			clear();
			Toast.makeText(gs.getContext(), "Tilldelning av delyteID misslyckades. Något är fel med linjetågen.", Toast.LENGTH_LONG).show();			
		} else {
			//Find coordinates where to put the numbers.
			findNumberCoord();
			//update delyteID for each småprovyta in database
			//save delytor to database if needed.
			if (hasUnsaved)
				save();
		}
	}

	private void saveNoOfDelytor() {
		int size = this.getDelytor()==null?0:this.getDelytor().size();
		al.getVariableUsingKey(al.createProvytaKeyMap(), "noOfDelytor").setValue(size+"");
	}

	private boolean updateSmaProvToDelytaAssociations() {
		Map<String, String> key;
		DbHelper db = gs.getDb();
		final int[] b = (Constants.isAbo(myPyID)?new int[]{1,2,4,8,16,32,64,128,256}:new int[]{1,2,4});

		Set<Delyta> hasSmaS = new HashSet<Delyta>();

		//select those delytor that have småprov
		int chkSum = 0;
		for  (Delyta d:myDelytor) 
			if ((d.getSmaProv())!=0) {
				hasSmaS.add(d);
				chkSum+=d.getSmaProv();
				Log.d("nils","Checksum added "+d.getSmaProv()+" for delyta "+d.getId());
			} else 
				Log.d("nils","Delyta "+d.getId()+" did not have a småyta");
		int allSum=0;
		for (int i=0;i<b.length;i++)
			allSum+=b[i];
		if (chkSum!=allSum) {
			Log.d("nils","UPS! CHECKSUM: "+chkSum+" ALLSUM: "+allSum);
			return false;
		}
		//erase existing.
		db.eraseSmaProvyDelytaAssoc(al.getCurrentRuta(),al.getCurrentProvyta());
		//update associations
		for (Delyta d:hasSmaS) {
			for (int i=0;i<b.length;i++) 
				if ((b[i]&d.getSmaProv())!=0) {
					Log.d("nils","adding Delyta ID "+d.getId()+" to småprovyta "+(i+1));
					key = al.createProvytaKeyMap();
					key.put("smaprovyta", (i+1)+"");
					db.fastInsert(key, "Delyta", d.getId()+"");					
				}
		}

		return true;

	}

	private void findNumberCoord() {
		//Find where to put the number.

		List<Segment> cTag;
		for (Delyta d:myDelytor) {


			cTag = d.getSegments();
			//Just a simple arc?
			Set<Segment>arcs=new HashSet<Segment>();
			for (Segment s:cTag) {
				if (s.isArc)
					arcs.add(s);
			}
			if (!arcs.isEmpty()) {
			Iterator<Segment> it = arcs.iterator();
			//if (arcs.size()==1) {				
				//Put in middle of pyramid.
				Segment arc=it.next();
				int dist;
				int arcMid,nyR;
				dist = Delyta.rDist(arc.start.rikt,arc.end.rikt);
				Log.d("nils","Delyta: "+d.getId());
				Log.d("nils","Start: "+arc.start.rikt+" End: "+arc.end.rikt+ "Dist: "+dist);
				arcMid =  (dist/2);
				Log.d("nils"," Medeldistans "+arcMid);
				nyR = arc.start.rikt+arcMid;
				Log.d("nils"," nyR "+nyR);
				if (nyR>360)
					nyR=nyR-360;
				if (arc.start.rikt>arc.end.rikt) 
					Log.d("nils","Start more: "+nyR);
				else
					Log.d("nils","Start less: "+nyR);

				Log.d("nils","DIST: "+dist);
				Coord m = new Coord(85,nyR);

				d.setNumberPos(m.x,m.y);
			} else {
				Log.e("nils","NO ARC FOUND!");
				for (Segment s:cTag)
					Log.d("nils","Start: "+s.start.rikt+" End: "+s.end.rikt);
			}
			/*} else if (arcs.size()==2) {

				Segment arc1=it.next();
				Segment arc2=it.next();

				int dist1 = Delyta.rDist(arc1.start.rikt,arc1.end.rikt);
				int dist2 = Delyta.rDist(arc2.start.rikt,arc2.end.rikt);
				int mid1 = dist1/2;
				int mid2 = dist2/2;

				int nyR1 = arc1.start.rikt+mid1;
				if (nyR1>360)
					nyR1=nyR1-360;
				int nyR2 = arc2.start.rikt+mid2;
				if (nyR2>360)
					nyR2=nyR2-360;

				int dist = Delyta.rDist(nyR1,nyR2);
				int arcMid =  (dist/2);
				int nyR = nyR1+arcMid;
				if (nyR>360)
					nyR=nyR-360;
				Coord m = new Coord(0,nyR1);
				//double x = 100 * Math.cos(nyR);
				//double y = 100 * Math.sin(nyR1);
				d.setNumberPos(m.x,m.y);
			}

			 */

		}


		//If its a normal arc, put it in the center of the Pyramid.

	}
	private float midP(float s,float e) {
		return (s+e)/2;
	}

	private boolean assignDelyteId() {
		if (myDelytor.size()==1) {
			Log.d("nils","Only one delyta...already has id. Exit asignDelyteId");
			return true;
		}

		int delyteIdC = 1;
		int backId = -1;
		SortedSet<Delyta> s = new TreeSet<Delyta>(new Comparator<Delyta>() {

			@Override
			public int compare(Delyta lhs, Delyta rhs) {
				return ((lhs.mySouth==rhs.mySouth?lhs.myWest-rhs.myWest:lhs.mySouth-rhs.mySouth)<0?-1:1);
				/*
				if (cmp == 0) {
					Log.d("nils","COMP RET 0 for ls "+lhs.mySouth+" rs "+rhs.mySouth+" lw "+lhs.myWest+" rw "+rhs.myWest);
				}
				return cmp;
				 */
			}});

		Log.d("nils","IN SORTOS");
		printDelytor();
		s.addAll(myDelytor);


		Log.d("nils","Mydelytor has "+myDelytor.size()+" delytor");
		Log.d("nils","Sorted ytor according to south/west has "+s.size()+" delytor");
		if (myDelytor.size()!=s.size()) {
			return false;
		}
		for (Delyta d:s) {
			d.setId(delyteIdC);
			if (d.isBackground()) {
				Log.d("nils","found background piece...");
				if (backId!=-1)
					d.setId(backId);
				else
					backId = delyteIdC;
			}
			Log.d("nils",
					"Assigned ID "+d.getId()+" to delyta with first segment S:"+d.getSegments().get(0).start.rikt+" E:"+d.getSegments().get(0).end.rikt+" isArc: "+d.getSegments().get(0).isArc);

			delyteIdC++;
		}
		return true;
	}


	public void calcRemainingYta() {
		List<Segment> freeArcs = new ArrayList<Segment>();
		List<Segment> bgPoly;

		SortedSet<Segment> sortedArcs = new TreeSet<Segment>(new Comparator<Segment>(){
			@Override
			public int compare(Segment lhs, Segment rhs) {
				return lhs.start.rikt-rhs.start.rikt;
			}});
		//Sort arcs. Save in set.
		for(Delyta d:myDelytor)
			for (Segment s:d.tag) {
				if (!s.isArc)
					continue;
				else {
					Log.d("nils","Adding existing arc piece. S:"+s.start.rikt+" E:"+s.end.rikt+" Delyta "+d.getId());
					sortedArcs.add(s);
				}
			}
		if (sortedArcs.isEmpty()) {
			Log.d("nils","NO FREE ARC!");
		} else {
			/*			//A free arc is an arc that stretches between the end of an existing arc, and the beginning of the next.
			Segment x = sortedArcs.last();
			Log.d("nils","number of arcs: "+sortedArcs.size());
			for (Segment s:sortedArcs) {
				freeArcs.add(new Segment(x.end,s.start,true));
				Log.d("nils","Adding free arc piece. S:"+x.end.rikt+" E:"+s.start.rikt);
				x=s;
			}
		}
			 */

			//A free arc is an arc that spans a gap between two existing arcs
			
			Segment x = sortedArcs.last();
	
			Log.d("nils","number of arcs: "+sortedArcs.size());
			for (Segment s:sortedArcs) {
				Log.d("nils","checking arc: "+x.end.rikt+","+s.start.rikt);
				if (Delyta.rDist(x.end.rikt, s.start.rikt)<1) {
					Log.d("nils","skipping "+x.end.rikt+","+s.start.rikt);
					continue;
				}
				Segment potential = new Segment(x.end,s.start,true);
				//check that this arc is not already covered.
				if (!hasSegment(sortedArcs,potential)) {
					freeArcs.add(new Segment(x.end,s.start,true));
					Log.d("nils","Adding free arc piece. S:"+x.end.rikt+" E:"+s.start.rikt);
				}
				x=s;
			}
		}

		int startP = freeArcs.size();
		//Extract all segments that are not arcs.
		for(Delyta d:myDelytor) {
			for (Segment s:d.tag) {
				if (s.isArc)
					continue;
				else
					freeArcs.add(new Segment(s.end,s.start,false));
			}
		}
		//starting piece should not be an arc. Use first other. If no other, there are no free pieces.
		Set<List<Segment>> bgPollies= new HashSet<List<Segment>>();
		int previousSize=0;
				
		while(freeArcs.size()>startP && freeArcs.size()!=previousSize) {
			previousSize = freeArcs.size();
		//if (freeArcs.size()>startP) {
			bgPoly = new ArrayList<Segment>();
			Segment c = freeArcs.get(startP);
			bgPoly.add(c);
			//sort.
			boolean found = true;
			while (freeArcs.size()>0 && found == true) {
				found = false;
				for (Segment n:freeArcs) {
					Log.d("nils","Comparing segment (("+c.start.rikt+","+c.start.avst+"),("+c.end.rikt+","+c.end.avst+")) to (("+n.start.rikt+","+n.start.avst+"),("+n.end.rikt+","+n.end.avst+"))");	
					if (n.start.rikt==c.end.rikt && n.start.avst == c.end.avst) {
						bgPoly.add(n);
						freeArcs.remove(c);
						c=n;
						found = true;
						break;						
					} 
									
				}
				
			}
			Log.d("nils","bgpoly found of size "+bgPoly.size());
			for(Segment s:bgPoly) 
				Log.d("nils","start: "+s.start.rikt+"end: "+s.end.rikt+" isarc: "+s.isArc);
			bgPollies.add(bgPoly);
		}
			/*
		for(Delyta d:myDelytor) {
			for (Segment s:d.tag) {
				if (s.isArc)
					continue;
				else
					noArcs.add(s);
			}
		}

		Log.d("nils","Free arcs: ");

		for(Segment s:freeArcs) {
			Log.d("nils","S: "+s.start.rikt+" E:"+s.end.rikt);
			bgPoly = new ArrayList<Segment>();
			//Add the free arc but reversed! 
			bgPoly.add(new Segment(s.start,s.end,true));
			//Begin from the end piece (in reverse = start)
			Coord currentCoord = s.end;
			//we want to find the line that  ends here and goes to our start. when we reach there it is done.
			Coord end = s.start;
			boolean notDone = true;
			int emergencyC = 15;
			while (notDone) {
				//Find corresponding "real" Segment.
				boolean noLuck = true;
				Log.d("nils","noArcs has "+noArcs.size()+" elements");
				for (Segment se:noArcs) {
					Log.d("nils","Comparing current: "+currentCoord.rikt+" with "+se.end.rikt);
					if (currentCoord.rikt == se.end.rikt) {
						Log.d("nils","Found END MATCH!");
						//Add the reversed segment.
						bgPoly.add(new Segment(se.end,se.start,se.isArc));

						if (se.isArc) {
							Log.e("nils","In DelyteManager,calcRemYta..segment seems to be arc...should not happen");
							Toast.makeText(gs.getContext(), "Tåget är feldefinierat. Kontrollera punkterna", Toast.LENGTH_LONG).show();;
						}
						Log.d("nils","SE SEGMENT has start rikt: "+se.start.rikt+" and end rikt: "+se.end.rikt);
						currentCoord = se.start;
						if (currentCoord.rikt==end.rikt) {
							Log.d("nils","Done, found end.");
							Log.d("nils","CurrentCoord.rikt: "+currentCoord.rikt+" DOES match end.rikt: "+end.rikt);
							missingPieces.add(bgPoly);
							notDone = false;
							noLuck = false;
							break;
						} else {	
							Log.d("nils","CurrentCoord.rikt: "+currentCoord.rikt+" does not match end.rikt: "+end.rikt);
							noLuck=(emergencyC--<0);						

						}

					} 

				}
				if (noLuck) {
					notDone = false;
					if(noArcs.size()>0) {
						Log.e("nils","went thorough all without finding coord...should not happen.");					
						Toast.makeText(gs.getContext(), "Tåget är feldefinierat. Kontrollera punkterna", Toast.LENGTH_LONG).show();;
					}
				}
			}
		}

		//Here we should have all missing polygons.
		Log.d("nils","found "+missingPieces.size()+" polygons");
		//Build delytor.

		for (List<Segment> ls:missingPieces) {
			Delyta d = new Delyta(this);
			d.createFromSegments(ls);
			myDelytor.add(d);
		}

			 * 
			 */
		for (List<Segment> bgp:bgPollies) {
			if (bgp.size()>0) {
			Delyta d = new Delyta(this);
			d.createFromSegments(bgp);
			myDelytor.add(d);
			}
		}
		
		Log.d("nils","myDelytor now contains "+myDelytor.size()+" delytor.");
		printDelytor();


		//Using the free arcs as starting point, build the missing delyta.

	}





	private boolean hasSegment(SortedSet<Segment> sortedArcs, Segment p) {
		for (Segment s:sortedArcs) {
			if (p.start.rikt==s.start.rikt && p.end.rikt == s.end.rikt)
				return true;
		}
		return false;
	}

	private void printDelytor() {
		for (Delyta d:myDelytor) {
			Log.d("nils","DELYTA ID: "+d.getId()+" isbg: "+d.isBackground()+" WEST: "+d.myWest+" SOUTH: "+d.mySouth);
			for(Segment s:d.getSegments()) {
				Log.d("nils","S: "+s.start.rikt+" E:"+s.end.rikt+" isArc: "+s.isArc);
			}
		}
	}

	private DelyteManager(GlobalState gs,int pyId) {
		this.gs = gs;
		myPyID=pyId;
		al = gs.getVariableConfiguration();
	}

	public static DelyteManager create(GlobalState gs, int pyId) {
		instance = new DelyteManager(gs,pyId);
		return instance;
	}

	public static DelyteManager getInstance() {
		if (instance == null) {
			GlobalState gs = GlobalState.getInstance(null);
			String py = gs.getVariableConfiguration().getCurrentProvyta();
			if (py!=null)
				instance = new DelyteManager(gs,Integer.parseInt(py));
		}
		return instance;
	}


	private void generateFromCurrentContext() {

		loadKnownDelytor(al.getVariableValue(null,"Current_Ruta"),al.getVariableValue(null,"Current_Provyta"));
	}



	public void loadKnownDelytor(String ruta,String provyta) {

		if (ruta==null||provyta==null) {
			Log.e("nils","provyta or ruta null in loadKnownDelytor, DelyteManager");
			return;
		}

		Log.d("nils","loadKnownDelytor with RutaID "+ruta+" and provytaID "+provyta);
		Variable v;Map<String,String> keys;
		Set<String>rawTags = new HashSet<String>();
		for (int delyteID=1;delyteID<=MAX_DELYTEID;delyteID++) {
			keys= Tools.createKeyMap(VariableConfiguration.KEY_YEAR,Constants.CurrentYear+"","ruta",ruta,"provyta",provyta,"delyta",delyteID+"");
			gs.setKeyHash(keys);
			v = al.getVariableInstance(NamedVariables.DELNINGSTAG);
			Log.d("nils","Found tåg under delyteID "+delyteID+" :"+v.getValue());
			if (v.getValue()!=null&&v.getValue().length()>0)
				rawTags.add(v.getValue());			
		}

		if (rawTags.size()==0 && !isNyUtlagg()) {
			hasUnsaved = true;
			Log.d("nils","No current values får tåg. Trying historical");
			for (int delyteID=1;delyteID<=MAX_DELYTEID;delyteID++) {
				keys= Tools.createKeyMap(VariableConfiguration.KEY_YEAR,Constants.CurrentYear+"","ruta",ruta,"provyta",provyta,"delyta",delyteID+"");
				gs.setKeyHash(keys);
				v = al.getVariableInstance(NamedVariables.DELNINGSTAG);
				String rawTag = v.getHistoricalValue();
				if (rawTag!=null && rawTag.length()>0) {
					Log.d("nils","Found historical tåg under delyteID "+delyteID+" :"+rawTag);
					//Save under this year.
					v.setValue(rawTag);
					rawTags.add(rawTag);
				}

			}

		}
		if (rawTags.size()==0) {
			Log.d("nils","Nyutlägg eller tom historia");
			addDefault();	
		}
		else {			
			for (String rawTag:rawTags) {
				if (rawTag != null && rawTag.length()>0) {
					String[] tagElems = rawTag.split("\\|");
					Delyta delyta = createDelyta(tagElems);
					if (delyta!=null) 					
						myDelytor.add(delyta);

				}
			}
		}


		Log.d("nils","found "+myDelytor.size()+" delytor");



	}

	public ErrCode addUnknownTag(List<Coord> tagCoordinateList) {
		Delyta delyta = new Delyta(this);
		ErrCode ec = delyta.create(tagCoordinateList);
		if (ec==ErrCode.ok) {
			hasUnsaved = true;
			myDelytor.add(delyta);
		}
		return ec;
	}

	public void clear() {
		hasUnsaved = false;
		myDelytor.clear();
	}


	public Delyta createDelyta(String[] tagElems) {								
		if (tagElems!=null) {
			for (String s:tagElems) {
				Log.d("nils","tagElem: "+s);
			}
			int avst=-1,rikt=-1;
			List<Coord> tagCoordinateList = new ArrayList<Coord>();
			for (int j=0;j<tagElems.length-1;j+=2) {
				avst =Integer.parseInt(tagElems[j]);
				rikt =Integer.parseInt(tagElems[j+1]);
				tagCoordinateList.add(new Coord(avst,rikt));

			}
			Delyta delyta = new Delyta(this);
			ErrCode ec = delyta.create(tagCoordinateList);
			if (ec==ErrCode.ok)
				return delyta;
			else 
				Log.e("nils","Failed to create delyta. error code: "+ec);
		}
		return null;
	}


	public List<Delyta> getDelytor() {
		return myDelytor;
	}

	public void init() {
		myDelytor.clear();
		//gs.setKeyHash(al.createDelytaKeyMap());
		generateFromCurrentContext();
		analyze();
	}

	public boolean save() {

		//Check if it is possible to determine småprovytans location. If not, return false
		if(!updateSmaProvToDelytaAssociations())
			return false;		
		Map<String, String> baseKey = al.createProvytaKeyMap();
		Variable tempVar;
		//Copy new
		for (Delyta d:myDelytor) {
			if (d!=null) {
				int id = d.getId();
				Log.d("nils","added delyta = "+id+" to base key");				
				baseKey.put("delyta", id+"");
				if (id!=0) {					
					String tag = d.getTag();
					tempVar = al.getVariableUsingKey(baseKey, "Delningstag");
					tempVar.setValue(tag);
					//Contains smaprovyta? In that case, set delytaId in variable
					//tillhorDelytaMedID
				}
			}
		}	
			
		hasUnsaved = false;
		return true;
	}

	public void addDefault() {
		Log.d("nils","No delytor found. Creating default delyta 0");
		Delyta delyta = createDelyta(new String[] {"100","359","100","0"});
		delyta.setId(0);
		myDelytor.add(delyta);
	}

	private boolean isNyUtlagg() {
		String isNy = al.getVariableValue(al.createProvytaKeyMap(), NamedVariables.NYUTLAGG);
		return isNy!=null;
	}

	public boolean hasUnsavedChanges() {
		return hasUnsaved;
	}

	public int getPyID() {
		return myPyID;
	}

	public void setSelected(Delyta dy) {
		for(Delyta d:myDelytor) {
			if (dy!=null && d.equals(dy))
				d.setSelected(true);
			else
				d.setSelected(false);
		}
	}

	public Delyta getDelyta(int id) {
		for(Delyta d:myDelytor) {
			if (d.getId()==id)
				return d;
		}
		return null;
	}

	public boolean isObsolete() {
		Map<String, String> baseKey = al.createProvytaKeyMap();
		Variable tempVar;
		//Copy new
		for (Delyta d:myDelytor) {
			if (d!=null) {
				int id = d.getId();
				Log.d("nils","added delyta = "+id+" to base key");				
				baseKey.put("delyta", id+"");
				if (id!=0) {					
					tempVar = al.getVariableUsingKey(baseKey, "Delningstag");
					if (tempVar.isInvalidated()) {
						Log.d("nils","I think my DYM IS OBSOLETE!!!!");
						return true;
					}
					//Contains smaprovyta? In that case, set delytaId in variable

				}
			}
		}
		return false;
	}







}
