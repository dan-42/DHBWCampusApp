/**
 * LICENSE: GPL v3 
 * 
 * Copyright (c) 2013 by
 * Daniel Friedrich <friedrda@dhbw-loerrach.de>
 * Simon Riedinger <riedings@dhbw-loerrach.de>
 * Patrick Strittmatter <strittpa@dhbw-loerrach.de> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3.0 as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * @author friedrda
 */

package de.dhbw.organizer.calendar.objects;

import android.util.Log;

public class CalendarEvent implements Comparable<CalendarEvent> {

	private static final String TAG = "CalendarEvent";

	long id = -1;
	long timeStamp;
	long startInMillis;
	long endInMillis;
	String title;
	String description;
	String location;
	String uid;

	/**
	 * flag to know if entry is in DB but has tob be updated
	 */
	boolean isToUpdate = false;

	/**
	 * flag if entry is not at all in DB
	 */
	boolean isToInsert = false;

	public CalendarEvent(String eventUid, long eventTimeStamp, long eventStartInMillis, long eventEndInMillis, String eventTitle,
			String eventDescription, String eventLocation) {

		this.uid = eventUid;
		this.timeStamp = eventTimeStamp;
		this.startInMillis = eventStartInMillis;
		this.endInMillis = eventEndInMillis;
		this.title = eventTitle;
		this.description = eventDescription;
		this.location = eventLocation;
	}

	public CalendarEvent(long id, String eventUid, long eventTimeStamp, long eventStartInMillis, long eventEndInMillis, String eventTitle,
			String eventDescription, String eventLocation) {
		this(eventUid, eventTimeStamp, eventStartInMillis, eventEndInMillis, eventTitle, eventDescription, eventLocation);

		this.id = id;
	}

	/**
	 * @return the isToUpdate
	 */
	public boolean isToUpdate() {
		return isToUpdate;
	}

	/**
	 * @param isToUpdate
	 *            the isToUpdate to set
	 */
	public void setToUpdate(boolean isToUpdate) {
		this.isToUpdate = isToUpdate;
	}

	/**
	 * @return the isToInsert
	 */
	public boolean isToInsert() {
		return isToInsert;
	}

	/**
	 * @param isToInsert
	 *            the isToInsert to set
	 */
	public void setToInsert(boolean isToInsert) {
		this.isToInsert = isToInsert;
	}

	/**
	 * @return the timeStamp
	 */
	public long getTimeStamp() {
		return timeStamp;
	}

	/**
	 * @param timeStamp
	 *            the timeStamp to set
	 */
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	/**
	 * @return the startInMillis
	 */
	public long getStartInMillis() {
		return startInMillis;
	}

	/**
	 * @param startInMillis
	 *            the startInMillis to set
	 */
	public void setStartInMillis(long startInMillis) {
		this.startInMillis = startInMillis;
	}

	/**
	 * @return the endInMillis
	 */
	public long getEndInMillis() {
		return endInMillis;
	}

	/**
	 * @param endInMillis
	 *            the endInMillis to set
	 */
	public void setEndInMillis(long endInMillis) {
		this.endInMillis = endInMillis;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title
	 *            the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description
	 *            the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the location
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * @param location
	 *            the location to set
	 */
	public void setLocation(String location) {
		this.location = location;
	}

	/**
	 * @return the uid
	 */
	public String getUid() {
		return uid;
	}

	/**
	 * @param uid
	 *            the uid to set
	 */
	public void setUid(String uid) {
		this.uid = uid;
	}

	/**
	 * 
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * compares only if the uid is equal and also the timestamp!
	 */
	@Override
	public int compareTo(CalendarEvent another) {
		int ret = uid.compareTo(another.getUid());
		if (ret == 0) {

			if (timeStamp == another.getTimeStamp()) {
				return 0;
			} else if (timeStamp > another.getTimeStamp()) {
				return 1;
			} else {
				return -1;
			}

		} else {
			return ret;
		}
	}

	/**
	 * checks if two events are euqual but excludes the timestamp!
	 * 
	 * @param e
	 * @return
	 */
	public boolean equals(CalendarEvent e) {
		
		boolean isEqual = false;
		Log.i(TAG, "equals()");
		Log.i(TAG, "me title \t " + title);
		Log.i(TAG, "you title \t " + e.getTitle());

		if (uid.equals(e.uid))
			if (Math.abs(startInMillis - e.startInMillis) < 1000)
				if (Math.abs(endInMillis - e.getEndInMillis()) < 1000)
					if (title.equals(e.getTitle()))
						if (description.equals(e.getDescription()))
							if (location.equals(e.getLocation())) {
								isEqual = true;

								//Log.i(TAG, "is equal");
							} else {
								Log.i(TAG, "NOT equal location");
							}
						else {
							Log.i(TAG, "NOT equal Description");
						}
					else {
						Log.i(TAG, "NOT equal title");
					}
				else {
					Log.i(TAG, "NOT equal end in millis");
				}
			else {
				Log.i(TAG, "NOT equal start in millis");
				Log.i(TAG, "me \t " + startInMillis);
				Log.i(TAG, "you \t " + e.getStartInMillis());
			}
		else {
			Log.i(TAG, "NOT equal uid");
		}

		if(!isEqual)
			Log.i(TAG, "----------");
		return isEqual;
	}
}
