package de.dhbw.organizer.calendar.frontend.activity;

import java.io.IOException;
import java.util.ArrayList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SyncStatusObserver;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import de.dhbw.organizer.R;
import de.dhbw.organizer.calendar.Constants;
import de.dhbw.organizer.calendar.backend.activity.AuthenticatorActivityTabed;
import de.dhbw.organizer.calendar.frontend.adapter.EventAdapter;
import de.dhbw.organizer.calendar.frontend.manager.CalendarManager;
import de.dhbw.organizer.calendar.helper.FileHelper;

/**
 * @author riedings
 * 
 */

public class Vorlesungsplan extends Activity {

	private static final String TAG = "calendar frontend Vorlesungsplan";

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
	private boolean isAddCalendarActivityShown = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate() ");

		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.calendar_activity_vorlesungsplan);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerListView = (ListView) findViewById(R.id.left_drawer);
		mDrawerListView.setOnItemClickListener(new DrawerItemClickListener());

		// create ActionBarDrawerToggle
		mActionBarDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
		mDrawerLayout, /* DrawerLayout object */
		R.drawable.ic_drawer, /* nav drawer icon to replace 'Up' caret */
		R.string.calendar_frontend_drawer_open, /* "open drawer" description */
		R.string.calendar_frontend_drawer_close /* "close drawer" description */
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
				// mDrawerListView.setItemChecked(mPosition, true);
			}
		};

		// Set actionBarDrawerToggle as the DrawerListener
		mDrawerLayout.setDrawerListener(mActionBarDrawerToggle);

		mAccountManager = AccountManager.get(this);

		mAccountManager = AccountManager.get(this);

		mCalenderSyncStatusObserver = new CalenderSyncStatusObserver();
		mAccountManager = AccountManager.get(this);

		/*
		 * Methode zum speichern des letzten ausgewaehlten Kalenders erstellen
		 */
		mCalendarManager = new CalendarManager();

		mUpdateViewDialog = new ProgressDialog(mContext);
		mUpdateViewDialog.setMessage(getString(R.string.calendar_frontend_updateing_calendar));

		setDrawerContent();

		try {
			mCalendarName = FileHelper.readFileAsString(this, "lastCalendarOpened");
			setListContent(this, mCalendarName);
		} catch (Exception e) {
			// create File
			FileHelper.createCacheFile(this, "lastCalendarOpened", ".txt");
			// Toast: Bitte f�ge einen neuen Kalender hinzu
			mCalendarName = null;
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume() ");
		mCalendarList = mCalendarManager.getCalendarList(this);

		mChangeListenerHandle = ContentResolver.addStatusChangeListener(ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE, mCalenderSyncStatusObserver);

		if (mDrawerLayout != null)
			mDrawerLayout.closeDrawers();

		if (mCalendarList.isEmpty()) {
			mCalendarName = null;
		}

		if (mCalendarList.isEmpty() && !isAddCalendarActivityShown) {
			isAddCalendarActivityShown = true;
			final Intent newCalendar = new Intent(this, AuthenticatorActivityTabed.class);
			this.startActivity(newCalendar);

		} else if (mCalendarList.isEmpty() && isAddCalendarActivityShown) {
			this.onBackPressed();

		}

		if (!mCalendarList.isEmpty() && mCalendarName == null) {
			mCalendarName = mCalendarList.get(0);
			setListContent(mContext, mCalendarName);
		}

		if (mCalendarName != null && mCalendarList.isEmpty())
			Toast.makeText(mContext, mCalendarName, Toast.LENGTH_SHORT).show();

	};

	@Override
	protected void onPause() {
		super.onPause();
		ContentResolver.removeStatusChangeListener(mChangeListenerHandle);

	};

	private void setDrawerContent() {

		mCalendarList = mCalendarManager.getCalendarList(this);

		if (mCalendarList.isEmpty()) {
			return;
		} else {

			Log.d("Kalendar: ", mCalendarList.get(0));
			if (mCalendarList.size() == 1) {
				try {
					FileHelper.writeFileAsString(Vorlesungsplan.this, "lastCalendarOpened", mCalendarList.get(0));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// Set the adapter for the list view
		mDrawerListView.setAdapter(new ArrayAdapter<String>(this, R.layout.calendar_drawer_listview_item, mCalendarList));

		try {
			String Calendarname = FileHelper.readFileAsString(this, "lastCalendarOpened");
			int actualSelectedCalendar = mCalendarList.indexOf(Calendarname);
			mDrawerListView.setItemChecked(actualSelectedCalendar, true);
		} catch (Exception e) {
			// create File
			FileHelper.createCacheFile(this, "lastCalendarOpened", ".txt");
			// Toast: Bitte f�ge einen neuen Kalender hinzu
		}

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
		/*
		 * currently un used case R.id.de_calendar_menu_preferences: final
		 * Intent preferences = new Intent(this, Preferences.class);
		 * this.startActivity(preferences);
		 * 
		 * break;
		 */

		case R.id.de_calendar_menu_delete_calendar:
			// delete Calendar
			getConfirmDeleteDialog();
			break;

		case R.id.jumptotoday:
			Toast.makeText(mContext, R.string.calendar_frontend_list_goto_today, Toast.LENGTH_LONG).show();
			goToActualEvent();
			break;

		case R.id.refresh:
			mUpdateViewDialog.show();
			mCalendarManager.syncCalendar(mCalendarName);
			break;

		default:
			break;
		}

		// Pass the event to ActionBarDrawerToggle, if it returns
		// true, then it has handled the app icon touch event
		if (mActionBarDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Set the content of the List and go to the actual event
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
			// DayAdapter mEvents = mCalendarManager.getCalendarDays(this,
			// calendarName);
			try {
				if (mEvents.getCount() == 0) {
					showDialogListEmpty();
				}

			} catch (IndexOutOfBoundsException e) {

				showDialogListEmpty();
			}

			mCalendarName = calendarName;
			// set Adapter to display List
			mEventList.setAdapter(mEvents);
			// if(mEventList.is == null){
			// showDialogListEmpty();
			// }
			Log.i("setListContent", "goto today");
			goToActualEvent();

			// Toast.makeText(mContext, mCalendarName,
			// Toast.LENGTH_SHORT).show();

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

			mDrawerLayout.closeDrawer(mDrawerListView);
			Log.d((((TextView) view).getText()).toString(), (((TextView) view).getText()).toString());

			selectItem(view);

			String calendarName = (((TextView) view).getText()).toString();

			// schreibe in die Datei
			try {
				FileHelper.writeFileAsString(Vorlesungsplan.this, "lastCalendarOpened", calendarName);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * creates a Dialog to confirm delete of calendar
	 * 
	 * @return
	 */
	private void getConfirmDeleteDialog() {
		new AlertDialog.Builder(this).setTitle(R.string.calendar_frontend_delete_calendar_dialog_heading)
				.setMessage(R.string.calendar_frontend_delete_calendar)
				.setPositiveButton(R.string.general_yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						deleteCalendar();
					}
				}).setNegativeButton(R.string.general_no, null).show();
	}

	/**
	 * Delete the actual Calendar
	 */
	private void deleteCalendar() {
		de.dhbw.organizer.calendar.backend.manager.CalendarManager cm = de.dhbw.organizer.calendar.backend.manager.CalendarManager.get(this);
		if (mCalendarName != null) {
			cm.deleteCalendar(mCalendarName);
			mCalendarList.remove(mCalendarName);

			if (mCalendarList.size() > 0) {
				mCalendarName = mCalendarList.get(0);
				setListContent(this, mCalendarName);
			} else {
				mCalendarName = null;
				setListContent(this, null);

				final Intent intent = new Intent(this, AuthenticatorActivityTabed.class);
				this.startActivity(intent);
			}
		}

	}

	/**
	 * show a dialog to inform the user, that the list is empty, and not broken
	 */
	private void showDialogListEmpty() {
		// only if its currently not updating
		if (!mUpdateViewDialog.isShowing()) {
			// TODO Auto-generated method stub
			new AlertDialog.Builder(this).setTitle(R.string.calendar_frontend_notice).setMessage(R.string.calendar_frontend_no_events)
					.setPositiveButton(R.string.general_ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					}).show();
		}
	}

	/**
	 * 
	 * @author friedrda
	 * 
	 */
	public class CalenderSyncStatusObserver implements SyncStatusObserver {
		private static final String TAG = "calendar CalenderSyncStatusObserver";

		private boolean mmStartSync = false;

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.content.SyncStatusObserver#onStatusChanged(int)
		 */
		@Override
		public void onStatusChanged(int which) {
			// Log.d(TAG, "onStatusChanged() ");
			Account accounts[] = mAccountManager.getAccountsByType(Constants.ACCOUNT_TYPE);

			// if Calendarname is not yet set, happens by first install, set the
			// first calender
			if (accounts.length > 0 && (mCalendarName == null || mCalendarName.equals(""))) {
				mCalendarName = accounts[0].name;
			}

			for (Account a : accounts) {
				// Log.d(TAG, "onStatusChanged() a.name = " + a.name);
				if (a.name.equals(mCalendarName)) {
					notifyView(ContentResolver.isSyncActive(a, CalendarContract.AUTHORITY));
				}
			}
		}

		/**
		 * 
		 * @param isSyncing
		 */
		public void notifyView(final boolean isSyncing) {

			if (!mmStartSync && isSyncing) {
				Log.d(TAG, "notifyView() show waitDialog");
				mmStartSync = true;
				runOnUiThread(new Runnable() {
					public void run() {
						mUpdateViewDialog.show();
					}
				});

			} else if (mmStartSync && !isSyncing) {
				Log.d(TAG, "notifyView() dismiss waitDialog");

				runOnUiThread(new Runnable() {
					public void run() {
						mUpdateViewDialog.dismiss();
						setListContent(mContext, mCalendarName);
					}
				});
				mmStartSync = false;
			}

		}
	}

}
