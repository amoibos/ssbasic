
/*
 * @author: Daniel Ölschlegel
 * @copyright: 2012/08
 * @version: 0.1
 * @license: 2-BSDL
 * */

public class BasicInt extends BasicType {
	public int value;
	
	public BasicInt(int value) {
		super.value = this.value = value;
	}
	
	public BasicInt(double value) {
		super.value = this.value = (int)value;
	}
	
	public BasicInt() {
		super.value = this.value = 0;
	}
	
}
