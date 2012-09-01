
/*
 * @author: Daniel Ölschlegel
 * @copyright: 2012/08
 * @version: 0.1
 * @license: 2-BSDL
 * */

public class Builtins {
	private BasicInterpreter basic;
	
	public Builtins(BasicInterpreter basic) {
		this.basic = basic;
	}
	
	public BasicType builtins(String token) {
		if(token.equals("STR$")) {
	        return new BasicString(""+(basic.expression()).value);
	    }
		if(token.equals("VAL")) {
			BasicType ret = basic.expression();
	        return new BasicDouble(Double.parseDouble(((BasicString)ret).value), true);
	    	
		}
		if(token.equals("INT")) {
	        return new BasicInt(Math.ceil((basic.expression()).value));
	    }
		if(token.equals("SGN")) {
	        return new BasicInt(Math.signum((basic.expression()).value)); 
	    }
		if(token.equals("ASC")) {
	        return new BasicInt(((BasicString)basic.expression()).value.charAt(0));
	    }
		if(token.equals("LEN")) {
			return new BasicInt(((BasicString)basic.factor()).value.length());
	    }
		if(token.equals("LEFT$")) {
			basic.expect("(");
			String str = ((BasicString)basic.expression()).value;
			basic.expect(",");
			int cnt = (int)(basic.expression()).value;
			if(cnt < 0) {basic.error(BasicInterpreter.Errors.Quantity, "" + cnt);}
			basic.expect(")");
			return new BasicString(str.substring(0, Math.min(str.length(), cnt)));
	    }
		if(token.equals("RIGHT$")) {
			basic.expect("(");
			String str = ((BasicString)basic.expression()).value;
			basic.expect(",");
			int cnt = (int)(basic.expression()).value;
			if(cnt < 0) {basic.error(BasicInterpreter.Errors.Quantity, "" + cnt);}
			basic.expect(")");
			return new BasicString(str.substring(Math.max(str.length() - cnt, 0), Math.min(cnt, str.length())));
		}
		if(token.equals("MID$")) {
			basic.expect("(");
			String str = ((BasicString)basic.expression()).value;
			basic.expect(",");
			int start = (int)(basic.expression()).value;
			if(start < 0) {basic.error(BasicInterpreter.Errors.Quantity, "" + start);}
			basic.expect(",");
			int cnt = (int)(basic.expression()).value;
			if(cnt < 0) {basic.error(BasicInterpreter.Errors.Quantity, "" + cnt);}
			basic.expect(")");
			return new BasicString(str.substring(Math.min(start, str.length()), Math.min(cnt, str.length())));
		}
		//TODO: current this equal with "spc" but java doesn't support curses features
		if(token.equals("TAB")) {
			BasicType b = basic.expression();
	        return new BasicString((int)(b.value));
	    }
	    if(token.equals("ABS")) {
	        return new BasicDouble(Math.abs((basic.expression()).value));
	    }
	    if(token.equals("INT")) {
	        return new BasicInt((int)Math.round((basic.expression()).value));
	    }
	    if(token.equals("LOG")) {            
	        return new BasicDouble((float) Math.log((basic.expression()).value), true);
	    }
	    if(token.equals("RND")) {    
	        return new BasicDouble(Math.random()*(basic.expression()).value, true);
	    }
	    if(token.equals("EXP")) {            
	        return new BasicDouble(Math.exp((basic.expression()).value), true);
	    }
	    if(token.equals("SIN")) {      
	        return new BasicDouble(Math.sin((basic.expression()).value), true);
	    }
	    if(token.equals("COS")) {
	        return new BasicDouble(Math.cos((basic.expression()).value), true);
	    }
	    if(token.equals("TAN")) {
	        return new BasicDouble(Math.tan((basic.expression()).value), true);
	    }
	    if(token.equals("ATN")) {
	        return new BasicDouble(Math.atan((basic.expression()).value), true);
	    }
	    if(token.equals("CHR$")) {
	        return new BasicString(""+(char)(basic.expression()).value);
	    }
	    if(token.equals("SQR")) {
	        return new BasicDouble((float) Math.sqrt((basic.expression()).value));
	    }
	    if(token.equals("HEX$")) {
	        return new BasicString(Integer.toHexString((int)(basic.expression()).value).toUpperCase());
	    }
	    if(token.equals("OCT$")) {
	        return new BasicString(Integer.toOctalString((int)(basic.expression()).value));
	    }
	    return null;
	}
}
