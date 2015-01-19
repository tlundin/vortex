package com.teraim.vortex.utils;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.Log;

import com.teraim.vortex.GlobalState;
import com.teraim.vortex.R;
import com.teraim.vortex.dynamic.VariableConfiguration;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.types.Variable.DataType;
import com.teraim.vortex.expr.Expr;
import com.teraim.vortex.expr.Parser;
import com.teraim.vortex.expr.SyntaxException;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.non_generics.Constants;

public class RuleExecutor {

	private GlobalState gs;
	private Map<String,Set<Entry<String,DataType>>> formulaCache = new HashMap<String,Set<Entry<String,DataType>>>();
	static RuleExecutor singleton=null;
	private VariableConfiguration al;
	private RuleExecutor(Context ctx) {
		gs = GlobalState.getInstance(ctx);
		al = gs.getVariableConfiguration();
	}

	public static RuleExecutor getInstance(Context ctx) {
		if (singleton==null) 
			singleton = new RuleExecutor(ctx);

		return singleton;

	}

	public void propagateRuleToDependants(String varId) {
		Log.d("nils","In propagaterule for "+varId);
		Set<String> dependants = relations.get(varId);
		if (dependants==null) {
			Log.d("nils","Dependants null");
			return;
		}
		Variable v;
		for (String dVarId:dependants) {
			Log.d("nils","refreshing rule state for "+dVarId);
			v = al.getVariableInstance(dVarId);
			v.refreshRuleState();
			Log.d("nils","Indirectly refreshed "+v.getId());
		}
	}

	public void parseFormulas(String formulas, String mainVar) {
		if (formulas == null||formulas.length()==0)
			return;
		String[] formulaA = formulas.split(",");
		for (String formula:formulaA) {
			parseFormula(formula,mainVar);
			Log.d("nils","Found formula: "+formula+" in ParseFormula");
		}



	}


