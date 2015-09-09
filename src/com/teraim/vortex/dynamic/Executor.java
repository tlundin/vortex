package com.teraim.vortex.dynamic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.R;
import com.teraim.vortex.Start;
import com.teraim.vortex.dynamic.blocks.AddEntryToFieldListBlock;
import com.teraim.vortex.dynamic.blocks.AddGisFilter;
import com.teraim.vortex.dynamic.blocks.AddGisLayerBlock;
import com.teraim.vortex.dynamic.blocks.AddGisPointObjects;
import com.teraim.vortex.dynamic.blocks.AddRuleBlock;
import com.teraim.vortex.dynamic.blocks.AddSumOrCountBlock;
import com.teraim.vortex.dynamic.blocks.AddVariableToEntryFieldBlock;
import com.teraim.vortex.dynamic.blocks.AddVariableToEveryListEntryBlock;
import com.teraim.vortex.dynamic.blocks.AddVariableToListEntry;
import com.teraim.vortex.dynamic.blocks.Block;
import com.teraim.vortex.dynamic.blocks.BlockAddColumnsToTable;
import com.teraim.vortex.dynamic.blocks.BlockAddVariableToTable;
import com.teraim.vortex.dynamic.blocks.BlockCreateListEntriesFromFieldList;
import com.teraim.vortex.dynamic.blocks.BlockCreateTableEntriesFromFieldList;
import com.teraim.vortex.dynamic.blocks.ButtonBlock;
import com.teraim.vortex.dynamic.blocks.ConditionalContinuationBlock;
import com.teraim.vortex.dynamic.blocks.ContainerDefineBlock;
import com.teraim.vortex.dynamic.blocks.CreateEntryFieldBlock;
import com.teraim.vortex.dynamic.blocks.CreateGisBlock;
import com.teraim.vortex.dynamic.blocks.CreateImageBlock;
import com.teraim.vortex.dynamic.blocks.CreateSortWidgetBlock;
import com.teraim.vortex.dynamic.blocks.DisplayValueBlock;
import com.teraim.vortex.dynamic.blocks.JumpBlock;
import com.teraim.vortex.dynamic.blocks.MenuEntryBlock;
import com.teraim.vortex.dynamic.blocks.MenuHeaderBlock;
import com.teraim.vortex.dynamic.blocks.PageDefineBlock;
import com.teraim.vortex.dynamic.blocks.RoundChartBlock;
import com.teraim.vortex.dynamic.blocks.SetValueBlock;
import com.teraim.vortex.dynamic.blocks.SetValueBlock.ExecutionBehavior;
import com.teraim.vortex.dynamic.blocks.TextFieldBlock;
import com.teraim.vortex.dynamic.blocks.VarValueSourceBlock;
import com.teraim.vortex.dynamic.types.Rule;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.types.Variable.DataType;
import com.teraim.vortex.dynamic.types.Workflow;
import com.teraim.vortex.dynamic.workflow_abstracts.Container;
import com.teraim.vortex.dynamic.workflow_abstracts.Event;
import com.teraim.vortex.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.vortex.dynamic.workflow_abstracts.EventListener;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Container;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Static_List;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Table;
import com.teraim.vortex.dynamic.workflow_realizations.gis.WF_Gis_Map;
import com.teraim.vortex.gis.Tracker;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.ui.MenuActivity;
import com.teraim.vortex.utils.RuleExecutor.TokenizedItem;
import com.teraim.vortex.utils.Tools;

/**
 * Executor - executes workflow blocks.  
 * Copyright Teraim 2015.
 * Core class in the Vortex Engine.
 * Redistribution and changes only after agreement with Teraim.
 */


public abstract class Executor extends Fragment implements AsyncResumeExecutorI {


	public static final String STOP_ID = "STOP";

	public static final String REDRAW_PAGE = "executor_redraw_page";

	protected Workflow wf;

	//Extended context.
	protected WF_Context myContext;


	//Keep track of input in below arraylist.

	protected final Map<Rule,Boolean>executedRules = new LinkedHashMap<Rule,Boolean>();	

	protected List<Rule> rules = new ArrayList<Rule>();


	protected abstract List<WF_Container> getContainers();
	public abstract void execute(String function, String target);

	protected GlobalState gs;

