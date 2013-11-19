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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Service to handle Account authentication. It instantiates the authenticator
 * and returns its IBinder.
 */
public class AuthenticationService extends Service {

	private static final String TAG = "iCal AuthenticationService";

	private Authenticator mAuthenticator;

	@Override
	public void onCreate() {
		if (mAuthenticator == null) {
			Log.i(TAG, "iCal SyncAdapter Authentication Service started.");
			mAuthenticator = new Authenticator(this);
		}
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "iCal SyncAdapter Authentication Service stopped.");
		mAuthenticator = null;
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG,"getBinder()...  returning the AccountAuthenticator binder for intent "+ intent);
		return mAuthenticator.getIBinder();
	}
}
