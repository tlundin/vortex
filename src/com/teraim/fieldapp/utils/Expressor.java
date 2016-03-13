/**
 *
 */
package com.teraim.fieldapp.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import android.database.Cursor;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.Table;
import com.teraim.fieldapp.dynamic.types.VarCache;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.Variable.DataType;
import com.teraim.fieldapp.loadermodule.configurations.WorkFlowBundleConfiguration;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.non_generics.DelyteManager;




/**
 * toolset class with
 *
 * Parser and Tokenizer for arithmetic and logic expressions.
 *
 *
 * Part of the Vortex Core classes. 
 *
 * @author Terje Lundin 
 *
 * Teraim Holding reserves the property rights of this Class (2015)
 *
 *
 */

public class Expressor {


	//Types of tokens recognized by the Engine. 
	//Some of these are operands, some functions, etc as denoted by the first argument.
	//Last argument indicates Cardinality or prescedence in case of Operands (Operands are X op Y)
	public enum TokenType {
		function(null,-1),
		booleanFunction(function,-1),
		valueFunction(function,-1),
		has(booleanFunction,1),
		hasAll(booleanFunction,1),
		hasMore(booleanFunction,1),
		hasSome(booleanFunction,1),
		hasMost(booleanFunction,1),
		hasSame(booleanFunction,-1),
		hasValue(booleanFunction,-1),
		hasNullValue(booleanFunction,-1),
		photoExists(booleanFunction,1),
		allHaveValue(booleanFunction,-1),
		not(booleanFunction,1),
		iff(valueFunction,3),
		getColumnValue(valueFunction,1),
		historical(valueFunction,1),
		hasSameValueAsHistorical(valueFunction,2),
		getCurrentYear(valueFunction,0),
		getCurrentMonth(valueFunction,0),
		getCurrentDay(valueFunction,0),
		getCurrentHour(valueFunction,0),
		getCurrentMinute(valueFunction,0),
		getCurrentSecond(valueFunction,0),
		getCurrentWeekNumber(valueFunction,0),
		getSweDate(valueFunction,0),
		sum(valueFunction,-1),
		concatenate(valueFunction,-1),
		getDelytaArea(valueFunction,1),
		abs(valueFunction,1),
		acos(valueFunction,1),
		asin(valueFunction,1),
		atan(valueFunction,1),
		ceil(valueFunction,1),
		cos(valueFunction,1),
		exp(valueFunction,1),
		floor(valueFunction,1),
		log(valueFunction,1),
		round(valueFunction,1),
		sin(valueFunction,1),
		sqrt(valueFunction,1),
		tan(valueFunction,1),
		atan2(valueFunction,1),
		max(valueFunction,2),
		min(valueFunction,2),
		unaryMinus(valueFunction,1),


		variable(null,-1),
		text(variable,0),
		numeric(variable,0),
		bool(variable,0),
		list(variable,0),
		existence(variable,0),
		auto_increment(variable,0),
		none(null,-1),
		literal(null,-1),		
		number(null,-1),
		operand(null,0), 
		and(operand,5),
		or(operand,4),
		add(operand,8),
		subtract(operand,8),
		multiply(operand,10),
		divide(operand,10),
		gte(operand,6),
		lte(operand,6),
		eq(operand,6),
		neq(operand,6),
		gt(operand,6),
		lt(operand,6),

		parenthesis(literal,-1),
		comma(literal,-1),
		leftparenthesis(parenthesis,-1),
		rightparenthesis(parenthesis,-1),

		unknown(null,-1),
		startMarker(null,-1),
		endMarker(null,-1),
		;

		private TokenType parent = null;
		private List<TokenType> children = new ArrayList<TokenType>();
		private int cardinalityOrPrescedence;

		public int cardinality() {
			return cardinalityOrPrescedence;
		}
		//only operands has prescedence.
		public int prescedence() {
			if (this.parent==operand)
				return cardinalityOrPrescedence;
			else
				return -1;
		}
		private TokenType(TokenType parent, int cardinalityOrPrescedence) {
			this.parent = parent;
			if (this.parent != null) {
				this.parent.addChild(this);
			}
			this.cardinalityOrPrescedence = cardinalityOrPrescedence;
		}
		//Methods to extract parent/child relationships.

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

		//Normally case is of no consequence
		public static TokenType valueOfIgnoreCase(String token) {
			for (TokenType t:TokenType.values()) {
				if (t.name().equalsIgnoreCase(token))
					return t;
			}
			return null;
		}

	}


	//Operands are transformed into functions. Below a name mapping.
	final static String[] Operands = new String[]	     {"=",">","<","+","-","*","/",">=","<=","<>","=>","=<"};
	final static String[] OperandFunctions= new String[]	 {"eq","gt","lt","add","subtract","multiply","divide","gte","lte","neq","gte","lte"};
	static LoggerI o;
	static GlobalState gs;
	static Map<String,String> currentKeyChain=null;
	//TODO solution for the  string reverse for string a+b..
	static String tret=null;

