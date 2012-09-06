import java.io.Console;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Stack;

/*
 * @author: Daniel Ölschlegel
 * @copyright: 2012/08
 * @version: 0.1
 * @license: 2-BSDL
 * */

//TODO:		file handling: OPEN, INPUT#, PRINT# , CLOSE
//TODO:		terminal
//TODO:		optimization
//TODO:		robustness for basic dialects
//FIXME:	repair "grammar style" and add more semantic checks

public class BasicInterpreter {
	// current token index
	int index;
	// all token
	String[] tokens;
	HashMap<Integer, Integer> tokenIndex;
	// holds variable state and type
	HashMap<String, BasicType> variables;
	// self defined functions(name->token index)
	HashMap<String, Integer> functions;
	// line to token index
	HashMap<String, Integer> lines;
	// stack used for functions and gosubs
	Stack<BasicType> stack;
	//for DATA and READ
	ArrayList<BasicType> memory;
	//pointer within memory structure
	int memptr = 0;
	//for dim
	HashMap<String, BasicType> arrays;
	HashMap<String, Boolean> arrayNames;
	
	static final String COMMANDSEPERATOR = Tokenizer.COMMANDSEPERATOR;
	static Scanner prompt = new Scanner(System.in);
	//Console console = System.console();
	
	public enum Errors{Syntax, Unknown, Unimplemented, Comparison, Handle, Jump, Mismatch, Quantity, Parameter};
	
