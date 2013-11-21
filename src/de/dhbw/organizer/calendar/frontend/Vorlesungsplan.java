package de.dhbw.organizer.calendar.frontend;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import de.dhbw.organizer.calendar.Constants;
import de.dhbw.organizer.R;
import de.dhbw.organizer.R.layout;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author riedings
 * 
 */

public class Vorlesungsplan extends Activity {

	private ListView mEventList;
	private String[] mDrawerListViewItems;
	private ListView mDrawerListView;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mActionBarDrawerToggle;
	private ArrayList<String> mCalendarList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.calendar_activity_vorlesungsplan);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		/*
		 * Methode zum speichern des letzten ausgewählten Kalenders erstellen
		 */
		// readCalendar(this, "bla");

		mCalendarList = getCalendarList();

		// get list items from strings.xml
		mDrawerListViewItems = getResources().getStringArray(R.array.items);

		// get ListView defined in activity_main.xml
		mDrawerListView = (ListView) findViewById(R.id.left_drawer);

		Log.d("Kalendar: ", mCalendarList.get(0));

		// Set the adapter for the list view
		mDrawerListView.setAdapter(new ArrayAdapter<String>(this,
				R.layout.calendar_drawer_listview_item, mCalendarList));

		// Welcher Onclick listener ist der richtige?!
		mDrawerListView.setOnItemClickListener(new DrawerItemClickListener());

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		// 2.1 create ActionBarDrawerToggle
		mActionBarDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
		mDrawerLayout, /* DrawerLayout object */
		R.drawable.ic_drawer, /* nav drawer icon to replace 'Up' caret */
		R.string.drawer_open, /* "open drawer" description */
		R.string.drawer_close /* "close drawer" description */
		);

		// 2.2 Set actionBarDrawerToggle as the DrawerListener
		mDrawerLayout.setDrawerListener(mActionBarDrawerToggle);

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Pass the event to ActionBarDrawerToggle, if it returns
		// true, then it has handled the app icon touch event
		if (mActionBarDrawerToggle.onOptionsItemSelected(item)) {
			// zurück auf den Homescreen einbauen!

			return true;
		}
		// Handle your other action bar items...

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.calendar_menu, menu);
		return true;
	}

	// The desired event columns
	public static final String[] EVENT_PROJECTION = new String[] { Events._ID, // 0
			Events.TITLE, // 1
			Events.EVENT_LOCATION // 2
	};

	// The desired calendar columns
	public static final String[] CALENDAR_PROJECTION = new String[] {
			Calendars._ID, // 0
			Calendars.ACCOUNT_NAME, // 1
			Calendars.ACCOUNT_TYPE, // 2
			Calendars.NAME // 3
	};

	// The desired columns to be bound
	String[] columns = new String[] { Instances._ID, // 0
			Instances.TITLE, // 1
			Instances.EVENT_LOCATION // 2
	};

	int[] to = new int[] { R.id.code, // 0
			R.id.name, // 1
			R.id.continent // 2
	};

	/**
	 * 
	 * @param context
	 *            The actual context
	 * @param CalendarName
	 *            The calendar name which should be read
	 */
	private void readCalendar(Context context, String CalendarName) {

		mEventList = (ListView) findViewById(R.id.listView1);
		Uri uri = CalendarContract.Events.CONTENT_URI;

		Cursor cur = null;

		ContentResolver cr = getContentResolver();

		cur = cr.query(uri, EVENT_PROJECTION, Instances.CALENDAR_ID + " = ?",
				new String[] { "" + 11 }, null);

		cur = cr.query(uri, EVENT_PROJECTION, Calendars.ACCOUNT_NAME + " = ?",
				new String[] { CalendarName }, null);

		/*
		 * while (cur2.moveToNext()) { String eventID = null; String Title =
		 * null; String eventLocation = null; eventID = cur2.getString(0); Title
		 * = cur2.getString(1); eventLocation = cur2.getString(2); }
		 */
		SimpleCursorAdapter dataAdapter = new SimpleCursorAdapter(this,
				R.layout.calendar_activity_listview, cur, columns, to, 0);

		mEventList.setAdapter(dataAdapter);

	}

	/**
	 * 
	 * @return ArrayList with calendar names with account type
	 *         Calendars.ACCOUNT_TYPE
	 */
	private ArrayList<String> getCalendarList() {
		// lv1 = (ListView) findViewById(R.id.listView1);
		String[] calendarList2 = new String[1];
		ArrayList<String> calendarList = new ArrayList<String>();

		Uri uri = Calendars.CONTENT_URI;

		Cursor cur = null;

		ContentResolver cr = getContentResolver();

		cur = cr.query(uri, CALENDAR_PROJECTION, Calendars.ACCOUNT_TYPE
				+ " = ?", new String[] { Constants.ACCOUNT_TYPE }, null);

		int i = 0;
		while (cur.moveToNext()) {
			String eventID = null;
			String calendarName = null;
			String Title2 = null;
			String Title3 = null;

			eventID = cur.getString(0);
			calendarName = cur.getString(1);
			Title2 = cur.getString(2);
			Title3 = cur.getString(3);

			calendarList.add(calendarName);
			i++;

			Log.d("CALENDAR_ID", eventID);
			Log.d("CALENDAR_NAME", calendarName);

		}

		return calendarList;

	}

	/**
	 * 
	 * @author Seimon
	 * 
	 */
	private class DrawerItemClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView parent, View view, int position,
				long id) {
			Toast.makeText(Vorlesungsplan.this, ((TextView) view).getText(),
					Toast.LENGTH_LONG).show();
			mDrawerLayout.closeDrawer(mDrawerListView);
			Log.d((((TextView) view).getText()).toString(),
					(((TextView) view).getText()).toString());

			selectItem(view);

		}
	}

	/**
	 * 
	 * @param view
	 *            the name of the selected item
	 */
	public void selectItem(View view) {

		readCalendar(this, (((TextView) view).getText()).toString());
	}

}
