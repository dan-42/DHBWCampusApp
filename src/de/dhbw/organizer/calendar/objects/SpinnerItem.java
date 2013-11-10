/**
 * 
 */
package de.dhbw.organizer.calendar.objects;

import android.annotation.SuppressLint;

/**
 * @author friedrda
 *
 */
public class SpinnerItem  implements Comparable<SpinnerItem>{

	private static final String PATTERN_PREFIX = "^kal-";
	private static final String PATTERN_POSTFIX = "-(.){2}$";
	
	//  shoud macht  kal-tif11a  and kal-tif11a-it
	
	
	
	private String value = null;
	
	/**
	 * 
	 */
	public SpinnerItem(String value) {
		this.value = value;
	}

	
	public String getValue() {
		return value;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@SuppressLint("DefaultLocale")
	@Override
	public String toString() {
		
		String s = value.replaceAll(PATTERN_POSTFIX, "");
		s = s.replaceAll(PATTERN_PREFIX, "");
		
		return s.toUpperCase();
	}


	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(SpinnerItem another) {		
		return value.compareTo(another.getValue());
	}
}
