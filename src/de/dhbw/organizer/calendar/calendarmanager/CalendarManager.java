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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.util.Log;
import biweekly.component.VEvent;
import biweekly.property.Classification;
import biweekly.property.ExceptionDates;
import biweekly.property.Priority;
import biweekly.property.Status;
import biweekly.property.Transparency;
import biweekly.util.Duration;
import biweekly.util.Recurrence;
import biweekly.util.Recurrence.DayOfWeek;

//A compatibility layer for joda-time
import com.google.ical.compat.javautil.DateIterator;
import com.google.ical.compat.javautil.DateIteratorFactory;

import de.dhbw.organizer.calendar.Constants;
import de.dhbw.organizer.calendar.objects.RecurringVEvent;
import de.dhbw.organizer.calendar.objects.SpinnerItem;

/**
 * @author schoko This class has only static functions to help handle all the
 *         calndar stuff
 */
public class CalendarManager {
	private static final String TAG = "CalendarManager";

	private static final String XML_SCHEMA_VERSION = "1.0";

	private static final String ASSET_XML_SCHEMA_CALENDAR_LIST = "xsd/calendar_calendarlist.xsd";

	private static final String ASSET_DEFAULT_CALENDAR_LIST = "xml/calendar_calendars.xml";

	private static final String DATA_EXTERN_CALENDAR_LIST = "xml/calendar_calendars.xml";

	// private static final

	private Context mContext = null;

	public CalendarManager(Context context) {
		mContext = context;
	}

	public List<SpinnerItem> getSelectableCalendars2() throws ParserConfigurationException {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();

		Document doc = db.newDocument();

		return null;
	}

	public List<SpinnerItem> getSelectableCalendars() {
		Log.d(TAG, "getSelectableCalendars()");
		AssetManager assetManager = mContext.getAssets();
		InputStream isCalendarsDefaultXml = null;
		InputStream isCalendarsXSD = null;

		ArrayList<SpinnerItem> selectableCalendars = null;

		try {
			// get Validator for validating XML Schema
			isCalendarsXSD = assetManager.open(ASSET_XML_SCHEMA_CALENDAR_LIST);
			StreamSource ssCalendarXsd = new StreamSource(isCalendarsXSD);

			/*
			 * SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			 * SchemaFactory factory =
			 * SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			 * Schema schema = factory.newSchema(ssCalendarXsd); Validator
			 * validator = schema.newValidator();
			 * 
			 * StreamSource ssXMLDefault = new
			 * StreamSource(assetManager.open(ASSET_DEFAULT_CALENDAR_LIST));
			 * 
			 * validator.validate(ssXMLDefault);
			 */
			isCalendarsDefaultXml = assetManager.open(ASSET_DEFAULT_CALENDAR_LIST);

			XmlPullParserFactory parserfactory = XmlPullParserFactory.newInstance();
			XmlPullParser parser = parserfactory.newPullParser();
			parser.setInput(new InputStreamReader(isCalendarsDefaultXml));

			String xmlNameSpace = parser.getNamespace();
			parser.next();
			String xmlVersion = parser.getAttributeValue(xmlNameSpace, "Version");
			String xmlLastUpdate = parser.getAttributeValue(xmlNameSpace, "LastUpdate");

			Log.i(TAG, "XMLversion = " + xmlVersion + "  xmlLastUpdate = " + xmlLastUpdate + " Namsespace = " + xmlNameSpace);

			CalendarXmlParser calParser = new CalendarXmlParser(xmlNameSpace);

			selectableCalendars = calParser.readCalendars(parser);

		} catch (IOException e) {
			Log.e(TAG, "IOException " + e.getMessage());
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			Log.e(TAG, "XmlPullParserException " + e.getMessage());
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "IllegalArgumentException " + e.getMessage());
		}

