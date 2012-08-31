
/*
 * @author: Daniel Ölschlegel
 * @copyright: 2012/08
 * @version: 0.1
 * @license: 2-BSDL
 * */

public class BasicDouble extends BasicType {
	public double value;
	public boolean explicit = false;
	
	/*
	 * caveat also for BasicInt
	 * built-ins uses BasicType.value instead of the specific Basic[String|Double].value
	 * there are always two value members
	 * */
	public BasicDouble(double value) {
		super.value = this.value = value;
	}
	
	public BasicDouble(double value, boolean explicit) {
		super.value = this.value = value;
		this.explicit = explicit;
	}
	
	
}
