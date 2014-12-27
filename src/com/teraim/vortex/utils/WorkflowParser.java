package com.teraim.vortex.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.util.Xml;

import com.teraim.vortex.FileLoadedCb;
import com.teraim.vortex.GlobalState;
import com.teraim.vortex.FileLoadedCb.ErrorCode;
import com.teraim.vortex.dynamic.blocks.AddEntryToFieldListBlock;
import com.teraim.vortex.dynamic.blocks.AddRuleBlock;
import com.teraim.vortex.dynamic.blocks.AddSumOrCountBlock;
import com.teraim.vortex.dynamic.blocks.AddVariableToEntryFieldBlock;
import com.teraim.vortex.dynamic.blocks.AddVariableToEveryListEntryBlock;
import com.teraim.vortex.dynamic.blocks.AddVariableToListEntry;
import com.teraim.vortex.dynamic.blocks.Block;
import com.teraim.vortex.dynamic.blocks.BlockCreateListEntriesFromFieldList;
import com.teraim.vortex.dynamic.blocks.ButtonBlock;
import com.teraim.vortex.dynamic.blocks.ConditionalContinuationBlock;
import com.teraim.vortex.dynamic.blocks.ContainerDefineBlock;
import com.teraim.vortex.dynamic.blocks.CreateEntryFieldBlock;
import com.teraim.vortex.dynamic.blocks.CreateSortWidgetBlock;
import com.teraim.vortex.dynamic.blocks.DisplayValueBlock;
import com.teraim.vortex.dynamic.blocks.JumpBlock;
import com.teraim.vortex.dynamic.blocks.LayoutBlock;
import com.teraim.vortex.dynamic.blocks.MenuEntryBlock;
import com.teraim.vortex.dynamic.blocks.MenuHeaderBlock;
import com.teraim.vortex.dynamic.blocks.PageDefineBlock;
import com.teraim.vortex.dynamic.blocks.SetValueBlock;
import com.teraim.vortex.dynamic.blocks.StartBlock;
import com.teraim.vortex.dynamic.types.Workflow;
import com.teraim.vortex.dynamic.types.Workflow.Unit;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Not_ClickableField_SumAndCountOfVariables;
import com.teraim.vortex.exceptions.SameOldException;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.non_generics.Constants;

/**
 * 
 * @author Terje
 *
 * Parser that reads XML descriptions of workflows.
 * Will try to parse the XML into a list.
 * TODO: Must implement good syntax error messages.
 * TODO: Remove parsing of Label. Not used.
 */
public class WorkflowParser extends AsyncTask<Context,Void,ErrorCode>{

	Context ctx;
	//Location of bundle.
	PersistenceHelper ph;
	FileLoadedCb cb;
	String myVersion,myApplication;
	List<Workflow> myFlow = null;
	LoggerI o;
	boolean workflowsExist;


	public WorkflowParser(GlobalState gs, FileLoadedCb fileLoadedCb) {
		this.ph=gs.getPersistence();
		this.cb = fileLoadedCb;
		Log.d("vortex","in workflowparser, workflows: "+gs.getWfs());
		workflowsExist = gs.getWfs()!=null;

	}



	//Take input file from remote web server and parse it.
	//Generates a list of workflows from a Bundle.
	@Override
	protected ErrorCode doInBackground(Context... params) {
		ctx = params[0];
		o = GlobalState.getInstance(ctx).getLogger();
		String serverUrl = ph.get(PersistenceHelper.SERVER_URL);
		if (serverUrl ==null || serverUrl.equals(PersistenceHelper.UNDEFINED) || serverUrl.length()==0)
			return ErrorCode.configurationError;
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

		return parse(serverUrl+bundle+"/"+ph.get(PersistenceHelper.BUNDLE_NAME)+".xml");
	}

	@Override
	protected void onPostExecute(ErrorCode code) {
		if (code == ErrorCode.newConfigVersionLoaded) {
			boolean ok= Tools.witeObjectToFile(ctx, myFlow, Constants.CONFIG_FILES_DIR+Constants.WF_FROZEN_FILE_ID);
			if (!ok)
				code = ErrorCode.ioError;
			else {
				o.addRow("Setting current version of workflow bundle to "+myVersion);
				ph.put(PersistenceHelper.CURRENT_VERSION_OF_WF_BUNDLE,myVersion);
				code = ErrorCode.newConfigVersionLoaded;

			}
		}
		cb.onFileLoaded(code,myVersion);	
	}
	public ErrorCode parse(String fileUrl)  {
		myFlow=null;
		Log.d("nils","File url: "+fileUrl);
		try {	
			URL url = new URL(fileUrl);
			o.addRow("Fetching workflow bundle "+fileUrl);

			/* Open a connection to that URL. */
			URLConnection ucon = url.openConnection();
			if (Build.VERSION.SDK != null && Build.VERSION.SDK_INT > 13) { ucon.setRequestProperty("Connection", "close"); }
			InputStream in = ucon.getInputStream();

			//InputStream in = c.getResources().openRawResource(R.raw.mainflow);

			//File flow = new File("res/raw/"+fileName+".xml");
			//in = new FileInputStream(flow);
			//We now have the XML file opened. Time to parse.
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(in, null);
			parser.nextTag();
			myFlow = readBundle(parser);
		} catch (XmlPullParserException e) {
			e.printStackTrace();
			return FileLoadedCb.ErrorCode.parseError;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return FileLoadedCb.ErrorCode.notFound;			
		} catch (IOException e) {			
			e.printStackTrace();
			return FileLoadedCb.ErrorCode.ioError;

		} catch (SameOldException e) {
			return FileLoadedCb.ErrorCode.sameold;

		} catch (Exception e) {
			o.addRow("");
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);		
			o.addRedText(sw.toString());
			e.printStackTrace();
		}

