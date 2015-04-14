package com.teraim.vortex.dynamic.blocks;

import java.util.Map;
import java.util.Set;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TableLayout.LayoutParams;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.R;
import com.teraim.vortex.Start;
import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.dynamic.types.CHash;
import com.teraim.vortex.dynamic.types.Rule;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.types.Workflow;
import com.teraim.vortex.dynamic.workflow_abstracts.Container;
import com.teraim.vortex.dynamic.workflow_abstracts.Drawable;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Context;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.vortex.dynamic.workflow_realizations.WF_Widget;
import com.teraim.vortex.expr.SyntaxException;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.utils.Exporter;
import com.teraim.vortex.utils.Exporter.ExportReport;
import com.teraim.vortex.utils.Exporter.Report;
import com.teraim.vortex.utils.PersistenceHelper;


/**
 * buttonblock
 * 
 * name is ID for now..
 * 
 * @author Terje
 *
 */
public  class ButtonBlock extends Block {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6454431627090793558L;
	String text,onClick,name,containerId,target;
	private Boolean validationResult = true;
	Type type;
	Variable statusVariable=null;
	private android.graphics.drawable.Drawable originalBackground;
	private Button button;

	WF_Context myContext;
	private boolean isVisible;
	private String statusVar=null;
	private OnclickExtra extraActionOnClick=null;
	private GlobalState gs;
	private Map<String, String> buttonContext;
	private PopupWindow mpopup=null;
	private Map<String,String> exportContext;
	private String exportContextS;
	private String exportFormat;
	private String exportFileName = null;


	enum Type {
		action,
		toggle
	}

	enum Status {
		none,
		started,
		started_with_errors,
		ready
	}

	//Function used with buttons that need to attach customized actions after click
	public ButtonBlock(String id,String lbl,String action, String name,String container,String target, String type, String statusVariable,boolean isVisible,
			OnclickExtra onclickExtra,Map<String,String> buttonContext, int dummy) {		
		this(id,lbl,action,name,container,target,type,statusVariable,isVisible,null,null);
		extraActionOnClick = onclickExtra;
		this.buttonContext=buttonContext;
	}
	public ButtonBlock(String id,String lbl,String action, String name,String container,String target, String type, String statusVariable,boolean isVisible,String exportContextS, String exportFormat) {
		Log.d("NILS","BUTTONBLOCK type Action. Action is set to "+action);
		this.blockId=id;
		this.text = lbl;
		this.onClick=action;
		this.name=name;
		this.containerId = container;
		this.target=target;
		this.type=type.equals("toggle")?Type.toggle:Type.action;
		this.isVisible = isVisible;
		this.statusVar = statusVariable;
		this.buttonContext=null;
		//Set null
		if (statusVar!=null&&statusVar.length()==0)
			this.statusVar=null;
		this.exportContextS = exportContextS;
		this.exportFormat = exportFormat;

	}


	public String getText() {
		return text;
	}


	public String getName() {
		return name;
	}
	public String getTarget() {
		return target;
	}


