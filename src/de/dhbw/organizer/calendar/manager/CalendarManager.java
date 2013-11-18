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
package de.dhbw.organizer.calendar.manager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import mf.javax.xml.transform.Source;
import mf.javax.xml.transform.stream.StreamSource;
import mf.javax.xml.validation.Schema;
import mf.javax.xml.validation.SchemaFactory;
import mf.javax.xml.validation.Validator;
import mf.org.apache.xerces.jaxp.validation.XMLSchemaFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.SAXException;
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
import android.net.http.AndroidHttpClient;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.util.Base64;
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

import com.google.ical.compat.javautil.DateIterator;
import com.google.ical.compat.javautil.DateIteratorFactory;

import de.dhbw.organizer.calendar.Constants;
import de.dhbw.organizer.calendar.objects.RecurringVEvent;
import de.dhbw.organizer.calendar.objects.SpinnerItem;

/**
 * This class has only functions to help handle all the calendar stuff
 * 
 * @author friedrda
 * 
 */
public class CalendarManager {
	private static final String TAG = "CalendarManager";

	private static final String EVENTS_DB_TIME_STEMP_COLUMN = Events.SYNC_DATA1;

	private static final String EVENTS_DB_HASH_COLUMN = Events.SYNC_DATA2;

	private static final String XML_SCHEMA_VERSION = "1.0";

	private static final String ASSET_XML_SCHEMA_CALENDAR_LIST = "xsd/calendar_calendarlist.xsd";

	private static final String ASSET_DEFAULT_CALENDAR_LIST = "xml/calendar_calendars.xml";

	private static final String DATA_EXTERN_CALENDAR_LIST = "calendar_calendars.xml";

	/**
	 * TIME OFFSET from to day, this is used, when a Recurring event has no
	 * COUNT or UNTIL so we don't try to add ad infinti amount of events
	 */
	private static final int EVENT_MAX_UNTIL_OFFSET_IN_YEAR = 5;

	private Context mContext = null;

	private CalendarManager(Context context) {
		mContext = context;
	}

	/**
	 * Returns a new Instance of the CalendarManager with the given Context
	 * 
	 * @param context
	 * @return CalendarManager instance
	 */
	public static CalendarManager get(Context context) {
		return new CalendarManager(context);
	}

