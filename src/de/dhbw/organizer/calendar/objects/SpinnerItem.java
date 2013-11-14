/**
 * 
 */
package de.dhbw.organizer.calendar.objects;

import android.annotation.SuppressLint;

/**
 * @author friedrda
 * 
 */
@SuppressLint("DefaultLocale")
public class SpinnerItem implements Comparable<SpinnerItem> {

	private String mDisplayName = null;
	private String mIcalUrl = null;

	public SpinnerItem(String displayName, String icalUrl) {
		mDisplayName = displayName;
		mIcalUrl = icalUrl;
	}

	/**
	 * @return the mDisplayName
	 */
	public String getmDisplayName() {
		return mDisplayName.toUpperCase();
	}

	/**
	 * @return the mIcalUrl
	 */
	public String getmIcalUrl() {
		return mIcalUrl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		return mDisplayName.toUpperCase();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(SpinnerItem another) {
		return toString().compareTo(another.toString());
	}
}
