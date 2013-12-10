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
 * 
 * 
 * THIS CLASS CONTAINS PARTS FROM The Android Open Source Project
 * http://code.google.com/p/toolib/source/browse/trunk/framework/core/java/android/accounts/AccountAuthenticatorActivity.java?r=52
 */

package de.dhbw.organizer.calendar.backend.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import de.dhbw.organizer.R;
import de.dhbw.organizer.calendar.Constants;
import de.dhbw.organizer.calendar.backend.manager.CalendarManager;
import de.dhbw.organizer.calendar.backend.manager.NetworkManager;
import de.dhbw.organizer.calendar.backend.objects.SpinnerItem;
import de.dhbw.organizer.calendar.helper.FileHelper;

/**
 * @author friedrda
 * 
 */

public class AuthenticatorActivityTabed extends Activity {

	private AccountAuthenticatorResponse mAccountAuthenticatorResponse = null;
	private Bundle mResultBundle = null;

	private static final String TAG = "iCalAuthenticatorActivity";

	private static final String DEFAULT_PASSWORD = "DEADBEAF";

	private static final int ICAL_URL_MIN_LENGTH = 11;

	private static final String REG_EX_DISPLAY_NAME_PATTERN = "^([a-zA-Z0-9.-_ ]{3,16})$";

	private static final int DISPLAY_NAME_MIN_LENGTH = 3;

	private static final int DISPLAY_NAME_MAX_LENGTH = 16;

	public static final String STATE_INFO_IS_SHOWING_DIALOG = "de.dhbw.campusapp.calendar.backend.showwaitdialog";
	public static final String STATE_INFO_CALENDAR_DISPLAY_NAME = "de.dhbw.campusapp.calendar.backend.cal.displayname";
	public static final String STATE_INFO_CALENDAR_URL = "de.dhbw.campusapp.calendar.backend.cal.url";
	public static final String STATE_INFO_CALENDAR_COLOR = "de.dhbw.campusapp.calendar.backend.cal.color";

	private AccountManager mAccountManager;

	private TextView mInfoMessage;

	private TextView mErrorMessageSpinner;

	private TextView mErrorMessageByhand;

	private ImageButton mUpdateListButton;

	private Spinner mIcalSpinner;

	private EditText mDisplayNameEditText;

	private EditText mICalUrlEditText;

	private TabHost mTabHost = null;

	private ArrayList<SpinnerItem> mItemList = null;

	private ArrayAdapter<SpinnerItem> mAdapter;

	private ProgressBar mProgress;

	// allow only one update
	private boolean mListUpdated = false;

	// for validating the Forms
	private boolean mFormIsValid;

	private String mCalendarDisplayName = null;

	private String mCalendarICalUrl = null;

	private int mCalendarColor = -1;

	private HttpConnectionTester mHttpConnectionTester = null;

	private ProgressDialog mProgressDialog = null;

	private CalenderSyncStatusObserver mCalenderSyncStatusObserver;

	private Object mCalenderSyncStatusObserverHandle;

	private boolean mIsManualInput = false;

	/**
	 * used to make sure therevare no refresh problems, e.g. when an accout with
	 * the same name is added, removed an added very quickly in a row
	 */
	private UUID mAccountUuid;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		CalendarManager cm = CalendarManager.get(this);
		mAccountManager = AccountManager.get(this);

		mCalenderSyncStatusObserver = new CalenderSyncStatusObserver();

