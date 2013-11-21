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

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import de.dhbw.organizer.R;
import de.dhbw.organizer.calendar.Constants;
import de.dhbw.organizer.calendar.backend.manager.CalendarManager;
import de.dhbw.organizer.calendar.backend.manager.NetworkManager;
import de.dhbw.organizer.calendar.backend.objects.SpinnerItem;

/**
 * @author friedrda
 * 
 */

public class AuthenticatorActivityTabed extends Activity {

	private AccountAuthenticatorResponse mAccountAuthenticatorResponse = null;
	private Bundle mResultBundle = null;

	private static final String TAG = "iCalAuthenticatorActivity";

	private static final String DEFAULT_PASSWORD = "DEADBEAF";

	private AccountManager mAccountManager;

	private TextView mInfoMessage;

	private Spinner mIcalSpinner;

	private TabHost mTabHost = null;

	ArrayList<SpinnerItem> mItemList = null;

	private ArrayAdapter<SpinnerItem> mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAccountAuthenticatorResponse = getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);

		if (mAccountAuthenticatorResponse != null) {
			mAccountAuthenticatorResponse.onRequestContinued();
		}

		setContentView(R.layout.activity_calendar_account_tabed);
		mTabHost = (TabHost) findViewById(R.id.tabhost);
		mTabHost.setup();

		/**
		 * Spinner TAB
		 */
		TabHost.TabSpec spec = mTabHost.newTabSpec("tab_spinner");
		spec.setContent(R.id.calenda_account_spinner_tab);
		spec.setIndicator("Spinner");
		mTabHost.addTab(spec);

		/**
		 * text input TAB
		 */
		spec = mTabHost.newTabSpec("tab_manual");
		spec.setContent(R.id.calendar_account_manual_tab);
		spec.setIndicator("Manual");
		mTabHost.addTab(spec);

		/**
		 * set current TAB
		 */
		mTabHost.setCurrentTab(0);

		mInfoMessage = (TextView) findViewById(R.id.message);
		mInfoMessage.setText(R.string.select_ical_activity_newaccount_text);

		CalendarManager cm = CalendarManager.get(this);

		if (findViewById(R.id.ical_calendar_spinner) instanceof Spinner) {

			mIcalSpinner = (Spinner) findViewById(R.id.ical_calendar_spinner);

			try {
				mItemList = (ArrayList<SpinnerItem>) cm.getSelectableCalendars();
			} catch (IOException e) {
				mItemList = new ArrayList<SpinnerItem>();
				mItemList.add(new SpinnerItem("XML-PARSE ERROR", " "));
				e.printStackTrace();
			}

			// sort
			Collections.sort(mItemList);

			mAdapter = new ArrayAdapter<SpinnerItem>(this, android.R.layout.simple_spinner_item, mItemList);

			mAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
			mIcalSpinner.setAdapter(mAdapter);

		} else {
			Log.e(TAG, "ERROR" + findViewById(R.id.ical_calendar_spinner).toString());
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.calendar_account_tabed, menu);
		return true;
	}

	/**
	 * Handles onClick event on the Submit button. Sends username/password to
	 * the server for authentication. The button is configured to call
	 * handleLogin() in the layout XML.
	 * 
	 * @param view
	 *            The Submit button for which this method is invoked
	 */
	public void addCalendar(View view) {
		SpinnerItem selected = (SpinnerItem) mIcalSpinner.getSelectedItem();

		String iCalUrl = selected.getmIcalUrl();
		String displayName = selected.getmDisplayName();

		Log.i(TAG, "Selected : " + iCalUrl);
		Log.i(TAG, "finishLogin()");

		final Account account = new Account(displayName, Constants.ACCOUNT_TYPE);

		mAccountManager.addAccountExplicitly(account, DEFAULT_PASSWORD, null);

		mAccountManager.setUserData(account, Constants.KEY_ACCOUNT_CAL_URL, iCalUrl);

		ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 1);
		ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, true);
		ContentResolver.addPeriodicSync(account, CalendarContract.AUTHORITY, new Bundle(), Constants.SYNC_INTERVALL_IN_SEC);

		final Intent intent = new Intent();

		intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, displayName);

		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();

	}

	public void updateCalendarList(View view) {

		Log.d(TAG, "updateCalendarList()");

		UpdateXmlTask uxt = new UpdateXmlTask();
		uxt.execute(this);

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

	private class UpdateXmlTask extends AsyncTask<Context, Integer, Boolean> {

		private Context mmContext = null;

		@Override
		protected Boolean doInBackground(Context... contexts) {
			mmContext = contexts[0];

			CalendarManager cm = CalendarManager.get(mmContext);
			NetworkManager nm = NetworkManager.getInstance(mmContext);
			ArrayList<SpinnerItem> itemList = null;
			boolean success = false;

			if (nm.isOnline()) {
				if (cm.loadExternalXml()) {
					try {
						itemList = (ArrayList<SpinnerItem>) cm.getSelectableCalendars();
					} catch (IOException e) {
						itemList = new ArrayList<SpinnerItem>();
						itemList.add(new SpinnerItem("XML-PARSE ERROR", " "));
						e.printStackTrace();
					}

					// sort
					Collections.sort(itemList);

					mItemList = itemList;

					success = true;
				}
			}

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
			mAdapter.clear();
			mAdapter.addAll(mItemList);
			mAdapter.notifyDataSetChanged();
			// mIcalSpinner.invalidate();

			// mDialog.dismiss();
			// mDialog.show();
		}

	}

}
