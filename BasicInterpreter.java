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
//TODO:	 	more builtins commands: DIM, DEF, RESTORE
//TODO:		optimization
//TODO:		robustness for basic dialects
//TODO:		DIM and array support
//FIXME:	repair "grammar style" and add more semantic checks

public class BasicInterpreter {
	// current token index
	int index;
	// all token
	String[] tokens;
	// holds variable state and type
	HashMap<String, BasicType> variables;
	// self defined functions(name->token index)
	HashMap<String, Integer> functions;
	// line to token index
	HashMap<String, Integer> lines;
	// stack used for subroutines and for loops
	Stack<BasicType> stack;
	//for DATA and READ
	ArrayList<BasicType> memory;
	//pointer within memory
	int memptr = 0;
	static final String COMMANDSEPERATOR = Tokenizer.COMMANDSEPERATOR;
	static Scanner prompt = new Scanner(System.in);
	Console console = System.console();
	
	public enum Errors{Syntax, Unknown, Unimplemented, Comparison, Handle, Jump, Mismatch, Quantity, Parameter};
	
	BasicInterpreter(String fileName) {
		lines = new HashMap<String, Integer>();
		try {
			tokens = new Tokenizer(fileName).run(lines);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		index = 0;
		variables = new HashMap<String, BasicType>();
		functions = new HashMap<String, Integer>();
		stack = new Stack<BasicType>();
		memory = new ArrayList<BasicType>();
	}	
	
	void lang_DIM() {
		error(Errors.Unimplemented, tokens[index-1]);
	}
	
	void lang_DEF() {
		//30 DEF FNE(X) = EXP(-X ^2 + 5)
		String functionName = tokens[index++];
		functions.put(functionName, index - 1);
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
		memory.removeAll(memory);
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
	
	String currentLine(int index) {
		int lastLine;
		//find position for first token in the current line and retrieve key from value 
		for(lastLine=index; !lines.containsValue(lastLine) && lastLine>0; --lastLine);
		for(Entry<String, Integer> i : lines.entrySet()) {
			if(i.getValue() == lastLine) {
				return i.getKey();
			}
		}	
		return "0";
	}
	
	 void lang_LET(String token) {
		 if(token.equals(COMMANDSEPERATOR)) {return;}
		 //for explicit LET usage
		 if(tokens[index - 1].equals("LET")) {token = tokens[index++];}
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
		 } else {
			 error(Errors.Unknown, token);
		 }
	 }
	
	void error(Errors err, String token) {
		String errorLine = currentLine(index);
			int start = lines.get(errorLine);
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
	     	for(int i = start; i <= index; ++i) {
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
	
	String shortener(BasicType var) {
		double value;
		if(var instanceof BasicDouble) {
			value = ((BasicDouble)var).value;
			if(Math.abs(value - (int)value) < 1e-9) {
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
				//expect(COMMANDSEPERATOR);
				/* SYNTAX HACK for 23-match.bas */
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
	
	void lang_FOR() {
        String name = tokens[index++];
        expect("=");
        variables.put(name, expression());
        expect("TO");
        double end = (expression()).value;
        double step = 1;
        if(at("STEP")) {
        	step = (expression()).value;
        }
       
        expect(COMMANDSEPERATOR);
        stack.push(new BasicDouble(step));
        stack.push(new BasicString(name));
        stack.push(new BasicDouble(end));
        stack.push(new BasicInt(index));
	}
	
	void lang_NEXT() { 
        if(!at(COMMANDSEPERATOR)) {
            ++index;
            expect(COMMANDSEPERATOR);
        }
        
        int element = stack.size() - 1;
        
        int index = ((BasicInt)stack.elementAt(element)).value;
        double end = (stack.elementAt(element - 1)).value;
        String name = ((BasicString) stack.elementAt(element - 2)).value;
        double step = (stack.elementAt(element - 3)).value;
        
        double value = ((variables.get(name)).value + step);
        variables.put(name, new BasicDouble(value));
        if(value >= end) {
            stack.pop();
            stack.pop();
            stack.pop();
            stack.pop();
        } else {
           this.index = index;
        }
	}
	
	boolean condition() {
        BasicType left = expression();
        String op = tokens[index++];
        BasicType right = expression();
        int compare = -1;
        if(left instanceof BasicString && right instanceof BasicString) {
        	compare = ((BasicString)left).value.compareTo(((BasicString)right).value);
        }
        if(op.equals("=")) {
            if(left instanceof BasicString) {return compare == 0;}
        	return left.value == right.value;
        }
        if(op.equals("<")) {
        	if(left instanceof BasicString) {return compare < 0;}
        	return left.value < right.value;
        }
        if(op.equals("<=")) {
        	if(left instanceof BasicString) {return compare <= 0;}
        	return left.value <= right.value;
        }
        if(op.equals(">")) {
        	if(left instanceof BasicString) {return compare > 0;}
        	return left.value > right.value;
        }
        if(op.equals(">=")) {
        	if(left instanceof BasicString) {return compare >= 0;}
        	return left.value >= right.value;
        }
        if(op.equals("<>")) {
        	if(left instanceof BasicString) {return compare != 0;}
        	return left.value != right.value;
        }
        error(Errors.Unknown, op);
        //never reached
        return false;
   }
	
	BasicType expression() {
        BasicType left = term();
        while(true) {
            if(at("+")) {
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
            if(at("*")) {
            	BasicType right = factor();
            	if(right instanceof BasicDouble) {
            		left = new BasicDouble(left.value * right.value);
            	} else if(right instanceof BasicDouble) {
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
        	//send parameter to stack (1)
        	while(true) {
        		stack.push(expression());
        		++cnt;
        		if(!at(",")) {
        			if(at(")")) {break;}
        		}
        	}
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
        if(Character.isDigit(token.charAt(0)) || token.charAt(0) == '.') {
            if(token.contains(".")) {
            	return new BasicDouble(Double.parseDouble(token), true);
            }
        	return new BasicDouble(Double.parseDouble(token));
        }
        if(Character.isAlphabetic(token.charAt(0))) {
            return variables.get(token);
        }
        if (token.charAt(0) == '\"') {
            return new BasicString(token.substring(1, token.length() - 1));
        }
        //number sign
        if (token.charAt(0) == '-') {
            BasicType val = expression();
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
		boolean cond = at("NOT")? !condition() : condition();   
	    while(true) {
	    	if(at("AND")) {
	    		cond = cond && condition();
	    		continue;
	    	}
	    	if(at("OR")) {
	    		cond = cond || condition();
	    		continue;
	    	}
	    	// optional keywords
	    	at("THEN");
	    	at("GOTO");
	    	break;
	    }  
	    String lineNumber = tokens[index++];
	    expect(":");
	    if(cond) {
	    	index = lines.get(lineNumber);
	    }
	}
	void lang_ON(){
		String name = tokens[index++];
		BasicType var = variables.get(name);
		int position = 0;
		boolean wasGosub = false;
		String lineNumber = null;
		int i;
		
		if(name.endsWith("$")) {
			error(Errors.Handle, name);
		}
		position = (int)var.value;
		
		if(at("GOSUB")) {
       		wasGosub = true;
       	} else if(!at("GOTO")) {error(Errors.Syntax, "GOSUB or GOTO");}
		for(i = 0; i < position; ++i){
   			if(at(COMMANDSEPERATOR)) {error(Errors.Jump, "" + position);}
			lineNumber = ""+(int)(expression()).value;
   			if(!at(",")) {error(Errors.Syntax, tokens[index]);}
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
		index = jumpTarget(lineNumber);
	}
	
	static String input() {
		/*console.readLine()*/
		return prompt.nextLine();
	}
	
	void lang_INPUT() {
		String name = tokens[index++];
		if(!name.startsWith("\"")) {
	    	output("? ");
	    	if(name.endsWith("$")) {
	    		variables.put(name, (new BasicString(input())));
	    	} else if(name.endsWith("%")) {
	    		variables.put(name, (new BasicInt(Integer.parseInt(input()))));
	    	} else {
	    		variables.put(name, (new BasicDouble(Double.parseDouble(input()))));
	    	}
	    } else {
	    	output(name);
	    }
	    
	    while(!at(COMMANDSEPERATOR)) {
	    	name = tokens[index++];
	    	if(name.endsWith("$")) {
	    		variables.put(name, (new BasicString(prompt.nextLine())));
	    	} else if(name.endsWith("%")) {
	    		variables.put(name, (new BasicInt(Integer.parseInt(prompt.nextLine()))));
	    	} else {
	    		variables.put(name, (new BasicDouble(Double.parseDouble(prompt.nextLine()))));
	    	}
	    	at(",");
	    } 
	    //prompt.close();
	}
	
	int jumpTarget(String lineNumber) {
		Integer jumpTarget = lines.get(lineNumber);
   		if(jumpTarget == null) {
   			error(Errors.Jump, lineNumber);
   		}
   		return jumpTarget;
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
    
	void run() {
		while(!tokens[index].equals("END")) {
			Method method = null;
			try {
			  method = getClass().getDeclaredMethod("lang_"+tokens[index++]);
			} catch (SecurityException e) {
			  e.printStackTrace();
			  System.exit(3);
			} catch(NoSuchMethodException e) {
				try {
					Class<?>[] arg = new Class[]{String.class};
					method = getClass().getDeclaredMethod("lang_LET", arg);
				} catch(NoSuchMethodException e1) {
					e1.printStackTrace();
				} catch(SecurityException e1) {
					e1.printStackTrace();
				}
			}
			try {
				if(method.getName().equals("lang_LET")) {
					method.invoke(this, tokens[index - 1]);
				}
				else {
					method.invoke(this);
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}
	
	static void usage() {
		output("usage: java BasicInterpreter <FILE>");
	}
	
	public static void main(String[] args) {
		if(args.length > 0) {
			new BasicInterpreter(args[0]).run();
		} else {BasicInterpreter.usage();}

	}
}
