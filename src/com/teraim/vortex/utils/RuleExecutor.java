package com.teraim.vortex.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import com.teraim.vortex.dynamic.types.Delyta;
import com.teraim.vortex.dynamic.types.Variable;
import com.teraim.vortex.dynamic.types.Variable.DataType;
import com.teraim.vortex.expr.Expr;
import com.teraim.vortex.expr.Parser;
import com.teraim.vortex.expr.SyntaxException;
import com.teraim.vortex.log.LoggerI;
import com.teraim.vortex.non_generics.Constants;
import com.teraim.vortex.non_generics.DelyteManager;



/**
 2nd layer Tokenizer and Parser for complex expressions.
 Copyright 2015 Teraim

 Use and modification subject to permission 
 */
@SuppressLint("DefaultLocale") 

public class RuleExecutor {

	GlobalState gs;
	
	public RuleExecutor(GlobalState gs) {
		this.gs = gs;
	}
	private static Map<String,List<TokenizedItem>> formulaCache = new HashMap<String,List<TokenizedItem>>();


	//Rather complex enum for all token types.


	public enum SubstiType {
		Boolean,
		String
	};
	public class SubstiResult {
		public String result;
		private SubstiType myType;

		public SubstiResult(String result, SubstiType myType) {
			this.result = result;
			this.myType = myType;
		}

		public boolean iAmAString() {
			return myType == SubstiType.String;
		}
		public boolean iAmABoolean() {
			return myType == SubstiType.Boolean;
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
			this.original=v.getId();			
		}
		
		public TokenizedItem(String s) {
			this.myType=TokenType.literal;
			this.original=s;			
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
		public void setVariable(Variable variableUsingKey) {
			var= variableUsingKey;
		}

	}

	//The tokens currently supported.

	public enum TokenType {
		function(null,-1),
		getColumnValue(function,1),
		has(function,1),
		hasAll(function,1),
		hasMore(function,1),
		hasSome(function,1),
		hasMost(function,1),
		hasSame(function,-1),
		hasValue(function,-1),
		allHaveValue(function,-1),
		historical(function,1),
		getCurrentYear(function,0),
		getCurrentMonth(function,0),
		getCurrentDay(function,0),
		getCurrentHour(function,0),
		getCurrentMinute(function,0),
		getCurrentSecond(function,0),
		getCurrentWeekNumber(function,0),
		getSweDate(function,0),
		sum(function,-1),
		getDelytaArea(function,1),
		ja(function,1),
		nej(function,1),
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
		literal(null,-1), 
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
		
		public TokenType getParent() {
			return parent;
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
			v = gs.getVariableConfiguration().getVariableInstance(dVarId);
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
		List<TokenizedItem> cached = formulaCache.get(formula);
		if (cached !=null) {
			//Log.d("vortex","FormulaCache returned "+ printTokens(cached) );
			return cached;
		}
		return findTokens(formula,mainVar,gs.getCurrentKeyHash());
	}
	
