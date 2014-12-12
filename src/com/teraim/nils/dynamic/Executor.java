package com.teraim.nils.dynamic;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.teraim.nils.GlobalState;
import com.teraim.nils.R;
import com.teraim.nils.bluetooth.BluetoothConnectionService;
import com.teraim.nils.bluetooth.LinjeDone;
import com.teraim.nils.bluetooth.LinjeStarted;
import com.teraim.nils.dynamic.blocks.AddEntryToFieldListBlock;
import com.teraim.nils.dynamic.blocks.AddRuleBlock;
import com.teraim.nils.dynamic.blocks.AddSumOrCountBlock;
import com.teraim.nils.dynamic.blocks.AddVariableToEntryFieldBlock;
import com.teraim.nils.dynamic.blocks.AddVariableToEveryListEntryBlock;
import com.teraim.nils.dynamic.blocks.AddVariableToListEntry;
import com.teraim.nils.dynamic.blocks.Block;
import com.teraim.nils.dynamic.blocks.BlockCreateListEntriesFromFieldList;
import com.teraim.nils.dynamic.blocks.ButtonBlock;
import com.teraim.nils.dynamic.blocks.ConditionalContinuationBlock;
import com.teraim.nils.dynamic.blocks.ContainerDefineBlock;
import com.teraim.nils.dynamic.blocks.CreateEntryFieldBlock;
import com.teraim.nils.dynamic.blocks.CreateSortWidgetBlock;
import com.teraim.nils.dynamic.blocks.DisplayValueBlock;
import com.teraim.nils.dynamic.blocks.JumpBlock;
import com.teraim.nils.dynamic.blocks.SetValueBlock;
import com.teraim.nils.dynamic.blocks.SetValueBlock.ExecutionBehavior;
import com.teraim.nils.dynamic.blocks.StartBlock;
import com.teraim.nils.dynamic.templates.SimpleRutaTemplate;
import com.teraim.nils.dynamic.types.Rule;
import com.teraim.nils.dynamic.types.Variable;
import com.teraim.nils.dynamic.types.Variable.DataType;
import com.teraim.nils.dynamic.types.Workflow;
import com.teraim.nils.dynamic.workflow_abstracts.Container;
import com.teraim.nils.dynamic.workflow_abstracts.Event;
import com.teraim.nils.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.nils.dynamic.workflow_abstracts.EventListener;
import com.teraim.nils.dynamic.workflow_realizations.WF_Container;
import com.teraim.nils.dynamic.workflow_realizations.WF_Context;
import com.teraim.nils.dynamic.workflow_realizations.WF_Event;
import com.teraim.nils.dynamic.workflow_realizations.WF_Event_OnLinjeStatusChanged;
import com.teraim.nils.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.nils.dynamic.workflow_realizations.WF_Static_List;
import com.teraim.nils.log.LoggerI;
import com.teraim.nils.non_generics.Constants;
import com.teraim.nils.non_generics.NamedVariables;
import com.teraim.nils.non_generics.Start;
import com.teraim.nils.utils.RuleExecutor;

/*
 * Executes workflow blocks. Child classes define layouts and other specialized behavior
 */
public abstract class Executor extends Fragment {


	public static final String STOP_ID = "STOP";

	protected Workflow wf;

	//Extended context.
	protected WF_Context myContext;

