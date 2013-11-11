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
package de.dhbw.organizer.calendar.syncadapter;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.accounts.Account;
import android.accounts.AccountManager;
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
import de.dhbw.organizer.calendar.calendarmanager.CalendarManager;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

	private static final String TAG = "SyncAdapter";

	private final AccountManager mAccountManager;

	private final Context mContext;

	private static final String SYNC_MARKER_KEY = "de.dhbw.organizer.calendar.syncadapter";

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		mContext = context;
		mAccountManager = AccountManager.get(context);

	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {

		HttpResponse httpResponse = null;
		Log.i(TAG, "SYNCC ME UP!");

		long lastSyncMarker = getServerSyncMarker(account);

		if (lastSyncMarker == 0) {

			if (!CalendarManager.calendarExists(mContext, account)) {
				Log.i(TAG, "Calendar doesn't exists");
				CalendarManager.createCalendar(mContext, account);
			} else {
				Log.i(TAG, "Calendar does exists");
			}
		}

		long nowInMillis = System.currentTimeMillis();

		// make sure we have some time between the last and new sync
		if (nowInMillis >= (lastSyncMarker + Constants.MIN_SYNC_INTERVRALL_IN_MILLIS)) {

			long calendarId = CalendarManager.getCalendarId(mContext, account);

			String calendarHttpUrl = Constants.DHBW_ICAL_URL.replace(Constants.CALENDAR_REEPLACE_TOKEN, account.name);

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
				
					//ArrayList<VEvent> events = CalendarManager.fixMicrosoftFuckUps((ArrayList<VEvent>) ical.getEvents());
					
					ArrayList<VEvent> events =  (ArrayList<VEvent>) ical.getEvents();

					CalendarManager.deleteAllEvents(mContext, account, calendarId);

					CalendarManager.insertEvents(mContext, account, calendarId, events);
					/*try {
						CalendarManager.insertTestEvents(mContext, account, calendarId);
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}*/
					
					
					

					// save timestamp of last succsessful sync
					setServerSyncMarker(account, System.currentTimeMillis());

				} else {
					Log.i(TAG, "entity == null");
				}

			} catch (ClientProtocolException e) {

				e.printStackTrace();
			} catch (IOException e) {

				e.printStackTrace();
			}
		} else {
			Log.i(TAG, "NO Sync MIN_SYNC_INTERVRALL_IN_MILLIS = " + Constants.MIN_SYNC_INTERVRALL_IN_MILLIS);
		}

		Log.i(TAG, "SYNC DONW");

	}

	/**
	 * This helper function fetches the last known high-water-mark we received
	 * from the server - or 0 if we've never synced.
	 * 
	 * @param account
	 *            the account we're syncing
	 * @return the change high-water-mark
	 */
	private long getServerSyncMarker(Account account) {
		String markerString = mAccountManager.getUserData(account, SYNC_MARKER_KEY);
		if (!TextUtils.isEmpty(markerString)) {
			return Long.parseLong(markerString);
		}
		return 0;
	}

	/**
	 * Save off the high-water-mark we receive back from the server.
	 * 
	 * @param account
	 *            The account we're syncing
	 * @param marker
	 *            The high-water-mark we want to save.
	 */
	private void setServerSyncMarker(Account account, long marker) {
		mAccountManager.setUserData(account, SYNC_MARKER_KEY, Long.toString(marker));
	}

}