	public List<TokenizedItem>findTokens(String formula,String mainVar, Map<String,String> keyHash) {	
		
		//Log.d("vortex","In findTokens for formula "+formula);
		//Log.d("vortex","No cached formula. parsing ["+formula+"]");
		List<Token> potVars = new ArrayList<Token>();
		LoggerI o = gs.getLogger();
		//Try parsing the formula.
		String pattern = "[]=+-*/().0123456789,";
		String inPattern = "[]=+-*/(),";
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
						if (curToken.length()>0) {
							//Log.d("nils","Adding functional argument: "+curToken);
							potVars.add(new Token(curToken,SimpleTokenType.arg));
							curToken = "";
						}
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
								if (parseFunctionParameters) {
									//Log.d("vortex","This is a function");
									potVars.add(new Token(curToken,SimpleTokenType.func));
								}
								else {
									//Log.d("vortex","This is a function or variable");
									potVars.add(new Token(curToken,SimpleTokenType.eitherfuncorvar));								
								}
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
				potVars.add(new Token(curToken,parseFunctionParameters?SimpleTokenType.arg:SimpleTokenType.eitherfuncorvar));

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



					//Log.d("vortex","Token: "+cToken.token+" Length: "+tokenName.length()+" Type: "+tokenType);

					if (tokenType != SimpleTokenType.arg) {

						if (args!=null) {
							if (hasCardinality && argCount>0) {
								Log.e("vortex","Too few arguments for function "+function+"!");
								o.addRow("");
								o.addRedText("Too few arguments for function "+function+"!");
							} 
							myFormulaTokens.add(new TokenizedItem(function.name(),function,args.toArray(new String[args.size()])));
							args = null;
						}
						//not argument...if function set, reset to null
						function = null;
						for (TokenType token:functions) {
							if (tokenName.equalsIgnoreCase(token.name())) {
								//Log.d("vortex", "found Function match:" +token.name());
								function = token;
								break;
							}
						}
						if (function == null) {
							for (TokenType token:math_primitives) {
								if (tokenName.equalsIgnoreCase(token.name())) {
									//Log.d("vortex", "found math_primitives match:" +token.name()); 
									function = token;
									break ;
								}
							}
						}

						//}
						//check for variable match if no function match,
						if (tokenType == SimpleTokenType.eitherfuncorvar && function == null) {
							Variable var = gs.getVariableConfiguration().getVariableUsingKey(keyHash,tokenName);
							if (var!=null) {
								//Log.d("vortex","Found variable match for "+tokenName+" in formula");
								myFormulaTokens.add(new TokenizedItem(var));

							} else {
								myFormulaTokens.add(new TokenizedItem(tokenName));
								//o.addRow("Found literal "+tokenName);
								continue;
							}
						}
					}
					else  {
						//is there a function to bind to?
						if (function == null) {
							Log.e("vortex","Found argument, but no function! Argument: "+tokenName);
							o.addRow("");
							o.addRedText("Found argument, but no function! Argument: "+tokenName);
						} else {
							if (args == null) {
								//Log.d("vortex","found first arg. will create list");
								args = new ArrayList<String>();
								argCount = function.cardinality();	
								hasCardinality = argCount>=0;
							}
							args.add(cToken.token);
							//Log.d("vortex",function+" had argument "+cToken.token);
							if (hasCardinality) {
								argCount--;

								if (argCount<0) {
									Log.e("vortex","too many arguments for function "+function);
								}
							}

						}
					}
					if (function!=null && function.cardinality==0)
						myFormulaTokens.add(new TokenizedItem(function.name(),function));


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
			} else
				Log.e("vortex","Potvars empty");
		} else 
			Log.e("vortex","formula was null!!");
		if (myFormulaTokens != null) 
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

	public SubstiResult substituteForValue(List<TokenizedItem> myTokens,String formula,Boolean stringT) {
		LoggerI o = gs.getLogger();
		//o.addRow("Before substitution: "+formula);
		//Log.d("vortex","in substituteforValue with formula "+formula+" and tokens "+printTokens(myTokens));
		if (formula ==null)
			return null;
		//No tokens to substitute? Then formula is ok as is.
		if (myTokens == null)
			return new SubstiResult(formula,stringT?SubstiType.String:null);
		String subst = formula.toLowerCase();
		subst = substFormulas(subst, myTokens);
		
		return substVariables(formula,subst,myTokens,stringT);


	}

	private SubstiResult substVariables(String formula,String subst, List<TokenizedItem> myTokens, Boolean stringT) {
		
		Variable st;
		String var;
		int max=-1;
		LoggerI o = gs.getLogger();
		Map<Integer,TokenizedItem> lengthMap = new HashMap<Integer,TokenizedItem>();
		//Throw away functions.
		for (TokenizedItem item:myTokens) {
			if (item.getType()!= TokenType.variable)
				continue;
			st =item.getVariable();
			if (st==null) {
				o.addRow("");
				o.addRedText("Variable "+item.get()+" in formula ["+formula+"] is null.");
			} else {
				int index = st.getId().length();
				while (lengthMap.get(index)!=null)
					index++;
				lengthMap.put(index,item);
				if (index>max)
					max =index;
			}
		}

		TokenizedItem item;
		while (max>0&&!lengthMap.isEmpty()) {
			//Log.d("vortex","MAX: "+max);
			item = lengthMap.remove(max);
			if (item!=null) {
				
				var = item.get();
				st = item.getVariable();		
				String value = st.getValue();
				if (st.getType()==DataType.text) 
					stringT=true;

				if (value==null) {
					o.addRow("");
					o.addRow("Variable value for "+var+" in formula ["+formula+"] is null.");
					//Log.d("nils","Before substitution of: "+st.getId()+": "+subst);
					subst = subst.replace(st.getId().toLowerCase(), "null");	
					//Log.d("nils","After substitutionx: "+subst);
				} else {
					//Is the value non numeric? 
					//Check if possible to substitute for logical value, eg. "foo" = "foo" = 1.
					//If not possible, same string is returned.
					if (!Tools.isNumeric(value)) {
						//Log.d("vortex","Non numeric value!");
						subst = replaceIfStringCompare(st.getId().toLowerCase(),subst,value);
												
					} else {
						Log.d("nils","Substituting Variable: ["+st.getId()+"] with value "+st.getValue());
						subst = subst.replace(st.getId().toLowerCase(), value);
						//Log.d("nils","After substitutiony: "+subst);						
					}
				}
			}
			max--;			
		}

		if (stringT) {
			Log.d("vortex","string type substitution");
			o.addRow("String type substitution result: "+subst);
			Log.d("vortex","After substitution: "+subst);
			return new SubstiResult(subst,SubstiType.String);
		}

		o.addRow("After substitution: "+subst);
		return new SubstiResult(subst,null);
	}

