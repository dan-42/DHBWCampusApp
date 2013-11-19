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

package de.dhbw.organizer.calendar.backend.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import de.dhbw.organizer.R;
import de.dhbw.organizer.calendar.Constants;
import de.dhbw.organizer.calendar.backend.manager.CalendarManager;
import de.dhbw.organizer.calendar.backend.manager.NetworkManager;
import de.dhbw.organizer.calendar.backend.objects.SpinnerItem;

/**
 * Activity which displays login screen to the user.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {

	private static final String TAG = "iCalAuthenticatorActivity";

	private static final String DEFAULT_PASSWORD = "DEADBEAF";

	private AccountManager mAccountManager;

	private TextView mMessage;

	private Spinner mIcalSpinner;

	private Button mOkButton;
	private Button mCancelButton;
	private Button mUpdateListButton;

	private ProgressDialog mDialog;

	ArrayList<SpinnerItem> mItemList = null;

	private ArrayAdapter<SpinnerItem> mAdapter;

	/**
	 * {@inheritDoc}
	 * 
	 * @param mAvailableCalendars
	 */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		mAccountManager = AccountManager.get(this);

		requestWindowFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.calendar_account_select_calendar_activity);
		getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_calendar_main);

		mMessage = (TextView) findViewById(R.id.message);
		mMessage.setText(R.string.select_ical_activity_newaccount_text);

		CalendarManager cm = CalendarManager.get(this);

		mIcalSpinner = (Spinner) findViewById(R.id.ical_calendar_spinner);
		mOkButton = (Button) findViewById(R.id.ok_button);

		mUpdateListButton = (Button) findViewById(R.id.update_list_button);

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

	}

	/*
	 * {@inheritDoc}
	 */
	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {

		mDialog = new ProgressDialog(this);

		mDialog.setMessage(getText(R.string.ui_activity_authenticating));
		mDialog.setIndeterminate(true);
		mDialog.setCancelable(true);

		mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				Log.i(TAG, "user cancelling add calendar");
				dialog.dismiss();
			}
		});

		return mDialog;
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
			//mIcalSpinner.invalidate();

			//mDialog.dismiss();
			//mDialog.show();
		}
		

	}
}