	public Set<Entry<String,DataType>> parseFormula(String formula,String mainVar) {		
		if (formulaCache.get(formula)!=null)
			return formulaCache.get(formula);

		List<String> potVars = new ArrayList<String>();
		Map<String,DataType> filters = new HashMap<String,DataType>();
		LoggerI o = gs.getLogger();
		boolean fail = false;
		//Try parsing the formula.
		String pattern = "<>=+-*/().0123456789";
		String inPattern = "<>=+-*/()";
		boolean in = false,parseFunctionParameters=false;
		String curVar = "";
		//TODO: Separate between tokens: function names, arguments, variables immediately.
		if (formula !=null) {
			for (int i = 0; i < formula.length(); i++){
				char c = formula.charAt(i);  
				//if function, collect parameters. 
				if (parseFunctionParameters) {
					if (Character.isWhitespace(c)||c==',') {
						parseFunctionParameters = false;
						in = false;
						Log.d("nils","Adding functional argument: "+curVar);
						potVars.add(curVar);
						curVar = "";
						continue;
					} else
						//add if not right parantesis.
						in = (c != ')');											
				} else {
					if (!in) {
						//assume its in & test
						in = true;   
						curVar = "";
						for(int j=0;j<pattern.length();j++)
							if (c == pattern.charAt(j)||Character.isWhitespace(c)) {
								//System.out.println("found non-var char: "+pattern.charAt(j));
								//fail.
								in = false;
								break;
							}
					} else {
						//ok we are in. check if char is part of inPattern
						for(int j=0;j<inPattern.length();j++) {
							//check if this is a function, eg. x(y). Look for left paranthesis.

							if (c == inPattern.charAt(j)||Character.isWhitespace(c)) {
								if (c == '(') {
									Log.d("nils","Found paranthesis. Parsing func parameter(s) next");
									parseFunctionParameters = true;									
								} else 
									System.out.println("found non-var char inside: ["
											+(c == inPattern.charAt(j)?inPattern.charAt(j):"<spc>")+"]");
								//fail.
								in = false;
								System.out.println("Found variable: "+curVar);
								potVars.add(curVar);								
								//special case for HAS.
								//								if (curVar.startsWith("has")) {
								curVar="";
								break;
							}
						}
					}
				}
				//Add if in.
				if (in)
					curVar += c;		    		    	
			}
			if (parseFunctionParameters)
				Log.d("nils","Adding final has argument: "+curVar);

			if (curVar.length()>0) 
				potVars.add(curVar);

			if (potVars.size()>1) {
				Iterator<String> it = potVars.iterator();
				String potentialVar;
				while (it.hasNext()) {
					potentialVar = it.next();
					Log.d("nils","PotentialVar: "+potentialVar+" Length: "+potentialVar.length());
					if (isKeyWord(potentialVar)) {

						if (potentialVar.equals("hasAll")) {
							it.remove();
							String nextVar = it.next();
							Log.d("nils","variable after HASALL is: "+nextVar);
							filters.put(nextVar,DataType.filterAll);
						} else if (potentialVar.equals("hasMost")) {
							it.remove();
							String nextVar = it.next();
							Log.d("nils","variable after HASMOST is: "+nextVar);
							filters.put(nextVar,DataType.filterMost);
						} else if (potentialVar.equals("hasSome")) {
							it.remove();
							String nextVar = it.next();
							Log.d("nils","variable after HASSOME is: "+nextVar);
							filters.put(nextVar,DataType.filterSome);
						} else if (potentialVar.equals("historical")) {
							it.remove();
							String nextVar = it.next();
							Log.d("nils","variable after historical is: "+nextVar);
							filters.put(nextVar,DataType.historical);
						} 
						else if (potentialVar.equals("getcurrentyear")) {
							potentialVar = Constants.getYear();
							Log.d("nils","inserting current year: "+Constants.getYear());

						} 


						it.remove();		
					}
					//else 
					//	xAffectVars(potentialVar,mainVar);

				}

			}
			Set<Entry<String,DataType>> myVariables=null;
			if (potVars.size()==0 && filters.size()==0) {
				fail = true; 
				o.addRow("Found no variables in formula "+formula+". Variables starts with a..zA..z");
			}
			else {
				myVariables = new HashSet<Entry<String,DataType>>();
				for (String var:potVars) {
					List<String> row = gs.getVariableConfiguration().getCompleteVariableDefinition(var);
					if (row == null) {
						//this could be a filter. Try..
						o.addRow("");
						o.addRedText("Couldn't find variable "+var+" referenced in formula "+formula);
						fail = true;
					} else {
						DataType type = gs.getVariableConfiguration().getnumType(row);					
						myVariables.add(new AbstractMap.SimpleEntry<String, DataType>(var.trim(),type));
					}
				}
				for (String f:filters.keySet())
					myVariables.add(new AbstractMap.SimpleEntry<String, DataType>(f.trim(),filters.get(f)));
			}

			if (!fail)
				return myVariables;

		} else {
			Log.d("nils","got null in formula, Tools.parseFormula");
			o.addRow("");
			o.addRedText("Formula evaluates to null in Tools.parseFormula");			
		}
		return null;
	}

	static final String[] KeyWordA = {"has","hasAll","hasSome","hasMost","historical","getcurrentyear","and","or","max","min","abs"};
	static final Set<String> KeyWordSet = new HashSet<String>(Arrays.asList(KeyWordA));

	private boolean isKeyWord(String s) {
		s = s.toLowerCase();
		boolean t =  KeyWordSet.contains(s);
		if (t) 
			Log.d("nils","Found keyword "+s+" in formula");	
		return t;
	}

	Map<String,Set<String>> relations = new HashMap<String,Set<String>>();


