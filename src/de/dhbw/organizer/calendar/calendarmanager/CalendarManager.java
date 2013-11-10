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
package de.dhbw.organizer.calendar.calendarmanager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.util.Log;
import biweekly.component.VEvent;
import biweekly.property.RecurrenceId;
import biweekly.util.Recurrence;
import biweekly.util.Recurrence.DayOfWeek;
import de.dhbw.organizer.calendar.Constants;
import de.dhbw.organizer.calendar.objects.CalendarEvent;

/**
 * @author schoko This class has only static functions to help handle all the
 *         calndar stuff
 */
public class CalendarManager {
	private static final String TAG = "CalendarManager";

	/**
	 * check if a calenda already exists
	 * 
	 * @param context
	 * @param account
	 * @return
	 */
	public static boolean calendarExists(Context context, Account account) {

		Cursor cur = null;
		boolean calendarExists = false;

		ContentResolver cr = context.getContentResolver();

		String[] projection = new String[] { Calendars.CALENDAR_DISPLAY_NAME };
		String selection = "((" + Calendars.ACCOUNT_NAME + " = ?)  AND (" + Calendars.ACCOUNT_TYPE + " = ?) )";
		String[] selectionArgs = new String[] { account.name, account.type };

		cur = cr.query(Calendars.CONTENT_URI, projection, selection, selectionArgs, null);

		if (cur.moveToFirst()) {
			calendarExists = true;
			do {
				Log.d(TAG, "Calendar =  " + cur.getString(0) + " alread exists");
			} while (cur.moveToNext());

		} else {
			Log.d(TAG, "Coursor is <= 0");
			calendarExists = false;
		}

		cur.close();
		return calendarExists;
	}

	/**
	 * returns the CalendarID depending on the account.name and acount.type
	 * 
	 * @param context
	 * @param account
	 * @return id
	 */
	public static long getCalendarId(Context context, Account account) {

		Cursor cur = null;

		long calendarId = -1;

		ContentResolver cr = context.getContentResolver();

		String[] projection = new String[] { Calendars._ID };
		String selection = "((" + Calendars.ACCOUNT_NAME + " = ?)  AND (" + Calendars.ACCOUNT_TYPE + " = ?) )";
		String[] selectionArgs = new String[] { account.name, account.type };

		cur = cr.query(Calendars.CONTENT_URI, projection, selection, selectionArgs, null);

		if (cur.getCount() == 1 && cur.moveToFirst()) {
			calendarId = cur.getLong(0);

		} else {
			Log.d(TAG, "FATAL ERROR");
		}

		cur.close();
		return calendarId;
	}

	/**
	 * creates a new Calendar with the given account
	 * 
	 * @param account
	 * @param context
	 * @return calendar id
	 */
	public static long createCalendar(Context context, Account account) {
		Log.d(TAG, "Calendar With by Account Name");

		ContentResolver cr = context.getContentResolver();

		Uri creationUri = asSyncAdapter(Calendars.CONTENT_URI, account.name, account.type);

		int color = getCalendarColor(context);
		ContentValues values = new ContentValues();

		values.put(Calendars.ACCOUNT_NAME, account.name);
		values.put(Calendars.ACCOUNT_TYPE, account.type);
		values.put(Calendars.NAME, account.name);
		values.put(Calendars.CALENDAR_DISPLAY_NAME, Constants.CALENDAR_DISPLAY_NAME_PREFIX + account.name);
		values.put(Calendars.CALENDAR_COLOR, color);
		values.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_READ);
		values.put(Calendars.OWNER_ACCOUNT, account.name);
		values.put(Calendars.SYNC_EVENTS, 1);
		values.put(Calendars.VISIBLE, 1);
		values.put(Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().getID());

		Uri created = cr.insert(creationUri, values);