	protected LoggerI o;
	private IntentFilter ifi;
	protected BroadcastReceiver brr;
	private Map<String,String> jump= new HashMap<String,String>();
	private Set<Variable> visiVars;

	protected VariableConfiguration al;

	private int savedBlockPointer=-1;

	private List<Block> blocks;

	private List<Integer> executedBlocks;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//If app has been murdered brutally, restart it. 
		if(!Start.alive) {
			Intent intent = new Intent(this.getActivity(), Start.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
		} else {
		Log.d("nils","GETS TO ONCREATE EXECUTOR");
		
		gs = GlobalState.getInstance();
		if (gs == null) {
			Log.e("vortex","globalstate null, exit");
			return;
		}
		myContext = new WF_Context((Context)this.getActivity(),this,R.id.content_frame);
		o = gs.getLogger();
		wf = getFlow();
		myContext.setWorkflow(wf);
		al = gs.getVariableConfiguration();

		ifi = new IntentFilter();
		ifi.addAction(REDRAW_PAGE);
		//ifi.addAction(BluetoothConnectionService.BLUETOOTH_MESSAGE_RECEIVED);
		//This receiver will forward events to the current context.
		//Bluetoothmessages are saved in the global context by the message handler.
		brr = new BroadcastReceiver() {
			@Override
			public void onReceive(Context ctx, Intent intent) {
				Log.d("nils","GETS HERE::::::");
				if (intent.getAction().equals(REDRAW_PAGE)) {

					myContext.registerEvent(new WF_Event_OnSave(Constants.SYNC_ID));
					Log.d("nils","Redraw page received in Executor. Sending onSave event.");
				} 
				/*
				else if (intent.getAction().equals(BluetoothConnectionService.BLUETOOTH_MESSAGE_RECEIVED)) {
					Log.d("nils","New bluetoot message received event!");
					myContext.registerEvent(new WF_Event_OnBluetoothMessageReceived());
				}
				*/
				
			}
		};
		}
	}



	@Override
	public void onResume() {
		Log.d("vortex","in Executor onResume");
		
		gs = GlobalState.getInstance();
		if (gs!=null) {
			this.getActivity().registerReceiver(brr, ifi);
			if(myContext!=null&&myContext.hasGPSTracker())
				gs.getTracker().startScan(gs.getContext());
			
		}
		super.onResume();
	}

	
	@Override
	public void onPause()
	{
		Log.d("NILS", "In the onPause() event");
		//Stop listening for bluetooth events.
		if (brr!=null)
			this.getActivity().unregisterReceiver(brr);
		if (gs!=null&&myContext!=null&&myContext.hasGPSTracker())
			gs.getTracker().stopUsingGPS();
		super.onPause();

	}

	protected Workflow getFlow() {
		Workflow wf=null;

		//Find out the name of the workflow to execute.
		Bundle b = this.getArguments();
		if (b!=null) {
			String name = b.getString("workflow_name");
			
			myContext.setStatusVariable(b.getString("status_variable"));

			if (name!=null && name.length()>0) 
				wf = gs.getWorkflow(name);

			if (wf==null&&name!=null&&name.length()>0) {
				o.addRow("");
				o.addYellowText("Workflow "+name+" NOT found!");
				return null;
			} 
			
		}
		return wf;
	}