	private void xAffectVars(String curVar, String mainVar) {
		//If a key word, the following variable would be a new keyvar.
		//TODO FIX THIS

		Log.d("nils","In XaffectVar with curVar: "+curVar+" and mainvar: "+mainVar);
		if (mainVar == null || curVar.equals(mainVar))
			return;

		Set<String> x = relations.get(curVar);
		if (x==null) {
			x = new HashSet<String>();
			relations.put(curVar,x);
		}
		x.add(mainVar);
		Log.d("nils",curVar+" now has dependants: "+x.toString());		
	}

	public String substituteVariables(Set<Entry<String,DataType>> myVariables,String formula,boolean stringT) {
		if (myVariables == null||formula ==null)
			return null;
		String subst = new String(formula.toLowerCase());
		String strRes = "";
		Variable st;
		LoggerI o = gs.getLogger();
		for (Entry<String, DataType> entry:myVariables) {
			if (!isFilter(entry.getValue())) {
				Log.d("nils","Substituting Variable: "+entry.getKey()+" with type "+entry.getValue());
				st = gs.getVariableConfiguration().getVariableInstance(entry.getKey());		
				if (st==null) {
					o.addRow("");
					o.addRedText("Variable "+entry.getKey()+" in formula ["+formula+"] is null.");
				} else
					if (stringT) {
						String stringValue = st.getValue();
						if (stringValue!=null) {
							strRes=stringValue+strRes;
							Log.d("vortex","String type in RuleExec. Adding "+st.getValue());

						}
						Log.d("vortex","Result "+strRes);
					} else

						if (st.getValue()==null) {
							o.addRow("");
							o.addRow("Variable value for "+entry.getKey()+" in formula ["+formula+"] is null.");
							Log.d("nils","Before substitution of: "+st.getId()+": "+subst);
							subst = subst.replace(st.getId().toLowerCase(), "null");	
							Log.d("nils","After substitutionx: "+subst);

						} else {

							Log.d("nils","Substituting Variable: ["+st.getId()+"] with value "+st.getValue());
							subst = subst.replace(st.getId().toLowerCase(), st.getValue());
							Log.d("nils","After substitutiony: "+subst);


						}
			} else {
				String filterRes = applyFilter(entry.getKey(),entry.getValue());
				Log.d("nils","Before substitution: "+subst);
				Log.d("nils","Matching filter "+entry.getKey()+" got result "+filterRes);
				String replaceThis = filterName(entry.getValue())+"("+entry.getKey()+")";
				subst = subst.replace(replaceThis.toLowerCase(), filterRes);
				Log.d("nils","Trying to substitute:["+replaceThis+"]. After Substitutionz: "+subst);

			}

		}		
		if (stringT)
			return strRes;
		subst = subst.replaceAll("(HAS|has)\\(null\\)", "0");
		subst = subst.replaceAll("(HAS|has)\\([+|-]?[0-9]+\\)", "1");
		Log.d("nils","Formula after substitution: "+subst);
		o.addRow("Formula after substitution: "+subst);
		int firstNull = subst.indexOf("null");
		if (firstNull!=-1) {
			Log.d("nils","First null at "+firstNull);			
			o.addRow("At least one variable did not have a value after substitution.");
			//return null;
		}

			return subst;
	}

	private String filterName(DataType value) {

		switch (value) {
		case filterAll:
			return "hasAll";
		case filterMost:
			return "hasMost";
		case filterSome:
			return "hasSome";
		case historical:
			return "historical";
		default:
			return null;
		}

	}

	private static boolean isFilter(DataType value) {
		return (value==DataType.filterAll||value==DataType.filterMost||value==DataType.filterSome||value==DataType.historical);
	}

