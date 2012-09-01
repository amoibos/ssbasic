
/*
 * @author: Daniel Ölschlegel
 * @copyright: 2012/08
 * @version: 0.1
 * @license: 2-BSDL
 * */

public class BasicString extends BasicType {
	public String value;
	
	public BasicString(String str) {
		this.value = str;
	}
	
	//padding string from the left with "value" spaces
	public BasicString(int value) {		
		this.value = String.format("%" + value + "s", "");
	}
	
	public BasicString() {
		this.value = "";
	}
	
}
