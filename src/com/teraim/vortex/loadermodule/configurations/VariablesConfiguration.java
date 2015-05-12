package com.teraim.vortex.loadermodule.configurations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.dynamic.types.SpinnerDefinition;
import com.teraim.vortex.dynamic.types.Table;
import com.teraim.vortex.dynamic.types.Table.ErrCode;
import com.teraim.vortex.loadermodule.CSVConfigurationModule;
import com.teraim.vortex.loadermodule.LoadResult;
import com.teraim.vortex.loadermodule.LoadResult.ErrorCode;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.PersistenceHelper;
import com.teraim.vortex.utils.Tools;

public class VariablesConfiguration extends CSVConfigurationModule {

	private final static int noOfRequiredColumns=5;			
	private final SpinnerDefinition sd=new SpinnerDefinition();
	private LoggerI o;
	private int pGroupIndex = 1;
	private int pNameIndex = 2;
	private Table myTable=null;
	private List<String> cheaderL;
	private boolean scanHeader;
	private String[] groupsFileHeaderS;
	private Map<String, List<List<String>>> groups;
	private int nameIndex;
	private int groupIndex;


	public VariablesConfiguration(PersistenceHelper globalPh,PersistenceHelper ph, String server, String bundle, LoggerI debugConsole) {
		super(globalPh,ph, Source.internet, server+bundle.toLowerCase()+"/",VariablesConfiguration.NAME,"Variables module      ");	 
		this.o = debugConsole;
		o.addRow("Parsing Variables.csv file");

	}

	public static String NAME = "Variables";
	

	@Override
	public String getFrozenVersion() {
		return (ph.get(PersistenceHelper.CURRENT_VERSION_OF_VARPATTERN_FILE));
	}

	@Override
	protected void setFrozenVersion(String version) {
		ph.put(PersistenceHelper.CURRENT_VERSION_OF_VARPATTERN_FILE,version);

	}

	@Override
	public boolean isRequired() {
		return true;
	}


	@Override
	protected LoadResult prepare() throws IOException {		
		GroupsConfiguration gc=null;
		cheaderL = new ArrayList<String>();

		scanHeader = true;

		//check if there is a groups configuration.
		if ((gc=GroupsConfiguration.getSingleton()) == null) {
			//TODO: Find a way to do this better.
			groupsFileHeaderS = Constants.defaultGroupHeader;
			groups = new HashMap<String, List<List<String>>>();

		} else {
			groupsFileHeaderS = gc.getGroupFileColumns();
			groups = gc.getGroups();
			nameIndex = gc.getNameIndex();
			groupIndex = gc.getGroupIndex();
		}
		return null;
	}



	@Override
	public LoadResult parse(String row, Integer currentRow) throws IOException {


		if (scanHeader && row!=null) {
			Log.d("vortex","header is: "+row);			
			String[] varPatternHeaderS = row.split(",");
			if (varPatternHeaderS==null||varPatternHeaderS.length<Constants.VAR_PATTERN_ROW_LENGTH) {
				o.addRow("");
				o.addRedText("Header corrupt in Variables.csv: "+varPatternHeaderS);
				return new LoadResult(this,ErrorCode.ParseError,"Corrupt header");
			}
			//Remove duplicte group column and varname. 
			Collections.addAll(cheaderL,groupsFileHeaderS); 
			cheaderL.remove(VariableConfiguration.Col_Functional_Group);
			cheaderL.remove(VariableConfiguration.Col_Variable_Name);
			List<String> vheaderL = new ArrayList<String>(trimmed(varPatternHeaderS));
			vheaderL.addAll(cheaderL);
			myTable = new Table(vheaderL,0,pNameIndex);
			scanHeader=false;

		} else {
			List<List<String>> elems;


			String[] r = Tools.split(row);

			if (r==null || r.length<Constants.VAR_PATTERN_ROW_LENGTH) {
				o.addRow("");
				o.addRedText("found null or too short row at "+currentRow+" in config file..skipping");
				return new LoadResult(this,ErrorCode.ParseError,"Parse error, row: "+currentRow);
			} else {	
				for(int i=0;i<r.length;i++) {
					if (r[i]!=null)
						r[i] = r[i].replace("\"", "");

				}
				String pGroup = r[pGroupIndex];
				List<String> trr=trimmed(r);
				if (pGroup==null || pGroup.trim().length()==0) {
					//Log.d("nils","found variable "+r[pNameIndex]+" in varpattern");							
					myTable.addRow(trr);
					o.addRow("Generated variable(1): ["+r[pNameIndex]+"]");
				} else {
					//Log.d("nils","found group name: "+pGroup);
					elems = groups.get(pGroup);
					String varPatternName = r[pNameIndex];
					if (elems==null) {
						//If the variable has a group,add it 
						//Log.d("nils","Group "+pGroup+" in line#"+rowC+" does not exist in config file. Will use name: "+varPatternName);								
						String name = pGroup.trim()+Constants.VariableSeparator+varPatternName.trim();
						o.addRow("Generated variable(2): ["+name+"]");
						trr.set(pNameIndex, name);
						myTable.addRow(trr);
					} else {
						for (List<String>elem:elems) {
							//Go through all rows in group. Generate variables.
							String cFileNamePart = elem.get(nameIndex);

							if (varPatternName==null) {
								o.addRow("");
								o.addRedText("varPatternNamepart evaluates to null at line#"+currentRow+" in varpattern file");
							} else {
								String fullVarName = pGroup.trim()+Constants.VariableSeparator+(cFileNamePart!=null?cFileNamePart.trim()+Constants.VariableSeparator:"")+varPatternName.trim();
								//Remove duplicate elements from Config File row.
								//Make a copy.
								List<String>elemCopy = new ArrayList<String>(elem);
								elemCopy.remove(nameIndex);
								elemCopy.remove(groupIndex);
								List<String>varPatternL = new ArrayList<String>(trimmed(r));
								varPatternL.addAll(elemCopy);
								//Replace name column with full name.
								varPatternL.set(pNameIndex, fullVarName);
								o.addRow("Generated variable(3): ["+fullVarName+"]");
								ErrCode err = myTable.addRow(varPatternL);
								if (err!=ErrCode.ok) {
									switch (err) {
									case keyError:
										o.addRow("");
										o.addRedText("KEY ERROR!");
										break;
									case tooFewColumns:
										o.addRow("");
										o.addRedText("TOO FEW COLUMNS!");
										return new LoadResult(this,ErrorCode.ParseError);
									case tooManyColumns:
										o.addRow("");
										o.addRedText("TOO MANY COLUMNS!");
										o.addRedText("row not inserted. Something wrong at line "+currentRow);
										break;
									}
									
								}
							}
						}
					}
				}
			}

		}


		return null;
	}

	

	private List<String> trimmed(String[] r) {
		ArrayList<String> ret = new ArrayList<String>();
		for(int i=0;i<Constants.VAR_PATTERN_ROW_LENGTH;i++)
			ret.add(r[i]);
		return ret;
	}

	@Override
	public void setEssence() {
		essence = myTable;
	}

}