	private String applyFilter(String filter, DataType filterType) {
		String value;	
		//Check if this is historical(x) function.
		if (filterType.equals(DataType.historical)) {
			Variable var = gs.getVariableCache().getVariable(filter);
			if (var != null) {
				value = var.getHistoricalValue();
				Log.d("nils","Found historical value "+value+" for variable "+filter);
				return value;				
			}
		}
		//Apply filter parameter <filter> on all variables in current table. Return those that match.
		float failC=0;
		//If any of the variables matching filter doesn't have a value, return 0. Otherwise 1.
		List<List<String>> rows = al.getTable().getRowsContaining(VariableConfiguration.Col_Variable_Name, filter);
		if (rows==null || rows.size()==0) {
			gs.getLogger().addRow("");
			gs.getLogger().addRedText("Filter returned emptylist in HASx construction. Filter: "+filter);
			return null;
		}
		float rowC=rows.size();

		for (List<String>row:rows) {
			value = gs.getVariableConfiguration().getVariableValue(gs.getCurrentKeyHash(), al.getVarName(row));
			if (value==null) {
				if (filterType==DataType.filterAll) {
					gs.getLogger().addRow("");
					gs.getLogger().addYellowText("hasAll filter stopped on variable "+al.getVarName(row)+" that is missing a value");
					return "0";
				}
				else 
					failC++;
			} else 
				if (filterType==DataType.filterSome) {					
					gs.getLogger().addRow("");
					gs.getLogger().addYellowText("hasSome filter succeeded on variable "+al.getVarName(row)+" that has value "+value);
					return "1";			
				}
		}
		if (failC == rowC && filterType == DataType.filterSome) {
			gs.getLogger().addRow("");
			gs.getLogger().addYellowText("hasSome filter failed. No variables with values found for filter "+filter);
			return "0";						
		} 
		if (filterType == DataType.filterAll) {
			gs.getLogger().addRow("");
			gs.getLogger().addYellowText("hasAll filter succeeded.");
			return "1";			
		}
		if (failC <= rowC/2) {
			gs.getLogger().addRow("");
			gs.getLogger().addYellowText("hasMost filter succeeded. Filled in: "+(int)((failC/rowC)*100f)+"%");
			return "1";				
		} 	
		gs.getLogger().addRow("");
		gs.getLogger().addYellowText("hasMost filter failed. Not filled in: "+(int)((failC/rowC)*100f)+"%");
		return "0";
	}

	public String parseExpression(String formula, String subst) {
		if (subst==null)
			return null;
		Parser p = gs.getParser();
		Expr exp=null;
		LoggerI o = gs.getLogger();

		try {
			exp = p.parse(subst);
		} catch (SyntaxException e1) {
			o.addRow("");
			o.addRedText("Syntax error for formula "+formula+" after substitution to "+subst);
			e1.printStackTrace();
		}
		if (exp==null) 
		{
			o.addRow("");
			o.addText("Parsing Expr "+formula+" evaluates to null");	
			return null;
		} else {
			//TODO: remove this to allow for calculations.
			//If expression equals null, evaluate as true.
			return exp.value()==null?null:(exp.value().intValue()+"");

			//return Double.toString(exp.value());	
		}
	}


	public CharSequence getRuleExecutionAsString(Map<String,Boolean> ruleResult) {
		if (ruleResult==null)
			return "";
		CharSequence myTxt = new SpannableString("");
		Set<String> keys = ruleResult.keySet();
		Iterator<String> it = keys.iterator();
		while (it.hasNext()) {
			String rule = it.next();
			Boolean res = ruleResult.get(rule);
			if (it.hasNext())
				rule +=",";
			if (!res) {
				Log.d("nils","I get here with string "+rule);
				SpannableString s = new SpannableString(rule);
				s.setSpan(new TextAppearanceSpan(gs.getContext(), R.style.RedStyle),0,s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				myTxt = TextUtils.concat(myTxt, s);	 
			}
			else
				myTxt = TextUtils.concat(myTxt,rule);

		}

		return myTxt;
	}


	/*
	public Boolean evaluate(String rule) {
		String result = parseExpression(rule, substituteVariables(parseFormula(rule,null),rule,false));		
		Log.d("nils","Evaluation of rule: "+rule+" returned "+result);
		return (result!=null?result.equals("1"):null);
	}
	 */


}
