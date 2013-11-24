package de.dhbw.organizer.calendar.frontend.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.format.Time;
import android.util.Log;
import de.dhbw.organizer.R;
import de.dhbw.organizer.calendar.Constants;
import de.dhbw.organizer.calendar.frontend.adapter.CalendarEvent;
import de.dhbw.organizer.calendar.frontend.adapter.EventAdapter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class CalendarManager {

	public int mIndexOfActualEvent;
	public boolean mIndexAlreadySet;

	// The desired event columns
	public static final String[] EVENT_PROJECTION = new String[] { Events._ID,
			Events.TITLE, Events.DTSTART, Events.DTEND, Events.EVENT_LOCATION,
			Events.DESCRIPTION };

	// The desired calendar columns
	public static final String[] CALENDAR_PROJECTION = new String[] { Calendars.ACCOUNT_NAME // 0
	};

	// The desired columns to be bound
	static String[] columns = new String[] { Instances._ID, Instances.TITLE,
			Instances.DTSTART, Instances.DTEND, Instances.EVENT_LOCATION,
			Instances.DESCRIPTION };

	static int[] to = new int[] { R.id.id, R.id.name, // 0
			R.id.time, // 1
			R.id.location // 2
	};

	/**
	 * 
	 * @param context
	 *            The actual context
	 * @param CalendarName
	 *            The calendar name which should be read
	 * @return
	 */
	public EventAdapter getCalendarEvents(Context context, String CalendarName) {
		mIndexAlreadySet = false;

		Uri uri = CalendarContract.Events.CONTENT_URI;

		Cursor cur = null;

		ContentResolver cr = context.getContentResolver();

		cur = cr.query(uri, EVENT_PROJECTION, Calendars.ACCOUNT_NAME + " = ?",
				new String[] { CalendarName }, Events.DTSTART);

		List<CalendarEvent> listOfEvents = new ArrayList<CalendarEvent>();

		int i = 0;
		while (cur.moveToNext()) {
			String eventID = null;
			String eventName = null;
			String eventLocation = null;
			long eventdtstart = 0;
			long eventdtend = 0;
			String date = null;
			String eventDescription = null;

			eventID = cur.getString(0);
			eventName = cur.getString(1);
			eventdtstart = cur.getLong(2);
			eventdtend = cur.getLong(3);
			eventLocation = cur.getString(4);
			eventDescription = cur.getString(5);

			listOfEvents.add(new CalendarEvent(eventName, eventdtstart,
					eventdtend, eventLocation, eventDescription));

			// set the index of the element to scroll to the actual element
			Time now = new Time();
			now.setToNow();

			long ms = now.toMillis(false);

			if (eventdtend >= ms & !mIndexAlreadySet) {
				mIndexOfActualEvent = i;
				mIndexAlreadySet = true;
			}
			i++;
		}

		// set Adapter for List
		EventAdapter adapter = new EventAdapter(context, listOfEvents);

		return adapter;
	}

	/**
	 * 
	 * @param context
	 * @param CalendarName
	 * @return
	 */
	
	/*
	public int getActualEvent(Context context, String CalendarName) {

		Uri uri = CalendarContract.Events.CONTENT_URI;

		Cursor cur = null;

		ContentResolver cr = context.getContentResolver();

		cur = cr.query(uri, EVENT_PROJECTION, Calendars.ACCOUNT_NAME + " = ?",
				new String[] { CalendarName }, null);

		while (cur.moveToNext()) {
			// Zeit mit der aktuellen vergleichen und die Position zurückliefern

		}

		SimpleCursorAdapter dataAdapter = new SimpleCursorAdapter(context,
				R.layout.calendar_activity_listview, cur, columns, to, 0);

		return 1;
	}*/

	/**
	 * 
	 * @return ArrayList with calendar names with account type
	 *         Calendars.ACCOUNT_TYPE
	 */
	public ArrayList<String> getCalendarList(Context context) {
		ArrayList<String> calendarList = new ArrayList<String>();

		Uri uri = Calendars.CONTENT_URI;

		Cursor cur = null;

		ContentResolver cr = context.getContentResolver();

		cur = cr.query(uri, CALENDAR_PROJECTION, Calendars.ACCOUNT_TYPE
				+ " = ?", new String[] { Constants.ACCOUNT_TYPE }, null);

		while (cur.moveToNext()) {
			String calendarName = null;

			calendarName = cur.getString(0);

			calendarList.add(calendarName);

			Log.d("CALENDAR_NAME", calendarName);
		}

		return calendarList;

	}

}
