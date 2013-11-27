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
 * 
 */
package de.dhbw.organizer.calendar.backend.syncadapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import de.dhbw.organizer.calendar.Constants;
import de.dhbw.organizer.calendar.backend.manager.CalendarManager;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

	private static final String TAG = "SyncAdapter";

	private final AccountManager mAccountManager;

	private final Context mContext;

	private final CalendarManager mCalendarManager;

	private static final String SYNC_MARKER_KEY = "de.dhbw.organizer.calendar.syncadapter";

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		mContext = context;
		mAccountManager = AccountManager.get(context);
		mCalendarManager = CalendarManager.get(mContext);

	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {

		HttpResponse httpResponse = null;
		Log.i(TAG, "SYNCC ME UP!");
		
		long lastSyncMarker = getLastServerSyncMarker(account);

		// make sure Calendar exists, other wise create it
		// could be deleted, by user interaction or by first Sync
		if (!mCalendarManager.calendarExists(account)) {
			mCalendarManager.createCalendar(account, null);
		}

		long nowInMillis = System.currentTimeMillis();

		// make sure we have some time between the last and new sync
		// some how the sync is triggerd twice in a very short intervall
		// to prevent this, we allow resync only every
		// MIN_SYNC_INTERVRALL_IN_MILLIS = 10 sec
		if (nowInMillis >= (lastSyncMarker + Constants.MIN_SYNC_INTERVRALL_IN_MILLIS)) {

			long calendarId = mCalendarManager.getCalendarId(account);

			String calendarHttpUrl = mAccountManager.getUserData(account, Constants.KEY_ACCOUNT_CAL_URL);
			if (calendarHttpUrl != null && calendarHttpUrl.length() > 11) {

				// todo check if URL is null
				HttpClient httpClient = new DefaultHttpClient();
				HttpGet httpGet = new HttpGet(calendarHttpUrl);

				if (!httpGet.containsHeader("Accept-Encoding")) {
					httpGet.addHeader("Accept-Encoding", "gzip");
				}

				try {

					Log.i(TAG, "http execute()");
					httpResponse = httpClient.execute(httpGet);

					int httpStatus = httpResponse.getStatusLine().getStatusCode();

					HttpEntity entity = httpResponse.getEntity();

					if (httpStatus == 200 && entity != null) {

						InputStream instream = AndroidHttpClient.getUngzippedContent(entity);

						ICalendar ical = Biweekly.parse(instream).first();

						instream.close();

						ArrayList<VEvent> events = (ArrayList<VEvent>) ical.getEvents();

						// mCalendarManager.deleteAllEvents(account,
						// calendarId);

						// mCalendarManager.insertEvents(account, calendarId,
						// events);

						mCalendarManager.updateEvents(account, calendarId, events, false);
						
						// save timestamp of last succsessful sync
						setLastServerSyncMarker(account, System.currentTimeMillis());

					} else {
						Log.i(TAG, "entity == null");
					}

				} catch (ClientProtocolException e) {

					e.printStackTrace();
				} catch (IOException e) {

					e.printStackTrace();
				}
			}
		} else {
			Log.i(TAG, "NO Sync MIN_SYNC_INTERVRALL_IN_MILLIS = " + Constants.MIN_SYNC_INTERVRALL_IN_MILLIS);
		}

		Log.i(TAG, "SYNC DONE");

	}

	/**
	 * get timestemp of last sync
	 * 
	 * @param account
	 * @return last sync timestemp in millis
	 */
	private long getLastServerSyncMarker(Account account) {
		String markerString = mAccountManager.getUserData(account, SYNC_MARKER_KEY);
		if (!TextUtils.isEmpty(markerString)) {
			return Long.parseLong(markerString);
		}
		return 0;
	}

	/**
	 * Saves last sync timestampt
	 * 
	 * @param account
	 * @param marker
	 *            timestamp in millisec
	 */
	private void setLastServerSyncMarker(Account account, long marker) {
		mAccountManager.setUserData(account, SYNC_MARKER_KEY, Long.toString(marker));
	}

	
}
