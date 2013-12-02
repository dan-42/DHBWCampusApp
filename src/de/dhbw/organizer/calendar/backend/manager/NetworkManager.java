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

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.AndroidHttpClient;
import android.util.Log;

/**
 * 
 * Handels Calendar-Modul specific http requests
 * 
 * @author friedrda
 * 
 *         Thanks to Vivek Parihar http://stackoverflow.com/questions/4238921
 *         /android-detect-whether-there-is-an-internet-connection-available
 * 
 */
public class NetworkManager {

	private static final String TAG = "Calendar Backend NetworkManager";
	private ConnectivityManager connectivityManager;
	private static NetworkManager instance = new NetworkManager();
	static Context mContext;

	NetworkInfo wifiInfo, mobileInfo;

	public static NetworkManager getInstance(Context ctx) {
		mContext = ctx;
		return instance;
	}

	/**
	 * tests if device got any internet connection
	 * 
	 * @return true if availabe, false otherwise
	 */
	public boolean isOnline() {
		boolean connected = false;
		try {
			connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

			NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
			connected = networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
			return connected;

		} catch (Exception e) {

			Log.e(TAG, "isOnline() ERROR " + e.toString());
		}
		return connected;
	}

	/**
	 * this function tests to download the file from the given URL and returns
	 * the HTTP status code
	 * 
	 * @param url
	 * @return http status code, or 0 by any error
	 */
	public int testHttpUrl(String url) {

		if (isOnline()) {

			HttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(url);
			
			
			

			if (!httpGet.containsHeader("Accept-Encoding")) {
				httpGet.addHeader("Accept-Encoding", "gzip");
			}

			HttpResponse httpResponse = null;

			try {
				httpResponse = httpClient.execute(httpGet);

				int httpStatus = httpResponse.getStatusLine().getStatusCode();
				Log.i(TAG, "url = " + url + " HTTP:  " + httpStatus);
				return httpStatus;

			} catch (ClientProtocolException e) {
				e.printStackTrace();
				Log.e(TAG, "ERROR: " + e.getMessage());
				return 0;
			} catch (IOException e) {
				Log.e(TAG, "ERROR: " + e.getMessage());
				e.printStackTrace();
				return 0;
			} catch (IllegalStateException e){
				Log.e(TAG, "ERROR: " + e.getMessage());
				e.printStackTrace();
				return 0;
			}
		} else
			return 0;

	}

	/**
	 * Downloads a File from the given URl via HTTP GET and gzipped and returns
	 * an open InputStream to read from
	 * 
	 * @param url
	 * @return InputStream
	 */
	public InputStream downloadHttpFile(String url) {

		if (isOnline()) {

			HttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(url);

			if (!httpGet.containsHeader("Accept-Encoding")) {
				httpGet.addHeader("Accept-Encoding", "gzip");
			}

			HttpResponse httpResponse = null;

			try {
				httpResponse = httpClient.execute(httpGet);

				int httpStatus = httpResponse.getStatusLine().getStatusCode();
				Log.i(TAG, "downloadHttpFile()  url = " + url + " HTTP:  " + httpStatus);

				HttpEntity entity = httpResponse.getEntity();

				return AndroidHttpClient.getUngzippedContent(entity);

			} catch (ClientProtocolException e) {
				e.printStackTrace();
				Log.e(TAG, "downloadHttpFile() ERROR: " + e.getMessage());
				return null;
			} catch (IOException e) {
				Log.e(TAG, "downloadHttpFile()  ERROR: " + e.getMessage());
				e.printStackTrace();
				return null;
			}
		} else
			return null;

	}

}
