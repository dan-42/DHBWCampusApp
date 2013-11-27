package de.dhbw.organizer.calendar.frontend.activity;

import java.io.IOException;
import java.util.ArrayList;

import de.dhbw.organizer.calendar.Constants;
import de.dhbw.organizer.calendar.backend.activity.AuthenticatorActivityTabed;
import de.dhbw.organizer.calendar.frontend.adapter.CalendarEvent;
import de.dhbw.organizer.calendar.frontend.adapter.EventAdapter;
import de.dhbw.organizer.calendar.frontend.manager.CalendarManager;
import de.dhbw.organizer.calendar.frontend.preferences.Preferences;
import de.dhbw.organizer.calendar.helper.FileHelper;
import de.dhbw.organizer.R;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author riedings
 * 
 */

public class Vorlesungsplan extends Activity {

	private ListView mEventList;
	private ListView mDrawerListView;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mActionBarDrawerToggle;
	private ArrayList<String> mCalendarList;
	private CalendarManager mCalendarManager;
	private String mCalendarName;
	private Context mContext;
	private CalenderSyncStatusObserver mCalenderSyncStatusObserver;
	private AccountManager mAccountManager;
	private ProgressDialog mUpdateViewDialog;
	private Object mChangeListenerHandle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.calendar_activity_vorlesungsplan);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		
		mAccountManager = AccountManager.get(this);		

		

		mCalenderSyncStatusObserver = new CalenderSyncStatusObserver();
		mAccountManager = AccountManager.get(this);


		/*
		 * Methode zum speichern des letzten ausgewaehlten Kalenders erstellen
		 */
		mCalendarManager = new CalendarManager();

		mUpdateViewDialog = new ProgressDialog(mContext);

		setDrawerContent();

		FileHelper fileHelper = new FileHelper();

		try {
			String Calendarname = fileHelper.readFileAsString(this, "lastCalendarOpened");
			setListContent(this, Calendarname);
		} catch (Exception e) {
			// create File
			FileHelper.createCacheFile(this, "lastCalendarOpened", ".txt");
			// Toast: Bitte f�ge einen neuen Kalender hinzu
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();	
	}
	protected void onResume() {
		super.onResume();
		mChangeListenerHandle = ContentResolver.addStatusChangeListener(ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE, mCalenderSyncStatusObserver);
	};

	@Override
	protected void onPause() {
		super.onPause();
		ContentResolver.removeStatusChangeListener(mChangeListenerHandle);

	};

	private void setDrawerContent() {

		mCalendarList = mCalendarManager.getCalendarList(this);

		// get ListView defined in activity_main.xml
		mDrawerListView = (ListView) findViewById(R.id.left_drawer);

		if (mCalendarList.isEmpty()) {

			// Hier ein Toast der sagt bitte neuen Kalender erstellen, dann von
			// links nach rechts wischen um Kalender auszuw�hlen
			Log.d("Kalendar: ", "mCalendarList is empty");

			final Intent intent = new Intent(this, AuthenticatorActivityTabed.class);
			this.startActivity(intent);

		} else {
			Log.d("Kalendar: ", mCalendarList.get(0));
			if (mCalendarList.size() == 1) {
				FileHelper fileHelper = new FileHelper();
				try {
					fileHelper.writeFileAsString(Vorlesungsplan.this, "lastCalendarOpened", mCalendarList.get(0));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// Set the adapter for the list view
		mDrawerListView.setAdapter(new ArrayAdapter<String>(this, R.layout.calendar_drawer_listview_item, mCalendarList));

		mDrawerListView.setOnItemClickListener(new DrawerItemClickListener());

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		// create ActionBarDrawerToggle
		mActionBarDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
		mDrawerLayout, /* DrawerLayout object */
		R.drawable.ic_drawer, /* nav drawer icon to replace 'Up' caret */
		R.string.drawer_open, /* "open drawer" description */
		R.string.drawer_close /* "close drawer" description */
		) {
			/**
			 * Called when a drawer has settled in a completely closed state.
			 * 
			 * */
			@Override
			public void onDrawerClosed(View view) {
			}

			/**
			 * Called when a drawer has settled in a completely open state.
			 */
			@Override
			public void onDrawerOpened(View drawerView) {
				setDrawerContent();
			}
		};

		// Set actionBarDrawerToggle as the DrawerListener
		mDrawerLayout.setDrawerListener(mActionBarDrawerToggle);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.calendar_menu, menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.calendar_refresh, menu);
		super.onCreateOptionsMenu(menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// Handle the New Calendar and the Settings Menu
		switch (item.getItemId()) {
		case R.id.de_calendar_menu_new_calendar:
			final Intent newCalendar = new Intent(this, AuthenticatorActivityTabed.class);
			this.startActivity(newCalendar);

			break;

		case R.id.de_calendar_menu_preferences:
			final Intent preferences = new Intent(this, Preferences.class);
			this.startActivity(preferences);

			break;

		case R.id.de_calendar_menu_delete_calendar:
			// delete Calendar
			de.dhbw.organizer.calendar.backend.manager.CalendarManager cm = de.dhbw.organizer.calendar.backend.manager.CalendarManager.get(this);
			cm.deleteCalendar(mCalendarName);
			mCalendarList.remove(mCalendarName);
			if (mCalendarList.size() > 0) {
				mCalendarName = mCalendarList.get(0);
				setListContent(this, mCalendarName);
			} else {
				mCalendarName = null;
				setListContent(this, null);
			}

			break;

		case R.id.jumptotoday:
			goToActualEvent();
			break;

		case R.id.refresh:
			mCalendarManager.syncCalendar(mCalendarName);
			break;

		default:
			break;
		}

		// Pass the event to ActionBarDrawerToggle, if it returns
		// true, then it has handled the app icon touch event
		if (mActionBarDrawerToggle.onOptionsItemSelected(item)) {
			// zurueck auf den Homescreen einbauen!

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
		// Exeption: Kalender nicht vorhanden behandeln!

		// find the Listview
		mEventList = (ListView) findViewById(R.id.calendar_frontend_listView);

		if (calendarName == null) {
			EventAdapter mEvents = null;
			mCalendarName = null;
			mEventList.setAdapter(mEvents);

		} else {
			// get the Events as an Adapter
			EventAdapter mEvents = mCalendarManager.getCalendarEvents(this, calendarName);
			mCalendarName = calendarName;
			// set Adapter to display List
			mEventList.setAdapter(mEvents);
			goToActualEvent();
		}

	}

	/**
	 * Method to set the selection to the actual event
	 */
	private void goToActualEvent() {
		// go to actual Date
		if (mCalendarManager != null && mCalendarManager.mIndexOfActualEvent > 0) {
			Log.d("Index of Actual Event", String.valueOf(mCalendarManager.mIndexOfActualEvent));
			mEventList.setSelectionFromTop(mCalendarManager.mIndexOfActualEvent, 0);

		} else {
			Log.i("goToActualEvent()", "Index of Actual Event not set, no Calendar");
		}
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
	 * @author riedings
	 * 
	 */
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView parent, View view, int position, long id) {
			Toast.makeText(Vorlesungsplan.this, ((TextView) view).getText(), Toast.LENGTH_LONG).show();
			mDrawerLayout.closeDrawer(mDrawerListView);
			Log.d((((TextView) view).getText()).toString(), (((TextView) view).getText()).toString());

			selectItem(view);

			String calendarName = (((TextView) view).getText()).toString();

			// schreibe in die Datei
			FileHelper fileHelper = new FileHelper();
			try {
				fileHelper.writeFileAsString(Vorlesungsplan.this, "lastCalendarOpened", calendarName);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	public class CalenderSyncStatusObserver implements SyncStatusObserver {
		private static final String TAG = "calendar CalenderSyncStatusObserver";

		private boolean mmLastSyncState = false;

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.content.SyncStatusObserver#onStatusChanged(int)
		 */
		@Override
		public void onStatusChanged(int which) {
			//Log.d(TAG, "onStatusChanged() ");
			Account accounts[] = mAccountManager.getAccountsByType(Constants.ACCOUNT_TYPE);

			
			//if Calendarname is not yet set, happens by first install, set the first calender
			if(accounts.length > 0 && (mCalendarName == null || mCalendarName.equals(""))){
				mCalendarName = accounts[0].name;
			}
			
			for (Account a : accounts) {
				//Log.d(TAG, "onStatusChanged() a.name = " + a.name);
				if (a.name.equals(mCalendarName)) {
					notifyView(ContentResolver.isSyncActive(a, CalendarContract.AUTHORITY));
				}
			}
		}

		public void notifyView(final boolean isSyncing) {

			runOnUiThread(new Runnable() {
				public void run() {
					if (isSyncing) {
						mUpdateViewDialog.show();
						Log.d(TAG, "notifyView()  is  syncing");
					} else {

						mUpdateViewDialog.dismiss();
						setListContent(mContext, mCalendarName);

						Log.d(TAG, "notifyView()  is not syncing");
					}
				}
			});

			mmLastSyncState = isSyncing;

		}

	}
}
