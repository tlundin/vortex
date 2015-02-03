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

import android.annotation.SuppressLint;
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



/**
 Parser for formulas.
 Author Terje Lundin

 Parses formulas and, if ok, allows evaluation.

 */
@SuppressLint("DefaultLocale") public class RuleExecutor {

	private GlobalState gs;
	private Map<String,List<TokenizedItem>> formulaCache = new HashMap<String,List<TokenizedItem>>();
	static RuleExecutor singleton=null;
	private VariableConfiguration al;

	//Rather complex enum for all token types.

	
	
	public class SubstiResult {
		public String result;
		public boolean IamAString;
		public SubstiResult(String result, boolean iamAString) {
			this.result = result;
			IamAString = iamAString;
		}
		
		
	}
	public class TokenizedItem {

		private String[] args;
		private TokenType myType;
		private String original;
		private Variable var;

		public TokenizedItem(String token, TokenType t, String ...arguments) {
			this.myType=t;
			this.args=arguments;
			this.original=token;
		}
		public TokenizedItem(Variable v) {
			this.myType=TokenType.variable;
			this.var = v;
			this.original=v.getLabel();			
		}

		//This is the string we found. 
		public String get() {
			return original;
		}
		public TokenType getType() {
			return myType;
		}
		public String[] getArguments() {
			return args;
		}

		public Variable getVariable() {
			return var;
		}

	}

	//The tokens currently supported.

	public enum TokenType {
		function(null,-1),
		has(function,1),
		hasAll(function,1),
		hasMore(function,1),
		hasSome(function,1),
		hasMost(function,1),
		historical(function,1),
		getCurrentYear(function,0),
		getCurrentMonth(function,0),
		getCurrentDay(function,0),
		getCurrentHour(function,0),
		getCurrentMinute(function,0),
		getCurrentSecond(function,0),
		getCurrentWeekNumber(function,0),
		variable(null,-1),
		text(variable,0),
		numeric(variable,0),
		bool(variable,0),
		list(variable,0),
		existence(variable,0),
		auto_increment(variable,0),
		math(function,-1),
		and(math,0),
		or(math,0),
		abs(math,0), 
		acos(math,0), 
		asin(math,0), 
		atan(math,0),  
		ceil(math,0), 
		cos(math,0), 
		exp(math,0), 
		floor(math,0), 
		log(math,0), 
		round(math,0), 
		sin(math,0), 
		sqrt(math,0),  
		tan(math,0),
		atan2(math,0),
		max(math,0),
		min(math,0),
		unknown(null,-1), 
		;
		private TokenType parent = null;
		private List<TokenType> children = new ArrayList<TokenType>();
		private int cardinality;
		public int cardinality() {
			return cardinality;
		}
		private TokenType(TokenType parent, int cardinality) {
			this.parent = parent;
			if (this.parent != null) {
				this.parent.addChild(this);
			}
			this.cardinality = cardinality;
		}

		private void addChild(TokenType child) {
			children.add(child);
		}
		public TokenType[] getChildren() {
			return children.toArray(new TokenType[children.size()]);
		}

		public TokenType[] allChildren() {
			List<TokenType> list = new ArrayList<TokenType>();
			addChildren(this, list);
			return list.toArray(new TokenType[list.size()]);
		}

