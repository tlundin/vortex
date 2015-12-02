/**
 * 
 */
package com.teraim.vortex.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import com.teraim.vortex.dynamic.blocks.Expr;



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
	enum TokenType {
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
		allHaveValue(booleanFunction,-1),
		iff(valueFunction,3),
		getColumnValue(valueFunction,1),
		historical(valueFunction,1),
		getCurrentYear(valueFunction,0),
		getCurrentMonth(valueFunction,0),
		getCurrentDay(valueFunction,0),
		getCurrentHour(valueFunction,0),
		getCurrentMinute(valueFunction,0),
		getCurrentSecond(valueFunction,0),
		getCurrentWeekNumber(valueFunction,0),
		getSweDate(valueFunction,0),
		sum(valueFunction,-1),
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
		and(operand,10),
		or(operand,5),
		add(operand,5),
		subtract(operand,5),
		multiply(operand,10),
		divide(operand,10),
		gte(operand,15),
		lte(operand,15),
		eq(operand,15),
		neq(operand,15),
		gt(operand,15),
		lt(operand,15),


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

	
	
	
	
	
	
	public static Expr analyze(String context) {
		// TODO Auto-generated method stub
		return null;
	}

	
	
	
	
	
	
	/**
	 * Class Token
	 * @author Terje
	 * Expressions are made up of Tokens. Tokens are for instance Numbers, Literals, Functions.
	 *  
	 */
	public static class Token {
		public String str;
		public TokenType type;
		public Token(String raw,TokenType t) {
			str=raw;
			type=t;
		}
	}

	//Exception for Evaluation failures. 

	private static class ExprEvaluationException extends Exception {

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

		public Object next() throws ExprEvaluationException {
			curr=null;
			while (mIterator.hasNext()) {
				Token t = mIterator.next();
				if (t.type==TokenType.text)
					return t.str;
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
							return ret.eval();
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
				if ((t == TokenType.operand || (t == TokenType.none&&(i==1||result.get(result.size()-1).type==TokenType.leftparenthesis))) && (c =='-') && (i+1)!=formula.length() && (!Character.isWhitespace(formula.charAt(i+1)))) {
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
		if (testTokens(result))
			return result;
		else {
			return null;
		}
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
							System.err.println("Syntax Error: Function "+prev.str+" does not exist!");
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
							System.err.println("The token "+prev.str+" is used as function, but is in fact a "+prev.type);
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
	abstract static class Expr {
		TokenType type=null;
	}

	abstract static class EvalExpr extends Expr {
		abstract Object eval();
	}

	private static class Atom extends EvalExpr {
		final Token myToken;
		public Atom(Token t) {
			myToken = t;
			type = t.type;
		}
		@Override
		public String toString() {
			if (myToken!=null)
				return myToken.str;
			else
				return null;
		}

		public Object eval() {
			switch(type) {
			case variable:
				if (myToken.str.equals("a")) {
					System.out.println("Variable value: a = "+3.1);
					return 3.1d;
				}
				if (myToken.str.equals("b")) {
					System.out.println("Variable value: b = "+4.2);
					return 4.2d;
				}
				if (myToken.str.equals("c")) {
					System.out.println("Variable value: c = "+5.3);
					return 5.3d;
				}
				System.out.println("Variable '"+this.toString()+"' does not have a value");
				return null;				
			case number:
				if (myToken !=null && myToken.str!=null) {
					//System.out.println("Numeric value: "+myToken.str);
					return Double.parseDouble(myToken.str);
				}
				else {
					System.err.println("Numeric value was null");
					return null;
				}
			case literal:
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
			myToken = t;
			type = t.type;
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
			Object arg1v = arg1.eval();
			Object arg2v = arg2.eval();
			if (arg1v==null || arg2v==null)
				return null;
			boolean isDoubleOperator = arg1v instanceof Double && arg2v instanceof Double;
			boolean isBooleanOperator = arg1v instanceof Boolean && arg2v instanceof Boolean;

			if (isDoubleOperator == false && isBooleanOperator == false) {
				System.err.println("Argument types are wrong in operand: "+arg1v.getClass().getSimpleName()+","+arg2v.getClass().getSimpleName());
				return null;
			}
			//Requires Double arguments.
			if (isDoubleOperator) {
				Double arg1F,arg2F;
				Object res=null;

				arg1F=(Double)arg1v;
				arg2F=(Double)arg2v;
				String opS =operator.myToken.str;
				if (opS!=null) {
					TokenType op = TokenType.valueOf(opS);
					switch (op) {
					//works for Double and literal (string).
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
						break;
					case neq:
						res = arg1F.compareTo(arg2F)==-1;
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
						break;
					}
				} else
					System.err.println("Unsupported operand: "+operator.type);

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
						//System.err.println("Gets to or");
						res = (arg1B||arg2B);
						break;
					case and:
						//System.err.println("Gets to and");
						res = (arg1B&&arg2B);
						break;
					default:
						System.err.println("Unsupported operand: "+op);
						break;
					}

				}
				return res;
			}
			return null;
		}

	}





	private static class Push extends Expr {
	}
	private static class Pop extends Expr {
	}


	//cut out function from full expression.
	private static class Function extends EvalExpr {
		//try to build a function from the tokens in the beg. of the given token stream.

		private static final int No_Null = 1;
		private static final int No_Null_Numeric=2;
		private static final int NO_CHECK = 3;

		private List<EvalExpr> args = new ArrayList<EvalExpr>();

		public Function(TokenType type, Iterator<Token> it) {
			//iterator reaches end?

			this.type = type;
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

			Object argEval=null,result=null;
			List<Object> evalL = new ArrayList<Object>();
			for (EvalExpr arg:args) {
				argEval= arg.eval();
				evalL.add(argEval);
			}
			//Now all arguments are evaluated. Execute function!
			switch (type) {
			case max:
				if (checkPreconditions(evalL,2,No_Null_Numeric))
					return Math.max((Double)evalL.get(0), (Double)evalL.get(1));
				break;
			case abs:
				if (checkPreconditions(evalL,1,No_Null_Numeric))
					return Math.abs((Double)evalL.get(0));
				break;
			case acos:
				if (checkPreconditions(evalL,1,No_Null_Numeric))
					return Math.acos((Double)evalL.get(0));
				break;
			case asin:
				if (checkPreconditions(evalL,1,No_Null_Numeric))
					return Math.asin((Double)evalL.get(0));
				break;
			case atan:
				if (checkPreconditions(evalL,1,No_Null_Numeric))
					return Math.atan((Double)evalL.get(0));
				break;
			case ceil:
				if (checkPreconditions(evalL,1,No_Null_Numeric))
					return Math.ceil((Double)evalL.get(0));
				break;
			case cos:
				if (checkPreconditions(evalL,1,No_Null_Numeric))
					return Math.cos((Double)evalL.get(0));
				break;
			case exp:
				if (checkPreconditions(evalL,1,No_Null_Numeric))
					return Math.exp((Double)evalL.get(0));
				break;
			case floor:
				if (checkPreconditions(evalL,1,No_Null_Numeric))
					return Math.floor((Double)evalL.get(0));
				break;
			case log:
				if (checkPreconditions(evalL,1,No_Null_Numeric))
					return Math.log((Double)evalL.get(0));
				break;
			case round:
				if (checkPreconditions(evalL,1,No_Null_Numeric))
					return Math.round((Double)evalL.get(0));
				break;
			case sin:
				if (checkPreconditions(evalL,1,No_Null_Numeric))
					return Math.sin((Double)evalL.get(0));
				break;
			case sqrt:
				if (checkPreconditions(evalL,1,No_Null_Numeric))
					return Math.sqrt((Double)evalL.get(0));
				break;
			case tan:
				if (checkPreconditions(evalL,1,No_Null_Numeric))
					return Math.tan((Double)evalL.get(0));
				break;
			case atan2:
				if (checkPreconditions(evalL,2,No_Null_Numeric))
					return Math.atan2((Double)evalL.get(0),(Double)evalL.get(1));
				break;
			case min:
				if (checkPreconditions(evalL,2,No_Null_Numeric))
					return Math.min((Double)evalL.get(0),(Double)evalL.get(1));
				break;

			case iff:
				if (checkPreconditions(evalL,3,NO_CHECK)) {
					if (evalL.get(0) instanceof Boolean) {
						if ((Boolean)evalL.get(0))
							return evalL.get(1);
						else
							return evalL.get(2);
					}
				}
				break;
			case unaryMinus:
				if (checkPreconditions(evalL,1,No_Null_Numeric))
					return -((Double)evalL.get(0));
				break;
			default:
				System.err.println("Unimplemented function: "+type.toString());
				break;


			}


			return null;
		}		


		private boolean checkPreconditions(List<Object> evaluatedArgumentsList,int cardinality, int flags) {
			if ((flags==No_Null || flags== No_Null_Numeric) && evaluatedArgumentsList.contains(null)) {
				System.err.println("Argument in function '"+type.toString()+"' is null");
				return false;
			}
			if (cardinality!=-1 && cardinality!=evaluatedArgumentsList.size()) {
				System.err.println("Too many or too few arguments for function '"+type.toString()+"'. Should be "+cardinality+" argument(s), not "+evaluatedArgumentsList.size()+"!");
				return false;
			}
			if (flags== No_Null_Numeric) {
				for (Object o:evaluatedArgumentsList) {
					if (!(o instanceof Double)) {
						System.err.println("Type error. Non numeric argument for function '"+type.toString()+"'.");
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

		int precedence;

		//empty expr
		if(tokens==null || tokens.isEmpty())
			return null;

		//Stack to implement binding order.
		Stack<Expr> s = new Stack<Expr>();

		ExpressionAnalyzer ef = new ExpressionAnalyzer(tokens.iterator());
		Expr e;		
		int depth=0;
		int[] prescedence=new int[1000],maxPrescedence=new int[1000];
		maxPrescedence[0]=-1;

		while (ef.hasNext()) {
			e = ef.next();
			
			//null if not an Expr.
			if (e==null)
				continue;
			System.out.println("Next E: "+e.toString());
			//System.out.println("Expr: "+e);
			//subexpr.
			if (e instanceof Push) {
				depth++;
				prescedence[depth] = 0;
				s.push(e);
			}
			else if (e instanceof Pop) {
				//calculate back to push marker.
				
				if (s.size()!=1)
					s.push(calcStack(s));
				else
					System.out.println("Didnt calc: "+s.peek().toString());
				//System.out.println("Stack iz now "+s.toString());
				depth--;
				continue;
			}
			else if (e instanceof Operand) {
				//System.out.println("Op: "+e.type.toString()+" Pres: "+TokenType.valueOf(e.toString()).prescedence());
				prescedence[depth] = TokenType.valueOfIgnoreCase(e.toString()).prescedence();
				//as long as operator is binding stronger, keep pushing.
				if (prescedence[depth] >= maxPrescedence[depth]) {
					//System.out.println("pres more than max: "+prescedence[depth]+","+maxPrescedence[depth]);
					maxPrescedence[depth]=prescedence[depth];
					s.push(e);
				} else {
					//calculate the current stack.
					//System.out.println("pres below max: "+prescedence[depth]+","+maxPrescedence[depth]);
					s.push(calcStack(s));
					s.push(e);
					maxPrescedence[depth]=-1;
				}

			}

			else
				s.push(e);


		}
		if (s.size()>1)
			return calcStack(s);
		else {
			if (s.peek() instanceof EvalExpr)
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
					System.out.println("Exiting on op push. Rez is "+((rez==null)?"null":rez.toString()));
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
				if (op instanceof Operand)
					rez = new Convoluted((EvalExpr)rez, (EvalExpr)arg2,(Operand)op);
				else
					printfail(rez,arg2,op);
			}
		}
		return (EvalExpr)rez;
	}




	private static void printfail(Expr rez, Expr arg2, Expr op) throws ExprEvaluationException {
		System.err.println("Missing or wrong parameters. This is likely caused by a misplaced paranthesis.");
		System.err.println("arg1: "+rez);
		System.err.println("arg2: "+arg2);
		System.err.println("operator: "+op);
		throw new ExprEvaluationException();
		
	}


	


}




