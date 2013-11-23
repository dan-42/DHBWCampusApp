package de.dhbw.organizer.calendar.frontend.activity;

import java.util.ArrayList;
import java.util.List;

import de.dhbw.organizer.calendar.backend.activity.AuthenticatorActivityTabed;
import de.dhbw.organizer.calendar.frontend.adapter.CalendarEvent;
import de.dhbw.organizer.calendar.frontend.adapter.EventAdapter;
import de.dhbw.organizer.calendar.frontend.manager.CalendarManager;
import de.dhbw.organizer.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
	private CalendarManager mCalendarManager;
	private TextView mTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.calendar_activity_vorlesungsplan);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		/*
		 * Methode zum speichern des letzten ausgew�hlten Kalenders erstellen
		 */
		mCalendarManager = new CalendarManager();

		setDrawerContent();

	}

	private void setDrawerContent() {

		mCalendarList = mCalendarManager.getCalendarList(this);
		// mCalendarList = getCalendarList();

		// get list items from strings.xml
		// mDrawerListViewItems = getResources().getStringArray(R.array.items);

		// get ListView defined in activity_main.xml
		mDrawerListView = (ListView) findViewById(R.id.left_drawer);

		if (mCalendarList.isEmpty()) {
			Log.d("Kalendar: ","mCalendarList is empty");
		} else {
			Log.d("Kalendar: ", mCalendarList.get(0));
		}

		// Set the adapter for the list view
		mDrawerListView.setAdapter(new ArrayAdapter<String>(this, R.layout.calendar_drawer_listview_item, mCalendarList));

		mDrawerListView.setOnItemClickListener(new DrawerItemClickListener());

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		// 2.1 create ActionBarDrawerToggle
		mActionBarDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
		mDrawerLayout, /* DrawerLayout object */
		R.drawable.ic_drawer, /* nav drawer icon to replace 'Up' caret */
		R.string.drawer_open, /* "open drawer" description */
		R.string.drawer_close /* "close drawer" description */
		) {
			/** Called when a drawer has settled in a completely closed state. */
			@Override
			public void onDrawerClosed(View view) {
			}

			/** Called when a drawer has settled in a completely open state. */
			@Override
			public void onDrawerOpened(View drawerView) {
				setDrawerContent();
			}
		};

		// 2.2 Set actionBarDrawerToggle as the DrawerListener
		mDrawerLayout.setDrawerListener(mActionBarDrawerToggle);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.calendar_menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// Handle the New Calendar and the Settings Menu
		switch (item.getItemId()) {
		case R.id.item1:
			Log.d("Menu pressed", "Menu 1 has been pressed");
			Toast.makeText(this, "Menu gredr�ckt", 1).show();
			final Intent intent = new Intent(this, AuthenticatorActivityTabed.class);
			this.startActivity(intent);

			break;
		case R.id.item2:
			Log.d("Menu pressed", "Menu 2 has been pressed");

			break;
		default:
			break;
		}

		// Pass the event to ActionBarDrawerToggle, if it returns
		// true, then it has handled the app icon touch event
		if (mActionBarDrawerToggle.onOptionsItemSelected(item)) {
			// zur�ck auf den Homescreen einbauen!

			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * 
	 * @param context
	 *            The actual context
	 * @param CalendarName
	 *            The calendar name which should be read
	 */
	private void setListContent(Context context, String calendarName) {

		// find the Listview
		mEventList = (ListView) findViewById(R.id.listView1);

		// get the Events as an Adapter
		EventAdapter mEvents = mCalendarManager.getCalendarEvents(this, calendarName);

		// set Adapter to display List
		mEventList.setAdapter(mEvents);

		// get actual number
		// eine funktion

		// go to acutal Date
		Log.d("Index of Actual Event", String.valueOf(mCalendarManager.mIndexOfActualEvent));
		mEventList.setSelectionFromTop(mCalendarManager.mIndexOfActualEvent, 0);

		// mEventList.setSelectionFromTop(5, 0);

		// oben in die Declarations schreiben

		// SimpleCursorAdapter dataAdapter =
		// mCalendarManager.getCalendarEvents(this,calendarName);

		// mEventList.setAdapter(dataAdapter);

		// String a = (String) dataAdapter.getItem(0);

		// get list of events retruns ArrayList<CalendarEvents> listOfEvents
		//

		// get EventAdapter()

		// get number of actual Event

		/*
		 * List<CalendarEvent> listOfEvents = new ArrayList<CalendarEvent>();
		 * listOfEvents.add(new CalendarEvent("Test", "9981728",
		 * "test@test.com")); listOfEvents.add(new CalendarEvent("Test1",
		 * "1234455", "test1@test.com")); listOfEvents.add(new
		 * CalendarEvent("Test2", "00000", "test2@test.com"));
		 * 
		 * EventAdapter adapter = new EventAdapter(this, listOfEvents);
		 */

		// mEventList.setAdapter(adapter);
		// mTextView.findViewById(R.id.@1380523500000);

		// mTextView.setText("hallo");

		// SimpleCursorAdapter dataAdapter2 =
		// mCalendarManager.getCalendarEvents(this,calendarName);

		// while(dataAdapter2.){

		// }

		// mEventList.setSelectionFromTop(5, 0);
	}

	/**
	 * 
	 * @param view
	 *            the name of the selected item
	 */
	public void selectItem(View view) {

		setListContent(this, (((TextView) view).getText()).toString());
	}

	/**
	 * 
	 * @author Seimon
	 * 
	 */
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView parent, View view, int position, long id) {
			Toast.makeText(Vorlesungsplan.this, ((TextView) view).getText(), Toast.LENGTH_LONG).show();
			mDrawerLayout.closeDrawer(mDrawerListView);
			Log.d((((TextView) view).getText()).toString(), (((TextView) view).getText()).toString());

			selectItem(view);

		}
	}
}