		private static void addChildren(TokenType root, List<TokenType> list) {
			list.addAll(root.children);
			for (TokenType child : root.children) {
				addChildren(child, list);
			}
		}

	}




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
			findTokens(formula,mainVar);
			Log.d("nils","Found formula: "+formula+" in ParseFormula");
		}



	}

	//Breaks down a formula into identified tokens.

	public List<TokenizedItem>findTokens(String formula,String mainVar) {		
		if (formulaCache.get(formula)!=null)
			return formulaCache.get(formula);
		Log.d("vortex","parsing formula ["+formula+"]");
		List<String> potVars = new ArrayList<String>();
		LoggerI o = gs.getLogger();
		//Try parsing the formula.
		String pattern = "<>=+-*/().0123456789,";
		String inPattern = "<>=+-*/(),";
		boolean in = false,parseFunctionParameters=false;
		String curToken = "";
		List<TokenizedItem> myFormulaTokens =null;

		if (formula !=null) {
			for (int i = 0; i < formula.length(); i++){
				char c = formula.charAt(i);  
				//if function, collect parameters. 
				if (parseFunctionParameters) {
					if (Character.isWhitespace(c)||c==',') {
						parseFunctionParameters = false;
						in = false;
						Log.d("nils","Adding functional argument: "+curToken);
						potVars.add(curToken);
						curToken = "";
						continue;
					} else
						//add if not right parantesis.
						in = (c != ')');											
				} else {
					if (!in) {
						//assume its in & test
						in = true;   
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
								System.out.println("Found variable: "+curToken);
								potVars.add(curToken);								
								//special case for HAS.
								//								if (curVar.startsWith("has")) {
								curToken="";
								break;
							}
						}
					}
				}
				if (in)
					curToken += c;		    		    	
			}
			if (parseFunctionParameters)
				Log.d("nils","Adding final has argument: "+curToken);


			if (curToken.length()>0) 
				potVars.add(curToken);
			//Now all the tokens have been identified. 
			//scan and match functions and variables to token
			if (potVars.size()>0) {
				myFormulaTokens = new ArrayList<TokenizedItem>();
				Iterator<String> it = potVars.iterator();
				String unclassifiedToken;
				String[] args;
				TokenizedItem ti;
				TokenType resultToken;
				while (it.hasNext()) {
					resultToken = null;
					args = null;
					unclassifiedToken = it.next();
					ti=null;
					Log.d("vortex","PotentialVar: "+unclassifiedToken+" Length: "+unclassifiedToken.length());
					unclassifiedToken = unclassifiedToken.toLowerCase();

					for (TokenType token:functions) {
						if (unclassifiedToken.equalsIgnoreCase(token.name())) {
							Log.d("vortex", "found Function match:" +token.name());
							if (token.cardinality()==-1) {
								Log.d("vortex","this is not a token...return null");

							} else {
								resultToken = token;
								break;
							}
						}
					}
					if (resultToken == null) {
						for (TokenType token:math) {
							if (unclassifiedToken.equalsIgnoreCase(token.name())) {
								Log.d("vortex", "found Math match:" +token.name()); 
								if (token.cardinality()==-1) {
									Log.d("vortex","this is a parent token...return null");
								} else {
									resultToken = token;
									break ;
								}
							}
						}
						if (resultToken == null) {
							Variable var = gs.getVariableConfiguration().getVariableInstance(unclassifiedToken);
							if (var!=null) {
								Log.d("vortex","Found variable match for "+unclassifiedToken+" in formula");
								ti = new TokenizedItem(var);

							} else {
								Log.e("vortex","unrecognized token in formula: "+unclassifiedToken);
								o.addRow("");
								o.addRedText("unrecognized token in formula: "+unclassifiedToken);
								continue;
							}
						}
					}
					if (ti==null) {
						//if token has arguments, catch
						int c = resultToken.cardinality();
						args = new String[c];
						int i = 0;
						while (c-->0 && it.hasNext()) 
							args[i++] = it.next(); 
						Log.d("vortex",resultToken+" had argument(s)"+args.toString());
						ti = new TokenizedItem(unclassifiedToken,resultToken,args);
					}

					myFormulaTokens.add(ti);
					//No match to token. This is either a variable or an error.
				}
			}
		}
		if (myFormulaTokens == null) {
			Log.d("vortex","Found no variables in formula "+formula+". Variables starts with a..zA..z");
			o.addRow("Found no variables in formula "+formula+". Variables starts with a..zA..z");
		} else
			formulaCache.put(formula, myFormulaTokens);
		return myFormulaTokens;

	}



	TokenType[] functions = TokenType.function.allChildren();
	TokenType[] math = TokenType.math.allChildren();
	TokenType[] varTypes = TokenType.variable.allChildren();





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

	public SubstiResult substituteForValue(List<TokenizedItem> myTokens,String formula,boolean stringT) {
		LoggerI o = gs.getLogger();
		o.addRow("Before substitution: "+formula);
		if (formula ==null)
			return null;
		//No tokens to substitute? Then formula is ok as is.
		if (myTokens == null)
			return new SubstiResult(formula,stringT);
		String strRes = "",subst = formula.toLowerCase(),var;
		TokenType type;
		Variable st;
		for (TokenizedItem item:myTokens) {

			type = item.getType();
			//check if function
			if (type.parent == TokenType.function) {
				String funcEval = evalFunc(item);
				Log.d("nils","Matching filter "+item.get()+" got result "+funcEval);
				//Try to replace fn(x,y,z) with evaluated value.
				String replaceThis = type.name()+"(";
				String args[] = item.getArguments();
				for (int i=0;i<args.length;i++) {
					if (i < (args.length-1))
						replaceThis+=args[i]+",";
					replaceThis+=args[i];
				}
				replaceThis+=")";
				subst = subst.replace(replaceThis.toLowerCase(), funcEval!=null?funcEval:"0");
				Log.d("nils","Trying to substitute:["+replaceThis+"]. After Substitutionz: "+subst);

			} else
				if (type == TokenType.variable){
					var = item.get();
					st = item.getVariable();		
					if (st==null) {
						o.addRow("");
						o.addRedText("Variable "+var+" in formula ["+formula+"] is null.");
					} else {
						String value = st.getValue();
						if (st.getType()==DataType.text) {
							stringT=true;
						}
						//Stringtype is true if either any individual variable is text type or the result variable is a text
						//If string type, just concatenate. Dont evaluate..
						if (stringT) {
							if (value!=null) {
								strRes=strRes+value;
								Log.d("vortex","string concatenation "+strRes);
							}
						} else
							if (value==null) {
								o.addRow("");
								o.addRow("Variable value for "+var+" in formula ["+formula+"] is null.");
								Log.d("nils","Before substitution of: "+st.getId()+": "+subst);
								subst = subst.replace(st.getId().toLowerCase(), "null");	
								Log.d("nils","After substitutionx: "+subst);
								strRes=strRes+"null";

							} else {
								Log.d("nils","Substituting Variable: ["+st.getId()+"] with value "+st.getValue());
								subst = subst.replace(st.getId().toLowerCase(), value);
								Log.d("nils","After substitutiony: "+subst);
								strRes=strRes+value;

							}
					}
				} 

		}		
		if (stringT) {
			Log.d("vortex","string type returned with values substituted");
			o.addRow("After substitution: "+strRes);
			return new SubstiResult(strRes,true);
		}
		int firstNull = subst.indexOf("null");
		if (firstNull!=-1) {
			Log.d("nils","First null at "+firstNull);			
			o.addRow("At least one variable did not have a value after substitution.");
			//return null;
		}
		
		
		o.addRow("After substitution: "+subst);
		return new SubstiResult(subst,false);
	}

	private String evalFunc(TokenizedItem item) {
		String value;	
		//Check if this is historical(x) function.
		if (item.getType()==TokenType.historical) {
			Variable var = gs.getVariableCache().getVariable(item.get());
			if (var != null) {
				value = var.getHistoricalValue();
				Log.d("nils","Found historical value "+value+" for variable "+item.get());
				return value;				
			}
		}

		if (item.getType()==TokenType.getCurrentYear) {
			Log.d("vortex","Returning current year!");
			return Constants.getYear();
		}
		if (item.getType()==TokenType.getCurrentMonth) {
			return Constants.getMonth();
		}
		if (item.getType()==TokenType.getCurrentDay) {
			return Constants.getDayOfMonth();
		}
		if (item.getType()==TokenType.getCurrentHour) {
			return Constants.getHour();
		}
		if (item.getType()==TokenType.getCurrentSecond) {
			return Constants.getSecond();
		}
		if (item.getType()==TokenType.getCurrentMinute) {
			return Constants.getMinute();
		}
		if (item.getType()==TokenType.getCurrentWeekNumber) {
			return Constants.getWeekNumber();
		}
		//Check if variable has value
		String[] args = item.getArguments();

		if (item.getType()==TokenType.has) {
			if (args!=null) {
			Variable v = gs.getVariableConfiguration().getVariableInstance(args[0]);
			if (v==null||v.getValue()==null)
				return "0";
			return "1";
			} else {
				gs.getLogger().addRow("");
				gs.getLogger().addRedText("HAS function is missing argument! ");
				return "0";
			}
 				
		}
		//Apply filter parameter <filter> on all variables in current table. Return those that match.
		float failC=0;
		//If any of the variables matching filter doesn't have a value, return 0. Otherwise 1.
		List<List<String>> rows = al.getTable().getRowsContaining(VariableConfiguration.Col_Variable_Name, args[0]);
		if (rows==null || rows.size()==0) {
			gs.getLogger().addRow("");
			gs.getLogger().addRedText("Filter returned emptylist in HASx construction. Filter: "+item.get());
			gs.getLogger().addRow("");
			gs.getLogger().addRedText("This is not good! A HASx function needs a list with something in. Check your rule!");
			
			return null;
		}
		float rowC=rows.size();

		for (List<String>row:rows) {
			value = gs.getVariableConfiguration().getVariableValue(gs.getCurrentKeyHash(), al.getVarName(row));
			if (value==null) {
				if (item.getType()==TokenType.hasAll) {
					gs.getLogger().addRow("");
					gs.getLogger().addYellowText("hasAll filter stopped on variable "+al.getVarName(row)+" that is missing a value");
					return "0";
				}
				else 
					failC++;
			} else 
				if (item.getType()==TokenType.hasSome) {					
					gs.getLogger().addRow("");
					gs.getLogger().addYellowText("hasSome filter succeeded on variable "+al.getVarName(row)+" that has value "+value);
					return "1";			
				}
		}
		if (failC == rowC && item.getType()==TokenType.hasSome) {
			gs.getLogger().addRow("");
			gs.getLogger().addYellowText("hasSome filter failed. No variables with values found for filter "+item.getType());
			return "0";						
		} 
		if (item.getType()==TokenType.hasAll) {
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
			return exp.value()==null?null:(exp.value().intValue()+"");
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