	BasicInterpreter(String fileName) {
		lines = new HashMap<String, Integer>();
		tokenIndex = new HashMap<Integer, Integer>();
		try {
			tokens = new Tokenizer(fileName).run(lines, tokenIndex);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		index = 0;
		variables = new HashMap<String, BasicType>();
		functions = new HashMap<String, Integer>();
		stack = new Stack<BasicType>();
		memory = new ArrayList<BasicType>();
		arrays = new HashMap<String, BasicType>();
		arrayNames = new HashMap<String, Boolean>();
	}
	
	static String input() {
		/*console.readLine()*/
		return prompt.nextLine();
	}
	
	String parameterList() {
		if(!at("(") && !at("[")) {error(Errors.Syntax, tokens[index]);}
		 StringBuilder dim = new StringBuilder();
		 do {
			BasicType dimSize = expression();
			dim.append("" + (int)dimSize.value);
			if(at(",")) {
				dim.append(",");
				continue;
			}
		} while(!at(")") && !at("]"));
		return dim.toString();
	}
	
	void input_helper(String name) {
		if(!name.startsWith("\"")) {output("? ");};
    	if(name.endsWith("$")) {
    		if(arrayNames.containsKey(name) && (at("(") || at("["))) {
    			--index;
    			arrays.put(name + "_" + parameterList(), (new BasicString(input())));
    		} else {
    			variables.put(name, (new BasicString(input())));
    		} 
    	} else if(name.endsWith("%")) {
    		if(arrayNames.containsKey(name) && (at("(") || at("["))) {
    			--index;
    			arrays.put(name + "_" + parameterList(), (new BasicInt(Integer.parseInt(input()))));
    		} else {
    			variables.put(name, (new BasicInt(Integer.parseInt(input()))));
    		}
    	} else {
    		if(arrayNames.containsKey(name) && (at("(") || at("["))) {
    			--index;
    			arrays.put(name + "_" + parameterList(), (new BasicDouble(Double.parseDouble(input()))));
    		} else {
    			variables.put(name, (new BasicDouble(Double.parseDouble(input()))));
    		} 
    	}
	}
	
	void lang_INPUT() {
		String name = tokens[index++];
		if(name.startsWith("\"")) {output(name);}
		input_helper(name);
	    
	    while(!at(COMMANDSEPERATOR)) {
	    	name = tokens[index++];
	    	input_helper(name);
	    	at(",");
	    } 
	}
	
	int jumpTarget(String lineNumber) {
		Integer jumpTarget = lines.get(lineNumber);
   		if(jumpTarget == null) {
   			error(Errors.Jump, lineNumber);
   		}
   		return jumpTarget;
	}
	
	void allocator(String varName, Integer size[], BasicType basis) {
		//TODO: check if element start by 0 or 1
		int value[] = new int[size.length];
		int position = size.length - 1;
		while(true) {
			StringBuilder sB = new StringBuilder();
			int i;
			for(i = 0; i < size.length - 1; ++i) {
				sB.append((value[i] + 1) + ",");	
			}
			sB.append((value[i] + 1) + "");
			try {
				arrays.put(varName + "_" + sB.toString(), basis.getClass().newInstance());
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		
			if(++value[position] == size[position]) {
				boolean changed = false;
				for(int k = position; k < size.length; ++k) {value[k] = 0;}
				for(int k = position - 1; k >= 0; --k) {
					if(value[k] < size[k]) {
						++value[k]; 
						changed = true; 
						break;
					}
				}
				if(!changed) {return;}
				position = size.length - 1;
			} 
		}
	}
	
	int firstTokenInLine(int index) {
		//find position for first token in the current line
		int currentLine = tokenIndex.get(index);
		while(true) {
			int line = tokenIndex.get(--index);
			if(line < currentLine){break;}
		}
		return index + 1;
	}
//*****************************************************************************************	
	
	void lang_DIM() {
		do {
			String varName = tokens[index++];
			if(!at("(") && !at("[")) {error(Errors.Syntax, tokens[index]);}
			ArrayList<Integer> dims = new ArrayList<Integer>();
			do {
				BasicType dimSize = expression();
				dims.add((int)dimSize.value);
				if(at(",")) {
					continue;
				}
			} while(!at(")") && !at("]"));
			Integer[] param = (Integer[]) dims.toArray(new Integer[dims.size()]);
			if(varName.endsWith("%")) {
				allocator(varName, param, new BasicInt(0));
			} else if(varName.endsWith("$")) {
				allocator(varName, param, new BasicString(""));
			} else {allocator(varName, param, new BasicDouble(0.0));}
			arrayNames.put(varName, true);
			if(!at(",")) {
				if(!at(COMMANDSEPERATOR)) {
					error(Errors.Syntax, tokens[index]);
				} else {break;}
			} 
		} while(true);
	}
	
	void lang_DEF() {
		String functionName = tokens[index++];
		functions.put(functionName, index - 1);
		//aCall for a real function, first time is aCall false for initialization
		boolean aCall = false;
		expect("(");
		int parameterNumber = 0, cnt = 0; 
		do {
			String varName = tokens[index++];
			//dummy for existence
			if(!variables.containsKey(functionName + "_" + varName)) {
				variables.put(functionName + "_" + varName, new BasicType());
			} else {
				if(!aCall) {
					//get number of function parameter
					cnt = parameterNumber = ((BasicInt)stack.pop()).value;
					aCall = true;
				}
				variables.put(functionName + "_" + varName, stack.remove(stack.size() - cnt));
				--cnt;
			}
			if(at(",")) continue;
		} while(!at(")"));
		if(cnt != 0) {
			error(Errors.Parameter, "" + parameterNumber);
		}
		expect("=");
		if(aCall) {
			//TODO: Recursion loop checks required
			//write return value in stack
			stack.push(expression());
			return;
		} else {
			//token replace: function special format parameter to function block
			while(!at(COMMANDSEPERATOR)) {
				String varName = tokens[index++];
				if(variables.containsKey(functionName + "_" + varName)) {
					tokens[index - 1] = functionName + "_" + varName;
				}
			}
		}
	}
	
	void lang_DATA() {
		while(!at(COMMANDSEPERATOR)) {
            memory.add(factor());
            //skip parameter delimiter token
            at(",");
        }
	}
	
	void lang_RESTORE() {
		memptr = 0;
	}
	
	void lang_READ() {
		if(memptr >= memory.size()) {return;}
		while(!at(COMMANDSEPERATOR)) {
			if((tokens[index].endsWith("$") && !(memory.get(memptr) instanceof BasicString)
					|| (tokens[index].endsWith("%") && !(memory.get(memptr) instanceof BasicInt)))) {
				error(Errors.Syntax, tokens[index]);
			}
			variables.put(tokens[index++], memory.get(memptr++));
            //skip parameter delimiter token
            at(",");
        }
	}
	
	/*void lang_STOP() {
		output("BREAK IN ");
		if(lines.containsKey(index)) {output(""+lines.get(index));}
		output(" READY\n");
		++index;
	}*/
	
	void lang_GOTO() {
		String lineNumber = tokens[index++];
       	expect(COMMANDSEPERATOR);	
   		index = jumpTarget(lineNumber);
	}
	
	 void lang_LET(String token) {				 
		 if(token.equals(COMMANDSEPERATOR)) {return;}
		 //for explicit LET usage
		 if(tokens[index - 1].equals("LET")) {token = tokens[index++];}
		 if(token.equals("D"))
			 System.out.println("");
		 if(at("=")) {
			 BasicType exp = expression();
			 //force float type because of name suffix
			 if(exp instanceof BasicDouble && token.endsWith("%")) {
				 variables.put(token, new BasicInt(exp.value));
			 } else if(exp instanceof BasicInt && !token.endsWith("%") && !token.endsWith("$")) {
            	variables.put(token, new BasicDouble(((BasicInt)exp).value, true));
            } else {
            	if(exp instanceof BasicString && !token.endsWith("$")) {
            		error(Errors.Mismatch, token);
            	}
            	variables.put(token, exp);
            }
            expect(COMMANDSEPERATOR);
		 } else if(arrayNames.containsKey(token)) {
			 String varName = token;
			 String dim = parameterList();
			 expect("=");
			 arrays.put(varName + "_" + dim, expression());
			 expect(COMMANDSEPERATOR);
		 	} else {
		 		error(Errors.Unknown, token);
		 	}
	 }
	
	void error(Errors err, String token) {
		int errorLine = tokenIndex.get(index - 1);
     	String str;
     	
		switch(err) {
		case Syntax: 
			str = errorLine + ": Expected \'" +
				token + "\' founded \'" + tokens[index - 1] + "\' in ";
			break;
		case Unknown: 
			str = errorLine + ": Unknown command \'" +
				token + "\' in ";
			break;
		case Unimplemented:
			str = errorLine + ": Unimplemented keyword/function \'" +
				token + "\' in ";	
			break;
		case Comparison:
			str = errorLine + ": Unknown comparision operator \'" + 
				token + "\' in ";
			break;
		case Handle:
			str = errorLine + ": Can't handle \'" + token + "\' in ";
			break;
		case Jump:
			str = errorLine + ": Jump target " + token + 
				" not found in ";
			break;
		case Mismatch:
			str = errorLine + ": Mismatch type " + token + 
				" in ";
		case Quantity:
			str = errorLine + ": Illegal quantity " + token + 
				" in ";	
			break;	
		case Parameter:
			str = errorLine + ": Number of function: " + token + 
				" in ";	
			break;		
		default:
			str = "ups";		
		}
		output(str);
     	for(int i = firstTokenInLine(index - 1); i < index; ++i) {
     		output(tokens[i] + " ");
     	}
		System.exit(-err.ordinal());
	} 
	 	
	void expect(String token) {
		if(!at(token)) {
			error(Errors.Syntax, token);
		}
	} 
	
	boolean at(String token) {	
		if(tokens[index++].equals(token)) {
			return true;
		}
		index -= 1;
		return false;
	}
	
	boolean similar(double value) {
		return Math.abs(value - (int)value) < 1e-9;
	}
	
	String shortener(BasicType var) {
		double value;
		if(var instanceof BasicDouble) {
			value = ((BasicDouble)var).value;
			if(similar(value)) {
				return new String("" + (int) value);
			}
			return "" + value;
		} else if(var instanceof BasicInt) {
			return "" + ((BasicInt)var).value;
		} else {
			return ((BasicString)var).value;
		}
	}
	
	static void output(String str) {
		//console.printf(str);
		System.out.print(str);
	}
	
	void lang_PRINT() {
		if(!at(COMMANDSEPERATOR)) {
			output(shortener(expression()) + " ");
			boolean entry = true;
			while(entry) {
				entry = false;
				while(at(";") || at(",")) {
					if(at(COMMANDSEPERATOR)) {return;}
					output(shortener(expression()) + " ");
				}
				if(!at(COMMANDSEPERATOR)) {
					output(shortener(expression()) + " ");
					entry = true;
				}
			}
		}
		output("\n");
	}
	
	void lang_REM() {	
		++index;
		expect(COMMANDSEPERATOR);
	}
	
	void lang_FOR() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        String cntVar = tokens[index++];
        expect("=");
        variables.put(cntVar, expression());
        expect("TO");
        double end = (expression()).value;
        double step;
        if(at("STEP")) {
        	step = (expression()).value;
        } else {step = 1;}
       
        expect(COMMANDSEPERATOR);
        int jumpBack = this.index;
        double value = ((variables.get(cntVar)).value);
        //DONT COMPARE FLOATS, ups!!
        for(; value != end + step; value += step) {
        	this.index = jumpBack;
        	while(!at("NEXT")) {run();}
        	if(!at(COMMANDSEPERATOR)) {	
        		if(!at(cntVar)) {error(Errors.Handle, tokens[this.index]);}
    		} 	
        }
        variables.put(cntVar, new BasicDouble(value));
	}
	
	BasicType bool(boolean bool) {
		return new BasicInt(bool==true? 1 : 0);
	}
	
	BasicType condition(BasicType left, String op, BasicType right) {
		int compare = -1;
        if(left instanceof BasicString && right instanceof BasicString) {
        	compare = ((BasicString)left).value.compareTo(((BasicString)right).value);
        }
        if(op.equals("=")) {
            if(left instanceof BasicString) {return bool(compare == 0);}
        	return bool(left.value == right.value);
        }
        if(op.equals("<")) {
        	if(left instanceof BasicString) {return bool(compare < 0);}
        	return bool(left.value < right.value);
        }
        if(op.equals("<=")) {
        	if(left instanceof BasicString) {return bool(compare <= 0);}
        	return bool(left.value <= right.value);
        }
        if(op.equals(">")) {
        	if(left instanceof BasicString) {return bool(compare > 0);}
        	return bool(left.value > right.value);
        }
        if(op.equals(">=")) {
        	if(left instanceof BasicString) {return bool(compare >= 0);}
        	return bool(left.value >= right.value);
        }
        if(op.equals("<>")) {
        	if(left instanceof BasicString) {return bool(compare != 0);}
        	return bool(left.value != right.value);
        }
        error(Errors.Unknown, op);
        //never reached
        return bool(false);
   }
	
	BasicType expression() {
        BasicType left = term();
        while(true) {
        	if("<\\>\\<>\\>=\\<=\\=".contains(tokens[index])) {
        		left = condition(left, tokens[index++] ,term());
        	} else  if(at("+")) {
            	if(left instanceof BasicString) {
            		left = new BasicString(((BasicString)left).value + ((BasicString)term()).value);
            	} else {
            		BasicType right = term();
            		if(right instanceof BasicDouble || left instanceof BasicDouble) {
            			left = new BasicDouble(left.value + right.value);
            		} else {
            			left =  new BasicInt(left.value + right.value);
            		}
            	}
            } else if(at("-")) {
            	BasicType right = term();
            	if(right instanceof BasicDouble || left instanceof BasicDouble) {
            		left = new BasicDouble(left.value - right.value);
            	} else {
            		left = new BasicInt(left.value - right.value);
            	}
            } else {
              	break;
              } 
        }
        return left;
	}
	
	//TODO: DIVISION BY ZERO CHECK
    BasicType term() {
        BasicType left = factor();
        while(true) {
        	if(at("OR") || at("AND")) {
            	BasicType right = factor();
            	if(left instanceof BasicDouble) {
            		if(similar(left.value) && !((BasicDouble)left).explicit) {
            			left = new BasicInt(left.value);
            		}
            	} 
        	    if(similar(right.value) && !((BasicDouble)right).explicit) {
	        			right = new BasicInt(right.value);
	        	}
            	if(!(left instanceof BasicInt) && !(right instanceof BasicInt)) {
            		error(Errors.Mismatch, tokens[index - 1]);
            	}
            	if(tokens[index - 2].equals("AND")) {
            		left = new BasicInt((int)left.value & (int)right.value);
            	} else {
            		left = new BasicInt((int)left.value | (int)right.value);
            	}
        	} else if(at("^")) {
            	BasicType right = factor();
            	if(right instanceof BasicDouble) {
            		left = new BasicDouble(Math.pow(left.value,  right.value));
            	} else if(right instanceof BasicInt) {
            		left = new BasicInt(Math.pow(left.value,  right.value));
            	}
        	} else if(at("*")) {
            	BasicType right = factor();
            	if(right instanceof BasicDouble) {
            		left = new BasicDouble(left.value * right.value);
            	} else if(right instanceof BasicInt) {
            		left = new BasicInt(left.value * right.value);
            	}
            } else if(at("/")) {
            	BasicType right = factor();
            	//type conversion
            	if(left instanceof BasicInt && right instanceof BasicDouble) {
            		if(((BasicDouble)right).explicit) {
            			right = new BasicInt(((BasicDouble)right).value);
            		} else {
            			left = new BasicDouble(((BasicInt)left).value);
            		}
            	} else if(right instanceof BasicInt && left instanceof BasicDouble) {
            		if(((BasicDouble)right).explicit) {
            			left = new BasicInt(((BasicDouble)left).value);
            		} else {
            			right = new BasicDouble(((BasicDouble)right).value);
            		} 
            	}
            	if(right instanceof BasicDouble) {
            		left = new BasicDouble(left.value / right.value);
            	} else if(right instanceof BasicInt) {
            		left = new BasicInt(((BasicInt)left).value / ((BasicInt)right).value);
            	}
            } else {
                break;
            }
        }
        return left;
    }

    BasicType factor() {
        String token = tokens[index++];
        Builtins builtins = new Builtins(this);
        BasicType ret = builtins.builtins(token);
        if(ret != null) {
        	return ret;
        }
        //self defined function
        if(functions.containsKey(token)) {
        	int cnt = 0;
        	expect("(");
        	//send parameter to stack
        	while(true) {
        		stack.push(expression());
        		++cnt;
        		if(!at(",")) {
        			if(at(")")) {break;}
        		}
        	}
        	//number of function parameter
        	stack.push(new BasicInt(cnt));
        	int jumpBack = index;
        	index = functions.get(token);
        	lang_DEF();
        	//jump back
        	index = jumpBack; 
        	return stack.pop();
        }
        if(token.equals("(")) {
            BasicType exp = expression();
            expect(")");
            return exp;
        }
        if(token.equals("NOT")) {
        	BasicInt exp = (BasicInt)expression();
        	exp.value = exp.value == 1? 0 : 1;
        	return exp;
        }
        if(Character.isDigit(token.charAt(0)) || token.charAt(0) == '.') {
            if(token.contains(".")) {
            	return new BasicDouble(Double.parseDouble(token), true);
            }
        	return new BasicDouble(Double.parseDouble(token));
        }
        if(Character.isAlphabetic(token.charAt(0))) {
        	//array
            if(arrayNames.containsKey(token) && (at("[") || at("("))) {        	
	   			 	--index;
            		String dim = parameterList();
	   			    return arrays.get(token + "_" + dim.toString());	
            	} else {
            		if(!variables.containsKey(token)) {
            			if(token.endsWith("$")) {
            				variables.put(token, new BasicString(""));
            			} else if(token.endsWith("%")) {
            				variables.put(token, new BasicInt(0));
            			} else {
            				variables.put(token, new BasicDouble(0.0));
            			}
            		}
            		return variables.get(token);
            	}
        }
        if(token.charAt(0) == '\"') {
            return new BasicString(token.substring(1, token.length() - 1));
        }
        //number sign
        if(token.equals("-")) {
            BasicType val = factor();
            if(val instanceof BasicDouble) { 
            	((BasicDouble)val).value *= -1; 
            	val.value *= -1; //unnecessary stuff
            } else if(val instanceof BasicInt) {
            	((BasicInt)val).value *= -1; 
            	val.value *= -1; //unnecessary stuff
            } else {return null;}
            return val; 
        } 
        return null;
    }
    
	void lang_IF() {
		boolean cond = ((BasicInt)expression()).value == 1? true : false;
		while(true) {
	    	if(at("AND")) {
	    		cond = cond && ((BasicInt)expression()).value == 1? true : false;
	    		continue;
	    	}
	    	if(at("OR")) {
	    		cond = cond || ((BasicInt)expression()).value == 1? true : false;
	    		continue;
	    	}	    	
	    	if(at("THEN")) {
	    		if(!Character.isDigit(tokens[index].charAt(0))) {
		    		if(!cond) {
		    			while(!at(COMMANDSEPERATOR)) {++index;};
		    		} 
	    		} else {
	    			if(cond) {lang_GOTO();}
	    			else {++index;}
	    		} 
	    	}
	    	return;
	    }  
	}
	
	void lang_ON(){
		String varName = tokens[index++];
		BasicType var = variables.get(varName);
		int position = 0;
		boolean wasGosub = false;
		String lineNumber = null;
		int i;
		
		if(varName.endsWith("$")) {
			error(Errors.Handle, varName);
		}
		position = (int)var.value;
		
		if(at("GOSUB")) {
       		wasGosub = true;
       	} else if(!at("GOTO")) {error(Errors.Syntax, "GOSUB or GOTO");}
		for(i = 0; i < position; ++i){
   			if(at(COMMANDSEPERATOR)) {error(Errors.Jump, "" + position);}
			lineNumber = ""+(int)(expression()).value;
   			if(!at(",") && !at(COMMANDSEPERATOR)) {error(Errors.Syntax, tokens[index]);}
   		}
		//get end of statement
		while(!at(COMMANDSEPERATOR)) {
			expression();
			if(at(",")) { continue;}
		}
		if(i == position) {
			if(wasGosub) {
				stack.push(new BasicInt(index));
			}
		} else {error(Errors.Syntax, ",");}
		//no valid input, jump target are from index 1 till number elements in list
		if(var.value != 0) {
			index = jumpTarget(lineNumber);
		}
	}
	
	
   
	void lang_GOSUB() {
	    String lineNumber = tokens[index++];
	    expect(":");
	    stack.push(new BasicInt(index));
   		index = jumpTarget(lineNumber);
	}

    void lang_RETURN() {
    	expect(":");
    	index = ((BasicInt)stack.pop()).value;
    }
    
    void run() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    	Method method = null;
		try {
		  method = getClass().getDeclaredMethod("lang_" + tokens[index++]);
		} catch (SecurityException e) {
		  e.printStackTrace();
		  System.exit(3);
		} catch(NoSuchMethodException e) {
			Class<?>[] arg = new Class[]{String.class};
			method = getClass().getDeclaredMethod("lang_LET", arg);
		}
		
		if(method.getName().equals("lang_LET")) {
				method.invoke(this, tokens[index - 1]);
		} else {
				method.invoke(this);
		}
    }
    
	void start() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		while(!tokens[index].equals("END")) {
			run();
		}
	}
	
	static void usage() {
		output("usage: java BasicInterpreter <FILE>");
	}
	
	public static void main(String[] args) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if(args.length > 0) {
			new BasicInterpreter(args[0]).start();
		} else {BasicInterpreter.usage();}

	}
}
