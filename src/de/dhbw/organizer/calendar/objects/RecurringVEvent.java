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

package de.dhbw.organizer.calendar.objects;

import java.util.ArrayList;

import android.util.Log;
import biweekly.component.VEvent;

/**
 * @author friedrda this calls is used to keep track of all recurring events
 *         that differ from the actual recurring event e.g. repeat every
 *         thursday, but on XXXX be on 9:00 o'Clock
 */
public class RecurringVEvent {
	
	private static final String TAG = "RecurringVEvent";

	private ArrayList<VEvent> mExceptions = null;
	
	private long id = 0;
	
	public VEvent e = null;

		
	
	/**
	 * @param e2
	 */
	public RecurringVEvent(VEvent e) {
		this.e =e;  
		mExceptions = new ArrayList<VEvent>();
		
	}

	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param id
	 *  the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	public ArrayList<VEvent> getExceptions() {
		return mExceptions;
	}

	public void addException(VEvent e) {
		if (e.getUid().getValue().equals(e.getUid().getValue())) {
			//Exceptions need an recurance ID
			if(e.getRecurrenceId() != null && e.getRecurrenceId().getValue() != null){
				mExceptions.add(e);
			}
			else {
				Log.e(TAG, "addException()  event : " + e.getSummary().getValue() + " has no recurring ID");
			}
		}
	}

}