	/**
	 * Execute the workflow.
	 */
	protected void run() {
			o.addRow("");
			o.addRow("");
			o.addRow("*******EXECUTING: "+wf.getLabel());
			String wfContext = wf.getContext();
			myContext.setHash(gs.evaluateContext(wfContext));
			gs.setCurrentContext(myContext);		
			gs.setKeyHash(myContext.getKeyHash());
			gs.setRawHash(myContext.getRawHash());
			gs.sendEvent(MenuActivity.REDRAW);
			visiVars = new HashSet<Variable>();
			//LinearLayout my_root = (LinearLayout) findViewById(R.id.myRoot);		
			blocks = wf.getCopyOfBlocks();
			Log.d("vortex","*******EXECUTING: "+wf.getLabel());
			String mh=null;
			if (myContext.getKeyHash()!=null)
				mh = myContext.getKeyHash().toString();
			Log.d("vortex","myHash: "+mh);
			execute(0);
	}
	private void execute(int blockP) {
			
			boolean notDone = true;
			Set<Variable>blockVars;
			boolean hasDrawer=false;
			myContext.clearExecutedBlocks();
			try {
			
			while(notDone) {
				if (blockP>=blocks.size()) {
					notDone=false;
					break;
				}
				Block b = blocks.get(blockP);
				//Add block to list of executed blocks.
				try { myContext.addExecutedBlock(Integer.parseInt(b.getBlockId())); } catch (NumberFormatException e) {Log.e("vortex","blockId was not Integer");}
				
				if (b instanceof PageDefineBlock) {
					PageDefineBlock bl = (PageDefineBlock)b;
					Log.d("vortex","Found pagedefine!");
					if (bl.hasGPS()) {
						myContext.enableGPS();
						o.addRow("GPS scanning started");
						Log.d("vortex","GPS scanning started");
						
						Tracker.ErrorCode code = gs.getTracker().startScan(gs.getContext());
						o.addRow("GPS SCANNER RETURNS: "+code.name());
						Log.d("vortex","got "+code.name());
					} else
						Log.e("vortex","This has no GPS");
					
	
				}

				else if (b instanceof ContainerDefineBlock) {
					o.addRow("");
					o.addYellowText("ContainerDefineBlock found "+b.getBlockId());
					String id = (((ContainerDefineBlock) b).getContainerName());
					if (id!=null) {
						if (myContext.getContainer(id)!=null) {
							o.addRow("found template container for "+id);
						}
						else {
							o.addRow("");
							o.addRedText("Could not find container "+id+" in template!");
						}

					}
				}			
				else if (b instanceof ButtonBlock) {
					o.addRow("");
					o.addYellowText("ButtonBlock found "+b.getBlockId());
					ButtonBlock bl = (ButtonBlock) b;
					bl.create(myContext);
				}			
				else if (b instanceof TextFieldBlock) {
					o.addRow("");
					o.addYellowText("CreatTextBlock found "+b.getBlockId());
					TextFieldBlock bl = (TextFieldBlock) b;
					bl.create(myContext);
				}
				else if (b instanceof CreateSortWidgetBlock) {
					o.addRow("");
					o.addYellowText("CreateSortWidgetBlock found "+b.getBlockId());
					CreateSortWidgetBlock bl = (CreateSortWidgetBlock) b;
					bl.create(myContext);
				}/*
			else if (b instanceof ListFilterBlock) {
				o.addRow("");
				o.addYellowText("ListFilterBlock found");
				ListFilterBlock bl = (ListFilterBlock)b;
				bl.create(myContext);
			}*/

				else if (b instanceof CreateEntryFieldBlock) {
					o.addRow("");
					o.addYellowText("CreateEntryFieldBlock found "+b.getBlockId());
					CreateEntryFieldBlock bl = (CreateEntryFieldBlock)b;
					Log.d("NILS","CreateEntryFieldBlock found");
					Variable v=bl.create(myContext);
					if (v!=null)
						visiVars.add(v);
				}
				else if (b instanceof AddSumOrCountBlock) {
					o.addRow("");
					o.addYellowText("AddSumOrCountBlock found "+b.getBlockId());
					AddSumOrCountBlock bl = (AddSumOrCountBlock)b;
					bl.create(myContext);
				}
				else if (b instanceof DisplayValueBlock) {
					o.addRow("");
					o.addYellowText("DisplayValueBlock found "+b.getBlockId());
					DisplayValueBlock bl = (DisplayValueBlock)b;
					bl.create(myContext);
				}
				else if (b instanceof AddVariableToEveryListEntryBlock) {
					o.addRow("");
					o.addYellowText("AddVariableToEveryListEntryBlock found "+b.getBlockId());
					AddVariableToEveryListEntryBlock bl = (AddVariableToEveryListEntryBlock)b;
					blockVars = bl.create(myContext);
					if (blockVars!=null)
						visiVars.addAll(blockVars);

				}
				else if (b instanceof BlockCreateListEntriesFromFieldList) {
					o.addRow("");
					o.addYellowText("BlockCreateListEntriesFromFieldList found "+b.getBlockId());
					BlockCreateListEntriesFromFieldList bl = (BlockCreateListEntriesFromFieldList)b;
					bl.create(myContext);
				}
				else if (b instanceof BlockCreateTableEntriesFromFieldList) {
					o.addRow("");
					o.addYellowText("BlockCreateTableEntriesFromFieldList found "+b.getBlockId());
					BlockCreateTableEntriesFromFieldList bl = (BlockCreateTableEntriesFromFieldList)b;
					bl.create(myContext);
				}
				
				else if (b instanceof BlockAddColumnsToTable) {
					o.addRow("");
					o.addYellowText("BlockAddColumn(s)ToTable found "+b.getBlockId());
					BlockAddColumnsToTable bl = (BlockAddColumnsToTable)b;
					bl.create(myContext);
				}
				
				else if (b instanceof BlockAddVariableToTable) {
					o.addRow("");
					o.addYellowText("BlockAddVariableToTable(s)ToTable found "+b.getBlockId());
					BlockAddVariableToTable bl = (BlockAddVariableToTable)b;
					bl.create(myContext);
				}
				
				else if (b instanceof AddVariableToEntryFieldBlock) {
					o.addRow("");
					o.addYellowText("AddVariableToEntryFieldBlock found "+b.getBlockId());
					AddVariableToEntryFieldBlock bl = (AddVariableToEntryFieldBlock)b;
					Variable v = bl.create(myContext);
					if (v!=null)
						visiVars.add(v);

				}
				else if (b instanceof AddVariableToListEntry) {
					o.addRow("");
					o.addYellowText("AddVariableToListEntry found "+b.getBlockId());
					AddVariableToListEntry bl = (AddVariableToListEntry)b;
					Variable v = bl.create(myContext);
					//TODO: REMOVE THIS??
				}
				else if (b instanceof AddEntryToFieldListBlock) {
					o.addRow("");
					o.addYellowText("AddEntryToFieldListBlock found "+b.getBlockId());
					AddEntryToFieldListBlock bl = (AddEntryToFieldListBlock)b;
					bl.create(myContext);

				}
				else if (b instanceof JumpBlock) {
					o.addRow("");
					o.addYellowText("Jumpblock found "+b.getBlockId());
					JumpBlock bl = (JumpBlock)b;			
					jump.put(bl.getBlockId(), bl.getJumpTo());
				}
				else if (b instanceof CreateImageBlock) {
					o.addRow("");
					o.addYellowText("CreateImageBlock found "+b.getBlockId());
					CreateImageBlock bl = (CreateImageBlock)b;			
					bl.create(myContext);
				}
				else if (b instanceof SetValueBlock) {
					final SetValueBlock bl = (SetValueBlock)b;
					o.addRow("");
					o.addYellowText("SetValueBlock "+b.getBlockId()+" ["+bl.getMyVariable()+"]");
					o.addRow("Formula: "+bl.getFormula());
					final List<TokenizedItem> tokens = gs.getRuleExecutor().findTokens(bl.getFormula(),null);
					if (bl.getBehavior()!=ExecutionBehavior.constant) {
						EventListener tiva = new EventListener() {
							@Override
							public void onEvent(Event e) {
								if (!e.getProvider().equals(bl.getBlockId())) {
									Variable v = al.getVariableInstance(bl.getMyVariable());

									if (v!=null) {
										String	eval = bl.evaluate(gs,bl.getFormula(),tokens,v.getType()== DataType.text);
										String val = v.getValue();
										o.addRow("Variable: "+v.getId()+" Current val: "+val+" New val: "+eval);
										if (!(eval == null && val == null)) {
											if (eval == null && val != null || val == null && eval != null || !val.equals(eval)) {
												v.setValue(eval);
												o.addRow("");
												o.addYellowText("Value has changed to or from null in setvalueblock OnSave for block "+bl.getBlockId());
												o.addRow("");
												o.addYellowText("BEHAVIOR: "+bl.getBehavior());
												if (bl.getBehavior()==ExecutionBehavior.update_flow) {
													Log.d("nils","Variable has sideEffects...re-executing flow");
													new Handler().postDelayed(new Runnable() {
														public void run() {
															myContext.resetState();

															Set<Variable> previouslyVisibleVars = visiVars;
															Executor.this.run();
															for (Variable v:previouslyVisibleVars) {
																Log.d("nils","Previously visible: "+v.getId());
																boolean found = false;
																for(Variable x:visiVars) {									
																	found = x.getId().equals(v.getId());
																	if (found)
																		break;
																}

																if (!found) {
																	Log.d("nils","Variable "+v.getId()+" not found.Removing");
																	v.deleteValue();
																}

															}	
														}
													}, 0);
												}
											}
										}
									} else {
										Log.e("vortex","variable null in SetValueBlock");
										o.addRow("Setvalueblock variable "+bl.getMyVariable()+" not found or missing columns");
									}
										
								} else
									o.addRow("Discarded onSave Event from myself in SetValueBlock "+bl.getBlockId());
							}
						};
						Log.d("nils","Adding eventlistener for the setvalue block");
						myContext.addEventListener(tiva, EventType.onSave);	
					}
					//Evaluate
					Variable v = al.getVariableInstance(bl.getMyVariable());
					if (v!=null) {
						String eval = bl.evaluate(gs,bl.getFormula(),tokens,v.getType()==DataType.text);
						if (eval==null) {
							o.addRow("");
							o.addRow("Execution stopped on SetValueBlock "+bl.getBlockId()+". Expression "+bl.getFormula()+"evaluates to null");
							//jump.put(bl.getBlockId(), Executor.STOP_ID);
							notDone = false;
						} 				

						String val = v.getValue();
						if ((val == null && eval == null)||
								(val != null && eval != null && val.equals(eval))) {
							o.addRow("No change. Value: "+val+" Eval: "+eval);
						}
						else {	
							v.setValue(eval);
							o.addRow(bl.getFormula()+" Eval: ["+eval+"]");
							//Take care of any side effects before triggering redraw.
							myContext.registerEvent(new WF_Event_OnSave(bl.getBlockId()));
							o.addRow("Continues after onSave Event in block "+bl.getBlockId());
						}
					} else {
						o.addRow("");
						o.addRedText("Variable ["+bl.getMyVariable()+"] is missing in SetValueBlock "+bl.getBlockId());
					}






				}
				else if (b instanceof ConditionalContinuationBlock) {
					o.addRow("");
					o.addYellowText("ConditionalContinuationBlock found");
					final ConditionalContinuationBlock bl = (ConditionalContinuationBlock)b;
					final String formula = bl.getFormula();
					final List<TokenizedItem> vars = gs.getRuleExecutor().findTokens(formula,null);
					if (vars!=null) {
						EventListener tiva = new EventListener() {
							@Override
							public void onEvent(Event e) {
								//If evaluation different than earlier, re-render workflow.
								if(bl.evaluate(gs,formula,vars)) {
									//myContext.onResume();
									new Handler().postDelayed(new Runnable() {
										public void run() {
											myContext.resetState();
											Set<Variable> previouslyVisibleVars = visiVars;
											Executor.this.run();
											for (Variable v:previouslyVisibleVars) {
												Log.d("nils","Previously visible: "+v.getId());
												boolean found = false;
												for(Variable x:visiVars) {									
													found = x.getId().equals(v.getId());
													if (found) 
														break;
												}

												if (!found) {
													Log.d("nils","Variable "+v.getId()+" not found.Removing");
													v.deleteValue();
												}

											}	
										}
									}, 0);

								}

							}
						};		
						Log.d("nils","Adding eventlistener for the conditional block");
						myContext.addEventListener(tiva, EventType.onSave);	
						//trigger event.
						bl.evaluate(gs,formula,vars);

						switch (bl.getCurrentEval()) {
						case ConditionalContinuationBlock.STOP:
							jump.put(bl.getBlockId(), Executor.STOP_ID);
							break;
						case ConditionalContinuationBlock.JUMP:
							jump.put(bl.getBlockId(), bl.getElseId());						
							break;
						case ConditionalContinuationBlock.NEXT:
							jump.remove(bl.getBlockId());
						}

					}
					else {
						o.addRow("");
						o.addRedText("Parsing of formula failed - no variables: ["+formula+"]");
						Log.d("nils","Parsing of formula failed - no variables: ["+formula+"]");
					}
				}
				else if (b instanceof AddRuleBlock) {

					((AddRuleBlock) b).create(myContext,blocks);

				}
				else if (b instanceof MenuHeaderBlock) {
					((MenuHeaderBlock) b).create(myContext);
					hasDrawer=true;
				}
				
				else if (b instanceof MenuEntryBlock) {
					((MenuEntryBlock) b).create(myContext);
					hasDrawer=true;
				}
				else if (b instanceof RoundChartBlock) {
					((RoundChartBlock) b).create(myContext);
					
				}
				else if (b instanceof VarValueSourceBlock) {
					((VarValueSourceBlock) b).create(myContext);
					
				}
				else if (b instanceof CreateGisBlock) {
					savedBlockPointer = blockP+1;
					//Will callback to this object after image is loaded.
					CreateGisBlock bl = ((CreateGisBlock) b);
					if (bl.hasCarNavigation())
						myContext.enableSatNav();
					else
						Log.e("vortex","This has no SATNAV");
					if(!bl.create(myContext,this))
						//Pause execution and wait for callback..
						return;
				}
				
				else if (b instanceof AddGisLayerBlock) {
					((AddGisLayerBlock) b).create(myContext);
					
				}
				else if (b instanceof AddGisPointObjects) {
					((AddGisPointObjects) b).create(myContext);
				}
				
				else if (b instanceof AddGisFilter) {
					((AddGisFilter) b).create(myContext);
				}
				
				String cId = b.getBlockId();
				String jNext = jump.get(cId);
				if (jNext!=null) {	
					if (jNext.equals(Executor.STOP_ID))
						notDone = false;
					else
						blockP = indexOf(jNext,blocks);
				} else
					blockP++;


			}
			Container root = myContext.getContainer("root");
			if (root==null && myContext.hasContainers()) {
				o.addRow("");
				o.addRedText("TEMPLATE ERROR: Cannot find the root container. \nEach template must have a root! Execution aborted.");				
			} else {
				//Now all blocks are executed.
				//Draw the UI.
				o.addRow("");
				o.addYellowText("Now Drawing components recursively");
				//Draw all lists first.
				for (WF_Static_List l:myContext.getLists()) 			
					l.draw();
				for (WF_Table t:myContext.getTables())
					t.draw();
				WF_Gis_Map gis = myContext.getCurrentGis();
				if (gis!=null)
					gis.initialize();
				//Trgger redraw event on lists.
				//myContext.registerEvent(new WF_Event_OnSave("fackabuudle"));
				if (root!=null) 
					myContext.drawRecursively(root);
				//open menu if any

				if (hasDrawer) {
					Log.d("vortex","Drawing menu");
					gs.getDrawerMenu().openDrawer();
				}
	/*
					Object mDrawerLayout;
					if (hasDrawerMenu) {
						mDrawerLayout = myContext.getDrawerMenu();
						if (mDrawerLayout.isDrawerOpen(mDrawerList))
							mDrawerLayout.closeDrawers();
						mDrawerLayout.openDrawer(Gravity.LEFT);
						} else {
							mDrawerLayout.setEnabled(false);
							Log.d("vortex","NO drawer menu!");
						}
						
					}
			*/
				
				
			}

		} catch (Exception e) {
			Tools.printErrorToLog(o,e);
		}
	}