	//Normal context
	protected Activity activity;
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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("nils","GETS TO ONCREATE EXECUTOR");
		activity = this.getActivity();
		gs = GlobalState.getInstance((Context)activity);
		myContext = new WF_Context((Context)activity,this,R.id.content_frame);
		al = gs.getArtLista();
		o = gs.getLogger();
		wf = getFlow();
		gs.setCurrentContext(myContext);
		ifi = new IntentFilter();
		ifi.addAction(BluetoothConnectionService.SYNK_DATA_RECEIVED);
		ifi.addAction(BluetoothConnectionService.LINJE_STARTED);
		ifi.addAction(BluetoothConnectionService.LINJE_DONE);
		ifi.addAction(BluetoothConnectionService.MASTER_CHANGED_MY_CONFIG);
		brr = new BroadcastReceiver() {
			@Override
			public void onReceive(Context ctx, Intent intent) {
				Log.d("nils","GETS HERE::::::");
				if (intent.getAction().equals(BluetoothConnectionService.SYNK_DATA_RECEIVED)) {
					gs.getArtLista().invalidateCache();	
					Log.d("nils","Cache invalidated");
					myContext.registerEvent(new WF_Event_OnSave(Constants.SYNC_ID));
					Log.d("nils","Reg event - onsave");
				} else if (intent.getAction().equals(BluetoothConnectionService.LINJE_STARTED)) {
					Log.d("nils","Linje started!");
					myContext.registerEvent(new WF_Event_OnLinjeStatusChanged(((LinjeStarted)gs.getOriginalMessage()).linjeId,true));
				} else if (intent.getAction().equals(BluetoothConnectionService.LINJE_DONE)) {
					Log.d("nils","Linje done!");
					myContext.registerEvent(new WF_Event_OnLinjeStatusChanged(((LinjeDone)gs.getOriginalMessage()).linjeId,false));
				} else if (intent.getAction().equals(BluetoothConnectionService.MASTER_CHANGED_MY_CONFIG)) {
					myContext.registerEvent(new WF_Event(EventType.onMasterChangedData,null,"Executor"));
				} 
			}
		};

	}



	@Override
	public void onResume() {
		activity.registerReceiver(brr, ifi);
		super.onResume();
	}

	@Override
	public void onPause()
	{
		Log.d("NILS", "In the onPause() event");
		//Stop listening for bluetooth events.
		activity.unregisterReceiver(brr);
		super.onPause();

	}

	protected Workflow getFlow() {
		Workflow wf=null;

		//Find out the name of the workflow to execute.
		Bundle b = this.getArguments();
		if (b!=null) {
			myContext.setStatusVariable(b.getString("status_variable"));
			String name = b.getString("workflow_name");
			if (name!=null && name.length()>0) 
				wf = gs.getWorkflow(name);

			if (wf==null&&name!=null&&name.length()>0) {
				o.addRow("");
				o.addYellowText("Workflow "+name+" NOT found!");
				return null;
			} else {
				o.addRow("*******EXECUTING: "+name);

			}
		}
		return wf;
	}


	/**
	 * Execute the workflow.
	 */
	protected void run() {
		try {
			visiVars = new HashSet<Variable>();
			//LinearLayout my_root = (LinearLayout) findViewById(R.id.myRoot);		
			List<Block>blocks = wf.getCopyOfBlocks();
			boolean notDone = true;
			int blockP = 0;
			Set<Variable>blockVars;
			boolean contextError = false;
			Variable missingVariable = null;
			String cContext = null;
			while(notDone) {
				Block b = blocks.get(blockP);
				if (b instanceof StartBlock) {
					o.addRow("");
					o.addYellowText("Startblock found "+b.getBlockId());
					StartBlock bl = (StartBlock)b;
					cContext = bl.getWorkFlowContext();
					if (cContext==null||cContext.isEmpty()) {
						Log.d("nils","No context!!");
						o.addRow("No context...will use existing");
					} else {
						Log.d("nils","Found context!!");
						String[] pairs = cContext.split(",");
						if (pairs==null||pairs.length==0) {
							o.addRow("Could not split context on comma (,). Syntax error?");
							contextError = true;
							break;
						} else {

							Map<String, String> keyHash = new HashMap<String, String>();
							Map<String, Variable> rawHash = new HashMap<String, Variable>();
							for (String pair:pairs) {
								Log.d("nils","found pair: "+pair);
								if (pair!=null&&!pair.isEmpty()) {
									String[] kv = pair.split("=");
									if (kv==null||kv.length<2) {
										o.addRow("");
										o.addRedText("Could not split context on comma (,). Syntax error?");
										contextError=true;
									} else {
										//Calculate value of context variables, if any.
										//is it a variable or a value?
										String arg = kv[0].trim();
										String val = kv[1].trim();
										Log.d("nils","Keypair: "+arg+","+val);

										if (val.isEmpty()||arg.isEmpty()) {
											o.addRow("");
											o.addRedText("Empty variable or argument in definition");
											contextError=true;
										} else {

											if (Character.isDigit(val.charAt(0))) {
												//constant
												keyHash.put(arg, val);
												Log.d("nils","Added "+arg+","+val+" to current context");
											} 
											else {
												//Variable. need to evaluate first..
												Variable v = gs.getArtLista().getVariableInstance(val);
												if (v==null) {
													contextError=true;
													o.addRow("");
													o.addRedText("One of the variables missing: "+val);
													Log.d("nils","Couldn't find variable "+val);
												} else {
													String varVal = v.getValue();
													if(varVal==null||varVal.isEmpty()) {
														contextError=true;
														missingVariable = v;
														o.addRow("");
														o.addRedText("One of the variables used in current context("+v.getId()+") has no value in database");
														Log.e("nils","var was null or empty: "+v.getId());
													} else {

														keyHash.put(arg, varVal);
														rawHash.put(arg,v);
														Log.d("nils","Added "+arg+","+varVal+" to current context");
														v.setKeyChainVariable(arg);
													}

												}

											}
										}
									}
								} else
									Log.d("nils","Found empty or null pair");
							} if (!contextError && !keyHash.isEmpty()) {
								Log.d("nils","added keyhash to gs");
								//This destroys the cache. Need to add back the keychain variables.
								gs.setKeyHash(keyHash);
								for (Variable v:rawHash.values()) 
									al.addToCache(v);
								//Keep this if the hash values change.
								gs.setRawHash(rawHash);
							} else {
								break;
							}

						}
					}
				}

				else if (b instanceof ContainerDefineBlock) {
					o.addRow("");
					o.addYellowText("ContainerDefineBlock found "+b.getBlockId());
					String id = (((ContainerDefineBlock) b).getContainerName());
					if (id!=null) {
						if (myContext.getContainer(id)!=null) {
							o.addRow("");
							o.addGreenText("found hardcoded templatecontainer for "+id);
						}
						else {
							o.addRow("");
							o.addRedText("Could not find container "+id+" in template! Will default to root");
						}

					}
				}			
				else if (b instanceof ButtonBlock) {
					o.addRow("");
					o.addYellowText("ButtonBlock found "+b.getBlockId());
					ButtonBlock bl = (ButtonBlock) b;
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

				else if (b instanceof SetValueBlock) {
					o.addRow("");
					o.addYellowText("Running SetValueBlock "+b.getBlockId());
					final SetValueBlock bl = (SetValueBlock)b;
					final Set<Entry<String, DataType>> vars = RuleExecutor.getInstance(gs.getContext()).parseFormula(bl.getFormula(),null);
					if (bl.getBehavior()!=ExecutionBehavior.constant) {
						EventListener tiva = new EventListener() {
							@Override
							public void onEvent(Event e) {
								if (!e.getProvider().equals(bl.getBlockId())) {
									String eval = bl.evaluate(gs,bl.getFormula(),vars);
									Variable v = al.getVariableInstance(bl.getMyVariable());
									if (v!=null) {
										String val = v.getValue();
										o.addRow("Value: "+val+" Eval: "+eval);
										if (!(eval == null && val == null)) {
											if (eval == null && val != null || val == null && eval != null || !val.equals(eval)) {
												v.setValue(eval);
												o.addRow("");
												o.addYellowText("Value has changed to or from null in setvalueblock OnSave for block "+bl.getBlockId());
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
									}
								} else
									o.addRow("Discarded onSave Event from myself in SetValueBlock "+bl.getBlockId());
							}
						};
						Log.d("nils","Adding eventlistener for the setvalue block");
						myContext.addEventListener(tiva, EventType.onSave);	
					}
					//Evaluate
					String eval = bl.evaluate(gs,bl.getFormula(),vars);
					if (eval==null) {
						o.addRow("");
						o.addRow("Execution stopped on SetValueBlock "+bl.getBlockId()+". Expression "+bl.getFormula()+"evaluates to null");
						//jump.put(bl.getBlockId(), Executor.STOP_ID);
						notDone = false;
					} else 
						o.addRow("SetValueBlock "+bl.getBlockId()+" eval result: "+eval);					
					Variable v = al.getVariableInstance(bl.getMyVariable());
					if (v!=null) {
						String val = v.getValue();
						if ((val == null && eval == null)||
								(val != null && eval != null && val.equals(eval))) {
							o.addRow("No change in Setvalueblock"+bl.getBlockId()+": Value: "+val+" Eval: "+eval);
						}
						else {	
							v.setValue(eval);
							o.addRow("Change! "+bl.getFormula()+"evaluates to "+eval);
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
					final Set<Entry<String, DataType>> vars = RuleExecutor.getInstance(gs.getContext()).parseFormula(formula,null);
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

					((AddRuleBlock) b).create(myContext);

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

				if (blockP>=blocks.size())
					notDone=false;
			}
			if (!contextError) {
				//Now all blocks are executed.
				//Draw the UI.
				o.addRow("");
				o.addYellowText("Now Drawing components recursively");
				//Draw all lists first.
				for (WF_Static_List l:myContext.getLists()) 			
					l.draw();
				//Trgger redraw event on lists.
				//myContext.registerEvent(new WF_Event_OnSave("fackabuudle"));
				Container root = myContext.getContainer("root");
				if (root!=null)
					myContext.drawRecursively(root);
				else {
					o.addRow("");
					o.addRedText("TEMPLATE ERROR: Cannot find the root container. \nEach template must have a root! Execution aborted.");				
				}
			} else {
				String dialogText = "Du kan inte köra flödet just nu pga fel i kontext: ["+cContext+"]";
				if (missingVariable!=null) 
					dialogText = "Du kan inte köra flödet just nu eftersom kontextvariabeln "+missingVariable.getId()+" saknar ett värde. Det kan hända om du kör flödet från genvägsmenyn.";
				new AlertDialog.Builder(getActivity())
				.setTitle("Kontext problem")
				.setMessage(dialogText) 
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setCancelable(false)
				.setNeutralButton("Okej!",new OnClickListener() {				
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						
					}
				} )
				.show();
			}

		} catch (Exception e) {
			o.addRow("");
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);		
			o.addRedText(sw.toString());
			e.printStackTrace();
		}
	}



	private int indexOf(String jNext, List<Block> blocks) {

		for(int i=0;i<blocks.size();i++) {
			String id = blocks.get(i).getBlockId();
			//			Log.d("nils","checking id: "+id);
			if(id.equals(jNext)) {
				Log.d("nils","found block to jump to!");
				return i;
			}
		}

		Log.e("nils","Jump pointer to non-existing block. Faulty ID: "+jNext);
		return blocks.size();
	}

	public Set<Variable> getVariables() {
		return visiVars;
	}

}