		if (myFlow !=null) {
			o.addRow("");
			o.addYellowText("Parsed in total: "+myFlow.size()+" workflows");			
			return ErrorCode.newConfigVersionLoaded;
		}
		//This should never happen.
		return FileLoadedCb.ErrorCode.parseError;
	}


	private List<Workflow> readBundle(XmlPullParser parser) throws XmlPullParserException, IOException, SameOldException {

		List<Workflow> bundle = new ArrayList<Workflow>();
		parser.require(XmlPullParser.START_TAG, null, "bundle");
		myApplication = parser.getAttributeValue(null, "application");
		myVersion = parser.getAttributeValue(null, "version");
		o.addRow("File workflow bundle version: ");o.addYellowText(myVersion);
		if (workflowsExist) {
			if (ph.getB(PersistenceHelper.VERSION_CONTROL_SWITCH_OFF)) {
				o.addRow("Version control is switched off.");
			} else 
				if(myVersion.equals(ph.get(PersistenceHelper.CURRENT_VERSION_OF_WF_BUNDLE))) 			
					throw new SameOldException();	
			o.addRow("Saved workflow bundle version: ");o.addYellowText(ph.get(PersistenceHelper.CURRENT_VERSION_OF_WF_BUNDLE));
		} else
			o.addRow("Workflows missing on SD card..will reload");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				Log.d("NILS","Skipping "+parser.getName());
				continue;
			}
			String name = parser.getName();
			Log.d("NILS","Doing: "+name);
			// Starts by looking for the entry tag
			if (name.equals("workflow")) {
				bundle.add(readWorkflow(parser));
			} else {
				skip(name,parser);
			}	
		}  
		return bundle;
	}

	static String errorString ="";
	static int errCount = 0;
	private Workflow readWorkflow(XmlPullParser parser) throws XmlPullParserException, IOException {
		Workflow wf = new Workflow(myVersion,myApplication);
		parser.require(XmlPullParser.START_TAG, null, "workflow");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}

			String name = parser.getName();
			if (name.equals("blocks")) {
				wf.addBlocks(readBlocks(parser));
			} else {
				skip(name,parser);
			}
		}

		return wf;


	}



	/**
	 * Read blocks. Create respective class and return as a list.
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	private static boolean isSum = false;
	private static boolean isCount = true;

	private List<Block> readBlocks(XmlPullParser parser) throws IOException, XmlPullParserException {
		List<Block> blocks=new ArrayList<Block>();
		parser.require(XmlPullParser.START_TAG, null,"blocks");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();

			if (name.equals("block_start")) 
				blocks.add(readBlockStart(parser));
			else if (name.equals("block_define_page")) 
				blocks.add(readPageDefineBlock(parser));				
			else if (name.equals("block_define_container")) 
				blocks.add(readContainerDefineBlock(parser));				
			else if (name.equals("block_layout")) 
				blocks.add(readBlockLayout(parser));				
			else if (name.equals("block_button")) 
				blocks.add(readBlockButton(parser));
			else if (name.equals("block_add_rule")) 
				blocks.add(readBlockAddRule(parser));
			else if (name.equals("block_add_sum_of_selected_variables_display")) 
				blocks.add(readBlockAddSelectionOrSum(parser,isSum));
			else if (name.equals("block_add_number_of_selections_display")) 
				blocks.add(readBlockAddSelectionOrSum(parser,isCount));
			else if (name.equals("block_create_sort_widget")) 
				blocks.add(readBlockCreateSorting(parser));
			//This is a dummy call. Not supported block.
			else if (name.equals("block_create_list_filter")) 
				dummyWarning("block_create_list_filter",parser);					
			else if (name.equals("block_create_list_entries")) 
				dummyWarning("block_create_list_entries",parser);
			else if (name.equals("block_create_entry_field")) 
				blocks.add(readBlockCreateEntryField(parser));
			else if (name.equals("block_create_display_field"))
				blocks.add(readBlockCreateDisplayField(parser));
			else if (name.equals("block_create_list_entries_from_field_list"))
				blocks.add(readBlockCreateListEntriesFromFieldList(parser));
			else if (name.equals("block_add_variable_to_every_list_entry"))
				blocks.add(readBlockAddVariableToEveryListEntry(parser));
			else if (name.equals("block_add_variable_to_entry_field"))
				blocks.add(readBlockAddVariableToEntryField(parser));	
			else if (name.equals("block_add_entry_to_field_list")) 
				blocks.add(readBlockAddEntryToFieldList(parser));
			else if (name.equals("block_add_variable_to_list_entry")) 
				blocks.add(readBlockAddVariableToListEntry(parser));
			else if (name.equals("block_conditional_continuation")) 
				blocks.add(readBlockConditionalContinuation(parser));
			else if (name.equals("block_jump")) 
				blocks.add(readBlockJump(parser));
			else if (name.equals("block_set_value"))
				blocks.add(readBlockSetValue(parser));
			else if (name.equals("block_add_rule"))
				blocks.add(readBlockAddRule(parser));
			else if (name.equals("block_defineMenuHeader"))
				blocks.add(readBlockDefineMenuHeader(parser));
			else if (name.equals("block_defineMenuEntry"))
				blocks.add(readBlockDefineMenuEntry(parser));			

			else {			
				skip(name,parser);
			}
		}

		return blocks;
	}


	private Block readBlockDefineMenuHeader(XmlPullParser parser) throws IOException, XmlPullParserException {
		//		o.addRow("Parsing block: block_set_value...");
		String id=null,label=null,textColor=null,bgColor=null;
		parser.require(XmlPullParser.START_TAG, null,"block_defineMenuHeader");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			} else if (name.equals("label")) {
				label = readText("label",parser);
			} else if (name.equals("text_color")) {
				textColor = readText("text_color",parser);
			} else if (name.equals("bck_color")) {
				bgColor = readText("bck_color",parser);
			} else
				skip(name,parser);

		}
		checkForNull("block_ID",id,"label",label,"text_color",textColor,"bck_color",bgColor);
		return new MenuHeaderBlock(id,label,textColor,bgColor);

	}	private Block readBlockDefineMenuEntry(XmlPullParser parser) throws IOException, XmlPullParserException {
		//		o.addRow("Parsing block: block_set_value...");
		String id=null,type=null,target=null,textColor=null,bgColor=null;
		parser.require(XmlPullParser.START_TAG, null,"block_defineMenuEntry");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			} else if (name.equals("target")) {
				target = readText("target",parser);
			} else if (name.equals("type")) {
				type = readText("type",parser);
			} else if (name.equals("text_color")) {
				textColor = readText("text_color",parser);
			} else if (name.equals("bck_color")) {
				bgColor = readText("bck_color",parser);
			} else
				skip(name,parser);

		}
		checkForNull("block_ID",id,"target",target,"type",type);
		return new MenuEntryBlock(id,target,type,bgColor,textColor);

	}
	private Block readBlockSetValue(XmlPullParser parser) throws IOException, XmlPullParserException {
		//		o.addRow("Parsing block: block_set_value...");
		String id=null,target=null,expression=null;
		String executionBehavior=null;
		parser.require(XmlPullParser.START_TAG, null,"block_set_value");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			} else if (name.equals("target")) {
				target = readText("target",parser);
			} else if (name.equals("expression")) {
				expression = readText("expression",parser);
			} else if (name.equals("execution_behavior")) {
				executionBehavior = readText("execution_behavior",parser);
			} else
				skip(name,parser);

		}
		checkForNull("block_ID",id,"target",target,"expression",expression,"execution_behavior",executionBehavior);
		return new SetValueBlock(id,target,expression,executionBehavior);

	}



	private Block readBlockJump(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addRow("Parsing block: block_jump...");
		String id=null,nextBlockId=null;

		parser.require(XmlPullParser.START_TAG, null,"block_jump");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			} else if (name.equals("next_block_ID")) {
				nextBlockId = readText("next_block_ID",parser);
			}
			else		
				skip(name,parser);
		}
		checkForNull("block_ID",id,"next_block_id",nextBlockId);
		return new JumpBlock(id,nextBlockId);
	}



	private Block readBlockConditionalContinuation(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addRow("Parsing block: block_conditional_continuation...");
		List<String> varL=null;
		String id=null,expr=null,elseBlockId=null; 
		parser.require(XmlPullParser.START_TAG, null,"block_conditional_continuation");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			}
			/*
			else if (name.equals("variables")) {
				varS = readText("variables",parser);
				o.addRow("VARIABLES: "+varS);	
				String[] varsA = varS.split(",");
				if (varsA!=null && varsA.length>0) {
					varL = new ArrayList<String>();
					String v;
					for (int i = 0;i< varsA.length;i++) {
						v = varsA[i].trim();
						if (v.length()>0)
							varL.add(v);						
					}
				}
			}
			 */
			else if (name.equals("expression")) {
				expr = readText("expression",parser);
			}
			else if (name.equals("else_block_ID")) {
				elseBlockId = readText("else_block_ID",parser);
			} 
			else
				skip(name,parser);

		}
		checkForNull("block_ID",id,"expression",expr,"else_block_ID",elseBlockId);

		return new ConditionalContinuationBlock(id,varL,expr,elseBlockId);
	}



	private Block readBlockAddVariableToListEntry(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addRow("Parsing block: block_add_variable_to_list_entry...");
		boolean isVisible = true,isDisplayed = false,showHistorical=false;
		String targetList= null,targetField= null,namn=null,format= null,id=null,initialValue=null; 
		parser.require(XmlPullParser.START_TAG, null,"block_add_variable_to_list_entry");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();

			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			}
			else if (name.equals("name")) {
				namn = readText("name",parser);
			} else if (name.equals("target_list")) {
				targetList = readText("target_list",parser);
			}
			else if (name.equals("target_field")) {
				targetField = readText("target_field",parser);
			} 
			else if (name.equals("is_displayed")) {
				isDisplayed = !readText("is_displayed",parser).equals("false");
			} 
			else if (name.equals("is_visible")) {
				isVisible = !readText("is_visible",parser).equals("false");
			}
			else if (name.equals("format")) {
				format = readText("format",parser);
			} else if (name.equals("show_historical")) {
				showHistorical = readText("show_historical",parser).equals("true");
			}  else if (name.equals("initial_value")) {
				initialValue = readText("initial_value",parser);
			}
			else
				skip(name,parser);
		}
		checkForNull("block_ID",id,"name",namn,"target_list",targetList,"target_field",targetField,"format",format);
		return new AddVariableToListEntry(id,namn,
				targetList,targetField, isDisplayed,format,isVisible,showHistorical,initialValue);	

	}




	private Block readBlockAddEntryToFieldList(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addRow("Parsing block: block_add_entry_to_field_list...");

		String target= null,namn= null,label=null,description=null,id=null;
		parser.require(XmlPullParser.START_TAG, null,"block_add_entry_to_field_list");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			}
			else if (name.equals("name")) {
				namn = readText("name",parser);
			} else if (name.equals("target")) {
				target = readText("target",parser);
			} 
			else if (name.equals("label")) {
				label = readText("label",parser);
			}
			else if (name.equals("description")) {
				description = readText("description",parser);
			}
			else
				skip(name,parser);
		}
		checkForNull("block_ID",id,"name",namn,"target",target,"label",label,"description",description);
		return new AddEntryToFieldListBlock(id,namn,target,label,description);
	}

	private Block readBlockAddVariableToEntryField(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addRow("Parsing block: block_add_variable_to_entry_field...");
		boolean isVisible = true,isDisplayed=false,showHistorical=false;
		String target= null,namn= null,format= null,id=null,initialValue=null; 
		parser.require(XmlPullParser.START_TAG, null,"block_add_variable_to_entry_field");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();

			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			}
			else if (name.equals("name")) {
				namn = readText("name",parser);
			} else if (name.equals("target")) {
				target = readText("target",parser);
			} 
			else if (name.equals("is_displayed")) {
				isDisplayed = !readText("is_displayed",parser).equals("false");
			} 
			else if (name.equals("is_visible")) {
				isVisible = !readText("is_visible",parser).equals("false");
			}
			else if (name.equals("format")) {
				format = readText("format",parser);

			} else if (name.equals("show_historical")) {
				showHistorical = readText("show_historical",parser).equals("true");
			}  else if (name.equals("initial_value")) {
				initialValue = readText("initial_value",parser);
			}
			else
				skip(name,parser);
		}
		checkForNull("block_ID",id,"name",namn,"target",target,"format",format);
		return new AddVariableToEntryFieldBlock(id,target,namn,isDisplayed,format,isVisible,showHistorical,initialValue);
	}
	private Block readBlockCreateListEntriesFromFieldList(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addRow("Parsing block: block_create_list_entries_from_field_list...");
		String namn=null, keyField = null, type=null,containerId=null,selectionField=null,selectionPattern=null,id=null;
		String labelField=null,descriptionField=null,typeField=null,uriField=null,variatorColumn=null;
		parser.require(XmlPullParser.START_TAG, null,"block_create_list_entries_from_field_list");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			}
			else if (name.equals("type")) {
				type = readText("type",parser);
			} else if (name.equals("selection_field")) {
				selectionField = readText("selection_field",parser);
			} else if (name.equals("selection_pattern")) {
				selectionPattern = readText("selection_pattern",parser);
			} else if (name.equals("key_field")) {
				keyField = readText("key_field",parser);
			} else if (name.equals("name")) {
				namn = readText("name",parser);
			} else if (name.equals("container_name")) {
				containerId = readText("container_name",parser);
			}  else if (name.equals("label_field")) {
				labelField = readText("label_field",parser);
			} else if (name.equals("description_field")) {
				descriptionField = readText("description_field",parser);
			} else if (name.equals("type_field")) {
				typeField = readText("type_field",parser);
			} else if (name.equals("variator")) {
				variatorColumn = readText("variator",parser);
			} else if (name.equals("uri_field")) {
				uriField = readText("uri_field",parser);
			} else
				skip(name,parser);

		}
		checkForNull("block_ID",id,"selection_field",selectionField,"key_field",keyField,"name",namn,"container_name",
				containerId,"label_field",labelField,"description_field",descriptionField,"type_field",typeField,
				"uri_field",uriField,"variator",variatorColumn);
		return new BlockCreateListEntriesFromFieldList(id,namn, type,
				containerId,selectionPattern,selectionField,variatorColumn,keyField);
	}



	private Block readBlockAddVariableToEveryListEntry(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addRow("Parsing block: block_add_variable_to_every_list_entry...");
		String target=null,variableSuffix=null,format=null,id=null,initialValue=null;
		boolean displayOut=false,isVisible=true,showHistorical=false;

		parser.require(XmlPullParser.START_TAG, null,"block_add_variable_to_every_list_entry");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();

			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			} else if (name.equals("target")) {
				target = readText("target",parser);
			} else if (name.equals("format")) {
				format = readText("format",parser);
			}else if (name.equals("name")) {
				variableSuffix = readText("name",parser);
			} else if (name.equals("is_displayed")) {
				displayOut = readText("is_displayed",parser).trim().equals("true");
			} else if (name.equals("is_visible")) {
				isVisible = readText("is_visible",parser).trim().equals("true");
			} else if (name.equals("show_historical")) {
				showHistorical = readText("show_historical",parser).equals("true");
			}  else if (name.equals("initial_value")) {
				initialValue = readText("initial_value",parser);
			}
			else
				skip(name,parser);

		}
		checkForNull("block_ID",id,"target",target,"format",format,"name",variableSuffix);
		return new 	AddVariableToEveryListEntryBlock(id,target,
				variableSuffix, displayOut,format,isVisible,showHistorical,initialValue);
	}

	private DisplayValueBlock readBlockCreateDisplayField(XmlPullParser parser)throws IOException, XmlPullParserException {
		//o.addRow("Parsing block: block_create_display_field...");
		boolean isVisible = true;
		String namn=null, formula = null, label=null,containerId=null,format = null,id=null;
		Unit unit=null;	
		parser.require(XmlPullParser.START_TAG, null,"block_create_display_field");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();

			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			} else if (name.equals("label")) {
				label = readText("label",parser);
			} else if (name.equals("expression")) {
				formula = readText("expression",parser);
			} else if (name.equals("name")) {
				namn = readText("name",parser);
			} else if (name.equals("container_name")) {
				containerId = readText("container_name",parser);
			}  else if (name.equals("unit")) {
				unit = Tools.convertToUnit(readText("unit",parser));
			} else if (name.equals("is_visible")) {
				isVisible = !readText("is_visible",parser).equals("false");
			} else if (name.equals("format")) {
				format = readText("format",parser);
			}
			else
				skip(name,parser);

		}
		checkForNull("label",label,"expression",formula,"block_ID",id,"name",namn,"container_name",containerId,"format",format);
		return new DisplayValueBlock(id,namn, label,unit,
				formula,containerId,isVisible,format);
	}


	private CreateEntryFieldBlock readBlockCreateEntryField(XmlPullParser parser)throws IOException, XmlPullParserException {
		//o.addRow("Parsing block: block_create_entry_field...");
		boolean isVisible = true,showHistorical = false;
		String namn=null,containerId=null,postLabel="",format=null,id=null,initialValue=null;
		Unit unit = Unit.nd;		
		parser.require(XmlPullParser.START_TAG, null,"block_create_entry_field");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();

			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			} else if (name.equals("name")) {
				namn = readText("name",parser);
			} else if (name.equals("container_name")) {
				containerId = readText("container_name",parser);
			} 
			else if (name.equals("is_visible")) {
				isVisible = !readText("is_visible",parser).equals("false");
			} 
			else if (name.equals("format")) {
				format = readText("format",parser);
			}  else if (name.equals("show_historical")) {
				showHistorical = readText("show_historical",parser).equals("true");
			}  else if (name.equals("initial_value")) {
				initialValue = readText("initial_value",parser);
			}
			else
				skip(name,parser);
		}
		checkForNull("block_ID",id,"name",namn,"container_name",containerId,"format",format);
		return new CreateEntryFieldBlock(id,namn, containerId,isVisible,format,showHistorical,initialValue);
	}

	/**
	 * Creates a Block for adding a sorting function on Target List. 
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */

	private void dummyWarning(String block,XmlPullParser parser) throws IOException, XmlPullParserException {
		o.addRow("Parsing block: "+block);
		o.addRow("");
		o.addRedText("This type of block is not supported");
	}


	/**
	 * Creates a Block for adding a sorting function on Target List. 
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	private CreateSortWidgetBlock readBlockCreateSorting(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addRow("Parsing block: block_create_sort_widget...");
		String id=null,namn=null,containerName=null,type=null,target=null,selectionField=null,displayField=null,selectionPattern=null;
		boolean isVisible = true;
		parser.require(XmlPullParser.START_TAG, null,"block_create_sort_widget");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();

			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			} else if (name.equals("container_name")) {
				containerName = readText("container_name",parser);

			} else if (name.equals("name")) {
				namn = readText("name",parser);

			} else if (name.equals("type")) {
				type = readText("type",parser);

			} else if (name.equals("target")) {
				target = readText("target",parser);

			} else if (name.equals("is_visible")) {
				isVisible = !readText("is_visible",parser).equals("false");

			}  else if (name.equals("selection_field")) {
				selectionField = readText("selection_field",parser);

			}  else if (name.equals("display_field")) {
				displayField = readText("display_field",parser);

			}  else if (name.equals("selection_pattern")) {
				selectionPattern = readText("selection_pattern",parser);

			}

			else 
				skip(name,parser);

		}
		checkForNull("BLOCK_ID",id,"CONTAINER_NAME: ",containerName,"SELECTION_PATTERN",selectionPattern,"DISPLAY_FIELD",displayField,
				"SELECTION_FIELD",selectionField,"TARGET"+target,"TYPE",type,
				"NAME",namn);
		return new CreateSortWidgetBlock(id,namn,type, containerName,target,selectionField,displayField,selectionPattern,isVisible);
	}

	/**
	 * Creates a Block for displaying the number of selected entries currently in a list. 
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	private AddSumOrCountBlock readBlockAddSelectionOrSum(XmlPullParser parser,boolean isCount) throws IOException, XmlPullParserException {
		String containerName=null,label=null,postLabel = null,filter=null,target=null,result=null,format=null,id=null;
		WF_Not_ClickableField_SumAndCountOfVariables.Type type;

		boolean isVisible = true;

		if (isCount)
			type = WF_Not_ClickableField_SumAndCountOfVariables.Type.count;
		else
			type = WF_Not_ClickableField_SumAndCountOfVariables.Type.sum;

		if (isCount) {
			//o.addRow("Parsing block: block_add_number_of_selections_display...");
			parser.require(XmlPullParser.START_TAG, null,"block_add_number_of_selections_display");
		}
		else {
			//o.addRow("Parsing block: block_add_sum_of_selected_variables_display...");
			parser.require(XmlPullParser.START_TAG, null,"block_add_sum_of_selected_variables_display");
		}
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}	
			String name= parser.getName();

			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			} else if (name.equals("container_name")) {
				containerName = readText("container_name",parser);
			} 
			else if (name.equals("label")) {
				label = readText("label",parser);
			}
			else if (name.equals("filter")) {
				filter = readText("filter",parser);
			}
			else if (name.equals("target")) {
				target = readText("target",parser);
			}
			else if (name.equals("unit")) {
				postLabel = readText("unit",parser);
			}
			else if (name.equals("result")) {
				result = readText("result",parser);
			}
			else if (name.equals("is_visible")) {
				isVisible = !readText("is_visible",parser).equals("false");
			} 
			else if (name.equals("format")) {
				format = readText("format",parser);

			}

			else
				skip(name,parser);

		}
		checkForNull("block_ID",id,"label",label,"container_name",containerName,"filter",filter,
				"target",target,"result",result,"format",format,"unit",postLabel);
		return new AddSumOrCountBlock(id,containerName,label,postLabel,filter,target,type,result,isVisible,format);
	}	


	/**
	 *  Creates a CreateListEntriesBlock. 
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 * @throws EvalException 
	 */	
	/*
	private CreateListEntriesBlock readBlockCreateListEntries(XmlPullParser parser) throws IOException, XmlPullParserException, EvalException {
		o.addRow("Parsing block: block_create_list_entries...");

		o.addRow("");
		o.addRedText("block_create_list_entries is no longer supported. Use block_create_list_entries_from_field_list instead");
		return null;
	}
	 */
	/*
		boolean isVisible = true;
		String type=null,fileName="",containerName=null,namn=null,selectionField=null,selectionPattern=null,filter=null;
		parser.require(XmlPullParser.START_TAG, null,"block_create_list_entries");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}	
			String name= parser.getName();
			//If a unique varname tag found, instantiate a new XML_variable. 
			if (name.equals("file_name")) {
				fileName = readText("file_name",parser);
				o.addRow("FILE_NAME: "+fileName);
			} else if (name.equals("container_name")) {
				containerName = readText("container_name",parser);
				o.addRow("CONTAINER_NAME: "+containerName);
			} else if (name.equals("name")) {
				namn = readText("name",parser);
				o.addRow("NAME: "+namn);
			} else if (name.equals("type")) {
				type = readText("type",parser);
				o.addRow("TYPE: "+type);
			}  else if (name.equals("selection_pattern")) {
				selectionPattern = readText("selection_pattern",parser);
				o.addRow("SELECTION_PATTERN: "+selectionPattern);
			} else if (name.equals("selection_field")) {
				selectionField = readText("selection_field",parser);
				o.addRow("SELECTION_FIELD: "+selectionField);
			} else if (name.equals("filter")) {
				filter = readText("filter",parser);
				o.addRow("FILTER: "+filter);
			} else if (name.equals("is_visible")) {
				isVisible = !readText("is_visible",parser).equals("false");
				o.addRow("IS_VISIBLE: "+isVisible);	
			} 			
			else
				skip(name,parser);


		}

		return new CreateListEntriesBlock(type,fileName,containerName,namn,selectionField,selectionPattern,filter,isVisible);
	}
	 */


	/**
	 *  Creates a Buttonblock. 
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	//For now just create dummy.
	private ButtonBlock readBlockButton(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addRow("Parsing block: block_button...");
		String label=null,onClick=null,myname=null,containerName=null,
				target=null,type=null,id=null,statusVariable=null;
		boolean isVisible = true;
		parser.require(XmlPullParser.START_TAG, null,"block_button");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}	
			String name = parser.getName();
			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			} else if (name.equals("onClick")) {
				onClick = readText("onClick",parser);
			}
			else if (name.equals("type")) {
				type = readText("type",parser);
			}
			else if (name.equals("name")) {
				myname = readText("name",parser);
			}
			else if (name.equals("label")) {
				label = readText("label",parser);
			}
			else if (name.equals("container_name")) {
				containerName = readText("container_name",parser);		
			}
			else if (name.equals("target")) {
				target = readText("target",parser);
			}
			else if (name.equals("is_visible")) {
				isVisible = !readText("is_visible",parser).equals("false");
			} 
			else if (name.equals("status_variable"))
				statusVariable = readText("status_variable",parser);
			else
				skip(name,parser);
		}
		checkForNull("block_ID",id,"type",type,"name",myname,"label",label,"container_name",
				containerName,"target",target);
		return new ButtonBlock(id,label,onClick,myname,containerName,target,type,statusVariable,isVisible);
	}

	/**
	 *  Creates a Startblock. 
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	//Block Start contains the name of the worklfow and the Arguments.
	private StartBlock readBlockStart(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addRow("Parsing block: block_start...");
		String workflowName=null; String args[]=null,context=null,id=null;
		parser.require(XmlPullParser.START_TAG, null,"block_start");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name= parser.getName();
			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			} else if (name.equals("workflowname"))  {
				workflowName = readSymbol("workflowname",parser);
				o.addRow("");
				o.addGreenText("Reading workflow: ["+workflowName+"]");
			}
			else if (name.equals("inputvar")) {
				args = readArray("inputvar",parser);
				o.addRow("input variables: ");
				for(int i=0;i<args.length;i++)
					o.addYellowText(args[i]+",");
			} 
			else if (name.equals("context")) {
				context = readText("context",parser);

			}
			else
				skip(name,parser);
		}
		if (workflowName == null)  {
			o.addRow("");
			o.addRedText("Error reading startblock. Workflowname missing");
			throw new XmlPullParserException("Parameter missing");
		}
		checkForNull("block_ID",id,"Context",context,"workflowname",workflowName);

		return new StartBlock(id,args,workflowName,context);
	}

	/*


	/**
	 * Creates a LayoutBlock. LayoutBlocks are used to set the direction of the layout 
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	private LayoutBlock readBlockLayout(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addRow("Parsing block: block_layout...");
		String layout=null,align=null,label=null,id=null;
		parser.require(XmlPullParser.START_TAG, null,"block_layout");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}		
			String name= parser.getName();
			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			} else if (name.equals("layout")) {
				layout = readText("layout",parser);

			} else if (name.equals("align")) {
				align = readText("align",parser);

			} else if (name.equals("label")) {
				label = readText("label",parser);


			} else 
				skip(name,parser);

		}
		checkForNull("block_ID",id,"layout",layout,"align",align,"label",label);
		return new LayoutBlock(id,label,layout,align);
	}

	/**
	 * Creates a PageDefinitionBlock. Pages are the templates for a given page. Defines layout etc. 
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	private PageDefineBlock readPageDefineBlock(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addRow("Parsing block: block_define_page...");
		String pageType=null,label="",id=null;
		parser.require(XmlPullParser.START_TAG, null,"block_define_page");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}		
			String name= parser.getName();
			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			} else if (name.equals("type")) {
				pageType = readText("type",parser);				
			} else if (name.equals("label")) {
				label = readText("label",parser);					
			} else
				skip(name,parser);
		}
		checkForNull("block_ID",id,"type",pageType,"label",label);
		return new PageDefineBlock(id,"root", pageType,label);
	}


	/**
	 * Creates a PageDefinitionBlock. Pages are the templates for a given page. Defines layout etc. 
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	private ContainerDefineBlock readContainerDefineBlock(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addRow("Parsing block: block_define_container...");
		String containerType=null,containerName="",id=null;
		parser.require(XmlPullParser.START_TAG, null,"block_define_container");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}		
			String name= parser.getName();
			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			} else if (name.equals("name")) {
				containerName = readText("name",parser);
			} else if (name.equals("type")) {
				containerType = readText("type",parser);
			} else
				skip(name,parser);
		}
		checkForNull("block_ID",id,"name",containerName,"container_type",containerType);
		return new ContainerDefineBlock(id,containerName, containerType);
	}
	/**
	 * Creates a AddRuleBlock. Adds a rule to a variable or object. 
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	private AddRuleBlock readBlockAddRule(XmlPullParser parser) throws IOException, XmlPullParserException {
		//o.addRow("Parsing block: block_add_rule...");
		String target=null, condition=null, action=null, errorMsg=null,myname=null,id=null;
		parser.require(XmlPullParser.START_TAG, null,"block_add_rule");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}		
			String name= parser.getName();
			if (name.equals("block_ID")) {
				id = readText("block_ID",parser);
			} else if (name.equals("target")) {
				target = readText("target",parser);
			} else if (name.equals("condition")) {
				condition = readText("condition",parser);
			} else if (name.equals("action")) {
				action = readText("action",parser);
			} else if (name.equals("errorMsg")) {
				errorMsg = readText("errorMsg",parser);
			} else if (name.equals("name")) {
				myname = readText("name",parser);
			} else 
				skip(name,parser);

		}
		checkForNull("block_ID",id,"name",myname,"target",target,"condition",condition,"action",action,"errorMsg",errorMsg);
		return new AddRuleBlock(id,myname,target,condition,action,errorMsg);
	}

	private void checkForNull(String...pars) {
		boolean nulls=false;
		boolean lbl = false;
		String lab=null;
		for (String par:pars) {
			lbl = !lbl;
			if (lbl) {
				lab = par;
				continue;
			} else if (par==null) {
				o.addRow("");
				o.addRedText("Parameter "+lab+" was NULL");
				nulls=true;
			}
		}/*
		if (!nulls) {
			o.addRow("");
			o.addGreenText("[OK]");
		}*/
	}



	// Read symbol from tag.
	private String readSymbol(String tag,XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null,tag);
		String text = readText(parser);
		parser.require(XmlPullParser.END_TAG, null,tag);
		//Check that it does not start with a number.
		if (text!=null) {
			if (text.length()>0 && Character.isDigit(text.charAt(0))) {
				o.addRow("");
				o.addRedText("XML: EXCEPTION - Symbol started with integer");
				throw new XmlPullParserException("Symbol cannot start with integer");	
			} 
		} else {
			o.addRow("");
			o.addRedText("XML: EXCEPTION - Symbol was NULL");
			throw new XmlPullParserException("Symbol cannot be null");
		}
		return text;
	}
	// Read string from tag.
	private static String readText(String tag,XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null,tag);
		String text = readText(parser);
		parser.require(XmlPullParser.END_TAG, null,tag);
		return text;
	}

	private static String[] readArray(String tag,XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null,tag);
		String temp = readText(parser);
		String[] res = null;
		if (temp!=null) 
			res = temp.split(",");			 

		parser.require(XmlPullParser.END_TAG, null,tag);
		return res;
	}

	// Extract string values.
	private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
		String result = "";
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			parser.nextTag();
		}
		return result;
	}
	//Skips entry...return one level up in recursion if end reached.
	private void skip(String name,XmlPullParser parser) throws XmlPullParserException, IOException {
		if (parser.getEventType() != XmlPullParser.START_TAG) {
			o.addRow("");
			o.addRedText("IllegalStateException while trying to read START_TAG");
			throw new IllegalStateException();

		}
		o.addRow("");
		o.addYellowText("Skipped TAG: ["+name+"]");
		int depth = 1;
		while (depth != 0) {
			switch (parser.next()) {
			case XmlPullParser.END_TAG:
				depth--;
				break;
			case XmlPullParser.START_TAG:
				depth++;
				break;
			}
		}
	}







}
