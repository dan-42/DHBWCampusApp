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
package de.dhbw.organizer.calendar;

import java.util.Calendar;

import android.graphics.Color;

public class Constants {
	
	/**
	 * Authority type  
	 */
	public static final String ACCOUNT_CALENDAR_AUTHORITY = "com.android.calendar";
	/**
     * Account type string.
     */
    public static final String ACCOUNT_TYPE = "de.dhbw.organizer.icalsync";

    /**
     * Authtoken type string.
     */
    public static final String AUTHTOKEN_TYPE = "de.dhbw.organizer.icalsync";
    
    /**
     * token to retrieve Calendar URL from AccountManager
     */
    public static final String KEY_ACCOUNT_CAL_URL = "de.dhbw.organizer.callendar.url";
    
      
    /**
     * this String is added in front of every Calendar 
     */
    public static final String CALENDAR_DISPLAY_NAME_PREFIX = "DHBW ";
    
    
    /**
     * default colors, if all are already used, the first is taken
     */
    public static final int[] CALENDAR_COLORS = new int[] {	Color.parseColor("#e2001a"), //DHBW RED    						
    														Color.parseColor("#b83347"), //between DHBW RED and DHBW dark red  														
    														Color.parseColor("#8b0011"), //DHBW dark red 
    														Color.parseColor("#ff5858"), //brighter than DHBW red
    														Color.parseColor("#ff9999"), //brighter than DHBW red, more like rosa
    														}; 
    /**
     * min time interval between two syncs in millis 
     */
	public static final long MIN_SYNC_INTERVRALL_IN_MILLIS = 10 * 1000;

	/**
	 * sync interval in seconds e.g. 60 * 60 * 12 for 12h
	 */
	public static final long SYNC_INTERVALL_IN_SEC = 60 * 60 * 12;
	
	/**
	 * URL where the XML is to obtain the up to date calendar list
	 */
	//public static final String EXTERNAL_CALENDAR_LIST_URL = "http://hemera.bf-it.eu/dh/calendar_calendars.xml";
	public static final String EXTERNAL_CALENDAR_LIST_URL = "http://www.dhbw-loerrach.de/fileadmin/pubdocs/cal-app/calendar_calendars.xml";
	
	
	
	/**
	 * TEST VERSION ends on 2013-12-31  23:59
	 * 1388530740
	 * 
	 */
	public static final long TIME_END_TEST_VERSION = 1388530740000l;
	//	public static final long TIME_END_TEST_VERSION = System.currentTimeMillis() + 10000;
	

	
}