	/**
	 * Takes an input string and replaces all expr with values.
	 */
	public static String analyze(String text) {
		StringBuilder endResult=null;
		List<Token> result = tokenize(text);
		if (result!=null) {
			if (testTokens(result)) {
				if (result!=null) {
					StreamAnalyzer streamAnalyzer = new StreamAnalyzer(result);
					endResult = new StringBuilder();
					while (streamAnalyzer.hasNext()) {

						Object rez=null;
						try {
							rez = streamAnalyzer.next();
						} catch (ExprEvaluationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (rez!=null) {
							endResult.append(rez);
						}
					}
				}
				//System.out.println();

			}
			else
				System.err.println("FAIL testtokens");

		} else
			System.err.println("FAIL tokenize");
		return endResult.toString();
	}


	public static List<EvalExpr> preCompileExpression(String expression) {
		if (expression==null)
			return null;
		o = WorkFlowBundleConfiguration.debugConsole;
		System.out.println("Precompiling: "+expression.toString());
		List<Token> result = tokenize(expression);
		List<EvalExpr> endResult = new ArrayList<EvalExpr>();
		if (result!=null && testTokens(result)) {
			StreamAnalyzer streamAnalyzer = new StreamAnalyzer(result);
			while (streamAnalyzer.hasNext()) {

				EvalExpr rez=null;
				try {
					rez = streamAnalyzer.next();
				} catch (ExprEvaluationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (rez!=null) {
					endResult.add(rez);
				} else {
					o.addRow("");
					o.addRedText("Failed to Precompile "+result.toString());
					System.err.println("Tokenstream evaluated to null for: "+streamAnalyzer.getFaultyTokens());
				}
			}
			if (endResult.size()>0){
				StringBuilder sb = new StringBuilder();
				for (EvalExpr e:endResult)
					sb.append(e);
				o.addRow("");
				o.addRow("Precompiled: "+sb);
				System.out.println("Precompiled: "+endResult.toString());
				return endResult;
			}

		}
		o.addRow("");
		o.addRedText("failed to precompile: "+expression);
		o.addRedText("End Result: "+endResult);
		Log.e("vortex","failed to precompile: "+expression);
		Log.e("vortex","End Result: "+endResult);
		printTokens(result);

		return null;
	}

	public static String analyze(List<EvalExpr> expressions) {
		return analyze(expressions,GlobalState.getInstance().getCurrentKeyMap());
	}

	public static String analyze(List<EvalExpr> expressions, Map<String,String> evalContext) {

		gs = GlobalState.getInstance();
		o = gs.getLogger();
		if (expressions == null) {
			o.addRow("");
			o.addRedText("Expression was null in Analyze. This is likely due to a syntax error in the original formula");
			return null;
		}
		//evaluate in default context.
		currentKeyChain = evalContext;
		//System.out.println("Analyzing "+expressions.toString());
		StringBuilder endResult = new StringBuilder();
		for (EvalExpr expr:expressions) {
			tret=null;
			Object rez=null;
			//System.out.println("Analyze: "+expr.toString());
			rez = expr.eval();
			if (rez!=null) {
				//System.out.println("Part Result "+rez.toString());
				endResult.append(rez);
			} else
				System.err.println("Got null back when evaluating "+expr.toString()+" . will not be included in endresult.");

		}
		
		//	System.out.println(expressions.toString()+" -->  "+endResult.toString());       
		return endResult.toString();
	}

	public static Boolean analyzeBooleanExpression(EvalExpr expr) {
		return analyzeBooleanExpression(expr,GlobalState.getInstance().getCurrentKeyMap());
	}

	public static Boolean analyzeBooleanExpression(EvalExpr expr, Map<String,String> evalContext) {
		tret=null;
		if (expr==null)
			return null;
		gs = GlobalState.getInstance();
		o = gs.getLogger();
		currentKeyChain = evalContext;		
		// Log.d("Vortex","Class "+expr.getClass().getCanonicalName());
		Object eval = expr.eval();
		Log.d("Vortex","BoolExpr: "+expr.toString()+" evaluated to "+eval);
		o.addRow("Expression "+expr.toString()+" evaluated to "+eval);
		
		if (eval instanceof String) {
			Log.e("vortex","String back in analyzeBoolean...likely missing [..]");
			o.addRow("");
			o.addRedText("The expression "+expr.toString()+" evaluated to Text: '"+eval+"'. This is likely because of missing [ ] parantheses around the expression in the XML");
			return false;
		} else
			return (Boolean)eval;
	}
	/**
	 * Class Token
	 * @author Terje
	 * Expressions are made up of Tokens. Tokens are for instance Numbers, Literals, Functions.
	 *
	 */
	public static class Token implements Serializable {

		private static final long serialVersionUID = -1975204853256767316L;
		public String str;
		public TokenType type;
		public Token(String raw,TokenType t) {
			str=raw;
			type=t;
		}
	}

	//Exception for Evaluation failures. 

	public static class ExprEvaluationException extends Exception {

		private static final long serialVersionUID = 1107622084592264591L;

	}

	/**
	 * Class StreamAnalyzer
	 * @author Terje
	 * Takes an Iterator and allows caller to read the evaluation objects as a stream.
	 */
	private static class StreamAnalyzer {
		Iterator<Token> mIterator;
		List<Token> curr;
		public StreamAnalyzer(List<Token> tokens) {
			mIterator = tokens.iterator();

		}
		public boolean hasNext() {
			return mIterator.hasNext();
		}

		public EvalExpr next() throws ExprEvaluationException {
			curr=null;
			while (mIterator.hasNext()) {
				Token t = mIterator.next();
				if (t.type==TokenType.text)
					return new Text(t);
				if (t.type==TokenType.startMarker) {
					curr = new ArrayList<Token>();
				} else if (t.type==TokenType.endMarker) {
					if (curr!=null && !curr.isEmpty()) {
						EvalExpr ret = analyzeExpression(curr);
						if (ret==null) {
							System.err.println("Eval of expression "+curr.toString()+" failed");
							return null;
						}
						else
							return ret;
					} else {
						System.err.println("Empty Expr or missing startTag.");
						return null;
					}
				}
				if (curr!=null)
					curr.add(t);
				else
					System.err.println("Discarded "+t.str);


			}
			System.err.println("Missing end marker for Expr ']'");
			return null;
		}

		public String getFaultyTokens() {
			StringBuilder sres=new StringBuilder();
			for (Token c:curr) {
				sres.append(c.toString());
			}
			return sres.toString();
		}
	}

	//Temp Entry point for testing purposes.



	public static List<Token> tokenize(String formula) {
		//System.out.println("Tokenize this: "+formula);
		List<Token> result=new ArrayList<Token>();
		char c;
		StringBuilder currToken=new StringBuilder("");
		TokenType t = TokenType.none;

		//This is added to support regexp sections that should not be interpreted.
		//Everything within {} will be treated as literals.
		boolean chompAnyCharacter=false;
		boolean inside = false,unary=false;

		for (int i = 0; i < formula.length(); i++){
			c = formula.charAt(i);

			if (!inside) {
				if (c=='[') {
					inside = true;
					add(currToken,TokenType.text,result);
					currToken.append(c);
					add(currToken,TokenType.startMarker,result);
					t = TokenType.none;
				}
				else {
					t=TokenType.text;
					currToken.append(c);
				}
				continue;
			}
			if (chompAnyCharacter) {
				if (c=='}') {
					add(currToken,t,result);
					t=TokenType.none;
					chompAnyCharacter=false;

				} else
					currToken.append(c);

				continue;
			}

			//if a digit, variable or letter comes after an operand, save it.
			if (t == TokenType.operand && (Character.isDigit(c) || Character.isLetter(c)||c=='$')) {
				//save operand.
				add(currToken,t,result);
				//add number.
				t = TokenType.none;
			}
			if (Character.isDigit(c)) {
				if (t == TokenType.none)
					t = TokenType.number;
				currToken.append(c);
			}
			else if (Character.isLetter(c)) {
				switch (t) {
				case none:
				case number:
					t=TokenType.literal;
					break;
				}
				currToken.append(c);
			}


			else if (Character.isWhitespace(c)) {
				add(currToken,t,result);
				//Discard whitespace.
				t= TokenType.none;
			}
			else if (c=='$') {
				t = TokenType.variable;
			}
			else if (c=='(' || c==')' || c==',') {
				if (t != TokenType.none){
					//add any token on left of operand
					add(currToken,t,result);
				}
				switch (c) {
				case '(':
					t=TokenType.leftparenthesis;
					break;
				case ')':
					t=TokenType.rightparenthesis;
					break;
				case ',':
					t=TokenType.comma;
					break;
				}
				add(c,t,result);
				t= TokenType.none;
			}

			else if (c=='<' || c=='>' || c=='=' || c == '+' || c == '*' || c == '/' || c=='-') {
				if (t != TokenType.none && t != TokenType.operand){
					//add any token on left of operand
					add(currToken,t,result);
				}
				//unary minus
				//System.out.println("Found zunary operator "+c+" i "+i);
				if ((t == TokenType.operand || (t == TokenType.none&&(i==1||result.get(result.size()-1).type==TokenType.leftparenthesis || result.get(result.size()-1).type==TokenType.operand))) && (c =='-') && (i+1)!=formula.length() && (!Character.isWhitespace(formula.charAt(i+1)))) {
					//if ((t == TokenType.operand || (t == TokenType.none&&(i==1||result.get(result.size()-1).type==TokenType.leftparenthesis))) && (c =='-') && (i+1)!=formula.length() && (!Character.isWhitespace(formula.charAt(i+1)))) {
					//System.out.println("Found unary operator "+c);
					//System.out.println("Currtorken: "+currToken.toString());
					add(currToken,t,result);
					currToken.append(c);
					t=TokenType.unaryMinus;
					add(currToken,t,result);
					t=TokenType.none;
				} else {

					currToken.append(c);
					t=TokenType.operand;
				}
			}
			else if (c=='{') {
				//all characters now treated as being literal.
				add(currToken,t,result);
				t=TokenType.literal;
				chompAnyCharacter=true;
			}
			else if (c==']') {
				add(currToken,t,result);
				currToken.append(c);
				add(currToken,TokenType.endMarker,result);

				inside = false;
			}

			else {
				currToken.append(c);
				//System.out.println("unrecognized: "+c+" AT POS "+i+" in "+formula+" chomp: "+chompAnyCharacter);
			}
		}
		if (inside) {
			System.err.println("Missing end bracket");
			return null;
		}
		//system.out.println("Reached end of tokenizer. CurrentToken is "+currToken+" and t is "+t.name());

		if (t != TokenType.none)
			add(currToken,t,result);

		return result;
	}


	private static void add(char c, TokenType t, List<Token> result) {
		result.add(new Token(String.valueOf(c),t));
	}


	private static void add(StringBuilder currToken, TokenType t,List<Token> result) {
		//need to change tokentype if literal and keyword.
		if (currToken.length()!=0)
			result.add(new Token(currToken.toString(),t));
		currToken.setLength(0);
	}



	//check rules between token pairs.

	private static boolean testTokens(List<Token> result) {
		//Rule 1: op op
		o = WorkFlowBundleConfiguration.debugConsole;
		boolean valueF=false,booleanF=false;
		Token current=null,prev=null;
		int pos=-1,lparC=0,rparC=0;
		for (Token t:result) {
			pos++;
			if (t.type==TokenType.text) {
				//Skipp text.
				continue;
			}
			//check number of parenthesis...
			if (t.type.getParent()==TokenType.parenthesis) {
				if (t.type==TokenType.rightparenthesis)
					rparC++;
				else
					lparC++;
			}
			if (current==null && prev == null) {
				prev=t;
				continue;
			}
			else if (current == null) {
				current = t;
			}
			else {
				prev = current;
				current = t;
			}

			//try to find supported functions and change to correct type.
			if (prev.type==TokenType.literal) {

				//Check for PI
				if (prev.str.equals("PI")) {
					prev.type=TokenType.number;
					prev.str=Double.toString(Math.PI);
					//System.out.println("Found PI!"+prev.str);
					continue;
				}

				TokenType x = TokenType.valueOfIgnoreCase(prev.str);

				//check if AND OR  
				if (isLogicalOperand(x))

					prev.type=TokenType.operand;

				else
					//check if function
					if (current.type == TokenType.leftparenthesis) {
						if (x==null) {
							o.addRedText("Syntax Error: Function "+prev.str+" does not exist!");
							return false;
						}
						if (isFunction(x)) {
							TokenType parent = x.getParent();
							//System.out.println("found function match : "+prev.str);
							prev.type = x;
							//Check that there aren't both logical and value functions in the same expression.
							//System.out.println("parent: "+parent);
							if (parent == TokenType.valueFunction)
								valueF=true;
							if (parent == TokenType.booleanFunction)
								booleanF = true;
						} else {
							o.addRow("");
							o.addRedText("The token "+prev.str+" is used as function, but is in fact a "+prev.type);
							return false;
						}
					}
			}

			else if (prev.type==TokenType.operand) {
				boolean found=false;
				for (int i=0;i<Operands.length;i++) {
					if(prev.str.equalsIgnoreCase(Operands[i])) {
						prev.str=OperandFunctions[i];
						//System.out.println("Replaced "+Operands[i]+" with corresponding operand function: "+prev.str);
						found =true;
					} 

				}
				if (!found) {
					o.addRow("");
					o.addRedText("Syntax Error: Operator "+prev.str+" does not exist.");
					System.err.println("Syntax Error: Operator "+prev.str+" does not exist.");
					return false;
				}
			}
			//if (prev.type == current.type && current.type.getParent()!=TokenType.parenthesis) {
			//	System.err.println("Rule 1. Syntax does not allow repetition of same type at token "+pos+": "+prev.str+":"+current.str);
			//	return false;
			//}
		}
		//Check for unbalanced paranthesis
		if (lparC!=rparC) {
			System.err.println("Rule 2. Equal number of left and right parenthesis. Left: "+lparC+" right: "+rparC);
			return false;
		}
		//Check for mix between data types
		//if (valueF&&booleanF) {
		//	System.err.println("Rule 3. Both logical(true-false) and value functions present. This is not allowed");
		//	return false;
		//}
		return true;
	}



	//And expression is one or a set of tokens, making up a semantic entity, such as function.
	//This tool cuts out the next expression from the token stream.

	private static class ExpressionAnalyzer {
		//stream to use
		private Iterator<Token>it;
		//current position along stream.
		private int pos = 0 ;

		public ExpressionAnalyzer(Iterator<Token> iterator) {
			it = iterator;
		}


		public boolean hasNext() {
			return it.hasNext();
		}
		public Expr next() {
			Token t;
			if (it.hasNext()) {
				t = it.next();
				//System.out.println("In next: "+t.type);
				assert(t!=null);
				TokenType type = t.type;

				switch (type) {
				case leftparenthesis:
					return new Push();
				case rightparenthesis:
					return new Pop();
				case variable:
				case number:
				case literal:
				case comma:
					return new Atom(t);
				case operand:
					return new Operand(t);
				case text:
					return new Text(t);

				}
				TokenType p = type.parent;
				if (isFunction(type)) {
					return new Function(type, it);
				}
			}

			return null;

		}
	}

	//marker class
	public abstract static class Expr implements Serializable {
		private static final long serialVersionUID = -1968204853256767316L;
		final TokenType type;

		public Expr(TokenType t) {
			type = t;
		}
	}

	public abstract static class EvalExpr extends Expr {
		public EvalExpr(TokenType t) {
			super(t);
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		abstract Object eval();
	}

	public static class Atom extends EvalExpr {
		final Token myToken;
		public Atom(Token t) {
			super (t.type);
			myToken = t;
		}
		@Override
		public String toString() {
			if (myToken!=null)
				return myToken.str;
			else
				return null;
		}

		public boolean isVariable() {
			return (type==TokenType.variable);
		}

		public Object eval() {
			//Log.d("vortex","In eval for Atom type "+type);
			String value;
			switch(type) {
			case variable:
				
				Variable v = gs.getVariableCache().getVariableUsingKey(currentKeyChain, myToken.str);
				if (v!=null && v.getValue() == null || v==null) {
					System.out.println("Variable '"+this.toString()+"' does not have a value or Variable is missing.");
					return null;
				}
				Log.d("vortex","Atom variable ["+v.getId()+"] Type "+v.getType());
				value = v.getValue();
				
				if (Tools.isNumeric(value)) {
					if (v.getType()== Variable.DataType.decimal || value.contains("."))
						return Double.parseDouble(value);
					else
						return Integer.parseInt(value);
				}
				if (v.getType()==Variable.DataType.bool) {
					if (value.equalsIgnoreCase("false")) {
						//Log.d("vortex","Returning false");
						return false;
					}
					else if (value.equalsIgnoreCase("true")) {
						//Log.d("vortex","Returning true");
						return true;
					}
					else {
						Log.e("vortex","Not a bool value: "+value);
						return null;
					}
				}
				return value;
			case number:
				Log.d("vortex","this is a numeric atom");
				if (myToken !=null && myToken.str!=null) {
					System.out.println("Numeric value: "+myToken.str);
					if (myToken.str.contains("."))
						return Double.parseDouble(myToken.str);
					else
						return Integer.parseInt(myToken.str);						
				}
				else {
					System.err.println("Numeric value was null");
					return null;
				}
			case literal:
				Log.d("vortex","this is a literal atom");
				if (myToken.str.equalsIgnoreCase("false"))
					return false;
				else if (myToken.str.equalsIgnoreCase("true"))
					return true;
				else
					return toString();

			default:
				System.err.println("Atom type has no value: "+this.type);
				return null;
			}
		}
	}

	private static class Operand extends Expr {
		final Token myToken;
		public Operand(Token t) {
			super(t.type);
			myToken = t;

		}
		@Override
		public String toString() {
			return myToken.str;
		}



	}

	private static class Convoluted extends EvalExpr {
		final EvalExpr arg1,arg2;
		final Operand operator;


		public Convoluted(EvalExpr existingArg, EvalExpr newArg,Operand operator) {
			super(null);
			this.arg2=newArg;
			this.arg1=existingArg;
			this.operator=operator;
		}
		@Override
		public String toString() {
			String arg1s = arg1.toString();
			String arg2s = arg2.toString();
			if (arg1s==null)
				arg1s="?";
			if (arg2s==null)
				arg2s="?";
			return operator.toString()+"("+arg1s.toString()+","+arg2s.toString()+")";
		}

		public Object eval() {
			Log.d("vortex","In eval for convo");
			Object arg1v = arg1.eval();
			//			Log.e("vortex","I am literal? "+isLiteralOperator);
			//			Log.e("vortex","arg1v: "+((arg1v==null)?"null":arg1v.toString()));
			//			Log.e("vortex","arg2v: "+((arg2v==null)?"null":arg2v.toString()));


			if (arg1v==null) {
				String opS =operator.myToken.str;
				if (opS!=null) {
					TokenType op = TokenType.valueOfIgnoreCase(opS);
					if (op.equals(TokenType.or))
						return arg2.eval();
				}
				return null;
			}

			Object arg2v = arg2.eval();

			if (arg2v==null) {
				String opS =operator.myToken.str;
				if (opS!=null) {
					TokenType op = TokenType.valueOfIgnoreCase(opS);
					if (op.equals(TokenType.or))
						if(arg1v instanceof Boolean)
							if ((Boolean)arg1v)
								return true;

				}
				return null;
			}

			//functions require both arguments be of same kind.

			boolean isIntegerOperator = arg1v instanceof Integer && arg2v instanceof Integer;
			boolean isBooleanOperator = arg1v instanceof Boolean && arg2v instanceof Boolean;
			//if any of the arguments are string, then treat both as literal.
			boolean isLiteralOperator = arg1v instanceof String || arg2v instanceof String;



			if (!isIntegerOperator && !isBooleanOperator && !isLiteralOperator) {
				System.err.println("Argument types are wrong in operand: "+arg1v.getClass().getSimpleName()+","+arg2v.getClass().getSimpleName());
				o.addRow("");
				o.addRedText("Argument types are wrong in operand: "+arg1v.getClass().getSimpleName()+","+arg2v.getClass().getSimpleName());
				return null;
			}
			if (isLiteralOperator) {
				if ((arg1v instanceof Double || Tools.isNumeric((String)arg1v)) && (arg2v instanceof Double || Tools.isNumeric((String)arg2v))) {
					Log.d("vortex","turning literal into numeric...both numbers");
					arg1v = arg1v instanceof Double ? arg1v : Integer.parseInt((String)arg1v);
					arg2v = arg2v instanceof Double ? arg2v : Integer.parseInt((String)arg2v);
					isIntegerOperator = true;
				}
			}
			//Requires Double arguments.
			if (isIntegerOperator) {
				Integer arg1F,arg2F;
				Object res=null;

				arg1F=(Integer)arg1v;
				arg2F=(Integer)arg2v;
				String opS =operator.myToken.str;
				if (opS!=null) {
					TokenType op = TokenType.valueOf(opS);
					switch (op) {

					case add:
						res = arg1F+arg2F;
						break;
					case subtract:
						res = arg1F-arg2F;
						break;
					case multiply:
						res = arg1F*arg2F;
						break;
					case divide:
						res = arg1F/arg2F;
						break;
					case eq:
						res = arg1F.compareTo(arg2F)==0;
						Log.e("vortex","arg1F eq arg2F? "+arg1F+" eq "+arg2F+": "+res);
						break;
					case neq:
						res = arg1F.compareTo(arg2F)!=0;
						Log.e("vortex","arg1F neq arg2F? "+arg1F+" neq "+arg2F+": "+res);
						break;
					case gt:
						res = arg1F > arg2F;
						break;
					case lt:
						res = arg1F < arg2F;
						break;
					case lte:
						res = arg1F <= arg2F;
						break;
					case gte:
						res = arg1F >= arg2F;
						break;
					default:
						System.err.println("Unsupported operand: "+op);
						o.addRow("");
						o.addRedText("Unsupported arithmetic operator: "+op);
						break;
					}
				} else {
					System.err.println("Unsupported arithmetic operand: "+operator.type);
					o.addRow("");
					o.addRedText("Unsupported arithmetic operand: "+operator.type);
				}

				return res;
			}
			//Requires boolean arguments.
			else if (isBooleanOperator) {
				Boolean arg1B,arg2B,res=null;

				arg1B=(Boolean)arg1v;
				arg2B=(Boolean)arg2v;
				String opS =operator.myToken.str;
				if (opS!=null) {
					TokenType op = TokenType.valueOfIgnoreCase(opS);
					switch (op) {
					case or:						
						res = (arg1B||arg2B);
						//Log.e("vortex","OR Evaluates to "+res+" for "+arg1B+" and "+arg2B);
						break;
					case and:
						//System.err.println("Gets to and");
						res = (arg1B&&arg2B);
						break;
					case eq:
						res = (arg1B==arg2B);
						break;
					default: 

						System.err.println("Unsupported boolean operand: "+op);
						o.addRow("");
						o.addRedText("Unsupported boolean operator: "+op);
						break;
					}

				}
				return res;
			} else if (isLiteralOperator) {
				String arg1S=arg1v.toString();
				String arg2S=arg2v.toString();
				TokenType op = TokenType.valueOfIgnoreCase(operator.myToken.str);
				System.out.println("in isliteral with exp: "+arg1S+" "+operator.myToken.str+" "+arg2S);
				o.addText("calculating literal expression "+arg1S+" "+operator.myToken.str+" "+arg2S);

				switch (op) {
				case add: 
					if (tret==null) {
						tret ="foock";
						Log.d("vortex","first so returning "+arg1S+arg2S);
						return arg1S+arg2S;
					} else
						return arg2S+arg1S;
				case eq:
					return arg1S.equals(arg2S);
				case neq:
					return !arg1S.equals(arg2S);
				default:
					System.err.println("Unsupported literal operand: "+op);
					o.addRow("");
					o.addRedText("Unsupported literal operator: "+op);
					break;
				}
			}
			return null;
		}

	}





	private static class Push extends Expr {

		public Push() {
			super(null);
		}
		private static final long serialVersionUID = 4443625476068076080L;
	}
	private static class Pop extends Expr {
		public Pop() {
			super(null);
		}
		private static final long serialVersionUID = 2499591806981280542L;
	}
	public static class Text extends EvalExpr {
		private String str;
		public Text(Token t) {
			super(TokenType.text);
			this.str=t.str;
		}
		@Override
		String eval() {
			return str;
		}
		@Override
		public String toString() {
			return str;
		}
	}

	//cut out function from full expression.
	private static class Function extends EvalExpr {
		//try to build a function from the tokens in the beg. of the given token stream.

		private static final int No_Null = 1;
		private static final int No_Null_Numeric=2;
		private static final int No_Null_Literal=3;
		private static final int NO_CHECK = 4;
		private static final int Null_Numeric = 5;
		private static final int Null_Literal = 6;
		private static final int No_Null_Boolean = 7;

		private List<EvalExpr> args = new ArrayList<EvalExpr>();

		public Function(TokenType type, Iterator<Token> it) {
			super(type);
			//iterator reaches end?
			int depth=0;
			Token e;
			final List<List<Token>> argsAsTokens = new ArrayList<List<Token>>();
			List<Token> funcArg = new ArrayList<Token>();
			boolean argumentReady=false;


			while(it.hasNext()) {
				e = it.next();
				//system.out.println("Expr "+e.type);

				if (e.type==TokenType.leftparenthesis) {
					depth++;
					//system.out.println("+ depth now "+depth);
					//discard paranthesis...not used.
					if (depth==1)
						continue;
				} else if (e.type==TokenType.rightparenthesis) {
					depth--;
					//system.out.println("- depth now "+depth);
					if (depth==0)
						argumentReady=true;
				}  else if (e.type==TokenType.comma && depth==1 ) {
					argumentReady = true;
				}

				if (!argumentReady) {
					//System.out.println("Added "+e.str+" to funcArg. I am  "+type);
					funcArg.add(e);
					if (depth==0 && type == TokenType.unaryMinus) {
						//System.out.println("Found argument for unary f "+funcArg.get(0).str+" l "+funcArg.size());
						argumentReady = true;
					}
				}

				if (argumentReady) {
					if (!funcArg.isEmpty()) {
						argsAsTokens.add(funcArg);
						funcArg=new ArrayList<Token>();
					}
					//					else 
					//						;
					//system.out.println("No argument in function "+type.name());


					if (e.type==TokenType.rightparenthesis || type==TokenType.unaryMinus)
						break;
					else
						argumentReady = false;

				}
			}
			if (!argumentReady) {
				System.err.println("Missing closing paranthesis in function "+type.name());
				return;
				//printTokens(funcArg);
			}
			//recurse for each argument.
			int i=1;
			for (List<Token> arg:argsAsTokens) {
				//system.out.println("Recursing over argument "+i++ +"in function "+type.name()+" :");
				//printTokens(arg);
				EvalExpr analyzedArg;
				try {
					analyzedArg = analyzeExpression(arg);
					if (analyzedArg != null) {
						args.add(analyzedArg);
					} else {
						System.err.println("Fail to parse: ");
						printTokens(arg);
					}
				} catch (ExprEvaluationException e1) {
					System.err.println("Fail to parse :");
					printTokens(arg);
				}

			}
		}

		@Override
		public Object eval() {

			Log.d("vortex","Function eval: "+type);

			Object argEval=null,result=null;
			List<Object> evalArgs = new ArrayList<Object>();
			VariableConfiguration al = gs.getVariableConfiguration();
			VarCache varCache = gs.getVariableCache();
			for (EvalExpr arg:args) {
				argEval= arg.eval();
				evalArgs.add(argEval);
			}



			//Now all arguments are evaluated. Execute function!
			switch (type) {
			case max:
				if (checkPreconditions(evalArgs,2,No_Null_Numeric))
					return Math.max((Double)evalArgs.get(0), (Double)evalArgs.get(1));
				break;
			case abs:
				if (checkPreconditions(evalArgs,1,No_Null_Numeric))
					return Math.abs((Double)evalArgs.get(0));
				break;
			case acos:
				if (checkPreconditions(evalArgs,1,No_Null_Numeric))
					return Math.acos((Double)evalArgs.get(0));
				break;
			case asin:
				if (checkPreconditions(evalArgs,1,No_Null_Numeric))
					return Math.asin((Double)evalArgs.get(0));
				break;
			case atan:
				if (checkPreconditions(evalArgs,1,No_Null_Numeric))
					return Math.atan((Double)evalArgs.get(0));
				break;
			case ceil:
				if (checkPreconditions(evalArgs,1,No_Null_Numeric))
					return Math.ceil((Double)evalArgs.get(0));
				break;
			case cos:
				if (checkPreconditions(evalArgs,1,No_Null_Numeric))
					return Math.cos((Double)evalArgs.get(0));
				break;
			case exp:
				if (checkPreconditions(evalArgs,1,No_Null_Numeric))
					return Math.exp((Double)evalArgs.get(0));
				break;
			case floor:
				if (checkPreconditions(evalArgs,1,No_Null_Numeric))
					return Math.floor((Double)evalArgs.get(0));
				break;
			case log:
				if (checkPreconditions(evalArgs,1,No_Null_Numeric))
					return Math.log((Double)evalArgs.get(0));
				break;
			case round:
				if (checkPreconditions(evalArgs,1,No_Null_Numeric))
					return Math.round((Double)evalArgs.get(0));
				break;
			case sin:
				if (checkPreconditions(evalArgs,1,No_Null_Numeric))
					return Math.sin((Double)evalArgs.get(0));
				break;
			case sqrt:
				if (checkPreconditions(evalArgs,1,No_Null_Numeric))
					return Math.sqrt((Double)evalArgs.get(0));
				break;
			case tan:
				if (checkPreconditions(evalArgs,1,No_Null_Numeric))
					return Math.tan((Double)evalArgs.get(0));
				break;
			case atan2:
				if (checkPreconditions(evalArgs,2,No_Null_Numeric))
					return Math.atan2((Double)evalArgs.get(0),(Double)evalArgs.get(1));
				break;
			case min:
				if (checkPreconditions(evalArgs,2,No_Null_Numeric))
					return Math.min((Double)evalArgs.get(0),(Double)evalArgs.get(1));
				break;

			case iff:
				if (checkPreconditions(evalArgs,3,NO_CHECK)) {
					if (evalArgs.get(0) instanceof Boolean) {
						if ((Boolean)evalArgs.get(0))
							return evalArgs.get(1);
						else
							return evalArgs.get(2);
					}
				}
				break;
			case unaryMinus:
				//Log.d("vortex","In function unaryminus");
				if (checkPreconditions(evalArgs,1,No_Null_Numeric)){
					Log.d("vortex","returning: "+ (-(Integer)evalArgs.get(0)));
					return -((Integer)evalArgs.get(0));
				}
				break;
			case not:
				//Log.d("vortex","in function not");
				if (checkPreconditions(evalArgs,1,No_Null_Boolean)) {
//					Log.d("vortex","evalArgs.get0 is "+evalArgs.get(0)+" type "+evalArgs.get(0).getClass().getSimpleName());
					return !((Boolean)evalArgs.get(0));

				}
				break;

			case historical:
				if (checkPreconditions(evalArgs,1,No_Null_Literal)) {
					Variable var = gs.getVariableCache().getVariable(evalArgs.get(0).toString());
					if (var != null) {
						String value = var.getHistoricalValue();
						Log.d("nils","Found historical value "+value+" for variable "+var.getLabel());
						if (value !=null && var.getType() == DataType.bool) {
							return value.equals("1");
						} else
							return value;
					} else {
						Log.e("vortex","Variable not found for literal: ["+evalArgs.get(0)+"]");
						o.addRow("");
						o.addRedText("Variable not found in historical: ["+evalArgs.get(0)+"]");
					}
				}
				break;
			case hasSameValueAsHistorical:
				String groupName = (String)evalArgs.get(0);
				String varName = (String)evalArgs.get(1);
				Log.d("vortex","in samevalueas historical with group ["+groupName+"] and variable ["+varName+"]");

				if (checkPreconditions(evalArgs,2,No_Null_Literal)) {
					Cursor c = gs.getDb().getAllVariablesForKeyMatchingGroupPrefixAndNamePostfix(gs.getCurrentKeyMap(),groupName,varName);
					Map<String,String> vars = new HashMap<String,String>();
					Map<String,String> histVars = new HashMap<String,String>();
					while (c.moveToNext())
						vars.put (c.getString(0),c.getString(1));
					c.close();
					Map<String, String> histKeyMap = Tools.copyKeyHash(gs.getCurrentKeyMap());
					histKeyMap.put("�r",Constants.HISTORICAL_TOKEN_IN_DATABASE);
					c = gs.getDb().getAllVariablesForKeyMatchingGroupPrefixAndNamePostfix(histKeyMap,groupName,varName);
					while (c.moveToNext())
						histVars.put (c.getString(0),c.getString(1));
					c.close();
					if (!vars.isEmpty()) {
						Log.d("vortex","Found candidates!");
						for (String name:vars.keySet()) {
							String value = vars.get(name);
							if (value!=null) {
								String historicalValue = histVars.get(name);
								if (historicalValue==null) {
									Log.d("vortex","hasSameValueAsHistorical returns false, since variable "+name+" has a value: "+value+" but no historical value.");
									o.addRow("hasSameValueAsHistorical returns false, since variable "+name+" has a value: "+value+" but no historical value.");
									return false;
								} else {
									if (historicalValue!=value) {
										Log.d("vortex","hasSameValueAsHistorical returns false, since variable "+name+" has a value: "+value+" that is not the same as the historical value: "+historicalValue);
										o.addRow("hasSameValueAsHistorical returns false, since variable "+name+" has a value: "+value+" that is not the same as the historical value: "+historicalValue);
										
										return false;
									}
								}
							}
						}
					}
				}
				return true;

				//Find all variables that matches groupName+____+varName in the cache.
				/*
					List<Variable> candidates = gs.getVariableCache().findVariablesBelongingToGroup(gs.getCurrentKeyMap(), groupName);
					if (candidates!=null) {
						varName = varName.toLowerCase();
						for (Variable v:candidates) {
							Log.d("vortex","checking "+v.getId()+" against "+varName);
							//Check thath they end with varName
							String id = v.getId().toLowerCase();
							if (id.endsWith(varName)) {
								Log.d("vortex","found "+v.getId());
								if (v.getValue()!=null) {
									if (v.getHistoricalValue()!=null)
										if (!v.getValue().equals(v.getHistoricalValue())) {
											Log.d("vortex","MISMATCH: "+v.getValue()+"\n"+v.getHistoricalValue());
											return false;
										}
								}
							}
						}
					}
					return true;
				 */

			case getCurrentYear:
				return Constants.getYear();
			case getCurrentMonth:
				return Constants.getMonth();
			case getCurrentDay:
				return Constants.getDayOfMonth();
			case getCurrentHour:
				return Constants.getHour();
			case getCurrentMinute:
				return Constants.getMinute();
			case getCurrentSecond:
				return Constants.getSecond();
			case getCurrentWeekNumber:
				return Constants.getWeekNumber();
			case getSweDate:
				return Constants.getSweDate();
			case getColumnValue:
				if (checkPreconditions(evalArgs,1,No_Null_Literal)) {
					if (currentKeyChain==null) {
						Log.e("vortex","Currentkeychain is missing in Expressor!");
						return null;
					}
					else {
						Log.d("vortex","keyhash "+currentKeyChain.toString());
						Log.d("votex","value for column is "+currentKeyChain.get(evalArgs.get(0)));
						return currentKeyChain.get(evalArgs.get(0));
					}
				}
				break;
			case photoExists:
				if (checkPreconditions(evalArgs,1,No_Null_Literal)) {
					System.out.println("Arg 0: "+evalArgs.get(0).toString());
					File dir = new File(Constants.PIC_ROOT_DIR);
					final String regexp = evalArgs.get(0).toString(); // needs to be final so the anonymous class can use it
					File[] matchingFiles = dir.listFiles(new FileFilter() {
						public boolean accept(File fileName) {
							//System.out.println("Testing "+fileName);
							return fileName.getName().matches(regexp);
						}
					});
					if (matchingFiles != null && matchingFiles.length != 0)
						return true;

				}
				return false;
				//Return 0 if one of the values are undefined.
			case sum:
				if (!checkPreconditions(evalArgs,-1,Null_Numeric))
					return 0;
				else {
					double sum = 0;
					for (Object arg : evalArgs) {
						if (arg!=null)
							sum += (Integer) arg;
					}
					return sum;
				}
			case concatenate:
				if (!checkPreconditions(evalArgs,-1,Null_Literal)) {
					return null;
				}
				else {
					StringBuilder stringSum= new StringBuilder();
					for (Object arg : evalArgs) {
						if (arg!=null)
							stringSum.append(arg);
					}
					return stringSum.toString();
				}
			case hasNullValue:
				return !checkPreconditions(evalArgs,-1,No_Null);
			case getDelytaArea:
				if (checkPreconditions(evalArgs,1,No_Null)) {
					Log.d("vortex", "running getDelytaArea function");
					DelyteManager dym = DelyteManager.getInstance();
					if (dym == null) {
						o.addRow("");
						o.addRedText("Cannot calculate delyta area...no provyta selected");
						return null;
					}
					float area = dym.getArea((Integer)evalArgs.get(0));
					if (area == 0) {
						Log.e("vortex","area 0 in getdelytaarea");
						o.addRow("Area 0");
						o.addRedText("Either Delyta "+evalArgs.get(0)+" does not exist or area is 0 (in function getDelytaArea)");
						return null;
					}
					return Float.toString(area/100);
				}
				return  null;
			case hasSame:
			case hasValue:
			case allHaveValue:

				String function = type.name();
				if (checkPreconditions(evalArgs,3,No_Null)) {

					List<List<String>> rows;
					//hasValue(pattern,op,constant)
					String pattern = (String) evalArgs.get(0);
					//Get all variables in Functional Group x.
					Table table = al.getTable();
					if (type == TokenType.hasSame)
						rows = table.getRowsContaining(VariableConfiguration.Col_Functional_Group, pattern);
					else
						rows = table.getRowsContaining(VariableConfiguration.Col_Variable_Name, pattern);
					if (rows == null || rows.size() == 0) {
						Log.e("vortex", "no variables found for filter " + pattern);
						return null;
					} else
						Log.d("vortex", "found " + (rows.size() - 1) + " variables for " + pattern);
					//Parse the expression. Find all references to Functional Group.
					//Each argument need to either exist or not exist.
					Map<String, String[]> values = new HashMap<String, String[]>();
					boolean allNull = true;
					Log.d("vortex","1st: "+evalArgs.get(0)+"op: "+evalArgs.get(1)+"constant: "+evalArgs.get(2));
					final String op = (String) evalArgs.get(1);
					final Object constant = evalArgs.get(2);
					boolean prep = false; EvalExpr fifo=null;
					for (List<String> row : rows) {
						Log.d("vortex", "Var name: " + al.getVarName(row));

						if (type == TokenType.hasValue ||
								type == TokenType.allHaveValue) {


							String formula = "[$" + al.getVarName(row) + op + constant+"]";
							Variable myVar = varCache.getVariable(al.getVarName(row));
							Boolean res=null;
							if (myVar != null && myVar.getValue() != null) {
								allNull = false;
								if (!prep) {
									try {
										List<Token> resulto = Expressor.tokenize(formula);
										Expressor.testTokens(resulto);
										fifo = Expressor.analyzeExpression(resulto);
									} catch (ExprEvaluationException e) {
										System.err.println("failed to analyze formula " + formula + " in hasValue/allHaveValue");
									}
									prep=true;
								}
								if (fifo!=null)
									res = Expressor.analyzeBooleanExpression(fifo);

								if (res == null) {
									Log.e("vortex", formula + " evaluates to null..something wrong");
								} else {

									if (!res && type == TokenType.allHaveValue) {
										o.addRow("");
										o.addYellowText("allHaveValue failed on expression " + formula);
										Log.e("vortex", "allHaveValue failed on " + formula);
										return false;
									} else {
										if (!res)
											continue;
										else {
											if (type == TokenType.hasValue) {
												o.addRow("");
												o.addGreenText("hasvalue succeeded on expression " + formula);
												Log.d("vortex", "hasvalue succeeded on expression " + formula);
												return (true);
											}
										}

									}
								}
							} else {
								Log.d("vortex", "null value...skipping");
							}

						} else if (type == TokenType.hasSame) {
							for (int i = 1; i < evalArgs.size(); i++) {
								String[] varNameA = al.getVarName(row).split(Constants.VariableSeparator);
								int size = varNameA.length;
								if (size < 3) {
									o.addRow("");
									o.addRedText("This variable has no Functional Group...cannot apply hasSame function. Variable id: " + varNameA);
									Log.e("vortex", "This is not a group variable...stopping.");
									return null;
								} else {
									String name = varNameA[size - 1];
									String art = varNameA[size - 2];
									String group = varNameA[0];
									Log.d("vortex", "name: " + name + " art: " + art + " group: " + group + " args[" + i + "]: " + evalArgs.get(i));
									if (name.equalsIgnoreCase((String) evalArgs.get(i))) {
										Log.d("vortex", "found varname. Adding " + art);
										Variable v = varCache.getVariable(al.getVarName(row));
										String varde = null;
										if (v == null) {
											Log.d("vortex", "var was null!");

										} else
											varde = v.getValue();
										String[] rezult;
										if (values.get(art) == null) {
											Log.d("vortex", "empty..creating new val arr");
											rezult = new String[evalArgs.size() - 1];
											values.put(art, rezult);
										} else
											rezult = values.get(art);
										rezult[i - 1] = varde;
										break;
									}
								}
							}
						}
					}
					if (type == TokenType.hasSame) {
						//now we should have an array containing all values for all variables.
						Log.d("vortex", "printing resulting map");
						for (String key : values.keySet()) {
							String vCompare = values.get(key)[0];
							for (int i = 1; i < evalArgs.size() - 1; i++) {
								String vz = values.get(key)[i];
								if (vCompare == null && vz == null || vCompare != null && vz != null)
									continue;
								else {
									o.addRow("hasSame difference detected for " + key + ". Stopping");
									Log.e("vortex", "Diffkey values for " + key + ": " + (vCompare == null ? "null" : vCompare) + " " + (vz == null ? "null" : vz));
									return false;
								}

							}
						}
						Log.d("vortex", "all values same. Success for hasSame!");
						return true;

					} else if (type == TokenType.hasValue) {
						//Hasvalue fails since none of the variables fullfilled the criteria
						o.addRow("");
						o.addYellowText("hasvalue failed to find any match");
						Log.e("vortex", "hasValue failed. No match found");
						return false;
					} else {
						if (!allNull) {
							o.addRow("");
							o.addYellowText("allHaveValue succeeded!");
							Log.e("vortex", "allHaveValue succeeded!");
							return true;
						} else {
							o.addRow("");
							o.addYellowText("allHaveValue failed - no values");
							Log.e("vortex", "allHaveValue failed on empty list");
							return false;
						}
					}
				}
				break;
			case has:
				Variable var = gs.getVariableCache().getVariable(evalArgs.get(0).toString());
				if (var != null) {
					String value = var.getValue();
					Log.d("nils","Found value "+value+" for variable "+var.getLabel()+" in has!");
					if (value== null)
						return false;
					else
						return true;
				} else {
					Log.e("vortex","Variable not found for literal: ["+evalArgs.get(0)+"]");
					o.addRow("");
					o.addRedText("Variable not found in historical: ["+evalArgs.get(0)+"]");
					return null;
				}
				//return checkPreconditions(evalArgs,1,No_Null);

			case hasSome:
			case hasMost:
			case hasAll:
				if (checkPreconditions(evalArgs,1,No_Null_Literal)) {
					Log.d("vortex", "HASx function");
					//Apply filter parameter <filter> on all variables in current table. Return those that match.
					float failC = 0;
					//If any of the variables matching filter doesn't have a value, return 0. Otherwise 1.
					List<List<String>> rows = al.getTable().getRowsContaining(VariableConfiguration.Col_Variable_Name, evalArgs.get(0).toString());
					if (rows == null || rows.size() == 0) {
						o.addRow("");
						o.addRedText("Filter returned empty list in HASx construction. Filter: " + type);
						o.addRow("");
						o.addRedText("Check your pattern: " + evalArgs.get(0));
						return null;
					}
					float rowC = rows.size();

					for (List<String> row : rows) {
						String value = varCache.getVariableValue(currentKeyChain, al.getVarName(row));
						if (value == null) {
							if (type == TokenType.hasAll) {
								o.addRow("");
								o.addYellowText("hasAll filter stopped on variable " + al.getVarName(row) + " that is missing a value");
								return false;
							} else
								failC++;
						} else if (type == TokenType.hasSome) {
							o.addRow("");
							o.addYellowText("hasSome filter succeeded on variable " + al.getVarName(row) + " that has value " + value);
							return true;
						}
					}
					if (failC == rowC && type == TokenType.hasSome) {
						o.addRow("");
						o.addYellowText("hasSome filter failed. No variables with values found for "+evalArgs.get(0));
						return false;
					}
					if (type == TokenType.hasAll) {
						o.addRow("");
						o.addYellowText("hasAll filter succeeded.");
						return true;
					}
					if (failC <= rowC / 2) {
						o.addRow("");
						o.addYellowText("hasMost filter succeeded. Filled in: " + (int) ((failC / rowC) * 100f) + "%");
						return true;
					}
					o.addRow("");
					o.addYellowText("hasMost filter failed. Not filled in: " + (int) ((failC / rowC) * 100f) + "%");
					return false;
				}
				break;


			default:
				System.err.println("Unimplemented function: "+type.toString());
				break;


			}
			return null;
		}
		/*
		private Boolean booleanValue(Object obj) {
			if (obj==null)
				return (Boolean)null;
			if (obj instanceof String) {
				if (obj.equals("true")||obj.equals("1")||obj.equals("1.0"))
					return true;
				if (obj.equals("false")||obj.equals("0")||obj.equals("0.0"))
					return false;



			} else if (obj instanceof Double) {
				if ((Double)obj==1.0d)
					return true;
				if ((Double)obj==0.0d)
					return false;

			}
			Log.e("vortex","no boolean value found for "+obj);
			o.addRedText("no boolean value found for "+obj);
			return null;
		}
		 */
		private boolean checkPreconditions(List<Object> evaluatedArgumentsList,int cardinality, int flags) {
			if ((flags==No_Null || flags== No_Null_Numeric || flags == No_Null_Literal || flags == No_Null_Boolean)
					&& evaluatedArgumentsList.contains(null)) {
				o.addRow("");
				o.addRedText("Argument in function '"+type.toString()+"' is null");
				Log.e("Vortex","Argument in function '"+type.toString()+"' is null");

				return false;
			}
			if (cardinality!=-1 && cardinality!=evaluatedArgumentsList.size()) {
				o.addRow("");
				o.addRedText("Too many or too few arguments for function '"+type.toString()+"'. Should be "+cardinality+" argument(s), not "+evaluatedArgumentsList.size()+"!");
				Log.e("Vortex","Too many or too few arguments for function '"+type.toString()+"'. Should be "+cardinality+" argument(s), not "+evaluatedArgumentsList.size()+"!");
				return false;
			}
			if (flags== No_Null_Numeric) {
				for (Object obj:evaluatedArgumentsList) {
					if ((obj instanceof Double)||(obj instanceof Integer)||!(obj instanceof Double)) {
						continue;
					} else {
						o.addRow("");
						o.addRedText("Type error. Non numeric argument for function '"+type.toString()+"'. Argument is a "+obj.getClass().getSimpleName());
						Log.e("Vortex","Type error. Non numeric argument for function '"+type.toString()+"'. Argument is a "+obj.getClass().getSimpleName());
						return false;
					}

				}

			}
			if (flags == No_Null_Literal) {
				for (Object obj:evaluatedArgumentsList) {
					if (!(obj instanceof String)) {
						o.addRow("");
						o.addRedText("Type error. Non literal argument for function '" + type.toString() + "'.");
						Log.e("Vortex","Type error. Non literal argument for function '"+type.toString()+"'.");
						return false;
					}
				}
			}
			if (flags == Null_Numeric) {
				for (Object obj:evaluatedArgumentsList) {
					if (obj !=null && !((obj instanceof Double)||!(obj instanceof Integer)||!(obj instanceof Double))) {
						o.addRow("");
						o.addRedText("Type error. Not null & not numeric argument for function '" + type.toString() + "'.");
						Log.e("Vortex","Type error. Not null & not numeric argument for function '"+type.toString()+"'.");
						return false;
					}
				}
			}
			if (flags == Null_Literal) {
				for (Object obj:evaluatedArgumentsList) {
					if (obj !=null && !(obj instanceof String)) {
						o.addRow("");
						o.addRedText("Type error. Not null & Non literal argument for function '" + type.toString() + "'.");
						Log.e("Vortex","Type error. Not null & Non literal argument for function '"+type.toString()+"'.");
						return false;
					}
				}
			}
			if (flags == No_Null_Boolean) {
				for (Object obj:evaluatedArgumentsList) {
					if (!(obj instanceof Boolean)) {
						Log.e("Vortex","Type error. Non boolean argument for function '"+type.toString()+"'.  Argument: "+obj.toString());
						o.addRow("");
						o.addRedText("Type error. Non boolean argument for function '" + type.toString() + "'. Argument: "+obj.toString());
						return false;
					}
				}
			}

			return true;

		}



		@Override
		public String toString() {
			return type.name()+"("+args.toString()+")";
		}
	}

	//Pass left to right. Rewrite expr in term of functions with arguments.
	public static EvalExpr analyzeExpression(List<Token> tokens) throws ExprEvaluationException {

		//empty expr
		if(tokens==null || tokens.isEmpty())
			return null;

		//Stack to implement binding order.
		Stack<Expr> s = new Stack<Expr>();

		ExpressionAnalyzer ef = new ExpressionAnalyzer(tokens.iterator());
		Expr e;
		int depth=0;
		int[] precedence=new int[1000], maxPrecedence =new int[1000];
		maxPrecedence[0]=-1;

		while (ef.hasNext()) {
			e = ef.next();

			//null if not an Expr.
			if (e==null)
				continue;
			//System.out.println("Next E: "+e.toString());
			//System.out.println("Expr: "+e.getClass().getCanonicalName());
			//subexpr.
			if (e instanceof Push) {
				depth++;
				precedence[depth] = 0;
				s.push(e);
			}
			else if (e instanceof Pop) {
				//calculate back to push marker.

				if (s.size()!=1)
					s.push(calcStack(s));
				//else
				//	System.out.println("Didnt calc: "+s.peek().toString());
				//System.out.println("Stack iz now "+s.toString());
				depth--;
				continue;
			}
			else if (e instanceof Operand) {
				//System.out.println("Op: "+e.type.toString()+" Pres: "+TokenType.valueOfIgnoreCase(e.toString()).prescedence());
				precedence[depth] = TokenType.valueOfIgnoreCase(e.toString()).prescedence();
				//as long as operator is binding stronger, keep pushing.
				if (precedence[depth] >= maxPrecedence[depth]) {
					//System.out.println("pres more than max: "+prescedence[depth]+","+maxPrescedence[depth]);
					maxPrecedence[depth]=precedence[depth];
					s.push(e);
				} else {
					//calculate the current stack.
					//System.out.println("pres below max: "+prescedence[depth]+","+maxPrescedence[depth]);
					s.push(calcStack(s));
					s.push(e);
					maxPrecedence[depth]=-1;
				}

			}

			else {

				s.push(e);
			}

		}
		if (s.size()>1)
			return calcStack(s);
		else {
			if (!s.isEmpty()&&s.peek() instanceof EvalExpr)
				return (EvalExpr)s.pop();
			else
				return null;
		}

	}

	static List<Expr> endres = new ArrayList<Expr>();



	private static void printResult() {
		for (Expr e:endres) {
			System.out.println(e.toString());
		}
	}


	private static boolean isLogicalOperand(TokenType x) {
		if (x==null)
			return false;
		String name = x.name();
		return ("AND".equalsIgnoreCase(name) || "OR".equalsIgnoreCase(name));
	}

	private static boolean isOperand(Token t) {
		if (t.type==TokenType.operand)
			return true;
		TokenType parent = t.type.getParent();
		if (parent==null)
			return false;
		return parent == TokenType.operand;
	}





	private static boolean  isFunction(TokenType t) {
		TokenType parent = t.getParent();
		return (parent !=null && parent.getParent() == TokenType.function);
	}




	//static int cc=0;
	private static void printTokens(List<Token> expr) {
		//System.out.print(cc+":");
		for(Token t:expr) {
			System.out.print(t.str+"["+t.type.name()+"]");

		}
		System.out.println();
	}

	private static EvalExpr calcStack(Stack<Expr> s) throws ExprEvaluationException{
		//printStack(s);
		int i = 0;
		//Contains result in the end.
		Expr rez = null;


		Expr op=null,arg1=null,arg2=null;
		//System.out.println("STACK: "+s.toString());

		while (!s.isEmpty()) {
			//If arg1 != null this is already a loop step and the result from prev is in arg1.
			//second argument should come next.
			if (!s.isEmpty()) {
				arg2 = s.pop();
				if (arg2 instanceof Push) {
					//System.out.println("Exiting on arg2 push with res: "+rez);
					//System.out.println("Stack is now:"+s.toString());
					return (EvalExpr)rez;
				}
			}
			//Op!

			if (!s.isEmpty()) {
				op = s.pop();
				if (op instanceof Push) {
					//System.out.println("Exiting on op push. Rez is "+((rez==null)?"null":rez.toString()));
					return (EvalExpr)arg2;
				}


			}
			if (rez==null&& s!=null && !s.isEmpty()) {
				//System.out.println("REZ is set ");
				rez=s.pop();
			}


			if (rez==null||arg2==null||op==null) {
				printfail(rez,arg2,op);
			} else {
				if (arg2.type==TokenType.operand) {
					//System.out.println("Swapping op and arg! ");
					Expr arg=op;
					op = arg2;
					arg2 = arg;
				}
				//System.out.println("(Arg1:) "+rez.toString()+" OP: "+op.toString()+" (Arg2:) "+arg2.toString());
				//Add new.
				if (op instanceof Operand) {
					try {
						rez = new Convoluted((EvalExpr)rez, (EvalExpr)arg2,(Operand)op);
					} catch (ClassCastException e) {
						printfail(rez,arg2,op);
					}
				}
				else
					printfail(rez,arg2,op);
			}
		}
		return (EvalExpr)rez;
	}




	private static void printfail(Expr rez, Expr arg2, Expr op) throws ExprEvaluationException {
		if (GlobalState.getInstance()!=null) {
			o = GlobalState.getInstance().getLogger();
			o.addRow("");
			o.addRedText("Missing or wrong parameters. This is likely caused by a misplaced paranthesis.");
			o.addRedText("arg1: "+rez);
			o.addRedText("arg2: "+arg2);
			o.addRedText("operator: "+op);
		}

		System.err.println("Missing or wrong parameters. This is likely caused by a misplaced paranthesis.");
		System.err.println("arg1: "+rez);
		System.err.println("arg2: "+arg2);
		System.err.println("operator: "+op);

		throw new ExprEvaluationException();

	}





}