		return selectableCalendars;
	}

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

		// NO RRULE, NO RECURRING_ID
		ArrayList<VEvent> recularEvents = new ArrayList<VEvent>();

		ArrayList<RecurringVEvent> recurringEvents = new ArrayList<RecurringVEvent>();
		ArrayList<VEvent> tempEventList = new ArrayList<VEvent>();

		/**
		 * split events into recurring events and regular events
		 */
		for (VEvent e : eventList) {

			// no RRULE or nor RECURRENCE-ID
			if (e.getRecurrenceRule() == null && e.getRecurrenceId() == null) {
				recularEvents.add(e);
			}

			else {

				if (e.getRecurrenceRule() != null) {
					// Log.d(TAG, "Recurring: ");
					// Log.d(TAG, "\t event: " + e.getSummary().getValue() +
					// "  " + e.getDateStart().getValue().toString());
					// Log.d(TAG, "\t e.getRRule() = " + buildRrule(e));

					recurringEvents.add(new RecurringVEvent(e));

				} else if (e.getRecurrenceId() != null) {

					// Log.d(TAG, "Exception: ");
					// Log.d(TAG, "\t event: " + e.getSummary().getValue() +
					// "  " + e.getDateStart().getValue().toString());
					// Log.d(TAG, "\t e.getRecurrenceId() = " +
					// e.getRecurrenceId().getValue().toString());

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
			// Log.d(TAG, "INSERT RECURING " + re.e.getSummary().getValue());
			ArrayList<VEvent> atomarEventList = seperateEvents(re);

			for (VEvent e : atomarEventList) {
				long id = insertEvent(context, account, calendarId, e);
				re.setId(id);
			}
		}

		Log.d(TAG, "insert recular Events ");
		// insertEventsAsBatch(context, account, calendarId, recularEvents);
		for (VEvent e : recularEvents) {
			insertEvent(context, account, calendarId, e);
		}

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
				// tz = TimeZone.getTimeZone(ed.getTimezoneId()).getID();
				tz = TimeZone.getDefault().getID();
			}

			if (tz != null) {
				sb.append("TZID=").append(tz).append(':');
			}

			for (Iterator<Date> iterator = dates.iterator(); iterator.hasNext();) {
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

			if (r.getWorkweekStarts() != null)
				sb.append(";WKST=").append(r.getWorkweekStarts().getAbbr());

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

			return sb.toString();
		} else {
			return "";
		}

	}

	private static ArrayList<VEvent> seperateEvents(RecurringVEvent rve) {

		ArrayList<VEvent> cleanEventList = new ArrayList<VEvent>();

		if (rve != null && rve.e != null && rve.e.getRecurrenceRule() != null) {

			ArrayList<VEvent> exceptions = rve.getExceptions();
			ArrayList<VEvent> atomarEvents = seperateRecurringEvent(rve.e);

			Calendar cal = Calendar.getInstance();
			for (VEvent atom : atomarEvents) {

				cal.setTime(atom.getDateStart().getValue());
				int atomYear = cal.get(Calendar.YEAR);
				int atomMonth = cal.get(Calendar.MONTH);
				int atomDay = cal.get(Calendar.DAY_OF_MONTH);
				// int atomDstOff = cal.get(Calendar.DST_OFFSET);

				boolean inserted = false;

				for (VEvent ex : exceptions) {
					if (ex.getRecurrenceId() == null) {
						Log.e(TAG, "MISSING RecurrenceID HELP! in seperateEvents() " + ex.getSummary().getValue() + " on Date "
								+ ex.getDateStart().getValue().toString());
						break;
					}
					Date recId = ex.getRecurrenceId().getValue();

					cal.setTime(recId);
					int exYear = cal.get(Calendar.YEAR);
					int exMonth = cal.get(Calendar.MONTH);
					int exDay = cal.get(Calendar.DAY_OF_MONTH);
					// int exDstOff = cal.get(Calendar.DST_OFFSET);

					// this here should always be the case!
					if (ex.getUid().getValue().equals(atom.getUid().getValue())) {

						// found atomar Event of reccuring, for which this
						// exception fits
						if (atomYear == exYear && atomMonth == exMonth && atomDay == exDay) {
							// ex.removeProperties(RecurrenceId.class);
							cleanEventList.add(ex);
							inserted = true;
							// if (!atomarEvents.remove(atom)) {
							// Log.e(TAG,
							// "can't remove VEvent from atomarEvents List");
							// }
							break;
						}

					} else {
						Log.e(TAG, "Missmatching UID in seperateEvents o0 WTF?");
					}
				}// inner for
				if (!inserted) {
					cleanEventList.add(atom);
				}
			}

		} else {
			Log.e(TAG, "ERROR rve != null && rve.e != null && rve.e.getRecurrenceRule() != null");
			// add single Event?
		}
		/*
		 * for (VEvent e : cleanEventList) { Log.d(TAG, "RECURRING EVENT : " +
		 * e.getSummary().getValue() + "  " +
		 * e.getDateStart().getValue().toString()); }
		 */
		return cleanEventList;
	}

	/**
	 * seperates a single recurring Event with an RRULE and EXDATE into a List
	 * of SingleEvents
	 * 
	 * @author friedrda
	 * @param recurringEvent
	 * @return List of events
	 */
	private static ArrayList<VEvent> seperateRecurringEvent(VEvent recurringEvent) {

		ArrayList<VEvent> atomarEvents = new ArrayList<VEvent>();

		if (recurringEvent != null && recurringEvent.getRecurrenceRule() != null && recurringEvent.getRecurrenceId() == null) {

			Date startDate = recurringEvent.getDateStart().getValue();
			Date endDate = recurringEvent.getDateEnd().getValue();

			Duration duration = Duration.diff(startDate, endDate);

			String title = recurringEvent.getSummary().getValue();
			String description = "";
			if (recurringEvent.getDescription() != null) {
				description = recurringEvent.getDescription().getValue();
			}
			String location = "";
			if (recurringEvent.getLocation() != null) {
				location = recurringEvent.getLocation().getValue();
			}
			String rdata = "RRULE:" + buildRrule(recurringEvent);

			if (recurringEvent.getExceptionDates() != null && recurringEvent.getExceptionDates().size() > 0) {
				rdata = rdata + "\nEXDATE;" + buildExdate(recurringEvent.getExceptionDates());
			}

			String uid = recurringEvent.getUid().getValue();

			Status status = recurringEvent.getStatus();

			Classification eventCalss = recurringEvent.getClassification();

			Priority prio = recurringEvent.getPriority();

			int sequence = recurringEvent.getSequence().getValue();
			Transparency transp = recurringEvent.getTransparency();

			List<String> categories = new ArrayList<String>();

			if (recurringEvent.getCategories() != null && !recurringEvent.getCategories().isEmpty())
				categories = recurringEvent.getCategories().get(0).getValues();

			TimeZone tz = TimeZone.getTimeZone(recurringEvent.getDateStart().getTimezoneId());

			try {
				Calendar cal = Calendar.getInstance();
				cal.setTime(startDate);

				int startDstOffset = cal.get(Calendar.DST_OFFSET);
				DateIterator dif = DateIteratorFactory.createDateIterator(rdata, startDate, tz, true);

				// Log.d(TAG, "EVENT: " + recurringEvent.getSummary().getValue()
				// + " Recurrs on: ");

				while (dif.hasNext()) {
					Date d = dif.next();

					cal.setTime(d);

					int dstOffset = cal.get(Calendar.DST_OFFSET);
					// create VEvent with this date
					VEvent e = new VEvent();
					e.setUid(uid);
					e.setSummary(title);
					e.setDescription(description);
					e.setLocation(location);

					e.setStatus(status);
					e.setClassification(eventCalss);
					e.setPriority(prio);
					e.setDateTimeStamp(new Date());
					e.setSequence(sequence);
					e.setTransparency(transp);
					e.addCategories(categories);

					if (startDstOffset != dstOffset) {
						// Log.i(TAG, "startDstOffset = " + startDstOffset +
						// "\t dstOffset = " + dstOffset);
					}
					e.setDateStart(new Date(d.getTime() + startDstOffset - dstOffset));

					Date end = new Date(d.getTime() + duration.toMillis() + startDstOffset - dstOffset);
					// Log.d(TAG, "Date FROM " + d.toString() + "  to: " +
					// end.toString());

					e.setDateEnd(end);

					atomarEvents.add(e);

				}
			} catch (ParseException e1) {
				e1.printStackTrace();
			}

		} else {
			Log.e(TAG, "NULL ERROR");
		}
		return atomarEvents;

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

	public static long insertEvent(Context context, Account account, long calendarId, VEvent e) {

		ContentResolver cr = context.getContentResolver();
		ContentValues values = new ContentValues();
		Uri uri = asSyncAdapter(Events.CONTENT_URI, account.name, account.type);

		values.put(Events.CALENDAR_ID, calendarId);
		values.put(Events.TITLE, e.getSummary().getValue());
		values.put(Events.DTSTART, e.getDateStart().getValue().getTime());
		values.put(Events.DTEND, e.getDateEnd().getValue().getTime());
		values.put(Events._SYNC_ID, e.getUid().getValue());
		values.put(Events.SYNC_DATA1, Long.toString(e.getDateTimeStamp().getValue().getTime()));

		if (e.getDescription() != null)
			values.put(Events.DESCRIPTION, e.getDescription().getValue());

		if (e.getLocation() != null)
			values.put(Events.EVENT_LOCATION, e.getLocation().getValue());

		values.put(Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());

		Uri ret = cr.insert(uri, values);

		if (ret == null) {
			Log.e(TAG, " INERT ERROR return URI is null");
			return 0;
		} else {
			// Log.d(TAG, " INERTed to " + ret.toString());
		}

		return ContentUris.parseId(ret);

	}

	static class CalendarXmlParser {

		private static final String XML_TAG_CALENDARS = "Calendars";
		private static final String XML_TAG_DISPLAY_NAME = "DisplayName";
		private static final String XML_TAG_ICAL_URL = "iCalUrl";
		private static final String XML_TAG_CALENDAR = "Calendar";
		private String mNameSpace = "";

		public CalendarXmlParser(String nameSpace) {
			this.mNameSpace = nameSpace;
		}

		public ArrayList<SpinnerItem> readCalendars(XmlPullParser parser) throws XmlPullParserException, IOException {
			ArrayList<SpinnerItem> cals = new ArrayList<SpinnerItem>();

			parser.require(XmlPullParser.START_TAG, mNameSpace, XML_TAG_CALENDARS);
			while (parser.next() != XmlPullParser.END_TAG) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String name = parser.getName();
				// Starts by looking for the entry tag
				if (name.equals(XML_TAG_CALENDAR)) {
					cals.add(readCalendar(parser));
				} else {
					skip(parser);
				}
			}

			return cals;

		}

		private SpinnerItem readCalendar(XmlPullParser parser) throws XmlPullParserException, IOException {

			parser.require(XmlPullParser.START_TAG, mNameSpace, XML_TAG_CALENDAR);
			String displayName = null;
			String iCalUrl = null;

			while (parser.next() != XmlPullParser.END_TAG) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String name = parser.getName();
				if (name.equals(XML_TAG_DISPLAY_NAME)) {
					displayName = readDisplayName(parser);
				} else if (name.equals(XML_TAG_ICAL_URL)) {
					iCalUrl = readICalUrl(parser);

				} else {
					skip(parser);
				}
			}
			return new SpinnerItem(displayName, iCalUrl);
		}

		private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				throw new IllegalStateException();
			}
			int depth = 1;
			while (depth != 0) {
				switch (parser.next()) {
				case XmlPullParser.END_TAG:
					depth--;
					break;
				case XmlPullParser.START_TAG:
					depth++;
					break;
				}
			}
		}

		private String readICalUrl(XmlPullParser parser) throws XmlPullParserException, IOException {

			parser.require(XmlPullParser.START_TAG, mNameSpace, XML_TAG_ICAL_URL);
			String iCalUrl = readText(parser);
			parser.require(XmlPullParser.END_TAG, mNameSpace, XML_TAG_ICAL_URL);
			return iCalUrl;
		}

		private String readDisplayName(XmlPullParser parser) throws XmlPullParserException, IOException {

			parser.require(XmlPullParser.START_TAG, mNameSpace, XML_TAG_DISPLAY_NAME);
			String displayName = readText(parser);
			parser.require(XmlPullParser.END_TAG, mNameSpace, XML_TAG_DISPLAY_NAME);
			return displayName;
		}

		// For the tags title and summary, extracts their text values.
		private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
			String result = "";
			if (parser.next() == XmlPullParser.TEXT) {
				result = parser.getText();
				parser.nextTag();
			}
			return result;
		}
	}

}
