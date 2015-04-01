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
 2nd layer Parser for formulas.
 Author Terje Lundin
 Part of Vortex Core, Copyright Teraim.
 Parses formulas and, if ok, allows evaluation.

 */
@SuppressLint("DefaultLocale") 

public class RuleExecutor {

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
		hasSame(function,-1),
		historical(function,1),
		getCurrentYear(function,0),
		getCurrentMonth(function,0),
		getCurrentDay(function,0),
		getCurrentHour(function,0),
		getCurrentMinute(function,0),
		getCurrentSecond(function,0),
		getCurrentWeekNumber(function,0),
		sum(function,-1),
		//		exist(function,2),
		variable(null,-1),
		text(variable,0),
		numeric(variable,0),
		bool(variable,0),
		list(variable,0),
		existence(variable,0),
		auto_increment(variable,0),
		math_primitives(function,-1),
		and(math_primitives,0),
		or(math_primitives,0),
		abs(math_primitives,0), 
		acos(math_primitives,0), 
		asin(math_primitives,0), 
		atan(math_primitives,0),  
		ceil(math_primitives,0), 
		cos(math_primitives,0), 
		exp(math_primitives,0), 
		floor(math_primitives,0), 
		log(math_primitives,0), 
		round(math_primitives,0), 
		sin(math_primitives,0), 
		sqrt(math_primitives,0),  
		tan(math_primitives,0),
		atan2(math_primitives,0),
		max(math_primitives,0),
		min(math_primitives,0),
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

	enum SimpleTokenType {
		func,arg,var, eitherfuncorvar
	}
	//Breaks down a formula into identified tokens.
	private class Token {
		String token;
		SimpleTokenType t;
		public Token(String token, SimpleTokenType t) {
			this.token = token;
			this.t = t;
		}


	}


