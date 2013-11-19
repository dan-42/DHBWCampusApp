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
 */
package de.dhbw.organizer.calendar.backend.manager;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * 
 * Handels Calendar-Modul specific http requests
 * 
 * @author friedrda
 * 
 *  Thanks to Vivek Parihar
 *         http://stackoverflow.com/questions/4238921
 *         /android-detect-whether-there-is-an-internet-connection-available
 * 
 */
public class NetworkManager {

	private ConnectivityManager connectivityManager;
	private static NetworkManager instance = new NetworkManager();
	static Context mContext;

	NetworkInfo wifiInfo, mobileInfo;
	boolean connected = false;

	public static NetworkManager getInstance(Context ctx) {
		mContext = ctx;
		return instance;
	}

	public NetworkManager() {
		// TODO Auto-generated constructor stub
	}

	public boolean isOnline() {
		try {
			connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

			NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
			connected = networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
			return connected;

		} catch (Exception e) {
			System.out.println("CheckConnectivity Exception: " + e.getMessage());
			Log.v("connectivity", e.toString());
		}
		return connected;
	}
}