		mAccountAuthenticatorResponse = getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);

		if (mAccountAuthenticatorResponse != null) {
			mAccountAuthenticatorResponse.onRequestContinued();
		}

		setContentView(R.layout.calendar_backend_add_account_tabed);
		mTabHost = (TabHost) findViewById(R.id.tabhost);
		mTabHost.setup();

		/**
		 * Spinner TAB
		 */
		TabHost.TabSpec specSpinner = mTabHost.newTabSpec("tab_spinner");
		specSpinner.setContent(R.id.calendar_backend_account_tab_spinner);

		specSpinner.setIndicator(getResources().getString(R.string.calendar_backend_account_tab_title_by_list));
		mTabHost.addTab(specSpinner);

		/**
		 * text input TAB
		 */
		TabHost.TabSpec specManual = mTabHost.newTabSpec("tab_manual");
		specManual.setContent(R.id.calendar_backend_account_manual_tab);
		specManual.setIndicator(getResources().getString(R.string.calendar_backend_account_tab_title_by_hand));
		mTabHost.addTab(specManual);

		/**
		 * set current TAB
		 */
		mTabHost.setCurrentTab(0);

		mUpdateListButton = (ImageButton) findViewById(R.id.calendar_backend_account_select_ical_update_button);

		mProgress = (ProgressBar) findViewById(R.id.calendar_backend_account_calendar_list_update_progressbar);

		mInfoMessage = (TextView) findViewById(R.id.calendar_backend_account_information_message);

		mErrorMessageByhand = (TextView) findViewById(R.id.calendar_backend_account_warning_message_manual);

		mErrorMessageSpinner = (TextView) findViewById(R.id.calendar_backend_account_warning_message);

		mIcalSpinner = (Spinner) findViewById(R.id.calendar_backend_account_ical_calendar_spinner);

		mDisplayNameEditText = (EditText) findViewById(R.id.calendar_backend_account_display_name_editText);

		mICalUrlEditText = (EditText) findViewById(R.id.calendar_backend_account_ical_url_editText);

		try {
			mItemList = (ArrayList<SpinnerItem>) cm.getSelectableCalendars();
		} catch (IOException e) {
			mItemList = new ArrayList<SpinnerItem>();
			mItemList.add(new SpinnerItem("XML-PARSE ERROR", " "));
			e.printStackTrace();
		}

		// sort
		Collections.sort(mItemList);

		mItemList.add(0, new SpinnerItem(getString(R.string.calendar_backend_input_select_calendar), ""));

		mAdapter = new ArrayAdapter<SpinnerItem>(this, android.R.layout.simple_spinner_item, mItemList);

		mAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
		mIcalSpinner.setAdapter(mAdapter);

		mProgressDialog = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);
		mProgressDialog.setMessage(getString(R.string.calendar_backend_adding_new_calendar));
		mProgressDialog.setCancelable(false);

		if (savedInstanceState != null) {
			if (savedInstanceState.getBoolean(STATE_INFO_IS_SHOWING_DIALOG)) {
				mProgressDialog.show();
			} else {
				mProgressDialog.dismiss();
			}
			mCalendarDisplayName = savedInstanceState.getString(STATE_INFO_CALENDAR_DISPLAY_NAME);
			mCalendarICalUrl = savedInstanceState.getString(STATE_INFO_CALENDAR_URL);
			mCalendarColor = savedInstanceState.getInt(STATE_INFO_CALENDAR_COLOR);

		}

	}

	@Override
	public void onResume() {
		super.onResume();

		mCalenderSyncStatusObserverHandle = ContentResolver.addStatusChangeListener(ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE,
				mCalenderSyncStatusObserver);

		if (!NetworkManager.getInstance(this).isOnline()) {
			mErrorMessageSpinner.setText(getText(R.string.calendar_backend_network_error));
			mErrorMessageByhand.setText(getText(R.string.calendar_backend_network_error));
		} else {
			mErrorMessageSpinner.setText(null);
			mErrorMessageByhand.setText(null);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		ContentResolver.removeStatusChangeListener(mCalenderSyncStatusObserverHandle);
		mProgressDialog.dismiss();
	}

	@Override
	protected void onSaveInstanceState(Bundle saveState) {
		super.onSaveInstanceState(saveState);
		saveState.putBoolean(STATE_INFO_IS_SHOWING_DIALOG, mProgressDialog.isShowing());
		saveState.putString(STATE_INFO_CALENDAR_DISPLAY_NAME, mCalendarDisplayName);
		saveState.putString(STATE_INFO_CALENDAR_URL, mCalendarICalUrl);
		saveState.putInt(STATE_INFO_CALENDAR_COLOR, mCalendarColor);

	}

	/**
	 * Handles onClick event on the Submit fro adding a Calendar by List
	 * 
	 * @param view
	 */
	public void addCalendarFromSpinner(View View) {

		if (!NetworkManager.getInstance(this).isOnline()) {
			mErrorMessageSpinner.setText(getText(R.string.calendar_backend_network_error));
			mErrorMessageByhand.setText(getText(R.string.calendar_backend_network_error));
			return;
		} else {
			mErrorMessageSpinner.setText(null);
			mErrorMessageByhand.setText(null);
		}

		SpinnerItem selected = (SpinnerItem) mIcalSpinner.getSelectedItem();
		if (mIcalSpinner.getSelectedItemPosition() == 0 || selected.equals(getString(R.string.calendar_backend_input_select_calendar))) {
			mInfoMessage.setText(R.string.calendar_backend_input_error_no_calendar_selected);
			return;
		}

		// no validation needed, since we assume the XML is valid and so are
		// just testing for httpconnection!
		mCalendarICalUrl = selected.getmIcalUrl();
		mCalendarDisplayName = selected.getmDisplayName();
		mFormIsValid = true;

		mIsManualInput = false;
		mProgressDialog.show();

		// test in background if file is accessable
		// when done addCalendar is called!
		mHttpConnectionTester = new HttpConnectionTester(this);
		mHttpConnectionTester.execute(mCalendarICalUrl);
	}

	/**
	 * Handles onClick event on the Submit button handels the Input from the
	 * "By HAND"-Form
	 * 
	 * @param view
	 */
	public void addCalendarFromInputForm(View view) {
		if (!NetworkManager.getInstance(this).isOnline()) {
			mErrorMessageSpinner.setText(getText(R.string.calendar_backend_network_error));
			mErrorMessageByhand.setText(getText(R.string.calendar_backend_network_error));
			return;
		} else {
			mErrorMessageSpinner.setText(null);
			mErrorMessageByhand.setText(null);
		}

		mCalendarDisplayName = mDisplayNameEditText.getText().toString();
		mCalendarICalUrl = mICalUrlEditText.getText().toString();

		mFormIsValid = true;
		CalendarManager cm = CalendarManager.get(this);

		/**
		 * Validate Display name
		 */
		if (mCalendarDisplayName.length() < DISPLAY_NAME_MIN_LENGTH) {
			mDisplayNameEditText.setError(getString(R.string.calendar_backend_input_error_displayname_to_short));
			mFormIsValid = false;
		} else if (mCalendarDisplayName.length() > DISPLAY_NAME_MAX_LENGTH) {
			mDisplayNameEditText.setError(getString(R.string.calendar_backend_input_error_displayname_to_long));
			mFormIsValid = false;
		} else if (!mCalendarDisplayName.matches(REG_EX_DISPLAY_NAME_PATTERN)) {
			mDisplayNameEditText.setError(getString(R.string.calendar_backend_input_error_displayname_invalid));
			mFormIsValid = false;
		} else {

			final Account account = new Account(mCalendarDisplayName, Constants.ACCOUNT_TYPE);

			if (cm.calendarExists(account)) {
				mDisplayNameEditText.setError(getString(R.string.calendar_backend_input_error_calendar_already_exists));
				mFormIsValid = false;
			} else {
				mDisplayNameEditText.setError(null);
				mFormIsValid = true;
			}
		}

		// stop do nothing if Display is still invalid
		if (!mFormIsValid) {
			return;
		}

		/**
		 * validate ICal Url
		 */
		if (mCalendarICalUrl.length() < ICAL_URL_MIN_LENGTH) {
			mICalUrlEditText.setError(getString(R.string.calendar_backend_input_error_icalurl_to_short));
			mFormIsValid = false;

		}
		if (!android.util.Patterns.WEB_URL.matcher(mCalendarICalUrl).matches()) {
			mICalUrlEditText.setError(getString(R.string.calendar_backend_input_error_icalurl_invalid));
			mFormIsValid = false;
		}

		if (mFormIsValid) {

			mIsManualInput = true;
			mProgressDialog.show();

			// test in background if file is accessable
			// when done addCalendar is called!
			mHttpConnectionTester = new HttpConnectionTester(this);
			mHttpConnectionTester.execute(mCalendarICalUrl);

		}

	}

	public void updateCalendarList(View view) {

		if (mListUpdated == false) {

			Log.d(TAG, "updateCalendarList()");

			UpdateXmlTask uxt = new UpdateXmlTask();
			uxt.execute(this);
		} else {
			mInfoMessage.setText(R.string.calendar_backend_update_already_done);
		}

	}

	public void cancel(View view) {
		Log.i(TAG, "Cancel()");
		finish();
	}

	/**
	 * Set the result that is to be sent as the result of the request that
	 * caused this Activity to be launched. If result is null or this method is
	 * never called then the request will be canceled.
	 * 
	 * @param result
	 *            this is returned as the result of the
	 *            AbstractAccountAuthenticator request
	 */
	public final void setAccountAuthenticatorResult(Bundle result) {
		mResultBundle = result;
	}

	/**
	 * Sends the result or a Constants.ERROR_CODE_CANCELED error if a result
	 * isn't present.
	 */
	public void finish() {
		if (mAccountAuthenticatorResponse != null) {
			// send the result bundle back if set, otherwise send an error.
			if (mResultBundle != null) {
				mAccountAuthenticatorResponse.onResult(mResultBundle);
			} else {
				mAccountAuthenticatorResponse.onError(AccountManager.ERROR_CODE_CANCELED, "canceled");
			}
			mAccountAuthenticatorResponse = null;
		}
		super.finish();
	}

	/**
	 * this function is called from HttpConnectiontester after user as Clicked
	 * "OK", we checked input and connection to iCalUrl now checking httpStatus
	 * and adding calendar
	 */
	private void addCalendar() {
		CalendarManager cm = CalendarManager.get(this);
		if (mFormIsValid) {
			/**
			 * HttpStatus is -1 if nothing is done 0 if no connection HTTPCode
			 * e.g 200 300 400 ...
			 */
			if (mHttpConnectionTester != null && mHttpConnectionTester.getHttpStatus() >= 200) {

				final Account account = new Account(mCalendarDisplayName, Constants.ACCOUNT_TYPE);

				if (!cm.calendarExists(account)) {

					mAccountUuid = UUID.randomUUID();
					mAccountManager.addAccountExplicitly(account, DEFAULT_PASSWORD, null);
					mAccountManager.setUserData(account, Constants.KEY_ACCOUNT_CAL_URL, mCalendarICalUrl);
					mAccountManager.setUserData(account, Constants.KEY_ACCOUNT_CAL_UUID, mAccountUuid.toString());

					ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 1);
					ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, true);
					ContentResolver.addPeriodicSync(account, CalendarContract.AUTHORITY, new Bundle(), Constants.SYNC_INTERVALL_IN_SEC);

					final Intent intent = new Intent();

					intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mCalendarDisplayName);
					intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
					setAccountAuthenticatorResult(intent.getExtras());
					setResult(RESULT_OK, intent);

					cm.createCalendar(account, mCalendarColor);

					triggerSync(account);
					try {
						FileHelper.writeFileAsString(this, "lastCalendarOpened", mCalendarDisplayName);
					} catch (IOException e) {
						e.printStackTrace();
					}

				} else {
					mInfoMessage.setText(R.string.calendar_backend_input_error_calendar_already_exists);
					mProgressDialog.dismiss();
				}

			} else {
				if (mIsManualInput) {
					mICalUrlEditText.setError(getString(R.string.calendar_backend_input_error_icalurl_invalid));
				} else {
					mErrorMessageByhand.setText(R.string.calendar_backend_input_error_icalurl_invalid);
				}
				Log.e(TAG, "addCalendar ERROR cannot add Calendar");
				mProgressDialog.dismiss();
			}
		}
	}

	private void triggerSync(Account account) {
		Log.e(TAG, "triggerSync() -- do it :-)");
		Bundle settingsBundle = new Bundle();
		settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
		settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
		/*
		 * Request the sync for the default account, authority, and manual sync
		 * settings
		 */
		ContentResolver.requestSync(account, CalendarContract.AUTHORITY, settingsBundle);

	}

	public class CalenderSyncStatusObserver implements SyncStatusObserver {
		private static final String TAG = "calendar backend  CalenderSyncStatusObserver";

		private boolean mmSyncStarted = false;
		private boolean mmSyncStoped = false;

		/**
		 * @return the mmSyncStarted
		 */
		public boolean isSyncStarted() {
			return mmSyncStarted;
		}

		/**
		 * @return the mmSyncStoped
		 */
		public boolean isSyncStoped() {
			return mmSyncStoped;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.content.SyncStatusObserver#onStatusChanged(int)
		 */
		@Override
		public void onStatusChanged(int which) {
			// Log.d(TAG, "onStatusChanged() ");
			Account accounts[] = mAccountManager.getAccountsByType(Constants.ACCOUNT_TYPE);

			for (Account a : accounts) {
				if (a.name.equals(mCalendarDisplayName)) {

					String aUuid = mAccountManager.getUserData(a, Constants.KEY_ACCOUNT_CAL_UUID);
					if (aUuid.equals(mAccountUuid.toString())) {
						Log.d(TAG, "onStatusChanged() a.name = " + a.name);
						setFlags(ContentResolver.isSyncActive(a, CalendarContract.AUTHORITY));
					} else {
						Log.i(TAG, "onStatusChanged() NOT the Same UUID a.name = " + a.name);
					}

				}
			}
		}

		public void setFlags(final boolean isSyncing) {
			Log.i(TAG, "notifyView() isSyncing = " + isSyncing);

			if (!mmSyncStarted && isSyncing) {
				Log.i(TAG, "notifyView() set mmSyncStarted true");
				mmSyncStarted = true;
			}

			if (mmSyncStarted && !isSyncing) {
				Log.i(TAG, "notifyView() set mmSyncStoped true");
				mmSyncStoped = true;

				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						mProgressDialog.dismiss();
						finish();

					}
				});

			}

		}

	}

	/**
	 * sub class to call an http get in background
	 * 
	 * @author friedrda
	 * 
	 */
	private class HttpConnectionTester extends AsyncTask<String, Integer, Integer> {

		private Context mmContext = null;
		private int mmHttpStatus = -1;

		public HttpConnectionTester(Context context) {
			mmContext = context;

		}

		public int getHttpStatus() {
			return mmHttpStatus;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
		 */
		@Override
		protected Integer doInBackground(String... params) {
			NetworkManager nm = NetworkManager.getInstance(mmContext);

			if (nm.isOnline()) {

				return nm.testHttpUrl(params[0]);

			} else {
				return 0;
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			mmHttpStatus = result;
			addCalendar();
		}

	}

	private class UpdateXmlTask extends AsyncTask<Context, Integer, Boolean> {

		private Context mmContext = null;

		@Override
		protected void onPreExecute() {
			mIcalSpinner.setEnabled(false);
			mIcalSpinner.setClickable(false);
			mUpdateListButton.setEnabled(false);
			mUpdateListButton.setClickable(false);
			mInfoMessage.setText(R.string.calendar_backend_update_updating);
		};

		@Override
		protected void onProgressUpdate(Integer[] value) {
			Log.d(TAG, "onProgressUpdate");
			mProgress.setProgress(value[0]);

		};

		@Override
		protected Boolean doInBackground(Context... contexts) {
			mmContext = contexts[0];
			publishProgress(20);

			CalendarManager cm = CalendarManager.get(mmContext);
			NetworkManager nm = NetworkManager.getInstance(mmContext);
			ArrayList<SpinnerItem> itemList = null;
			boolean success = false;

			publishProgress(10);
			if (nm.isOnline()) {
				publishProgress(15);
				if (cm.loadExternalXml()) {
					publishProgress(30);
					try {
						itemList = (ArrayList<SpinnerItem>) cm.getSelectableCalendars();
						publishProgress(40);
					} catch (IOException e) {
						itemList = new ArrayList<SpinnerItem>();
						itemList.add(new SpinnerItem("XML-PARSE ERROR", " "));
						e.printStackTrace();
					}
					publishProgress(50);
					// sort
					Collections.sort(itemList);
					publishProgress(60);
					mItemList = itemList;
					mItemList.add(0, new SpinnerItem(getString(R.string.calendar_backend_input_select_calendar), ""));
					publishProgress(70);
					success = true;
				}
			}
			publishProgress(100);

			return success;

		}

		/**
		 * this method runs on the UI-Thread, so now we can update the Spinner
		 * view
		 * 
		 * @param result
		 */
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			Log.d(TAG, "onPostExecute() update spinneritems");

			mIcalSpinner.setEnabled(true);
			mIcalSpinner.setClickable(true);

			if (result) {
				mAdapter.clear();
				mAdapter.addAll(mItemList);
				mAdapter.notifyDataSetChanged();

				mInfoMessage.setText(R.string.calendar_backend_update_successful);
				mListUpdated = true;
				mUpdateListButton.setVisibility(View.GONE);
			} else {
				mInfoMessage.setText(R.string.calendar_backend_update_xml_error);
				mUpdateListButton.setEnabled(true);
				mUpdateListButton.setClickable(true);
			}

		}

	}

}
