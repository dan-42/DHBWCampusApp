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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.util.Log;




//A compatibility layer for joda-time
import com.google.ical.compat.javautil.*;

import biweekly.component.VEvent;
import biweekly.property.ExceptionDates;
import biweekly.property.RecurrenceId;
import biweekly.util.Recurrence;
import biweekly.util.Recurrence.DayOfWeek;
import de.dhbw.organizer.calendar.Constants;
import de.dhbw.organizer.calendar.objects.RecurringVEvent;

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
		// values.put(Calendars.CALENDAR_ACCESS_LEVEL,
		// Calendars.CAL_ACCESS_READ);
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

	public static void deleteAllEvents(Context context, Account account, long calendarId) {
		Log.d(TAG, "delte all Events from CalendarId = " + calendarId);

		ContentResolver cr = context.getContentResolver();
		Uri uri = asSyncAdapter(Events.CONTENT_URI, account.name, account.type);
		String where = "( ( " + Events.CALENDAR_ID + " = ? ))";
		String[] selectionArgs = new String[] { Long.toString(calendarId) };

		int del = cr.delete(uri, where, selectionArgs);

		Log.d(TAG, "deleted " + del + " Events");

	}

	public static void insertEvents(Context context, Account account, long calendarId, ArrayList<VEvent> eventList) {
		Log.d(TAG, "insertEvents() " + eventList.size() + " events to add in total");

		ArrayList<VEvent> smallList = null;

		// NO RRULE, NO RECURRING_ID
		ArrayList<VEvent> recularEvents = new ArrayList<VEvent>();

		ArrayList<RecurringVEvent> recurringEvents = new ArrayList<RecurringVEvent>();
		ArrayList<VEvent> tempEventList = new ArrayList<VEvent>();

		/**
		 * split events into recurring events and recular events
		 */
		for (VEvent e : eventList) {
			if (e.getRecurrenceRule() == null && e.getRecurrenceId() == null) {
				recularEvents.add(e);
			}

			else {
				if (e.getRecurrenceRule() != null) {

					recurringEvents.add(new RecurringVEvent(e));

				} else if (e.getRecurrenceId() != null) {

					boolean isInserted = false;
					for (RecurringVEvent re : recurringEvents) {
						if (re.e.getUid().getValue().equals(e.getUid().getValue())) {
							re.addException(e);
							isInserted = true;
							break;
						}
					}
					if (isInserted == false) {
						// not fitting? keep it for later
						tempEventList.add(e);
					}
				}
			}
		}

		// check if some are need sorting in

		for (VEvent e : tempEventList) {
			boolean isInserted = false;
			for (RecurringVEvent re : recurringEvents) {
				if (re.e.getUid().getValue().equals(e.getUid().getValue())) {
					re.addException(e);
					isInserted = true;
					break;
				}
			}
			if (isInserted == false) {
				Log.e(TAG, "ERRO cannot find fitting Recurring event: evenUID = " + e.getUid().getValue());
			}
		}

		Log.d(TAG, "insert recurring Events ");
		for (RecurringVEvent re : recurringEvents) {	
			

			Log.d(TAG, "RECURING-Event: " + re.e.getSummary().getValue() + "   " + re.e.getDateStart().getRawComponents().toString(true));
			Log.d(TAG, "RECURING-Event: UID \t" + re.e.getUid().getValue());
			Log.d(TAG, "RECURING-Event: DTSTART \t" + re.e.getDateStart().getValue().getTime());
			Log.d(TAG, "RECURING-Event: DTSTART \t" + re.e.getDateStart().getValue().toString());
			
			long id = insertEvent(context, account, calendarId, re.e, null);
			re.setId(id);
			Log.d(TAG, "RECURING-Event: DB-ID " + re.getId());

			for (VEvent e : re.getExceptions()) {
				Log.d(TAG, "-- RECURING-ID: " + e.getSummary().getValue() + "   " + e.getDateStart().getValue().toString());
				if (e.getRecurrenceId().getRawComponents() != null) {
					Log.d(TAG, "\t" + e.getRecurrenceId().getRawComponents().toString(true));
				}
				if (e.getLocation() != null) {
					Log.d(TAG, "\tLOCATION: " + e.getLocation().getValue());
				}

				insertEvent(context, account, calendarId, e, re);
			}
			Log.d(TAG, "---------------------------------------------\n");

		}

		Log.d(TAG, "insert recular Events ");
		// insertEventsAsBatch(context, account, calendarId, recularEvents);
		for (VEvent e : recularEvents) {
			insertEvent(context, account, calendarId, e, null);
		}

	}

	public static void insertEventsAsBatch(Context context, Account account, long calendarId, ArrayList<VEvent> eventList) {

		for (VEvent vEvent : eventList) {

		}

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

			if (!event.getExceptionDates().isEmpty()) {
				String exdate = buildExdate(event.getExceptionDates());
				Log.i(TAG, "add EXDATE " + exdate);
				values[idx].put(Events.EXDATE, exdate);
			}

			if (event.getRecurrenceId() != null) {
				// long orgEventId = getEventByUID(event.getUid().getValue());
			}

			idx++;

		}

		Log.d(TAG, "insertEventsAsBatch()  bulkInsert()");
		cr.bulkInsert(uri, values);

	}

	/**
	 * @param exceptionDates
	 * @return
	 */
	private static String buildExdate(List<ExceptionDates> exceptionDates) {
		if (exceptionDates != null) {

			StringBuilder sb = new StringBuilder();
			ArrayList<Date> dates = new ArrayList<Date>();

			String tz = null;

			for (ExceptionDates ed : exceptionDates) {
				dates.addAll(ed.getValues());
				tz = ed.getTimezoneId();
			}

			if (tz != null) {
				sb.append("TZID=").append(tz).append(':');
			}

			for (Iterator iterator = dates.iterator(); iterator.hasNext();) {
				Date date = (Date) iterator.next();

				if (tz != null) {
					sb.append(parseIcalDateToString(date));
				} else {
					sb.append(parseIcalDateToString(date));
				}

				if (iterator.hasNext()) {
					sb.append(",");
				}
			}
			return sb.toString();
		}

		return "";
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
				sb.append(";UNTIL=").append(parseIcalDateToString(r.getUntil())).append('Z');
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

			// BYDAY
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

			// BYMONTHDAY
			if (!r.getByMonthDay().isEmpty()) {
				sb.append(";BYMONTHDAY=");
				int idx = 0;

				for (int i : r.getByMonthDay()) {
					sb.append(i);
					idx++;
					if (idx < r.getByDay().size()) {
						sb.append(",");
					}
				}
			}

			// BYYEARDAY
			if (!r.getByYearDay().isEmpty()) {
				sb.append(";BYYEARDAY=");
				int idx = 0;

				for (int i : r.getByYearDay()) {
					sb.append(i);
					idx++;
					if (idx < r.getByDay().size()) {
						sb.append(",");
					}
				}
			}

			// BYWEEKNO
			if (!r.getByWeekNo().isEmpty()) {
				sb.append(";BYWEEKNO=");
				int idx = 0;

				for (int i : r.getByWeekNo()) {
					sb.append(i);
					idx++;
					if (idx < r.getByDay().size()) {
						sb.append(",");
					}
				}
			}

			// BYMONTH
			if (!r.getByMonth().isEmpty()) {
				sb.append(";BYMONTH=");
				int idx = 0;

				for (int i : r.getByMonth()) {
					sb.append(i);
					idx++;
					if (idx < r.getByDay().size()) {
						sb.append(",");
					}
				}
			}

			// BYSETPOS
			if (!r.getBySetPos().isEmpty()) {
				sb.append(";BYSETPOS=");
				int idx = 0;

				for (int i : r.getBySetPos()) {
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
	private static String parseIcalDateToString(Date until) {

		StringBuilder sb = new StringBuilder();
		SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmSS");
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

		sb.append(dateFormat.format(until));
		sb.append("T");
		sb.append(timeFormat.format(until));

		return sb.toString();
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

				// not the same Object!
				if (!outerEvent.equals(innerEvent)) {

					// events have same UID
					if (outerEvent.getUid().getValue().equals(innerEvent.getUid().getValue())) {

						// outer has RRULE inner has RECURREND-ID
						if (outerEvent.getRecurrenceRule() != null && innerEvent.getRecurrenceId() != null) {

							/**
							 * FIX wrong RECURRING-ID
							 */

							if (innerEvent.getRecurrenceId().getRange() != null) {
								System.err.println("RECURRING-ID RANGE NOT SUPPORTED");

							}

							SimpleDateFormat sdfDateOnly = new SimpleDateFormat("yyyyMMdd");

							String outerDTStart = sdfDateOnly.format(outerEvent.getDateStart().getValue());
							String innerReqId = sdfDateOnly.format(innerEvent.getRecurrenceId().getValue());

							// if(outerDTStart.equals(innerReqId)){

							RecurrenceId rId = new RecurrenceId(outerEvent.getDateStart().getValue(), true);
							rId.setLocalTime(true);
							rId.setTimezoneId(outerEvent.getDateStart().getTimezoneId());
							innerEvent.setRecurrenceId(rId);
							// }

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

	public static long insertEvent(Context context, Account account, long calendarId, VEvent e, RecurringVEvent re) {

		ContentResolver cr = context.getContentResolver();
		ContentValues values = new ContentValues();
		Uri uri = asSyncAdapter(Events.CONTENT_URI, account.name, account.type);

		values.put(Events.DTSTART, e.getDateStart().getValue().getTime());

		values.put(Events.TITLE, e.getSummary().getValue());

		values.put(Events._SYNC_ID, e.getUid().getValue());
		values.put(Events.SYNC_DATA1, Long.toString(e.getDateTimeStamp().getValue().getTime()));

		// if (re == null) {
		values.put(Events.CALENDAR_ID, calendarId);
		values.put(Events.DTEND, e.getDateEnd().getValue().getTime());
		values.put(Events.EVENT_TIMEZONE, TimeZone.getTimeZone(e.getDateStart().getTimezoneId()).toString());

		if (e.getDescription() != null)
			values.put(Events.DESCRIPTION, e.getDescription().getValue());

		if (e.getLocation() != null)
			values.put(Events.EVENT_LOCATION, e.getLocation().getValue());

		if (e.getRecurrenceRule() != null) {
			String rrule = buildRrule(e);
			Log.i(TAG, "add RRULE " + rrule);			
			values.put(Events.RRULE, rrule);

		}

		if (!e.getExceptionDates().isEmpty()) {
			String exdate = buildExdate(e.getExceptionDates());
			Log.i(TAG, "add EXDATE " + exdate);
			values.put(Events.EXDATE, exdate);
		}

		// }

		if (re != null) {

			// Uri.Builder eventUriBuilder =
			// asSyncAdapter(Events.CONTENT_EXCEPTION_URI, account.name,
			// account.type).buildUpon();
			// ContentUris.appendId(eventUriBuilder, re.getId());

			// uri = eventUriBuilder.build();
			
			Calendar cal = Calendar.getInstance();

			values.put(Events.ORIGINAL_ID, re.getId());
			// values.put(Events.ORIGINAL_SYNC_ID, re.e.getUid().getValue());
			
			Date recurrenceID = e.getRecurrenceId().getValue();
			cal.setTime(recurrenceID);
			int recIdYear 	= cal.get(Calendar.YEAR);
			int recIdMonth 	= cal.get(Calendar.MONTH);
			int recIdDay 	= cal.get(Calendar.DAY_OF_MONTH);
			int recIdDstOff = cal.get(Calendar.DST_OFFSET);
			
			
			String rrule = "RRULE:" + buildRrule(re.e);
			Date startRec = re.e.getDateStart().getValue();
			TimeZone tz = TimeZone.getTimeZone(re.e.getDateStart().getTimezoneId());
			DateIterator dif = null;
			
			long dateStartOriginal = 0;
			try {
				dif = DateIteratorFactory.createDateIterator(rrule, startRec, tz, false);
				
				while(dif.hasNext()){
					Date d = dif.next();
					
					cal.setTime(d);
					int year 	= cal.get(Calendar.YEAR);
					int month = cal.get(Calendar.MONTH);
					int day 	= cal.get(Calendar.DAY_OF_MONTH);
					int dstOff = cal.get(Calendar.DST_OFFSET);
					
					if(recIdYear == year && recIdMonth == month && recIdDay == day){
						
						if(recIdDstOff != dstOff){
							Log.e(TAG, "\t WIRED_SHIT   recIdDstOff=" + recIdDstOff + " != dstOff="+dstOff);
						}	
						dateStartOriginal = d.getTime() + (3600000 - dstOff);
						Log.e(TAG, "\t DST_OFF  = " + dstOff);
						Log.e(TAG, "\t START DATE  = " + d.toString());
						Log.e(TAG, "\t START DATE  = " + d.getTime());
						values.put(Events.ORIGINAL_INSTANCE_TIME, dateStartOriginal);
						
						break;
					}
				}
				
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}	
			
			

			Log.d(TAG, "\tRECURRING: Events.ORIGINAL_ID = " + re.getId());
			Log.d(TAG, "\tRECURRING: Events.ORIGINAL_SYNC_ID " + re.e.getUid().getValue());
			Log.d(TAG, "\tRECURRING: Events.ORIGINAL_INSTANCE_TIME " + dateStartOriginal);

		}

		Uri ret = cr.insert(uri, values);

		if (ret == null) {
			Log.e(TAG, " INERT ERROR return URI is null");
			return 0;
		}else {
			//Log.d(TAG, " INERTed to " + ret.toString());
		}

		return ContentUris.parseId(ret);

	}

	public static void insertTestEvents(Context context, Account account, long calendarId) throws ParseException {

		ContentResolver cr = context.getContentResolver();
		ContentValues valuesRecurring = new ContentValues();
		ContentValues valuesException = new ContentValues();

		String uid = UUID.randomUUID().toString();
		Uri uri = asSyncAdapter(Events.CONTENT_URI, account.name, account.type);

		long start = new SimpleDateFormat("yyyy MM dd HH:mm").parse("2013 10 01 13:00 ").getTime();
		long end = new SimpleDateFormat("yyyy MM dd HH:mm").parse("2013 10 01 15:30 ").getTime();

		valuesRecurring.put(Events.DTSTART, start);
		valuesRecurring.put(Events.DTEND, end);

		valuesRecurring.put(Events.TITLE, "TEST RECURRING ");
		valuesRecurring.put(Events._SYNC_ID, uid);

		valuesRecurring.put(Events.CALENDAR_ID, calendarId);
		valuesRecurring.put(Events.EVENT_TIMEZONE, TimeZone.getDefault().toString());
		valuesRecurring.put(Events.RRULE, "FREQ=WEEKLY;UNTIL=20131022T110000Z;INTERVAL=1;BYDAY=TU;WKST=MO");

		Uri ret = cr.insert(uri, valuesRecurring);

		long id = 0;
		if (ret == null) {
			Log.e(TAG, " INERT ERROR return URI is null");
			return;

		} else {
			id = ContentUris.parseId(ret);
		}

		Uri.Builder eventUriBuilder = asSyncAdapter(Events.CONTENT_EXCEPTION_URI, account.name, account.type).buildUpon();
		ContentUris.appendId(eventUriBuilder, id);

		long startEx = new SimpleDateFormat("yyyy MM dd HH:mm").parse("2013 10 08 09:00 ").getTime();
		long startOrg = new SimpleDateFormat("yyyy MM dd HH:mm").parse("2013 10 08 13:00 ").getTime();
		long endEx = new SimpleDateFormat("yyyy MM dd HH:mm").parse("2013 10 08 11:30 ").getTime();

		valuesException.put(Events.DTSTART, startEx);
		valuesException.put(Events.DTEND, endEx);

		valuesException.put(Events.TITLE, "TEST RECURRING EXCEPTION");
		valuesException.put(Events._SYNC_ID, uid);

		valuesException.put(Events.CALENDAR_ID, calendarId);
		valuesException.put(Events.EVENT_TIMEZONE, TimeZone.getDefault().toString());

		valuesException.put(Events.ORIGINAL_ID, id);
		valuesException.put(Events.ORIGINAL_SYNC_ID, uid);
		valuesException.put(Events.ORIGINAL_INSTANCE_TIME, startOrg);

		ret = cr.insert(uri, valuesException);

	}

	/*
	 * public static ArrayList<CalendarEvent> getAllCalendarEvents(Context
	 * context, Account account, long calendarId) { Cursor cur = null;
	 * ArrayList<CalendarEvent> eventList = new ArrayList<CalendarEvent>();
	 * ContentResolver cr = context.getContentResolver();
	 * 
	 * Uri uri = asSyncAdapter(Events.CONTENT_URI, account.name, account.type);
	 * 
	 * String[] projection = new String[] { Events._ID, Events._SYNC_ID,
	 * Events.SYNC_DATA1, Events.TITLE, Events.DTSTART, Events.DTEND,
	 * Events.DESCRIPTION, Events.EVENT_LOCATION };
	 * 
	 * String selection = "((" + Events.CALENDAR_ID + " = ?) )"; String[]
	 * selectionArgs = new String[] { Long.toString(calendarId) };
	 * 
	 * cur = cr.query(uri, projection, selection, selectionArgs, null);
	 * 
	 * if (cur.moveToFirst()) {
	 * 
	 * String eventUid = ""; long eventId = 0; long eventTimeStamp = 0; long
	 * start = 0; long end = 0; String title = ""; String location = ""; String
	 * description = "";
	 * 
	 * do {
	 * 
	 * eventId = cur.getLong(0); eventUid = cur.getString(1); eventTimeStamp =
	 * Long.parseLong(cur.getString(2)); title = cur.getString(3); start =
	 * cur.getLong(4); end = cur.getLong(5); description = cur.getString(6);
	 * location = cur.getString(7);
	 * 
	 * eventList.add(new CalendarEvent(eventId, eventUid, eventTimeStamp, start,
	 * end, title, description, location)); } while (cur.moveToNext());
	 * 
	 * } else { Log.d(TAG, "getAllCalendarEvents() courser is empty");
	 * 
	 * }
	 * 
	 * cur.close(); return eventList;
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * /* private static long getEventByUID(Context context, Account account,
	 * String id) {
	 * 
	 * Cursor cur = null; ContentResolver cr = context.getContentResolver();
	 * 
	 * Uri uri = asSyncAdapter(Events.CONTENT_URI, account.name, account.type);
	 * 
	 * String[] projection = new String[] { Events._ID, Events._SYNC_ID};
	 * 
	 * String selection = "((" + Events._SYNC_ID + " = ?) )"; String[]
	 * selectionArgs = new String[] { Long.toString(calendarId) };
	 * 
	 * cur = cr.query(uri, projection, selection, selectionArgs, null);
	 * 
	 * if (cur.moveToFirst()) {
	 * 
	 * 
	 * 
	 * do {
	 * 
	 * 
	 * 
	 * eventList.add(new CalendarEvent(eventId, eventUid, eventTimeStamp, start,
	 * end, title, description, location));
	 * 
	 * } while (cur.moveToNext());
	 * 
	 * } else { Log.d(TAG, "getAllCalendarEvents() courser is empty");
	 * 
	 * }
	 * 
	 * cur.close(); return eventList;
	 * 
	 * return 0; }
	 */

	/*
	 * public static void updateEvent(Context context, Account account, long
	 * calendarId, CalendarEvent event) {
	 * 
	 * ContentResolver cr = context.getContentResolver(); ContentValues values =
	 * new ContentValues(); Uri uri = asSyncAdapter(Events.CONTENT_URI,
	 * account.name, account.type);
	 * 
	 * String where = "((" + Events._ID + " = ?) AND  (" + Events._SYNC_ID +
	 * " = ?) )";
	 * 
	 * String[] selectionArgs = new String[] { Long.toString(event.getId()),
	 * event.getUid() };
	 * 
	 * values.put(Events.DTSTART, event.getStartInMillis());
	 * values.put(Events.DTEND, event.getEndInMillis());
	 * values.put(Events.TITLE, event.getTitle());
	 * values.put(Events.DESCRIPTION, event.getDescription());
	 * values.put(Events.CALENDAR_ID, calendarId);
	 * values.put(Events.EVENT_LOCATION, event.getLocation()); //
	 * values.put(Events._SYNC_ID, event.getUid());
	 * values.put(Events.SYNC_DATA1, Long.toString(event.getTimeStamp()));
	 * values.put(Events.EVENT_TIMEZONE, TimeZone.getDefault().toString());
	 * 
	 * cr.update(uri, values, where, selectionArgs);
	 * 
	 * }
	 */

}