	private String replaceIfStringCompare(String variableName, String expr,String value) {
		//Log.d("vortex","In replaceifstring with var "+variableName+" expr: "+expr+" val: ["+value+"]");
		//find variable.
		int index = expr.indexOf(variableName);
		//Not found! Return same.
		if (index==-1) {
			Log.d("vortex","did not find variable "+variableName);
			return expr;
		}
		int startIndex = index;
		//Scan for foo = foo.
		//Look for = or <> first.
		boolean eq = false; boolean neq = false;

		for (int i = index+variableName.length();i<expr.length();i++) {
			char c = expr.charAt(i);  
			if (Character.isWhitespace(c))
				continue;
			//find value
			if (c=='=') {
				eq = true;
				index = i+1;
				break;
			}
			//If next char is not outside and this + next is either <> or ><...then...
			if (((c+1)<expr.length()) && (c=='<'&&expr.charAt(i+1)=='>'||c=='>'&&expr.charAt(i+1)=='<')) {
				neq = true;
				//skip next
				index = i+2;
				break;
			}
		}
		if (neq || eq) {
			//we have found a string compare. Find the value.
			boolean before=true;boolean spc =false;
			String exprValue="";
			for (int i = index;i<expr.length();i++) {
				char c = expr.charAt(i);  
				if (before && Character.isWhitespace(c))
					continue;
				if (c==')' || (!before && Character.isWhitespace(c))) {
					//ok, break. i contains end.
					index = i;
					spc=true;
					break;
				} else {
				before = false;
				exprValue+=c;
				}
			}
			//Index is either = last char or first space -1.
			if (!spc)
				index = expr.length();
			//Log.d("vortex","Found value to string compare: "+exprValue);
			if (!exprValue.isEmpty()) { 
			boolean found = false;
			if (eq) {
				if (exprValue.equalsIgnoreCase(value)) {
					//Log.d("vortex","equals!");
					found=true;
				}
			} else if (neq) {
				if (!exprValue.equalsIgnoreCase(value)) {
					//Log.d("vortex","n_equals!");
					found=true;
				}
			}
			if (found) {
				expr = expr.replace(expr.substring(startIndex, index), found?"1":"0");
			}
			} else {
				Log.d("vortex","empty string!!");
			}
			
		}
		Log.d("vortex","Returning: "+expr);
		return expr;
	}

