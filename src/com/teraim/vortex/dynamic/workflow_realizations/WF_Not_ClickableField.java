package com.teraim.vortex.dynamic.workflow_realizations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.graphics.Color;
import android.renderscript.Element.DataType;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.teraim.vortex.R;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.utils.CombinedRangeAndListFilter;

public abstract class WF_Not_ClickableField extends WF_ListEntry {
	protected WF_Context myContext;
	protected String myDescription;
	final LinearLayout outputContainer;
	protected Map<Variable,OutC> myOutputFields = new HashMap<Variable,OutC>();

	//Hack! Used to determine what is the master key for this type of element.
	//If DisplayOut & Virgin --> This is master key.
	boolean virgin=true;
	//Removed myVar 2.07.15
	//protected Variable myVar;
	public abstract LinearLayout getFieldLayout();



	//	public abstract String getFormattedText(Variable varId, String value);


	@Override
	public Set<Variable> getAssociatedVariables() {
		Set<Variable> s = new HashSet<Variable>();
		s.add(myVar);
		return s;
	}

	public class OutC {
		public OutC(LinearLayout ll, String f) {
			view = ll;
			format = f;
		}
		public OutC() {};
		public LinearLayout view;
		public String format;
	}

	public class OutSpin extends OutC {
		public String[] opt,val;

		public OutSpin(LinearLayout ll, String[] opt, String[] val) {
			this.view = ll;
			this.opt=opt;
			this.val=val;
		}

	}


	public WF_Not_ClickableField(final String label,final String descriptionT, WF_Context myContext, 
			View view,boolean isVisible) {
		super(label,view,myContext,isVisible);
		TextView myHeader;

		this.myContext = myContext;
		myHeader = (TextView)getWidget().findViewById(R.id.editfieldtext);
		outputContainer = (LinearLayout)getWidget().findViewById(R.id.outputContainer);
		//outputContainer.setLayoutParams(params);
		if (myHeader!=null)
			myHeader.setText(label);
		this.label = label;
		myDescription = descriptionT;


	}

	public void addVariable(Variable var, boolean displayOut, String format, boolean isVisible) {

		if (displayOut && virgin) {
			virgin = false;
			super.setKeyRow(var);
		}		
		if (displayOut) {
			LinearLayout ll = getFieldLayout();

			/*
			 TextView o = (TextView)ll.findViewById(R.id.outputValueField);
			TextView u = (TextView)ll.findViewById(R.id.outputUnitField);

			String value = Variable.getPrintedValue();
			if (!value.isEmpty()) {
				o.setText(varLabel+": "+value);	
				u.setText(" ("+Variable.getPrintedUnit()+")");
			}
			 */
			myOutputFields.put(var,new OutC(ll,format));
			outputContainer.addView(ll);
			myVar = var;
		}

	}


	public void refreshOutputField(Variable variable,OutC outC) {
		LinearLayout ll = outC.view;
		TextView o = (TextView)ll.findViewById(R.id.outputValueField);
		TextView u = (TextView)ll.findViewById(R.id.outputUnitField);			
		String value = variable.getValue();

		//Log.d("nils","In refreshoutputfield for variable "+varId.getId()+" with value "+varId.getValue());

		if (value!=null&&!value.isEmpty()) {
			CombinedRangeAndListFilter filter = variable.getLimitFilter();
			if (filter!=null)
				filter.testRun();

			if (variable.hasBrokenRules()||variable.hasValueOutOfRange()) {
				Log.d("nils","VARID: "+variable.getId()+" hasBroken: "+variable.hasBrokenRules()+" hasoutofRange: "+variable.hasValueOutOfRange());
				o.setTextColor(Color.RED);	
			} else {
				if (variable.isUsingDefault()) {
					Log.d("nils","Variable "+variable.getId()+" is purple");
					o.setTextColor(myContext.getContext().getResources().getColor(R.color.purple));
				} else
					o.setTextColor(Color.BLACK);
			}
			String outS="";

			if (variable.getType() != Variable.DataType.bool) {

				if (outC instanceof OutSpin) {
					outS = value;
					OutSpin os = ((OutSpin)outC);
					if (os.opt!=null && os.val!=null)						
						for (int i=0;i<os.val.length;i++)
							if (os.val[i].equals(value)) {
								outS = os.opt[i];
								break;
							}
				} else
					outS = getFormattedText(value,outC.format);
			} 
			//boolean..use yes or no.
			else {
				if (variable.getValue()!=null&&variable.getValue().length()>0) {
					if(variable.getValue().equals("0"))
						outS=myContext.getContext().getString(R.string.no);
						else if (variable.getValue().equals("1"))
							outS=myContext.getContext().getString(R.string.yes);
					Log.e("vortex","VARIABELVÄRDE: "+variable.getValue());
				}
			}
			o.setText(outS);	
			u.setText(variable.getPrintedUnit());				
		}
		else {
			o.setText("");
			u.setText("");
		}	
	}

	@Override
	public void refresh() {
		//Log.d("nils","refreshoutput called on "+myHeader);
		Iterator<Map.Entry<Variable,OutC>> it = myOutputFields.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Variable,OutC> pairs = (Map.Entry<Variable,OutC>)it.next();
			//Log.d("nils","Iterator has found "+pairs.getKey()+" "+pairs.getValue());
			refreshOutputField(pairs.getKey(),pairs.getValue());
		}	
	}



	public static String getFormattedText(String value, String format) {
		int lf=0,rf=0;
		boolean hasFormat = false, hasDot = false;
		if (value!=null&&value.length()>0) {
			if (format!=null) {
				if (format.contains(".")) {
					hasDot = true;
					String[] p = format.split("\\.");
					if (p!=null && p.length==2) {
						lf = p[0].length();
						rf = p[1].length();
					} 
				} else
					lf = format.length();
				//hasformat true if lf or rf is not 0 length.
				hasFormat = (lf!=0||rf!=0);
			}

			if (hasFormat) {
				
				if (hasDot) {
					if (!value.contains(".")) {
						value += ".0";
					}
					String[] p = value.split("\\.");
					if (p!=null && p.length==2) {
						String Rf = p[1];
						if (Rf.length()>rf) 
							Rf = p[1].substring(0, rf);					
						if (Rf.length()<rf)
							Rf = addZeros(Rf,rf-Rf.length());
						String Lf = p[0];
						if (Lf.length()>lf) 
							Lf = p[0].substring(0,lf);					
						if (Lf.length()<lf)
							Lf = addSpaces(Lf,lf-Lf.length());
						value = Lf+"."+Rf;
					}		
				} else {
					if(value.contains(".")) {
						String p[]  = value.split("\\.");
						value = p[0];
					}
					if (value.length()<lf) 
						value = addSpaces(value,lf-value.length());

				}

			}
		}
		return value;
	}

	private static String addZeros(String s,int i) {
		while (i-->0)
			s="0"+s;
		return s;
	}
	private static String addSpaces(String s,int i) {
		while (i-->0)
			s=" "+s;
		return s;
	}


}
