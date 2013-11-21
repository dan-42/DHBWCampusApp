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

package de.dhbw.organizer.calendar.backend.authenticator;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import de.dhbw.organizer.calendar.backend.activity.AuthenticatorActivityTabed;

class Authenticator extends AbstractAccountAuthenticator {

	/** The tag used to log to adb console. **/
	private static final String TAG = "Ical Authenticator";

	// Authentication Service context
	private final Context mContext;

	public Authenticator(Context context) {
		super(context);
		mContext = context;
	}

	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures,
			Bundle options) {
		Log.v(TAG, "addAccount()");

		final Intent intent = new Intent(mContext, AuthenticatorActivityTabed.class);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

		final Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, intent);

		return bundle;
	}

	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) {
		Log.v(TAG, "confirmCredentials()");
		return null;
	}

	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
		Log.v(TAG, "editProperties()");
		throw new UnsupportedOperationException();
	}

	@Override
	public String getAuthTokenLabel(String authTokenType) {
		// null means we don't support multiple authToken types
		Log.v(TAG, "getAuthTokenLabel()");
		return null;
	}

	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) {
		// This call is used to query whether the Authenticator supports
		// specific features. We don't expect to get called, so we always
		// return false (no) for any queries.
		Log.v(TAG, "hasFeatures()");
		final Bundle result = new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
		return result;
	}

	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle loginOptions) {
		Log.v(TAG, "updateCredentials()");
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.accounts.AbstractAccountAuthenticator#getAuthToken(android.accounts
	 * .AccountAuthenticatorResponse, android.accounts.Account,
	 * java.lang.String, android.os.Bundle)
	 */
	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options)
			throws NetworkErrorException {
		Log.v(TAG, "getAuthToken()");
		// TODO Auto-generated method stub
		return null;
	}

}