	public WF_Widget create(final WF_Context myContext) {
		gs = GlobalState.getInstance();
		o=gs.getLogger();
		Log.d("nils","In CREATE for BUTTON "+getText());
		Container myContainer = myContext.getContainer(containerId);
		if (myContainer!=null) {
			final Context ctx = myContext.getContext();
			

			if (extraActionOnClick==null) {
				Log.d("nils","Buttoncontext set to: "+gs.getCurrentKeyHash()+" for button: "+getText());
				buttonContext = gs.getCurrentKeyHash();
			}
			WF_Widget misu = null;
			if (type == Type.action) {
				LayoutInflater inflater = (LayoutInflater)ctx.getSystemService
						(Context.LAYOUT_INFLATER_SERVICE);

				o.addRow("Creating Action Button.");
				int layoutId = R.layout.button_normal;

				if (statusVar != null) {
					VariableConfiguration al = gs.getVariableConfiguration();
					Variable statusVariable = al.getVariableUsingKey(buttonContext,statusVar);
					if (statusVariable!=null) {
						Log.e("nils","STATUSVAR: "+statusVariable.getId()+" key: "+statusVariable.getKeyChain()+ "Value: "+statusVariable.getValue());
						String valS = statusVariable.getValue();
						Integer val=null;
						if (valS == null)
							val = 0;
						else 
							try {val=Integer.parseInt(valS);} 
						catch (NumberFormatException e){val = 0;};

						switch (val) {

						case 0:
							layoutId = R.layout.button_status_none;
							break;

						case 1:
							layoutId = R.layout.button_status_started;
							break;

						case 2:
							layoutId = R.layout.button_status_started_with_errors;
							break;

						case 3:
							layoutId = R.layout.button_status_ready;
							break;

						}
					} else {
						o.addRow("");
						o.addRedText("Statusvariable ["+statusVar+"], buttonblock "+blockId+" does not exist. Will use normal button");
					}
				}

				button = (Button)inflater.inflate(layoutId,null);


				//button.setBackgroundDrawable(ctx.getResources().getDrawable(R.drawable.button_bg_selector));
				//button.setTextAppearance(ctx, R.style.WF_Text);
				Log.d("nils","BUTTON TEXT:"+getText());
				button.setText(getText());

				button.setOnClickListener(new View.OnClickListener() {
					boolean clickOngoing = false;

					@Override
					public void onClick(View arg0) {
						if (clickOngoing)
							return;
						else
							clickOngoing=true;
						originalBackground = button.getBackground();					
						button.setBackgroundColor(Color.parseColor(Constants.Color_Pressed));
						//ACtion = workflow to execute.
						//Commence!
						if (extraActionOnClick!=null) {
							extraActionOnClick.onClick();
						}

						if (onClick.startsWith("template"))
							myContext.getTemplate().execute(onClick,target);
						else {


							if (onClick.equals("Go_Back")) {

								String statusVar = myContext.getStatusVariable();
								if (statusVar != null) {

									VariableConfiguration al = gs.getVariableConfiguration();
									Log.d("nils","My button context is: "+buttonContext.toString());
									statusVariable = al.getVariableUsingKey(buttonContext,statusVar);
								} else
									statusVariable = null;
								Set<Rule> myRules = myContext.getRules(name);
								boolean showPop=false;
								LayoutInflater inflater = (LayoutInflater)ctx.getSystemService
										(Context.LAYOUT_INFLATER_SERVICE);
								View popUpView = null; // inflating popup layout

								if (myRules != null && myRules.size()>0) {
									Log.d("nils","I have "+myRules.size()+" rules!: "+myRules.toString());
									validationResult  = null;
									//We have rules. Each rule adds a line in the popup.
									popUpView = inflater.inflate(R.layout.rules_popup, null);
									mpopup = new PopupWindow(popUpView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, true); //Creation of popup
									mpopup.setAnimationStyle(android.R.style.Animation_Dialog);   
									LinearLayout frame = (LinearLayout)popUpView.findViewById(R.id.pop);
									Button avsluta = (Button)popUpView.findViewById(R.id.avsluta);
									Button korrigera = (Button)popUpView.findViewById(R.id.korrigera);
									avsluta.setOnClickListener(new OnClickListener() {

										@Override
										public void onClick(View v) {
											if (statusVariable!=null) {
												statusVariable.setValue(validationResult?"3":"2");
												Log.e("nils","SETTING STATUSVAR: "+statusVariable.getId()+" key: "+statusVariable.getKeyChain()+ "Value: "+statusVariable.getValue());
												//Save value of all variables to database in current flow.

											}


											else
												Log.d("nils","Found no status variable");

											Set<Variable> variablesToSave = myContext.getTemplate().getVariables();
											Log.d("nils", "Variables To save contains "+variablesToSave==null?"null":variablesToSave.size()+" objects.");
											for (Variable var:variablesToSave) {
												Log.d("nils","Saving "+var.getLabel());
												var.setValue(var.getValue());
											}
											myContext.registerEvent(new WF_Event_OnSave(ButtonBlock.this.getBlockId()));
											mpopup.dismiss();
											myContext.getActivity().getFragmentManager().popBackStackImmediate();

										}
									});

									korrigera.setOnClickListener(new OnClickListener() {

										@Override
										public void onClick(View v) {
											mpopup.dismiss();
										}
									});
									LinearLayout row;
									TextView header,body;
									ImageView indicator;
									//Assume correct.
									validationResult = true;
									boolean isDeveloper = gs.getGlobalPreferences().getB(PersistenceHelper.DEVELOPER_SWITCH);

									for (Rule r:myRules) {
										boolean ok=false;
										try {
											ok = r.execute();								
										} catch (SyntaxException e) {
											o.addRow("");
											o.addRedText("Rule "+r.id+" has syntax errors in condition: "+r.condition);										
										}
										Rule.Type type = r.getType();
										int indicatorId=0;
										boolean bok = false;
										if (ok) {
											indicatorId = R.drawable.btn_icon_ready;
											bok=true;
										}
										else
											if (type == Rule.Type.ERROR) {
												indicatorId = R.drawable.btn_icon_started_with_errors;
												bok = false;
											}
											else {
												indicatorId = R.drawable.btn_icon_started;
												bok = true;
											}
										if (!bok)
											validationResult = false;
										if (!ok || ok && isDeveloper) {
											showPop=true;
											row = (LinearLayout)inflater.inflate(R.layout.rule_row, null);
											header = (TextView)row.findViewById(R.id.header);
											body = (TextView)row.findViewById(R.id.body);
											indicator = (ImageView)row.findViewById(R.id.indicator);
											indicator.setImageResource(indicatorId);
											Log.d("nils"," Rule header "+r.getRuleHeader()+" rule body: "+r.getRuleText());
											header.setText(r.getRuleHeader());
											body.setText(r.getRuleText());
											frame.addView(row);
										}
									}

								} 

								if (showPop)
									mpopup.showAtLocation(popUpView, Gravity.TOP, 0, 0);    // Displaying popup
								else {
									//no rules? Then validation is always ok. 
									Log.d("nils","No rules found - exiting");
									if (statusVariable!=null) {
										statusVariable.setValue("3");
										//myContext.registerEvent(new WF_Event_OnSave(ButtonBlock.this.getBlockId()));

									}
									else
										Log.d("nils","Found no status variable");
									Set<Variable> variablesToSave = myContext.getTemplate().getVariables();
									Log.d("nils", "Variables To save contains "+variablesToSave==null?"null":variablesToSave.size()+" objects.");
									for (Variable var:variablesToSave) {
										Log.d("nils","Saving "+var.getLabel());
										var.setValue(var.getValue());
									}
									myContext.getActivity().getFragmentManager().popBackStackImmediate();
								}
							}
							else if (onClick.equals("Start_Workflow")) {

								Workflow wf = gs.getWorkflow(target);
								if (wf == null) {
									Log.e("NILS","Cannot find wf ["+target+"] referenced by button "+getName());

								} else {
									o.addRow("");
									o.addRow("Action button pressed. Executing wf: "+target);
									//save all changes
									if (statusVar!=null) {
										VariableConfiguration al = gs.getVariableConfiguration();
										statusVariable = al.getVariableUsingKey(buttonContext,statusVar);
										if (statusVariable == null) {
											o.addRow("");
											o.addRedText("Statusvariabel "+statusVar+" saknas på knapp "+name);
										} else {
											String valS = statusVariable.getValue();
											if (valS==null || valS.equals("0"))
												statusVariable.setValue("1");
											myContext.registerEvent(new WF_Event_OnSave(ButtonBlock.this.getBlockId()));
										}
									}
									Start.singleton.changePage(wf,statusVar);
									//final FragmentTransaction ft = myContext.getActivity().getFragmentManager().beginTransaction(); 
									//ft.replace(myContext.getRootContainer(), f);
									//ft.addToBackStack(null);
									//ft.commit(); 
									//Validation?
								}

							} else if (onClick.equals("export")) {
								Log.d("vortex","Export button clicked!");
								CHash r = gs.evaluateContext(exportContextS);
								//Context ok?
								String msg, btnText;
								if (r.err==null) {
									exportFileName = gs.getGlobalPreferences().get(PersistenceHelper.BUNDLE_NAME)+"_";
									if (target!=null)
										exportFileName += target+"_";
									exportFileName+=Constants.getTimeStamp();

									if (exportFormat  == null) 
										exportFormat = "csv";
									exportFormat = exportFormat.toLowerCase();
									Report jRep = gs.getDb().export(exportContext, Exporter.getInstance(ctx, exportFormat), exportFileName);
									if (jRep.er == ExportReport.OK) {
										msg = jRep.noOfVars+" variables exported to file: "+exportFileName+"."+exportFormat+"\n";
										msg+= "You can find this file under "+Constants.EXPORT_FILES_DIR+" on your device";
										btnText = "Ok, thanks";
									} else {
										if (jRep.er==ExportReport.NO_DATA)
											msg = "Nothing to export! Have you entered any values? Have you marked your export variables as 'global'? (Local variables are not exported)";
										else
											msg = "Export failed. Reason: "+jRep.er.name();
										btnText = "Ok";
									}
									//Context was broken
								} else {
									msg = "Export failed. export_context contain errors. Error: "+r.err;
									btnText= "Ok";
								}
								new AlertDialog.Builder(ctx)
								.setTitle("Export done")
								.setMessage(msg)
								.setPositiveButton(btnText, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) { 								
									}
								})			    
								.setIcon(android.R.drawable.ic_dialog_alert)
								.show();
							}
							else {
								o.addRow("");
								o.addRedText("Action button had no associated action!");
							}
						}

						clickOngoing = false;
						button.setBackgroundDrawable(originalBackground);
					}

				});
				misu = new WF_Widget(text,button,isVisible,myContext);
				myContainer.add(misu);
			} else if (type == Type.toggle) {
				o.addRow("Creating Toggle Button with text: "+text);
				ToggleButton toggleB = (ToggleButton)LayoutInflater.from(ctx).inflate(R.layout.toggle_button,null);
				//ToggleButton toggleB = new ToggleButton(ctx);
				toggleB.setTextOn(text);
				toggleB.setTextOff(text);
				toggleB.setChecked(false);
				LayoutParams params = new LayoutParams();
				params.width = LayoutParams.MATCH_PARENT;
				params.height = LayoutParams.WRAP_CONTENT;
				toggleB.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
				toggleB.setLayoutParams(params);

				toggleB.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						if(onClick==null||onClick.trim().length()==0) {
							o.addRow("");
							o.addRedText("Button "+text+" has no onClick action!");
							Log.e("nils","Button clicked ("+text+") but found no action");
						} else {

							o.addRow("Togglebutton "+text+" pressed. Executing function "+onClick);
							if (onClick.startsWith("template")) 
								myContext.getTemplate().execute(onClick,target);	
							else if (onClick.equals("toggle_visible")) {
								Log.d("nils","Executing toggle");
								Drawable d = myContext.getDrawable(target);
								if (d!=null) {
									if(d.isVisible())
										d.hide();
									else
										d.show();
								} else {
									Log.e("nils","Couldn't find target "+target+" for button");
									o.addRow("");
									o.addRedText("Target for button missing: "+target);
								}

							}
						}
					}
				});
				misu = new WF_Widget(text,toggleB,isVisible,myContext);
				myContainer.add(misu);
			}
			return misu;
		} else {
			o.addRow("");
			o.addRedText("Failed to add text field block with id "+blockId+" - missing container "+myContainer);
			return null;
		}
	}
}