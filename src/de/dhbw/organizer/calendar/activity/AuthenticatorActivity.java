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

package de.dhbw.organizer.calendar.activity;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import de.dhbw.organizer.R;
import de.dhbw.organizer.calendar.Constants;

/**
 * Activity which displays login screen to the user.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {

	private static final String DEFAULT_PASSWORD = "DEADBEAF";
	/** The Intent flag to confirm credentials. */
	public static final String PARAM_CONFIRM_CREDENTIALS = "confirmCredentials";

	/** The Intent extra to store password. */
	public static final String PARAM_ICAL_URL = "iCalUrl";

	/** The tag used to log to adb console. */
	private static final String TAG = "iCalAuthenticatorActivity";
	private AccountManager mAccountManager;

	private TextView mMessage;

	private String mICalUrl;

	private Spinner mIcalSpinner;



	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		mAccountManager = AccountManager.get(this);

		final Intent intent = getIntent();

		mICalUrl = intent.getStringExtra(PARAM_ICAL_URL);

		requestWindowFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.select_ical_activity);
		getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, android.R.drawable.ic_dialog_info);

		mMessage = (TextView) findViewById(R.id.message);
		mMessage.setText(getMessage());

		mIcalSpinner = (Spinner) findViewById(R.id.ical_calendar_spinner);

		//mAvailableCalendars = getResources().getStringArray(R.id.ical_calendar_spinner);

		/*String[] calendars = mAvailableCalendars.clone();

		for (int i = 0; i < mAvailableCalendars.length; i++) {
			calendars[i].replace("kal-", "");
			calendars[i] = calendars[i].toUpperCase();
		}
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, calendars);
*/
		

		 ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
		 this, R.array.calendars_array,
		 android.R.layout.simple_spinner_item);

		adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
		mIcalSpinner.setAdapter(adapter);

	}

	/*
	 * {@inheritDoc}
	 */
	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		final ProgressDialog dialog = new ProgressDialog(this);

		dialog.setMessage(getText(R.string.ui_activity_authenticating));
		dialog.setIndeterminate(true);
		dialog.setCancelable(true);

		dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				Log.i(TAG, "user cancelling authentication");
			}
		});
		return dialog;
	}

	/**
	 * Handles onClick event on the Submit button. Sends username/password to
	 * the server for authentication. The button is configured to call
	 * handleLogin() in the layout XML.
	 * 
	 * @param view
	 *            The Submit button for which this method is invoked
	 */
	public void handleLogin(View view) {
		String selected = (String) mIcalSpinner.getSelectedItem();

	
		mICalUrl = selected;
		
		Log.i(TAG, "Selected : " + mICalUrl);

		Log.i(TAG, "finishLogin()");
		final Account account = new Account(mICalUrl, Constants.ACCOUNT_TYPE);

		mAccountManager.addAccountExplicitly(account, DEFAULT_PASSWORD, null);
		
		// Set contacts sync for this account.
		ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 1);
		ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, true);
		ContentResolver.addPeriodicSync(account, CalendarContract.AUTHORITY, new Bundle(), Constants.SYNC_INTERVALL_IN_SEC);

		final Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mICalUrl);
		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();

		ContentValues values = new ContentValues();

		values.put(

		Calendars.ACCOUNT_NAME, Constants.ACCOUNT_TYPE);

		values.put(Calendars.ACCOUNT_TYPE, CalendarContract.CALLER_IS_SYNCADAPTER);
		values.put(Calendars.NAME, mICalUrl);
		values.put(Calendars.CALENDAR_DISPLAY_NAME, mICalUrl);
		values.put(Calendars.CALENDAR_COLOR, 0xffff0000);
		values.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_READ);
		values.put(Calendars.OWNER_ACCOUNT, mICalUrl);
		values.put(Calendars.CALENDAR_TIME_ZONE, "Europe/Berlin");

		Uri.Builder builder = CalendarContract.Calendars.CONTENT_URI.buildUpon();

		builder.appendQueryParameter(Calendars.ACCOUNT_NAME, mICalUrl);
		builder.appendQueryParameter(Calendars.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
		builder.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true");

		

	}

	public void onAuthenticationCancel() {
		Log.i(TAG, "onAuthenticationCancel()");
	}

	/**
	 * Returns the message to be displayed at the top of the login dialog box.
	 */
	private CharSequence getMessage() {
		getString(R.string.account_label);
		if (TextUtils.isEmpty(mICalUrl)) {
			// If no username, then we ask the user to log in using an
			// appropriate service.
			final CharSequence msg = getText(R.string.select_ical_activity_newaccount_text);
			return msg;
		}
		return null;
	}

}
