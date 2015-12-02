package com.teraim.vortex.dynamic.types;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.teraim.vortex.exceptions.ParseException;
import com.teraim.vortex.log.LoggerI;


enum TokenType {
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

class TokenizedItem {

	private String[] args;
	private TokenType myType;
	private String original;


	public TokenizedItem(String token, TokenType t, String ...arguments) {
		this.myType=t;
		this.args=arguments;
		this.original=token;
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

	
}

enum SimpleTokenType {
	func,arg,var, eitherfuncorvar
}
//Breaks down a formula into identified tokens.
class Token {
	String token;
	SimpleTokenType t;
	public Token(String token, SimpleTokenType t) {
		this.token = token;
		this.t = t;
	}


}


public class TokenizedExpression {

	String mRaw = null;
	List<TokenizedItem> tokens;
	
	public static TokenizedExpression tokenize(String formula) {
		try { 
			findTokens(formula);
		} catch(ParseException e) {
			
		}
		return null;
	}
	
	private TokenizedExpression(String rawFormula) {
		mRaw = rawFormula;
	
	}
	
	
	
	final static String OutPattern = "[]=+-*/().0123456789,";
	final static String InPattern = "[]=+-*/(),";
	
	enum ParseState {
		initial,
		parsingLiteral,
		parsingVariable,
		parsingInteger,
	}
		

	/**
	 * Identifies all tokens and checks syntax
	 * @param formula - the string representation
	 * @return a list of tokenized Items.
	 * x+3 x + 3 = sum(x,3)
	 */
	
	private static List<TokenizedItem> findTokens(String formula) throws ParseException {	
		
		Log.d("vortex","In findTokens for formula "+formula);
		if (formula == null) {
			Log.e("vortex","formula null in findTokens.");
			return null;
		}
			
		List<Token> tokens = new ArrayList<Token>();
		
		//Try parsing the formula.
		String curToken = "";
		List<TokenizedItem> myFormulaTokens =null;
		boolean in = false, parsingFunction = false;
		ParseState parseState=ParseState.initial;

		for (int i = 0; i < formula.length(); i++){
			
			char c = formula.charAt(i);  
			
			
			if (Character.isDigit(c)) {
				if (parseState == ParseState.initial){
					parseState = ParseState.parsingInteger;
					
				}
			}
				
			
			if (Character.isLetter(c)) {
			
			}
			
			if (Character.isWhitespace(c)) {

					identifyToken(curToken, parseState);
			}
			
			if (c == '(') {
				if(parseState != ParseState.initial) {
					parsingFunction = true;
				} else {
					
				}
			}
			if (c == ',') {
				
			}
		}
		return null;
	}

			
private	static void identifyToken(String token, ParseState parseState) {
	
	if (token.isEmpty()) {
		Log.d("vortex","empty token..discarding");
	}
	
}
}
			/*
			if (!in) {
					//assume its in & test
					in = true;   
					for(int j=0;j<OutPattern.length();j++)
						if (c == OutPattern.charAt(j)||Character.isWhitespace(c)) {
							in = false;
							break;
							}
					} else {
						//ok we are in. check if char is part of inPattern
						for(int j=0;j<InPattern.length();j++) {
							//check if this is a function, eg. x(y). Look for left paranthesis.

							if (c == InPattern.charAt(j)||Character.isWhitespace(c)) {
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
				Log.d("vortex","Potvars empty [rule executor]");
		} else 
			Log.e("vortex","formula was null!!");
		if (myFormulaTokens != null) 
			formulaCache.put(formula, myFormulaTokens);
		return myFormulaTokens;

	}*/
	