	/**
	 * validates a xml file with and xmlSchema Thanks to: James Oravec
	 * http://stackoverflow.com/questions/801144/android-schema-validation
	 * 
	 * @param xmlFilePath
	 * @param xmlSchemaFilePath
	 * @return true if valid, false otherwise
	 */
	private static boolean validate(InputStream xmlInputStream, InputStream xmlSchemaInputStream) {

		// Try the validation, we assume that if there are any issues with the
		// validation
		// process that the input is invalid.
		try {
			SchemaFactory factory = new XMLSchemaFactory();
			Source schemaFile = new StreamSource(xmlSchemaInputStream);
			Source xmlSource = new StreamSource(xmlInputStream);
			Schema schema = factory.newSchema(schemaFile);
			Validator validator = schema.newValidator();
			validator.validate(xmlSource);
		} catch (SAXException e) {
			return false;
		} catch (IOException e) {
			return false;
		} catch (Exception e) {
			// Catches everything beyond: SAXException, and IOException.
			e.printStackTrace();
			return false;
		} catch (Error e) {
			// Needed this for debugging when I was having issues with my 1st
			// set of code.
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * access XML-Files and extract the possible calendras (DisplayName und
	 * iCal-Url) if an extern XML exists and is valid
	 * "asstes/xsd/calendar_calendarlist.xsd" this file will be taken, if not
	 * the local XML ("assets/xml/calendar_calendars.xml") is taken
	 * 
	 * @return returns a List of SpinnerItems
	 * @throws IOException
	 */
	public List<SpinnerItem> getSelectableCalendars() throws IOException {

		Log.d(TAG, "getSelectableCalendars()");
		boolean takeLocalXml = false;

		AssetManager assetManager = mContext.getAssets();
		InputStream isCalendarsDefaultXml = null;
		FileInputStream isExternalCalList = null;
		InputStream isCalendarsXSD = null;
		XmlPullParserFactory parserfactory = null;
		XmlPullParser parser = null;

		ArrayList<SpinnerItem> selectableCalendars = null;

		// open XML-Schema and default XML-File
		try {
			isCalendarsDefaultXml = assetManager.open(ASSET_DEFAULT_CALENDAR_LIST);
			isCalendarsXSD = assetManager.open(ASSET_XML_SCHEMA_CALENDAR_LIST);

			parserfactory = XmlPullParserFactory.newInstance();
			parser = parserfactory.newPullParser();
		} catch (IOException e1) {
			Log.e(TAG, "cant open calendar default xml and Xml-Schema");
			throw new IOException();

		} catch (XmlPullParserException e) {
			Log.e(TAG, "cant greate XMLParser " + e.getMessage());
			throw new IOException();
		}

		// check if external file exitsts, and is valid
		try {

			isExternalCalList = mContext.openFileInput(DATA_EXTERN_CALENDAR_LIST);

			if (validate(isExternalCalList, isCalendarsXSD)) {
				// after validation, we need to reset the InputStreams
				isExternalCalList.close();
				isExternalCalList = mContext.openFileInput(DATA_EXTERN_CALENDAR_LIST);
				Log.i(TAG, "XML External is Valid");
				parser.setInput(new InputStreamReader(isExternalCalList));

			} else {
				Log.i(TAG, "XML External is not Valid");
				Log.e(TAG, "failed to parse external calendar xml, take locale one");
				takeLocalXml = true;

			}

		} catch (FileNotFoundException e) {
			Log.i(TAG, "no external File found, so take locale calendar list");
			takeLocalXml = true;

		} catch (XmlPullParserException e) {
			Log.e(TAG, "failed to set input in parser");
			throw new IOException();
		} finally {
			isCalendarsXSD.close();
		}

		if (takeLocalXml == true) {
			isCalendarsXSD = assetManager.open(ASSET_XML_SCHEMA_CALENDAR_LIST);
			if (validate(isCalendarsDefaultXml, isCalendarsXSD)) {
				isCalendarsDefaultXml.close();
				isCalendarsDefaultXml = assetManager.open(ASSET_DEFAULT_CALENDAR_LIST);
				Log.i(TAG, "XML Default is Valid");
				try {
					parser.setInput(new InputStreamReader(isCalendarsDefaultXml));
				} catch (XmlPullParserException e) {
					Log.e(TAG, "failed to set input in parser");
					throw new IOException();
				}
			} else {
				throw new IOException();
			}
		}
		isCalendarsXSD.close();

		try {

			String xmlNameSpace = parser.getNamespace();
			parser.next();
			String xmlVersion = parser.getAttributeValue(xmlNameSpace, "Version");
			String xmlLastUpdate = parser.getAttributeValue(xmlNameSpace, "LastUpdate");

			Log.i(TAG, "XMLversion = " + xmlVersion + "  xmlLastUpdate = " + xmlLastUpdate + " Namsespace = " + xmlNameSpace);

			if (!xmlVersion.equals(XML_SCHEMA_VERSION)) {
				Log.e(TAG, "XML VERSION don't match, error!");
				throw new IOException();
			}

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
		} finally {
			// close inputstreams
			isCalendarsDefaultXml.close();
			if (isExternalCalList != null)
				isExternalCalList.close();

		}

		return selectableCalendars;
	}

	public boolean loadExternalXml() {
		boolean success = false;
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(Constants.EXTERNAL_CALENDAR_LIST_URL);

		if (!httpGet.containsHeader("Accept-Encoding")) {
			httpGet.addHeader("Accept-Encoding", "gzip");
		}

		Log.i(TAG, "http execute()");
		HttpResponse httpResponse;
		try {
			httpResponse = httpClient.execute(httpGet);

			int httpStatus = httpResponse.getStatusLine().getStatusCode();

			HttpEntity entity = httpResponse.getEntity();

			if (httpStatus == 200 && entity != null) {

				InputStream instream = AndroidHttpClient.getUngzippedContent(entity);

				FileOutputStream fos = mContext.openFileOutput(DATA_EXTERN_CALENDAR_LIST, Context.MODE_PRIVATE);
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));

				BufferedReader reader = new BufferedReader(new InputStreamReader(instream));
				String s = "";
				while ((s = reader.readLine()) != null) {
					writer.write(s);
				}
				reader.close();
				writer.close();

				fos.flush();
				fos.close();
				instream.close();
				success = true;

			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			return success;
		}

	}

	/**
	 * check if a calendar already exists
	 * 
	 * @param mContext
	 * @param account
	 * @return
	 */
	public boolean calendarExists(Account account) {

		loadExternalXml();

		Cursor cur = null;
		boolean calendarExists = false;

		ContentResolver cr = mContext.getContentResolver();

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
	 * @param mContext
	 * @param account
	 * @return id
	 */
	public long getCalendarId(Account account) {

		Cursor cur = null;

		long calendarId = -1;

		ContentResolver cr = mContext.getContentResolver();

		String[] projection = new String[] { Calendars._ID };
		String selection = "((" + Calendars.ACCOUNT_NAME + " = ?)  AND (" + Calendars.ACCOUNT_TYPE + " = ?) )";
		String[] selectionArgs = new String[] { account.name, account.type };

		cur = cr.query(Calendars.CONTENT_URI, projection, selection, selectionArgs, null);

		if (cur.getCount() == 1 && cur.moveToFirst()) {
			calendarId = cur.getLong(0);

		} else {
			Log.e(TAG, "getCalendarId() FATAL ERROR cur.getCount()=" + cur.getCount());
		}

		cur.close();
		return calendarId;
	}

	/**
	 * creates a new Calendar with the given account
	 * 
	 * @param account
	 * @param mContext
	 * @return calendar id
	 */
	public long createCalendar(Account account) {
		Log.d(TAG, "Calendar With by Account Name");

		ContentResolver cr = mContext.getContentResolver();

		Uri creationUri = asSyncAdapter(Calendars.CONTENT_URI, account.name, account.type);

		int color = getNextCalendarColor();
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
	 * Deletes all Events from the EventsDB and the Calendar from the CalendarDB
	 * identified by the _ID and ACCOUNT_TYPE
	 * 
	 * @param mContext
	 * @param account
	 * @return true if delete was successful
	 */
	public boolean deleteCalendar(Account account, long calendarId) {

		deleteAllEvents(account, calendarId);

		ContentResolver cr = mContext.getContentResolver();
		String selection = "((" + Calendars._ID + " = ?) AND  (" + Calendars.ACCOUNT_TYPE + " = ?))";
		String[] selectionArgs = new String[] { Long.toString(calendarId), account.type };

		long ret = cr.delete(Calendars.CONTENT_URI, selection, selectionArgs);

		if (ret == 1) {
			return true;
		} else if (ret == 0) {
			return false;
		} else {
			Log.w(TAG, "WARNING deleteCalendar() deleted " + ret + " rows, should be only one!");
			return true;
		}

	}

	/**
	 * Deletes all Events from a given CalendarID
	 * 
	 * @param account
	 * @param calendarId
	 * @return returns true if deletion was scussessful
	 */
	public boolean deleteAllEvents(Account account, long calendarId) {
		Log.d(TAG, "delte all Events from CalendarId = " + calendarId);

		ContentResolver cr = mContext.getContentResolver();
		Uri uri = asSyncAdapter(Events.CONTENT_URI, account.name, account.type);
		String where = "( ( " + Events.CALENDAR_ID + " = ? ))";
		String[] selectionArgs = new String[] { Long.toString(calendarId) };

		int del = cr.delete(uri, where, selectionArgs);

		Log.d(TAG, "deleted " + del + " Events");

		if (del > 0) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * Inserts a List of VEvents into the Database, but splits and seperates
	 * recurring Events into Single events
	 * 
	 * @param account
	 * @param calendarId
	 * @param eventList
	 */
	public void insertEvents(Account account, long calendarId, ArrayList<VEvent> eventList) {

		ArrayList<VEvent> atomarEventList = seperateAllEvents(eventList);
		Log.d(TAG, "insertEvents() " + atomarEventList.size() + " events to add in total");

		// TODO BATCH insert
		for (VEvent e : atomarEventList) {
			insertEvent(account, calendarId, e);
		}
	}

	/**
	 * only updates the Events from the list to the Database, based upon the
	 * hashCode
	 * 
	 * @param account
	 * @param calendarId
	 * @param eventList
	 */
	public void updateEvents(Account account, long calendarId, ArrayList<VEvent> eventList) {

		ArrayList<VEvent> atomarEventList = seperateAllEvents(eventList);
		ArrayList<String> hashList = getHashOfEventsInDb(account, calendarId);

		ArrayList<VEvent> eventsToInsert = new ArrayList<VEvent>();

		for (VEvent e : atomarEventList) {
			String hash = calcEventHash(e);

			// this event is already in the DB
			// so remove it from the hash list
			if (hashList.contains(hash)) {
				hashList.remove(hash);
			}
			// event is not in the DB, so add it
			else {
				eventsToInsert.add(e);
			}
		}

		// remaining event in the hashList can be removed from the DB
		Log.i(TAG, "Upodate()  delete " + hashList.size() + " events");
		for (String s : hashList) {

			deleteEventByHash(account, calendarId, s);
		}

		// eventsTo insert need to be added to the DB
		Log.i(TAG, "Upodate()  insert " + eventsToInsert.size() + " events");
		for (VEvent e : eventsToInsert) {
			insertEvent(account, calendarId, e);
		}

	}

	/**
	 * Deletes an event from the db identified by calendar-ID and the events
	 * Hash-String
	 * 
	 * @param account
	 * @param calendarId
	 * @param hash
	 * @return
	 */
	private boolean deleteEventByHash(Account account, long calendarId, String hash) {

		if (hash == null) {
			return false;
		}

		ContentResolver cr = mContext.getContentResolver();
		Uri uri = asSyncAdapter(Events.CONTENT_URI, account.name, account.type);
		String where = "( ( " + Events.CALENDAR_ID + " = ? ) AND ( " + EVENTS_DB_HASH_COLUMN + " = ? ))";
		String[] selectionArgs = new String[] { Long.toString(calendarId), hash };

		int del = cr.delete(uri, where, selectionArgs);

		if (del > 0) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * seperates an event list into single events, but first
	 * 
	 * @param eventList
	 * @return
	 */
	private ArrayList<VEvent> seperateAllEvents(ArrayList<VEvent> eventList) {

		ArrayList<RecurringVEvent> recurringEvents = new ArrayList<RecurringVEvent>();
		ArrayList<VEvent> tempEventList = new ArrayList<VEvent>();

		ArrayList<VEvent> atomarEventList = new ArrayList<VEvent>();

		/**
		 * split events into recurring events and regular events
		 */
		for (VEvent e : eventList) {

			// no RRULE or nor RECURRENCE-ID
			// we keep those seperate, to avoid unneeded processing
			// regular events can be inserted without any problems
			if (e.getRecurrenceRule() == null && e.getRecurrenceId() == null) {
				atomarEventList.add(e);
			}

			else {

				if (e.getRecurrenceRule() != null) {
					// this event is a recurring one,
					// it has an RRULE so we need to seperate these
					recurringEvents.add(new RecurringVEvent(e));

				} else if (e.getRecurrenceId() != null) {
					// this event is an exception of an recurring event
					// e.g.Event is evey monday at 9:00 but this time its at
					// 13:00
					// so we need to find the fitting recurring Event and add id
					// for later processing

					boolean isInserted = false;
					for (RecurringVEvent re : recurringEvents) {
						if (re.e.getUid().getValue().equals(e.getUid().getValue())) {
							re.addException(e);
							isInserted = true;
							break;
						}
					}
					if (isInserted == false) {
						// if the exception was bevor the recurring event, we
						// check later again
						tempEventList.add(e);
					}
				}
			}
		}

		// add last exceptions to recurring event,
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

		// Seperate recurring events
		// check and replece single event with exception, if it fits
		// add them to teh DB
		Log.d(TAG, "insert recurring Events ");
		for (RecurringVEvent re : recurringEvents) {
			atomarEventList.addAll(seperateEvents(re));

		}

		return atomarEventList;

	}

	private ArrayList<String> getHashOfEventsInDb(Account account, long calendarID) {

		Cursor cur = null;
		ArrayList<String> listOfEventHashes = new ArrayList<String>();

		ContentResolver cr = mContext.getContentResolver();

		String[] projection = new String[] { EVENTS_DB_HASH_COLUMN };
		String selection = "((" + Events.CALENDAR_ID + " = ?))";
		String[] selectionArgs = new String[] { Long.toString(calendarID) };

		cur = cr.query(Events.CONTENT_URI, projection, selection, selectionArgs, null);

		if (cur.getCount() > 0 && cur.moveToFirst()) {
			do {
				listOfEventHashes.add(cur.getString(0));
			} while (cur.moveToNext());

		} else {
			Log.d(TAG, "FATAL ERROR");
		}

		cur.close();
		return listOfEventHashes;
	}

	/**
	 * calcs hashvalue of the concatinated values of Summary Description
	 * Location DateStart DateEnd
	 * 
	 * @param e
	 * @return sha1 hash as String
	 */
	private String calcEventHash(VEvent e) {
		StringBuilder sb = new StringBuilder();

		sb.append(e.getSummary().getValue());
		if (e.getDescription() != null)
			sb.append(e.getDescription().getValue());
		if (e.getLocation() != null)
			sb.append(e.getLocation().getValue());
		sb.append(e.getDateStart().getValue().toString());
		sb.append(e.getDateEnd().getValue().toString());

		byte[] eventByteString = sb.toString().getBytes(Charset.defaultCharset());

		MessageDigest md = null;

		try {
			md = MessageDigest.getInstance("SHA1");

			byte[] sha1 = Base64.encode(md.digest(eventByteString), Base64.DEFAULT);

			return new String(sha1);
		} catch (NoSuchAlgorithmException e1) {

			e1.printStackTrace();
		}

		return null;
	}

	/**
	 * find next free predefined calendar color if all are taken, take first
	 * pedefined as default
	 * 
	 * @param mContext
	 * @return
	 */
	private int getNextCalendarColor() {

		for (int i = 0; i < Constants.CALENDAR_COLORS.length; i++) {
			if (!isColorUsed(Constants.CALENDAR_COLORS[i])) {
				return Constants.CALENDAR_COLORS[i];
			}
		}
		return Constants.CALENDAR_COLORS[0];
	}

	/**
	 * Check if a given color is already used as calendar-color
	 * 
	 * @param mContext
	 * @param account
	 * @param color
	 * @return
	 */
	private boolean isColorUsed(int color) {
		boolean isColorUsed = false;
		ContentResolver cr = mContext.getContentResolver();

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

	private Uri asSyncAdapter(Uri uri, String account, String accountType) {
		return uri.buildUpon().appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
				.appendQueryParameter(Calendars.ACCOUNT_NAME, account).appendQueryParameter(Calendars.ACCOUNT_TYPE, accountType).build();
	}

	/**
	 * Inserts a single Event into the Databses
	 * 
	 * @param account
	 * @param calendarId
	 * @param e
	 * @return
	 */
	private long insertEvent(Account account, long calendarId, VEvent e) {

		ContentResolver cr = mContext.getContentResolver();
		ContentValues values = new ContentValues();
		Uri uri = asSyncAdapter(Events.CONTENT_URI, account.name, account.type);

		String hash = calcEventHash(e);

		values.put(Events.CALENDAR_ID, calendarId);
		values.put(Events.TITLE, e.getSummary().getValue());
		values.put(Events.DTSTART, e.getDateStart().getValue().getTime());
		values.put(Events.DTEND, e.getDateEnd().getValue().getTime());
		values.put(Events._SYNC_ID, e.getUid().getValue());
		values.put(EVENTS_DB_TIME_STEMP_COLUMN, Long.toString(e.getDateTimeStamp().getValue().getTime()));
		values.put(EVENTS_DB_HASH_COLUMN, hash);

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
			// Log.d(TAG, " INSERT event with hash " + hash);
		}

		return ContentUris.parseId(ret);

	}

	/**
	 * @param exceptionDates
	 * @return
	 */
	private static String buildExdate(List<ExceptionDates> exceptionDates) {
		if (exceptionDates != null) {

			StringBuilder sb = new StringBuilder();
			ArrayList<Date> dates = new ArrayList<Date>();

			TimeZone tz = TimeZone.getDefault();

			for (ExceptionDates ed : exceptionDates) {
				dates.addAll(ed.getValues());
				tz = TimeZone.getTimeZone(ed.getTimezoneId());

			}

			if (tz != null) {
				sb.append("TZID=").append(tz.getID()).append(':');
			}

			Calendar cal = Calendar.getInstance(tz);
			//TimeZone.setDefault(tz);
			for (Iterator<Date> iterator = dates.iterator(); iterator.hasNext();) {
				Date date = (Date) iterator.next();
					
				cal.setTime(date);
				cal.setTimeZone(tz);
				
				sb.append(parseIcalDateToString(cal.getTime()));

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
			if (r.getUntil() != null)
				sb.append(";UNTIL=").append(parseIcalDateToString(r.getUntil())).append('Z');
			else if (r.getCount() != null)
				sb.append(";COUNT=").append(r.getCount());
			else {
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(System.currentTimeMillis());

				int year = cal.get(Calendar.YEAR);
				int month = cal.get(Calendar.MONTH);
				int day = cal.get(Calendar.DAY_OF_MONTH);

				cal.set(year + EVENT_MAX_UNTIL_OFFSET_IN_YEAR, month, day);
				Date untilDate = cal.getTime();

				String until = parseIcalDateToString(untilDate);
				Log.i(TAG, "buildRrule() no UNTIL or COUNT, so set UNTIL to " + untilDate.toString() + "   ICAL:" + until);
				sb.append(";UNTIL=").append(until).append('Z');
			}

			if (r.getWorkweekStarts() != null)
				sb.append(";WKST=").append(r.getWorkweekStarts().getAbbr());

			if (r.getInterval() != null)
				sb.append(";INTERVAL=").append(r.getInterval());
			else
				sb.append(";INTERVAL=").append(1);

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

	/**
	 * seperates an Recurring event into a list of single events, and also
	 * includes the exceptions in the RecuringVEvent
	 * 
	 * @param rve
	 * @return
	 */
	private static ArrayList<VEvent> seperateEvents(RecurringVEvent rve) {

		ArrayList<VEvent> cleanEventList = new ArrayList<VEvent>();

		if (rve != null && rve.e != null && rve.e.getRecurrenceRule() != null) {

			// we have two lists, the list of atomar events, but with no
			// exceptions
			ArrayList<VEvent> atomarEvents = splitRecurringEvent(rve.e);
			// and a list of Events which are the exception
			ArrayList<VEvent> exceptions = rve.getExceptions();

			/**
			 * we need to find the event, for which the exception is an
			 * exception as an RECURRING-ID which represenst the DateTime or
			 * only Date on which the event should have been. the MS-Exchange
			 * exports only a Date not a Date Time, so we check only if Year,
			 * Month and Day fist
			 */
			Calendar cal = Calendar.getInstance();
			for (VEvent atom : atomarEvents) {

				cal.setTime(atom.getDateStart().getValue());
				int atomYear = cal.get(Calendar.YEAR);
				int atomMonth = cal.get(Calendar.MONTH);
				int atomDay = cal.get(Calendar.DAY_OF_MONTH);

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
							// add the exception to the list, not the "normal"
							// recurring event
							cleanEventList.add(ex);
							inserted = true;
							break;
						}

					} else {
						Log.e(TAG, "Missmatching UID in seperateEvents o0 WTF?");
					}
				}// inner for

				// if there is no exception for tis event, we use this event
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
	 * splits an recurring Event with an RRULE and EXDATE into a List of
	 * SingleEvents
	 * 
	 * @author friedrda
	 * @param recurringEvent
	 * @return List of events
	 */
	private static ArrayList<VEvent> splitRecurringEvent(VEvent recurringEvent) {

		ArrayList<VEvent> atomarEvents = new ArrayList<VEvent>();
		boolean hasRec = false;

		// avoid nullPointer by checking if needed attributes are set
		if (recurringEvent != null && recurringEvent.getRecurrenceRule() != null && recurringEvent.getRecurrenceId() == null) {

			// collect data from event, to but into new atomar events
			String title = recurringEvent.getSummary().getValue();
			String description = "";
			if (recurringEvent.getDescription() != null) {
				description = recurringEvent.getDescription().getValue();
			}
			String location = "";
			if (recurringEvent.getLocation() != null) {
				location = recurringEvent.getLocation().getValue();
			}

			String uid = recurringEvent.getUid().getValue();

			Status status = recurringEvent.getStatus();

			Classification eventCalss = recurringEvent.getClassification();

			Priority prio = recurringEvent.getPriority();

			int sequence = 0;
			if (recurringEvent.getSequence() != null) {
				sequence = recurringEvent.getSequence().getValue();
			}

			Transparency transp = recurringEvent.getTransparency();

			List<String> categories = new ArrayList<String>();

			if (recurringEvent.getCategories() != null && !recurringEvent.getCategories().isEmpty())
				categories = recurringEvent.getCategories().get(0).getValues();

			// handle DATE stuff
			Date startDate = recurringEvent.getDateStart().getValue();
			String timeZoneIdFromStartDate = recurringEvent.getDateStart().getTimezoneId();
			

			Log.d(TAG, "--------------------------------------");
			Log.d(TAG, "RECURRING EVENT");
			Log.d(TAG, "\tSTARTDATE: toString()\t" + startDate.toString());
			Log.d(TAG, "\tSTARTDATE: timeZone \t" + timeZoneIdFromStartDate);
			Log.d(TAG, "\tSTARTDATE: timeZoneConv \t" + TimeZone.getTimeZone("timeZoneIdFromStartDate").getID());

			Date endDate = recurringEvent.getDateEnd().getValue();

			Duration duration = Duration.diff(startDate, endDate);

			// get RRULE
			StringBuilder rdata = new StringBuilder();
			rdata.append("RRULE:").append(buildRrule(recurringEvent));

			// get EXDATE
			if (recurringEvent.getExceptionDates() != null && recurringEvent.getExceptionDates().size() > 0) {
				hasRec = true;

				String exdate = buildExdate(recurringEvent.getExceptionDates());

				Log.d(TAG, "RDATA ");
				Log.d(TAG, "\t RRULE: \t" + rdata.toString());
				Log.d(TAG, "\t EXDATE:\t" + exdate);

				rdata.append("\nEXDATE;").append(exdate);
				//rdata.append("\nEXDATE;").append("TZID=GMT:20131120T090000");
			}

			
			Log.d(TAG, "RDATA: ");
			Log.d(TAG, "\t --------------");
			Log.d(TAG, rdata.toString());
			Log.d(TAG, "\t --------------");
			

			TimeZone tz = TimeZone.getTimeZone(timeZoneIdFromStartDate);
			try {
				Calendar cal = Calendar.getInstance();


				cal.setTimeZone(tz);
				cal.setTime(startDate);

				Date da = cal.getTime();

				int startDstOffset = cal.get(Calendar.DST_OFFSET);
				DateIterator dif = DateIteratorFactory.createDateIterator(rdata.toString(), da, tz, true);

				/*
				 * if (hasRec) { Log.d(TAG, "timeZoneId " +
				 * timeZoneIdFromStartDate); Log.d(TAG, "timeZoneId in tz" +
				 * tz.getDisplayName()); Log.d(TAG, "RDATA : " + rdata);
				 * Log.d(TAG, "RDATA TZ: " + tz.toString()); Log.d(TAG,
				 * "RDATA startDate: " + startDate.toString()); }
				 */

				// + " Recurrs on: ");

				while (dif.hasNext()) {
					Date d = dif.next();

					cal.setTime(d);
					//cal.setTimeZone(tz);
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

					if (hasRec == true) {
						Log.d(TAG, "has EXDATE: " + title + "  -- " + e.getDateStart().getValue().toString());
					}

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
		SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm");
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		timeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

		sb.append(dateFormat.format(until));
		sb.append("T");
		sb.append(timeFormat.format(until));
		sb.append("00");

		return sb.toString();
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