	private int indexOf(String jNext, List<Block> blocks) {

		for(int i=0;i<blocks.size();i++) {
			String id = blocks.get(i).getBlockId();
			//			Log.d("nils","checking id: "+id);
			if(id.equals(jNext)) {
				Log.d("vortex","Jumping to block "+jNext);
				o.addRow("Jumping to block "+jNext);
				return i;
			}
		}

		Log.e("nils","Jump pointer to non-existing block. Faulty ID: "+jNext);
		o.addRow("");
		o.addRedText("Jump pointer to non-existing block. Faulty ID: "+jNext);
		return blocks.size();
	}

	public Set<Variable> getVariables() {
		return visiVars;
	}

	
	@Override
	public void continueExecution() {
		if (savedBlockPointer!=-1)
			this.execute(savedBlockPointer);
		else
			Log.e("vortex","No saved block pointer...aborting execution");
	}

	@Override
	public void abortExecution(String reason) {
		Log.e("vortex","Execution aborted.");
		new AlertDialog.Builder(myContext.getContext())
		.setTitle("Execution aborted")
		.setMessage(reason) 
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setCancelable(false)
		.setNeutralButton("Ok",new Dialog.OnClickListener() {				
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub

			}
		} )
		.show();
	}
	
	public void restart() {
		new Handler().postDelayed(new Runnable() {
			public void run() {
				myContext.resetState();
				Executor.this.run();
				Log.d("vortex","workflow restarted");
			}
		}, 0);
	}
}