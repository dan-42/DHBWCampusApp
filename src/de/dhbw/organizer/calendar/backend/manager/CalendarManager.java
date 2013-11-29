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
package de.dhbw.organizer.calendar.backend.manager;

//import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.xmlpull.v1.XmlPullParserException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.util.Log;
import biweekly.component.VEvent;
import de.dhbw.organizer.calendar.Constants;
import de.dhbw.organizer.calendar.backend.objects.SpinnerItem;
import de.dhbw.organizer.calendar.helper.FileHelper;
import de.dhbw.organizer.calendar.helper.ICalHelper;

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

	private static final String ASSET_XML_SCHEMA_CALENDAR_LIST = "xsd/calendar_calendarlist.xsd";

	private static final String ASSET_DEFAULT_CALENDAR_LIST = "xml/calendar_calendars.xml";

	private static final String DATA_EXTERN_CALENDAR_LIST = "calendar_calendars.xml";

	private Context mContext = null;

	private CalendarManager(Context context) {
		mContext = context;
		TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
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
	 * deletes the account
	 * 
	 * @return
	 */
	public boolean deleteCalendar(String calenderName) {
		Log.v(TAG, "deleteCalendar() " + calenderName);
		final Account account = new Account(calenderName, Constants.ACCOUNT_TYPE);
		long id = getCalendarId(account);

		AccountManager ac = AccountManager.get(mContext);

		deleteCalendar(account, id);
		boolean sucess = ac.removeAccount(account, null, null).isDone();

		return sucess;

	}

	/**
	 * access XML-Files and extract the possible calendars (DisplayName and
	 * iCal-Url) if an external XML exists
	 * "assets/xsd/calendar_calendarlist.xsd" this file will be taken, if not
	 * the local XML ("assets/xml/calendar_calendars.xml") is taken
	 * 
	 * since the external file is validated when dowenload, we do not need to
	 * validate
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

		ArrayList<SpinnerItem> selectableCalendars = null;

		// open XML-Schema and default XML-File
		try {
			isCalendarsDefaultXml = assetManager.open(ASSET_DEFAULT_CALENDAR_LIST);

		} catch (IOException e1) {
			Log.e(TAG, "cant open calendar default xml and Xml-Schema");
			throw new IOException();

		}

		// check if external file exists, and is valid
		try {
			isExternalCalList = mContext.openFileInput(DATA_EXTERN_CALENDAR_LIST);

			selectableCalendars = ICalHelper.parseXmlFromInputStream(isExternalCalList);

		} catch (FileNotFoundException e) {
			Log.i(TAG, "no external File found, so take locale calendar list");
			takeLocalXml = true;

		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (takeLocalXml == true) {

			try {
				selectableCalendars = ICalHelper.parseXmlFromInputStream(isCalendarsDefaultXml);
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.i(TAG, "XML Default is Valid");

		}

		return selectableCalendars;
	}

	/**
	 * Loads the XML file from the Server, validates it and stores it on the
	 * internal storage
	 * 
	 * @return true if no error occurred
	 */
	public boolean loadExternalXml() {
		boolean success = false;

		NetworkManager nm = NetworkManager.getInstance(mContext);
		AssetManager assetManager = mContext.getAssets();

		Log.i(TAG, "http execute()");

		InputStream instream = nm.downloadHttpFile(Constants.EXTERNAL_CALENDAR_LIST_URL);

		if (instream != null) {

			InputStream isXmlSchema;
			try {
				isXmlSchema = assetManager.open(ASSET_XML_SCHEMA_CALENDAR_LIST);

				// save data to cache
				File cacheFile = FileHelper.createCacheFile(mContext, "tempExternal", "ical");
				cacheFile = FileHelper.writeInputStreamToFile(instream, cacheFile);

				InputStream isExternalXml = new FileInputStream(cacheFile);

				// validate data
				if (ICalHelper.validate(isExternalXml, isXmlSchema)) {

					FileOutputStream fos = mContext.openFileOutput(DATA_EXTERN_CALENDAR_LIST, Context.MODE_PRIVATE);
					FileHelper.writeFileToOutputStream(cacheFile, fos);
					success = true;
				} else {
					Log.e(TAG, "Downloaded List ist not Valid");
				}

				instream.close();
				isXmlSchema.close();

			} catch (IOException e) {
				Log.e(TAG, "loadExternalXml() ERROR " + e.getMessage());
				return false;
			}

		} else {
			Log.e(TAG, "loadExternalXml() Could not download file");
		}

		return success;
	}

	/**
	 * check if a calendar already exists
	 * 
	 * @param mContext
	 * @param account
	 * @return
	 */
	public boolean calendarExists(Account account) {

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
	 * @param Color
	 *            , can be null for default!
	 * @return calendar id
	 */
	public long createCalendar(Account account, Color color) {
		Log.d(TAG, "Calendar With by Account Name");

		ContentResolver cr = mContext.getContentResolver();

		Uri creationUri = asSyncAdapter(Calendars.CONTENT_URI, account.name, account.type);

		int colorInHex;
		if (color == null) {
			colorInHex = getNextCalendarColor();
		} else {
			// colorInHex = Color.r
			colorInHex = getNextCalendarColor();
		}
		ContentValues values = new ContentValues();

		values.put(Calendars.ACCOUNT_NAME, account.name);
		values.put(Calendars.ACCOUNT_TYPE, account.type);
		values.put(Calendars.NAME, account.name);
		values.put(Calendars.CALENDAR_DISPLAY_NAME, Constants.CALENDAR_DISPLAY_NAME_PREFIX + account.name);
		values.put(Calendars.CALENDAR_COLOR, colorInHex);
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
		Uri url = asSyncAdapter(Calendars.CONTENT_URI, account.name, account.type);

		ContentResolver cr = mContext.getContentResolver();
		String selection = "((" + Calendars._ID + " = ?) AND  (" + Calendars.ACCOUNT_TYPE + " = ?))";
		String[] selectionArgs = new String[] { Long.toString(calendarId), Constants.ACCOUNT_TYPE };

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
	public void insertEvents(Account account, long calendarId, ArrayList<VEvent> eventList, boolean ignorePrivateEvents) {

		ArrayList<VEvent> atomarEventList = ICalHelper.seperateAllEvents(eventList, ignorePrivateEvents);
		Log.d(TAG, "insertEvents() " + atomarEventList.size() + " events to add in total");

		insertEvents(account, calendarId, atomarEventList, TimeZone.getDefault());

	}

	/**
	 * only updates the Events from the list to the Database, based upon the
	 * hashCode
	 * 
	 * @param account
	 * @param calendarId
	 * @param eventList
	 */
	public void updateEvents(Account account, long calendarId, ArrayList<VEvent> eventList, boolean ignorePrivateEvents) {

		ArrayList<VEvent> atomarEventList = ICalHelper.seperateAllEvents(eventList, ignorePrivateEvents);
		ArrayList<String> hashList = getHashOfEventsInDb(account, calendarId);

		ArrayList<VEvent> eventsToInsert = new ArrayList<VEvent>();

		for (VEvent e : atomarEventList) {
			String hash = ICalHelper.calcEventHash(e);

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
		insertEvents(account, calendarId, eventsToInsert, TimeZone.getDefault());
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
	 * Inserts a whole List of VEvents
	 * 
	 * @param account
	 * @param calendarId
	 * @param list
	 *            of VEvents
	 * @param tz
	 * @return true if successful
	 */
	private boolean insertEvents(Account account, long calendarId, ArrayList<VEvent> list, TimeZone tz) {

		ContentResolver cr = mContext.getContentResolver();
		Uri uri = asSyncAdapter(Events.CONTENT_URI, account.name, account.type);
		ContentValues[] cv = new ContentValues[list.size()];

		for (int i = 0; i < list.size(); i++) {
			cv[i] = perpareValues(calendarId, list.get(i), tz);
		}

		int ret = cr.bulkInsert(uri, cv);

		if (ret <= 0) {
			Log.e(TAG, "insertEvents() did't insert anything");
			return false;
		} else {
			Log.i(TAG, "insertEvents() inserted " + ret + " events");
			return true;
		}

	}

	/**
	 * Inserts a single Event into the Databses
	 * 
	 * @param account
	 * @param calendarId
	 * @param e
	 * @return keep it for test purpose
	 */
	@SuppressWarnings("unused")
	private long insertEvent(Account account, long calendarId, VEvent e, TimeZone tz) {

		ContentResolver cr = mContext.getContentResolver();
		Uri uri = asSyncAdapter(Events.CONTENT_URI, account.name, account.type);
		ContentValues values = perpareValues(calendarId, e, tz);
		Uri ret = cr.insert(uri, values);
		if (ret == null) {
			Log.e(TAG, " INERT ERROR return URI is null");
			return 0;
		} else {
			// Log.d(TAG, " INSERT event with hash " + hash);
		}
		return ContentUris.parseId(ret);
	}

	private ContentValues perpareValues(long calendarId, VEvent e, TimeZone tz) {

		ContentValues values = new ContentValues();
		String hash = ICalHelper.calcEventHash(e);

		values.put(Events.CALENDAR_ID, calendarId);
		values.put(Events.TITLE, e.getSummary().getValue());
		values.put(Events.DTSTART, e.getDateStart().getValue().getTime());
		values.put(Events.DTEND, e.getDateEnd().getValue().getTime());
		values.put(Events._SYNC_ID, e.getUid().getValue());
		values.put(EVENTS_DB_TIME_STEMP_COLUMN, Long.toString(e.getDateTimeStamp().getValue().getTime()));
		values.put(EVENTS_DB_HASH_COLUMN, hash);

		if (e.getClassification() != null)
			if (e.getClassification().isPrivate())
				values.put(Events.ACCESS_LEVEL, Events.ACCESS_PRIVATE);
			else
				values.put(Events.ACCESS_LEVEL, Events.ACCESS_PUBLIC);
		else
			values.put(Events.ACCESS_LEVEL, Events.ACCESS_DEFAULT);

		if (e.getDescription() != null)
			values.put(Events.DESCRIPTION, e.getDescription().getValue());

		if (e.getLocation() != null)
			values.put(Events.EVENT_LOCATION, e.getLocation().getValue());

		values.put(Events.EVENT_TIMEZONE, tz.getID());

		return values;
	}

}