	public List<TokenizedItem>findTokens(String formula,String mainVar) {		
		if (formulaCache.get(formula)!=null)
			return formulaCache.get(formula);
		Log.d("vortex","parsing formula ["+formula+"]");
		List<Token> potVars = new ArrayList<Token>();
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
					//skip blanks.
					if (Character.isWhitespace(c))
						continue;
					//next argument.
					if (c==','|| c== ')') {						
						Log.d("nils","Adding functional argument: "+curToken);
						potVars.add(new Token(curToken,SimpleTokenType.arg));
						curToken = "";
						//parse until right paranthesis found.
						parseFunctionParameters = (c != ')');	
					} else {
						curToken+=c;
						continue;
					}
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
									//Log.d("nils","Found paranthesis. Parsing func parameter(s) next");
									parseFunctionParameters = true;									
								}// else 
								//System.out.println("found non-var char inside: ["
								//		+(c == inPattern.charAt(j)?inPattern.charAt(j):"<spc>")+"]");
								//fail.
								in = false;
								//System.out.println("Found token: "+curToken);
								if (parseFunctionParameters)
									potVars.add(new Token(curToken,SimpleTokenType.func));
								else
									potVars.add(new Token(curToken,SimpleTokenType.eitherfuncorvar));								
								curToken="";
								break;
							}
						}
					}
				}
				if (in)
					curToken += c;		    		    	
			}
			//add if remaining.
			if (curToken.length()>0) 
				potVars.add(new Token(curToken,parseFunctionParameters?SimpleTokenType.arg:SimpleTokenType.var));
			//Now all the tokens have been identified. 
			//scan and match functions and variables to token
			if (potVars.size()>0) {
				myFormulaTokens = new ArrayList<TokenizedItem>();
				Iterator<Token> it = potVars.iterator();
				String tokenName=null;

				List<String> args=null;
				int argCount = 0;
				SimpleTokenType tokenType;
				Token cToken;
				TokenType function=null;
				boolean hasCardinality = true;

				while (it.hasNext()) {
					cToken = it.next();
					tokenName = cToken.token.toLowerCase();
					tokenType = cToken.t;
					if (args!=null && tokenType != SimpleTokenType.arg) {
						if (hasCardinality && argCount>0) {
							Log.e("vortex","Too few arguments for function "+function+"!");
							o.addRow("");
							o.addRedText("Too few arguments for function "+function+"!");
						} 
						myFormulaTokens.add(new TokenizedItem(function.name(),function,args.toArray(new String[args.size()])));
						args = null;
					}
					//Log.d("vortex","Token: "+cToken.token+" Length: "+tokenName.length()+" Type: "+tokenType);

					//if (tokenType == SimpleTokenType.func) {
					for (TokenType token:functions) {
						if (tokenName.equalsIgnoreCase(token.name())) {
							Log.d("vortex", "found Function match:" +token.name());
							function = token;
							break;
						}
					}
					if (function == null) {
						for (TokenType token:math_primitives) {
							if (tokenName.equalsIgnoreCase(token.name())) {
								Log.d("vortex", "found math_primitives match:" +token.name()); 
								function = token;
								break ;
							}
						}
					}
					if (function!=null && function.cardinality==0)
						myFormulaTokens.add(new TokenizedItem(function.name(),function));

					//}
					//check for variable match.
					if (tokenType != SimpleTokenType.func && function == null) {
						Variable var = gs.getVariableConfiguration().getVariableInstance(tokenName);
						if (var!=null) {
							Log.d("vortex","Found variable match for "+tokenName+" in formula");
							myFormulaTokens.add(new TokenizedItem(var));

						} else {
							Log.e("vortex","unrecognized token in formula: "+tokenName);
							o.addRow("");
							o.addRedText("unrecognized token in formula: "+tokenName);
							continue;
						}
					}
					else if (tokenType == SimpleTokenType.arg) {
						//is there a function to bind to?
						if (function == null) {
							Log.e("vortex","Found argument, but no function! Argument: "+tokenName);
							o.addRow("");
							o.addRedText("Found argument, but no function! Argument: "+tokenName);
						} else {
							if (args == null) {
								Log.d("vortex","found first arg. will create list");
								args = new ArrayList<String>();
								argCount = function.cardinality();	
								hasCardinality = argCount>=0;
							}
							args.add(cToken.token);
							Log.d("vortex",function+" had argument "+cToken.token);
							if (hasCardinality) {
								argCount--;

								if (argCount<0) {
									Log.e("vortex","too many arguments for function "+function);
								}
							}

						}
					}

				}
				if (args!=null) {
					if (hasCardinality && argCount>0) {
						Log.e("vortex","Too few arguments for function "+function+"!");
						o.addRow("");
						o.addRedText("Too few arguments for function "+function+"!");
					} 
					Log.d("vortex","Added arguments for function: "+args.toString());
					myFormulaTokens.add(new TokenizedItem(function.name(),function,args.toArray(new String[args.size()])));
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
	TokenType[] math_primitives = TokenType.math_primitives.allChildren();
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
		Log.d("vortex","in substituteforValue with formula "+formula+" and tokens "+printTokens(myTokens));
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
					else
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

		o.addRow("After substitution: "+subst);
		return new SubstiResult(subst,false);
	}

	private String printTokens(List<TokenizedItem> myTokens) {
		String res = "";
		if (myTokens==null)
			return null;
		for (TokenizedItem t:myTokens) {
			String args="";
			if (t.args!=null&&t.args.length>0) {
				for (String arg:t.args) {
					args+=arg+",";
				}
			}
			res+= "\ntoken: "+t.get()+" type: "+t.getType()+" args: "+args;
		}
		return res;
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

		if (item.getType()==TokenType.sum) {
			float sum = 0;
			if (args!=null) {
				Log.d("vortex","Calculating sum!  ");
				gs.getLogger().addRow("Calculating sum function...");
				for (String arg:args) {
					Log.d("vortex","arg: "+arg);
					if (Tools.isNumeric(arg)) {
						float argn = Float.parseFloat(arg);
						Log.d("vortex","numeric argument: "+arg+" evaluates to "+argn);
						sum+=argn;
					} else {
						Variable v = gs.getVariableConfiguration().getVariableInstance(arg);
						if (v==null) {
							gs.getLogger().addRow("");
							gs.getLogger().addRedText("Function has variable that does not exist in current context: "+arg);							
						} else {
							if (v.getType()!=DataType.numeric) {
								Log.e("vortex","Function sum has non numeric argument: "+arg);
								gs.getLogger().addRow("");
								gs.getLogger().addRedText("Function sum has non numeric argument: "+arg);

							} else {
								String val = v.getValue();
								Log.d("vortex","Value: "+val);
								if (val == null) {
									Log.d("vortex","skipping null value");

								} else
									sum+=Float.parseFloat(val);
							}
						}
					}
				}
				gs.getLogger().addRow("Sum evaluates to "+sum);
				Log.d("vortex","Sum evaluates to "+sum);
				//TODO: Remove Round when going to float from integer.
				return Float.toString(Math.round(sum));
			} else {
				Log.e("vortex","sum function without arguments");
				gs.getLogger().addRow("");
				gs.getLogger().addRedText("Sum Function without arguments. Evaluates to 0");
				return "0";
			}

		}

		if (item.getType()==TokenType.hasSame) {
			Log.d("vortex","running hasSame function");
			if(args==null || args.length<3) {
				Log.e("vortex","no or too few arguments in hasSame");
				gs.getLogger().addRow("");
				gs.getLogger().addRedText("No or too few arguments in hasSame");
			} else {
				//Get all variables in Functional Group x.
				List<List<String>> rows = al.getTable().getRowsContaining(VariableConfiguration.Col_Functional_Group, item.getArguments()[0]);
				Log.d("vortex","found "+(rows.size()-1)+" variables in "+item.getArguments()[0]);
				//Parse the expression. Find all references to Functional Group.
				//Each argument need to either exist or not exist.
				Map<String, String[]>values=new HashMap<String,String[]>();
				for (List<String>row:rows) {
					Log.d("vortex","Var name: "+al.getVarName(row));
					for (int i = 1; i<args.length;i++) {
						String[] varName = al.getVarName(row).split("_");
						int size = varName.length;
						if (size<3) {
							gs.getLogger().addRow("");
							gs.getLogger().addRedText("This variable has no Functional Group...cannot apply hasSame function. Variable id: "+varName);
							Log.e("vortex","This is not a group variable...stopping.");
							return null;
						} else {

							String name = varName[size-1];
							String art = varName[size-2];
							String group = varName[0];
							Log.d("vortex","name: "+name+" art: "+art+" group: "+group+" args["+i+"]: "+args[i]);
							if (name.equalsIgnoreCase(args[i])) {
								Log.d("vortex","found varname. Adding "+art);
								Variable v = al.getVariableInstance(al.getVarName(row));
								String varde = null;
								if (v == null) {
									Log.d("vortex","var was null!");

								} else
									varde = v.getValue();
								String [] result;
								if (values.get(art)==null) {
									Log.d("vortex","empty..creating new val arr");
									result = new String[args.length-1];
									values.put(art, result);
								} else 
									result = values.get(art);
								result[i-1]=varde;
								break;
							}
						}
					}


				}
				//now we should have an array containing all values for all variables.
				Log.d("vortex","printing resulting map");
				for (String key:values.keySet()) {
					String vCompare = values.get(key)[0];
					for (int i = 1;i<args.length-1;i++) {
						String vz = values.get(key)[i];
						if (vCompare==null && vz ==null ||vCompare!=null&&vz!=null)
							continue;
						else {
							gs.getLogger().addRow("hasSame difference detected for "+key+". Stopping");
							Log.e("vortex","Diffkey values for "+key+": "+(vCompare==null?"null":vCompare)+" "+(vz==null?"null":vz));
							return "0";
						}

					}
				}
				Log.d("vortex","all values same. Success for hasSame!");
				return "1";

			}
		}

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
