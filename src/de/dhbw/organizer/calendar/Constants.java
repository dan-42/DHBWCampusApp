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

import android.graphics.Color;

public class Constants {
// terst kommentar
	//zweiter komentar
	 /**
     * Account type string.
     */
    public static final String ACCOUNT_TYPE = "de.dhbw.organizer.icalsync";

    /**
     * Authtoken type string.
     */
    public static final String AUTHTOKEN_TYPE = "de.dhbw.organizer.icalsync";
    
    /**
     * token to rertive Calendar URL from AccountManager
     */
    public static final String KEY_ACCOUNT_CAL_URL = "de.dhbw.organizer.callendar.url";
    
    /**
     * the token which will be replaced in the calendar url
     */
    public static final String CALENDAR_REEPLACE_TOKEN = "{{CALENDAR}}";
    
    
    
    /**
     * this String is added in front of every Calendar 
     */
    public static final String CALENDAR_DISPLAY_NAME_PREFIX = "DHBW ";
    
    
    /**
     * default colors, if all are already used, the first is taken
     */
    public static final int[] CALENDAR_COLORS = new int[] {	Color.RED, //FF0000	default
    														Color.parseColor("#FF3636"), //brighter
    														Color.parseColor("#FF7070"), //brighter
    														Color.parseColor("#DB0000"), //darker 
    														Color.parseColor("#A80000")}; //darker

    /**
     * min time intervall between two syncs in millis 
     */
	public static final long MIN_SYNC_INTERVRALL_IN_MILLIS = 10 * 1000;

	/**
	 * sync intervall in seconds e.g. 60 * 60 * 12 for 12h
	 */
	public static final long SYNC_INTERVALL_IN_SEC = 60 * 60 * 12;
	
	/**
	 * URL wehere the XML is to optain the up to date calendar list
	 */
	public static final String EXTERNAL_CALENDAR_LIST_URL = "http://hemera.bf-it.eu/dh/calendar_calendars.xml";
}
