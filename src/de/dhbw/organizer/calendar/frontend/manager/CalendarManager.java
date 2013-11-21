package de.dhbw.organizer.calendar.frontend.manager;

import java.util.ArrayList;
import java.util.Date;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import de.dhbw.organizer.R;
import de.dhbw.organizer.calendar.Constants;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class CalendarManager {

	// The desired event columns
	public static final String[] EVENT_PROJECTION = new String[] { 
		Events._ID,
		Events.TITLE,
		Events.DTSTART,
		Events.EVENT_LOCATION
		
		
		//Events._ID, // 0
			//Events.TITLE, // 1
			//Events.EVENT_LOCATION // 2
	};

	// The desired calendar columns
	public static final String[] CALENDAR_PROJECTION = new String[] { Calendars.ACCOUNT_NAME // 0

	};

	// The desired columns to be bound
	static String[] columns = new String[] { 
		Instances._ID,
		Instances.TITLE,
		Instances.DTSTART,
		Instances.EVENT_LOCATION
		
		
		
		
		//Instances._ID, // 0
			//Instances.TITLE, // 1
			//Instances.EVENT_LOCATION // 2
	};

	static int[] to = new int[] { R.id.id,
		R.id.name, // 0
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
	public SimpleCursorAdapter getCalendarEvents(Context context,
			String CalendarName) {

		Uri uri = CalendarContract.Events.CONTENT_URI;

		Cursor cur = null;

		ContentResolver cr = context.getContentResolver();

		cur = cr.query(uri, EVENT_PROJECTION, Calendars.ACCOUNT_NAME + " = ?",
				new String[] { CalendarName }, null);

		while(cur.moveToNext()){
			String eventID = null;
			String eventName = null;
			String eventLocation = null;
			long eventdtstart = 0;
			String date = null;

			eventID = cur.getString(0);
			eventName = cur.getString(1);
			eventdtstart = cur.getLong(2);
			date = getDate(eventdtstart, "dd.MM.yyyy hh:mm");
			
			eventLocation = cur.getString(3);
			
			
			/*
			 * Hier gehts weiter
			 * alles in ein mehrdimensionales Array schreiben, die Zeit von ms in normalanzeige konvertieren 
			 * 
			 * Dann auf einen Adapter 
			 * 
			 * Dann zurück geben
			 * 
			 * 
			 * */

			//calendarList.add(calendarName);

			//Log.d("CALENDAR_NAME", calendarName);
			
		}
		
		
		SimpleCursorAdapter dataAdapter = new SimpleCursorAdapter(context,
				R.layout.calendar_activity_listview, cur, columns, to, 0);

		//SimpleCursorAdapter a = new SimpleCursorAdapter(context, layout, c, from, to)
		
		return dataAdapter;
	}
	
	/**
	 * Return date in specified format.
	 * @param milliSeconds Date in milliseconds
	 * @param dateFormat Date format 
	 * @return String representing date in specified format
	 */
	public static String getDate(long milliSeconds, String dateFormat)
	{
	    // Create a DateFormatter object for displaying date in specified format.
	    DateFormat formatter = new SimpleDateFormat(dateFormat);

	    // Create a calendar object that will convert the date and time value in milliseconds to date. 
	     Calendar calendar = Calendar.getInstance();
	     calendar.setTimeInMillis(milliSeconds);
	     return formatter.format(calendar.getTime());
	}
	
	
	
	/**
	 * 
	 * @param context
	 * @param CalendarName
	 * @return
	 */
	public int getActualEvent(Context context,
			String CalendarName) {

		Uri uri = CalendarContract.Events.CONTENT_URI;

		Cursor cur = null;

		ContentResolver cr = context.getContentResolver();

		cur = cr.query(uri, EVENT_PROJECTION, Calendars.ACCOUNT_NAME + " = ?",
				new String[] { CalendarName }, null);

		while(cur.moveToNext()){
			// Zeit mit der aktuellen vergleichen und die Position zurückliefern
			
		}
		
		
		SimpleCursorAdapter dataAdapter = new SimpleCursorAdapter(context,
				R.layout.calendar_activity_listview, cur, columns, to, 0);

		return 1;
	}
	
	

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