		Log.d(TAG, "Calendar created " + created.toString());
		long cal_id = Long.parseLong(created.getLastPathSegment());
		return cal_id;
	}

	/**
	 * find next free predefined calendar color if all are taken, take first
	 * pedefined as default
	 * 
	 * @param context
	 * @return
	 */
	private static int getCalendarColor(Context context) {

		for (int i = 0; i < Constants.CALENDAR_COLORS.length; i++) {
			if (!isColorUsed(context, Constants.CALENDAR_COLORS[i])) {
				return Constants.CALENDAR_COLORS[i];
			}
		}
		return Constants.CALENDAR_COLORS[0];
	}

	/**
	 * Check if a given color is already used as calendar-color
	 * 
	 * @param context
	 * @param account
	 * @param color
	 * @return
	 */
	private static boolean isColorUsed(Context context, int color) {
		boolean isColorUsed = false;
		ContentResolver cr = context.getContentResolver();

		String[] projection = new String[] { Calendars.CALENDAR_DISPLAY_NAME };

		String selection = "((" + Calendars.CALENDAR_COLOR + " = ?)) ";

		String[] selectionArgs = new String[] { Integer.toString(color) };

		Cursor c = cr.query(Calendars.CONTENT_URI, projection, selection, selectionArgs, null);

		if (c.moveToFirst()) {
			isColorUsed = true;
			Log.d(TAG, "color " + Integer.toHexString(color) + " is alreay used by " + c.getString(0));
		}

		c.close();

		return isColorUsed;
	}

	private static Uri asSyncAdapter(Uri uri, String account, String accountType) {
		return uri.buildUpon().appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
				.appendQueryParameter(Calendars.ACCOUNT_NAME, account).appendQueryParameter(Calendars.ACCOUNT_TYPE, accountType).build();
	}

	/**
	 * check if a calenda already exists
	 * 
	 * @param context
	 * @param account
	 * @return
	 */
	public static long deleteCalendar(Context context) {

		ContentResolver cr = context.getContentResolver();

		String selection = "((" + Calendars.ACCOUNT_NAME + " = ?) )";
		String[] selectionArgs = new String[] { "stuv", CalendarContract.ACCOUNT_TYPE_LOCAL };

		long ret = cr.delete(Calendars.CONTENT_URI, selection, selectionArgs);
		return ret;

	}

	/**
	 * check if a calenda already exists
	 * 
	 * @param context
	 * @param account
	 * @return
	 */
	public static void printCalendars(Context context) {

		Cursor cur = null;

		ContentResolver cr = context.getContentResolver();

		String[] projection = new String[] { Calendars.ACCOUNT_NAME, Calendars.ACCOUNT_TYPE, Calendars.CALENDAR_DISPLAY_NAME };

		cur = cr.query(Calendars.CONTENT_URI, projection, null, null, null);

		if (cur.moveToFirst()) {

			do {
				Log.d(TAG, "Calendar.ACCOUNT_NAME = \t " + cur.getString(0));
				Log.d(TAG, "Calendar.ACCOUNT_TYPE = \t " + cur.getString(1));
				Log.d(TAG, "Calendar.CALENDAR_DISPLAY_NAME = \t " + cur.getString(2));

				Log.d(TAG, "----------------------------------");
				Log.d(TAG, "----------------------------------");
			} while (cur.moveToNext());

		} else {
			Log.d(TAG, "Coursor is <= 0");

		}

		cur.close();

	}

	public static ArrayList<CalendarEvent> getAllCalendarEvents(Context context, Account account, long calendarId) {

		Cursor cur = null;
		ArrayList<CalendarEvent> eventList = new ArrayList<CalendarEvent>();
		ContentResolver cr = context.getContentResolver();

		Uri uri = asSyncAdapter(Events.CONTENT_URI, account.name, account.type);

		String[] projection = new String[] { Events._ID, Events._SYNC_ID, Events.SYNC_DATA1, Events.TITLE, Events.DTSTART, Events.DTEND,
				Events.DESCRIPTION, Events.EVENT_LOCATION };

		String selection = "((" + Events.CALENDAR_ID + " = ?) )";
		String[] selectionArgs = new String[] { Long.toString(calendarId) };

		cur = cr.query(uri, projection, selection, selectionArgs, null);

		if (cur.moveToFirst()) {

			String eventUid = "";
			long eventId = 0;
			long eventTimeStamp = 0;
			long start = 0;
			long end = 0;
			String title = "";
			String location = "";
			String description = "";

			do {

				eventId = cur.getLong(0);
				eventUid = cur.getString(1);
				eventTimeStamp = Long.parseLong(cur.getString(2));
				title = cur.getString(3);
				start = cur.getLong(4);
				end = cur.getLong(5);
				description = cur.getString(6);
				location = cur.getString(7);

				eventList.add(new CalendarEvent(eventId, eventUid, eventTimeStamp, start, end, title, description, location));

			} while (cur.moveToNext());

		} else {
			Log.d(TAG, "getAllCalendarEvents() courser is empty");

		}

		cur.close();
		return eventList;
	}

	public static void deleteAllEvents(Context context, Account account, long calendarId) {
		Log.d(TAG, "delte all Events from CalendarId = " + calendarId);

		ContentResolver cr = context.getContentResolver();
		Uri uri = asSyncAdapter(Events.CONTENT_URI, account.name, account.type);
		String where = "( ( " + Events.CALENDAR_ID + " = ? ))";
		String[] selectionArgs = new String[] { Long.toString(calendarId) };

		int del = cr.delete(uri, where, selectionArgs);

		Log.d(TAG, "deleted " + del + " Events");

	}

	public static void insertEvent(Context context, Account account, long calendarId, CalendarEvent event) {

		Log.d(TAG, "insertEvent() ");
		ContentResolver cr = context.getContentResolver();
		ContentValues values = new ContentValues();
		Uri uri = asSyncAdapter(Events.CONTENT_URI, account.name, account.type);

		values.put(Events.DTSTART, event.getStartInMillis());
		values.put(Events.DTEND, event.getEndInMillis());
		values.put(Events.TITLE, event.getTitle());
		values.put(Events.DESCRIPTION, event.getDescription());
		values.put(Events.CALENDAR_ID, calendarId);
		values.put(Events.EVENT_LOCATION, event.getLocation()); //
		values.put(Events._SYNC_ID, event.getUid());
		values.put(Events.SYNC_DATA1, Long.toString(event.getTimeStamp()));
		values.put(Events.EVENT_TIMEZONE, TimeZone.getDefault().toString());

		cr.insert(uri, values);

	}

	public static void insertEvents(Context context, Account account, long calendarId, ArrayList<VEvent> eventList) {
		Log.d(TAG, "insertEvents() " + eventList.size() + " events to add in total");

		ArrayList<ArrayList<VEvent>> listListEventCalendar = new ArrayList<ArrayList<VEvent>>();
		ArrayList<VEvent> smallList = null;
		int idx = 0;
		int BATCH_SIZE = 40;

		for (int i = 0; i < eventList.size(); i++) {
			if (i % BATCH_SIZE == 0) {

				if (smallList != null && smallList.size() > 0) {
					Log.d(TAG, "insertEvents() new full list ");
					listListEventCalendar.add(smallList);
					insertEventsAsBatch(context, account, calendarId, smallList);
				}

				smallList = new ArrayList<VEvent>();
				idx = 0;

			}
			smallList.add(eventList.get(i));
			idx++;
		}
		if (idx % BATCH_SIZE != 0) {
			Log.d(TAG, "insertEvents() last list ");
			listListEventCalendar.add(smallList);
			insertEventsAsBatch(context, account, calendarId, smallList);
		}

	}

	public static void insertEventsAsBatch(Context context, Account account, long calendarId, ArrayList<VEvent> eventList) {

		Log.d(TAG, "insertEventsAsBatch() ");
		ContentResolver cr = context.getContentResolver();
		ContentValues[] values = new ContentValues[eventList.size()];

		Uri uri = asSyncAdapter(Events.CONTENT_URI, account.name, account.type);

		Log.d(TAG, "insertEventsAsBatch() " + eventList.size() + " events to add");

		int idx = 0;
		for (VEvent event : eventList) {

			values[idx] = new ContentValues();
			values[idx].put(Events.CALENDAR_ID, calendarId);
			values[idx].put(Events.EVENT_TIMEZONE, TimeZone.getDefault().toString());

			values[idx].put(Events.DTSTART, event.getDateStart().getValue().getTime());
			values[idx].put(Events.DTEND, event.getDateEnd().getValue().getTime());
			values[idx].put(Events.TITLE, event.getSummary().getValue());

			values[idx].put(Events._SYNC_ID, event.getUid().getValue());
			values[idx].put(Events.SYNC_DATA1, Long.toString(event.getDateTimeStamp().getValue().getTime()));

			if (event.getDescription() != null)
				values[idx].put(Events.DESCRIPTION, event.getDescription().getValue());
			else
				values[idx].put(Events.DESCRIPTION, "");

			if (event.getLocation() != null)
				values[idx].put(Events.EVENT_LOCATION, event.getLocation().getValue());

			if (event.getRecurrenceRule() != null) {
				String rrule = buildRrule(event);
				Log.i(TAG, "RRULE " + rrule);
				values[idx].put(Events.RRULE, rrule);
			}
			
			if(event.getRecurrenceId() != null){
				long orgEventId = getEventByUID(event.getUid().getValue());
			}
			
			idx++;

		}

		Log.d(TAG, "insertEventsAsBatch() FOR done now bulkInsert");
		cr.bulkInsert(uri, values);

	}

	private static long getEventByUID(Context context, Account account,  String id) {		
		
		Cursor cur = null;	
		ContentResolver cr = context.getContentResolver();

		Uri uri = asSyncAdapter(Events.CONTENT_URI, account.name, account.type);

		String[] projection = new String[] { Events._ID, Events._SYNC_ID};

		String selection = "((" + Events._SYNC_ID + " = ?) )";
		String[] selectionArgs = new String[] { Long.toString(calendarId) };

		cur = cr.query(uri, projection, selection, selectionArgs, null);

		if (cur.moveToFirst()) {

			

			do {

				

				eventList.add(new CalendarEvent(eventId, eventUid, eventTimeStamp, start, end, title, description, location));

			} while (cur.moveToNext());

		} else {
			Log.d(TAG, "getAllCalendarEvents() courser is empty");

		}

		cur.close();
		return eventList;
		
		return 0;
	}

	/**
	 * RRULE:FREQ=WEEKLY;UNTIL=20131106T080000Z;INTERVAL=1;BYDAY=TU,WE;WKST=MO
	 * 
	 * @param e
	 * @return
	 */
	private static String buildRrule(VEvent e) {
		if (e.getRecurrenceRule() != null) {
			Recurrence r = e.getRecurrenceRule().getValue();
			StringBuilder sb = new StringBuilder();

			sb.append("FREQ=").append(r.getFrequency().toString());
			if (r.hasTimeUntilDate())
				sb.append(";UNTIL=").append(parseIcalUtcDate(r.getUntil()));
			else
				sb.append(";COUNT=").append(r.getCount());

			sb.append(";INTERVAL=").append(r.getInterval());

			// BYSECOND
			if (!r.getBySecond().isEmpty()) {
				sb.append(";BYSECOND=");
				int idx = 0;
				for (int s : r.getBySecond()) {
					sb.append(s);
					idx++;
					if (idx < r.getByDay().size()) {
						sb.append(",");
					}
				}
			}

			// BYMINUTE
			if (!r.getByMinute().isEmpty()) {
				sb.append(";BYMINUTE=");
				int idx = 0;
				for (int s : r.getBySecond()) {
					sb.append(s);
					idx++;
					if (idx < r.getByDay().size()) {
						sb.append(",");
					}
				}
			}
			
			// BYHOURE
			if (!r.getByHour().isEmpty()) {
				sb.append(";BYHOURE=");
				int idx = 0;
				for (int s : r.getBySecond()) {
					sb.append(s);
					idx++;
					if (idx < r.getByDay().size()) {
						sb.append(",");
					}
				}
			}

			//BYDAY
			if (!r.getByDay().isEmpty()) {
				sb.append(";BYDAY=");
				int idx = 0;

				for (DayOfWeek dow : r.getByDay()) {
					sb.append(dow.getAbbr());
					idx++;
					if (idx < r.getByDay().size()) {
						sb.append(",");
					}
				}
			}

			//BYMONTHDAY
			if (!r.getByMonthDay().isEmpty()) {
				sb.append(";BYMONTHDAY=");
				int idx = 0;

				for (int  i : r.getByMonthDay()) {
					sb.append(i);
					idx++;
					if (idx < r.getByDay().size()) {
						sb.append(",");
					}
				}
			}
			
			
			//BYYEARDAY
			if (!r.getByYearDay().isEmpty()) {
				sb.append(";BYYEARDAY=");
				int idx = 0;

				for (int  i : r.getByYearDay()) {
					sb.append(i);
					idx++;
					if (idx < r.getByDay().size()) {
						sb.append(",");
					}
				}
			}
			
			//BYWEEKNO
			if (!r.getByWeekNo().isEmpty()) {
				sb.append(";BYWEEKNO=");
				int idx = 0;

				for (int  i : r.getByWeekNo()) {
					sb.append(i);
					idx++;
					if (idx < r.getByDay().size()) {
						sb.append(",");
					}
				}
			}
			
			//BYMONTH
			if (!r.getByMonth().isEmpty()) {
				sb.append(";BYMONTH=");
				int idx = 0;

				for (int  i : r.getByMonth()) {
					sb.append(i);
					idx++;
					if (idx < r.getByDay().size()) {
						sb.append(",");
					}
				}
			}
			
			//BYSETPOS
			if (!r.getBySetPos().isEmpty()) {
				sb.append(";BYSETPOS=");
				int idx = 0;

				for (int  i : r.getBySetPos()) {
					sb.append(i);
					idx++;
					if (idx < r.getByDay().size()) {
						sb.append(",");
					}
				}
			}
		
			
			
			if (r.getWorkweekStarts() != null)
				sb.append(";WKST=").append(r.getWorkweekStarts().getAbbr());

			return sb.toString();
		} else {
			return "";
		}

	}

	@SuppressLint("SimpleDateFormat")
	private static Object parseIcalUtcDate(Date until) {

		StringBuilder sb = new StringBuilder();
		SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmSS");
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

		sb.append(dateFormat.format(until));
		sb.append("T");
		sb.append(timeFormat.format(until));
		sb.append("Z");

		return sb.toString();
	}

	public static void updateEvent(Context context, Account account, long calendarId, CalendarEvent event) {

		ContentResolver cr = context.getContentResolver();
		ContentValues values = new ContentValues();
		Uri uri = asSyncAdapter(Events.CONTENT_URI, account.name, account.type);

		String where = "((" + Events._ID + " = ?) AND  (" + Events._SYNC_ID + " = ?) )";

		String[] selectionArgs = new String[] { Long.toString(event.getId()), event.getUid() };

		values.put(Events.DTSTART, event.getStartInMillis());
		values.put(Events.DTEND, event.getEndInMillis());
		values.put(Events.TITLE, event.getTitle());
		values.put(Events.DESCRIPTION, event.getDescription());
		values.put(Events.CALENDAR_ID, calendarId);
		values.put(Events.EVENT_LOCATION, event.getLocation()); //
		values.put(Events._SYNC_ID, event.getUid());
		values.put(Events.SYNC_DATA1, Long.toString(event.getTimeStamp()));
		values.put(Events.EVENT_TIMEZONE, TimeZone.getDefault().toString());

		cr.update(uri, values, where, selectionArgs);

	}

	/**
	 * This function cleans up some fuckUps from Microsoft! FIRST sometimes in
	 * the iCal-Files there are two Events with same attributes except the
	 * sequencnumber. The SequenceNumber should only be increased, not being
	 * duplicated!
	 * 
	 * SECOND Microsoft uses an own property called X-MICROSOFT-CDO-INSTTYPE to
	 * indicate if an event is once, recurring or an exception of an recurring
	 * But there are no further information about this, so we have to filter for
	 * those by ourselves
	 * 
	 * @param dirtyList
	 * @return cleanList
	 */
	public static ArrayList<VEvent> fixMicrosoftFuckUps(ArrayList<VEvent> dirtyList) {

		ArrayList<VEvent> cleanList = new ArrayList<VEvent>();

		for (VEvent outerEvent : dirtyList) {
			boolean toInsert = true;
			for (VEvent innerEvent : dirtyList) {

				
				
				//not the same Object!
				if (!outerEvent.equals(innerEvent)) {
					
					// events have same UID
					if (outerEvent.getUid().getValue().equals(innerEvent.getUid().getValue())) {
						
						
						
						//outer has RRULE  inner has RECURREND-ID
						if(outerEvent.getRecurrenceRule() != null && innerEvent.getRecurrenceId() != null){
							
							/**
							 * FIX wrong RECURRING-ID
							 */
							
							if(innerEvent.getRecurrenceId().getRange() != null ){
								System.err.println("RECURRING-ID RANGE NOT SUPPORTED");
								
							}

							SimpleDateFormat sdfDateOnly = new SimpleDateFormat("yyyyMMdd");
							
							String outerDTStart = sdfDateOnly.format(outerEvent.getDateStart().getValue());
							String innerReqId =  sdfDateOnly.format(innerEvent.getRecurrenceId().getValue());
							
							//if(outerDTStart.equals(innerReqId)){
							
								RecurrenceId rId = new RecurrenceId(outerEvent.getDateStart().getValue(), true);
								rId.setLocalTime(true);
								rId.setTimezoneId(outerEvent.getDateStart().getTimezoneId());
								innerEvent.setRecurrenceId(rId);
							//}
							
						}					
					
					} // same UID
				}
			}

			if (toInsert) {
				cleanList.add(outerEvent);
			}
		}

		return cleanList;
	}
}