	private String substFormulas(String subst, List<TokenizedItem> myTokens) {
		for (TokenizedItem item:myTokens) {
			TokenType type = item.getType();
			if (type.parent == TokenType.function) {
				String funcEval = evalFunc(item);
				//Log.d("nils","Matching filter "+item.get()+" got result "+funcEval);
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
				//Log.d("nils","Trying to substitute:["+replaceThis+"]. After Substitutionz: "+subst);

			}
		}
		return subst;
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
		Log.d("vortex","Evaluating function "+item.get()+"with arg "+print(item.getArguments()));
		String value;	
		String[] args = item.getArguments();

		VariableConfiguration al = gs.getVariableConfiguration();
		//Check if this is historical(x) function.
		if (item.getType()==TokenType.historical) {
			if (args!=null) {
				if (args.length!=1) {
					gs.getLogger().addRow("");
					gs.getLogger().addRedText("historical() has more than one argument..will use first: "+args[0]);							
				}
				Variable var = gs.getVariableCache().getVariable(args[0]);
				if (var != null) {
					value = var.getHistoricalValue();
					Log.d("nils","Found historical value "+value+" for variable "+item.get());
					return value;				
				} else {
					Log.e("vortex","Variable not found: "+args[0]);
					gs.getLogger().addRow("");
					gs.getLogger().addRedText("Variable not found in historical: "+args[0]);							
				}
			} else {
				Log.e("vortex","Historical() have no arguments. You must specify a variable!");
				gs.getLogger().addRow("");
				gs.getLogger().addRedText("Historical() have no arguments. You must specify a variable!");
			}
		}
		else if (item.getType()==TokenType.getCurrentYear) {
			Log.d("vortex","Returning current year!");
			return Constants.getYear();
		}
		else if (item.getType()==TokenType.getCurrentMonth) {
			return Constants.getMonth();
		}
		else if (item.getType()==TokenType.getCurrentDay) {
			return Constants.getDayOfMonth();
		}
		else if (item.getType()==TokenType.getCurrentHour) {
			return Constants.getHour();
		}
		else if (item.getType()==TokenType.getCurrentSecond) {
			return Constants.getSecond();
		}
		else if (item.getType()==TokenType.getCurrentMinute) {
			return Constants.getMinute();
		}
		else if (item.getType()==TokenType.getCurrentWeekNumber) {
			return Constants.getWeekNumber();
		}
		else if (item.getType()==TokenType.getSweDate) {
			return Constants.getSweDate();
		}
		else if (item.getType()==TokenType.getColumnValue) {
			if (args!=null && args.length!=0) {
				Log.d("vortex","Getting current value for column "+args[0]);
				Map<String, String> kh = gs.getCurrentKeyHash();
				if (kh==null)
					return null;
				else {
					Log.d("vortex","keyhash "+kh.toString());
					Log.d("votex","value for column is "+kh.get(args[0]));
					return kh.get(args[0]);
				}
			}
		}

		else if (item.getType()==TokenType.sum) {
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
							if (!Tools.isNumeric(v.getValue())) {
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
				return Float.toString(sum);
			} else {
				Log.e("vortex","sum function without arguments");
				gs.getLogger().addRow("");
				gs.getLogger().addRedText("Sum Function without arguments. Evaluates to 0");
				return "0";
			}

		}

		else if (item.getType()==TokenType.getDelytaArea) {
			Log.d("vortex","running getDelytaArea function");
			DelyteManager dym = DelyteManager.getInstance();
			if (dym==null) {
				gs.getLogger().addRow("");
				gs.getLogger().addRedText("Cannot calculate delyta area...no provyta selected");
				return null;
			}
			if (args==null||args.length!=1) {
				Log.e("vortex","no or too few arguments in getDelytaArea");
				gs.getLogger().addRow("");
				gs.getLogger().addRedText("No or too few arguments in getDelytaArea");
				return null;
			}
			//TODO This is wrong...variable arg should be tokenized.
			String arg = args[0];
			if (arg !=null && arg.length()>0) {

			} else {
				Log.e("vortex","no first argument in getDelytaArea");
				gs.getLogger().addRow("");
				gs.getLogger().addRedText("Argument missing in getDelytaArea");
				return null;
			}
			value=null;
			if (!Tools.isNumeric(arg)) {
				Variable v = al.getVariableInstance(arg);
				if (v==null || v.getValue()==null) {
					Log.e("vortex","variable or value null in getdelytaarea");
					if (v==null) {
						gs.getLogger().addRow("");
						gs.getLogger().addRedText("Variable "+arg+" does not exist (in function getDelytaArea)");
						return null;
					} else
						return null;
				} else  
					value = v.getValue();

			} else
				value = arg;

			float area = dym.getArea(Integer.parseInt(value));
			if (area == 0) {
				Log.e("vortex","area 0 in getdelytaarea");
				gs.getLogger().addRow("Area 0");
				gs.getLogger().addRedText("Either Delyta "+args[0]+" does not exist or area is 0 (in function getDelytaArea)");
				return null;
			}
			return Float.toString(area/100);
		}

		else if (item.getType()==TokenType.hasSame||item.getType()==TokenType.hasValue||item.getType()==TokenType.allHaveValue) {
			String function = item.getType().name();
			Log.d("vortex","running "+function);
			if(args==null || args.length<3) {
				Log.e("vortex","no or too few arguments in hasSame");
				gs.getLogger().addRow("");
				gs.getLogger().addRedText("No or too few arguments in "+function);
			} else {
				List<List<String>> rows;
				//Get all variables in Functional Group x.
				if (item.getType()==TokenType.hasSame)
					rows= al.getTable().getRowsContaining(VariableConfiguration.Col_Functional_Group, args[0]);
				else
					rows = al.getTable().getRowsContaining(VariableConfiguration.Col_Variable_Name, args[0]);
				if (rows == null||rows.size()==0) {
					Log.e("vortex","no variables found for filter "+args[0]);
					return null;
				} else
					Log.d("vortex","found "+(rows.size()-1)+" variables for "+args[0]);
				//Parse the expression. Find all references to Functional Group.
				//Each argument need to either exist or not exist.
				Map<String, String[]>values=new HashMap<String,String[]>();
				boolean allNull = true;
				for (List<String>row:rows) {
					Log.d("vortex","Var name: "+al.getVarName(row));

					if (item.getType()==TokenType.hasValue||
							item.getType()==TokenType.allHaveValue) {

						String operator = args[1];
						String val = args[2];
						String formula = al.getVarName(row)+operator+val;
						Variable myVar = al.getVariableInstance(al.getVarName(row));
						String res=null;
						if (myVar!=null && myVar.getValue()!=null) {
							allNull = false;
							res = parseExpression(formula, myVar.getValue()+operator+val);
							if (!Tools.isNumeric(res)) {
								Log.e("vortex", formula+" evaluates to null..something wrong");
							} else {
								int intRes = Integer.parseInt(res);
								if (intRes==0 && item.getType()==TokenType.allHaveValue) {
									gs.getLogger().addRow("");
									gs.getLogger().addYellowText("allHaveValue failed on expression "+formula);
									Log.e("vortex","allHaveValue failed on "+formula);
									return "0";
								} else {
									if (intRes==0)
										continue;
									else {
										if (item.getType()==TokenType.hasValue) {
											gs.getLogger().addRow("");
											gs.getLogger().addGreenText("hasvalue succeeded on expression "+formula);
											Log.d("vortex","hasvalue succeeded on expression "+formula);
											return("1");
										}
									}

								}
							}
						} else {
							Log.d("vortex","null value...skipping");
						}

					}
					else if (item.getType()==TokenType.hasSame) {
						for (int i = 1; i<args.length;i++) {
							String[] varName = al.getVarName(row).split(Constants.VariableSeparator);
							int size = varName.length;
							if (size<3) {
								gs.getLogger().addRow("");
								gs.getLogger().addRedText("This variable has no Functional Group...cannot apply hasSame function. Variable id: "+varName);
								Log.e("vortex","This is not a group variable...stopping.");
								return null;
							} else
							{
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
				}
				if (item.getType()==TokenType.hasSame) {
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

				} else if (item.getType()==TokenType.hasValue) {
					//Hasvalue fails since none of the variables fullfilled the criteria
					gs.getLogger().addRow("");
					gs.getLogger().addYellowText("hasvalue failed to find any match");
					Log.e("vortex","hasValue failed. No match found");
					return "0";
				} else {
					if (!allNull) {
						gs.getLogger().addRow("");
						gs.getLogger().addYellowText("allHaveValue succeeded!");
						Log.e("vortex","allHaveValue succeeded!");
						return "1";		
					} else {
						gs.getLogger().addRow("");
						gs.getLogger().addYellowText("allHaveValue failed - no values");
						Log.e("vortex","allHaveValue failed on empty list");						
					}

				}
			}
		}
		else if (item.getType()==TokenType.has) {
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

		} else if (item.getType().name().startsWith("has")){
			Log.d("vortex","HASx function");
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
				gs.getLogger().addYellowText("hasSome filter failed. No variables with values found for "+((item.getArguments()!=null&&item.getArguments().length>0)?item.getArguments()[0]:""));
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
		gs.getLogger().addRow("");
		gs.getLogger().addYellowText("Function evaluation failed for "+item.get()+". Returning 0");
		return "0";		
	}

	private String print(String[] arguments) {
		String ret="";
		if (arguments == null)
			return "null";
		for (String a:arguments) {
			ret+=a+",";
		}
		return ret;
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
			Log.d("vortex","FFF "+exp.value());
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

	public void destroyCache() {
		Log.d("vortex","destroying formulacache on "+this);
		formulaCache.clear();
	}

	/*
	public Boolean evaluate(String rule) {
		String result = parseExpression(rule, substituteVariables(parseFormula(rule,null),rule,false));		
		Log.d("nils","Evaluation of rule: "+rule+" returned "+result);
		return (result!=null?result.equals("1"):null);
	}
	 */


}
