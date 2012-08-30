import java.io.BufferedReader;
import java.io.Console;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/*
 * @author: Daniel Ölschlegel
 * @copyright: 2012/08
 * @version: 0.1
 * @license: 2-BSDL
 * */

public class Tokenizer {
	String fileName;
	Console console = System.console();
	// Is AND,OR, XOR operator definition really necessary?
	static final Pattern TOKENS = Pattern.compile("(?<=REM).*|\\.?\\d+|\\w+[\\$|%]?|[():,;=+\\-*/]|<[=>]?|>=?|\"[^\"]*\"");
	
	public static final String COMMANDSEPERATOR = ":";
		
	Tokenizer(String fileName) {
		this.fileName = fileName;
	}
	
	void output(String str) {
		//console.printf(shortener(expression()) + " ");
		System.err.print(str);
	}
	
	public String[] splitIntoToken(String[] lines, HashMap<String, Integer> lineNumbers) {
		Stack<String> tokenList = new Stack<String>();
		String token = null;
		int cnt = 0;
		//add dummy for peek
		tokenList.push("");
		for(String line: lines) {
			++cnt;
			if(line.equals("")) {continue;}
			Matcher match = TOKENS.matcher(line);
			// first element is line number
			match.find();
			try {
				token = match.group().trim();
				Integer.parseInt(token);
				if(lineNumbers.containsKey(token)) {
					output(cnt + ": Duplicated linenumber " + token);
					System.exit(-2);
				}
				lineNumbers.put(token, tokenList.size() - 1);
			} catch(NumberFormatException e) {
				//no line number detected
				tokenList.push(token.toUpperCase());
			}
			
			//for case insensitity
			while(match.find()) {
				token = match.group().trim();
				if(!token.equals("") && token.charAt(0) != '\"') {
					//workaround for space between two comparator symbols
					if((tokenList.peek().equals("<") || tokenList.peek().equals(">")) && token.equals("=")) {
						token = tokenList.pop() + token;
					} else if(tokenList.peek().equals("<") && token.equals(">")) {
						token = tokenList.pop() + token;	  
					}
					token = token.toUpperCase();
				}
				tokenList.push(token);
	        }
			//every command is separated by delimiter
			if(!tokenList.lastElement().endsWith(COMMANDSEPERATOR)) {
				tokenList.push(COMMANDSEPERATOR);
			}
				
		}
		//final line 
		tokenList.add("END");
		//remove first dummy
		tokenList.remove(0);
		return (String[]) tokenList.toArray(new String[tokenList.size()]);
	}
	
	String[] readFromFile(String fileName) throws IOException, FileNotFoundException {
		ArrayList<String> content = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(fileName))));
		String str;
		do {
			str = br.readLine();
			if(str != null) {
				content.add(str);
			}	
		} while(str != null);
		br.close();
		return (String[]) content.toArray(new String[content.size()]);
	}
	
	String[] run(HashMap<String, Integer> lineNumber) throws IOException {	
		String[] lines = null;
		
		lines = readFromFile(fileName);
		return splitIntoToken(lines, lineNumber);
	}
	
}
