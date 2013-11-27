/**
 * 
 */
package de.dhbw.organizer.calendar.helper;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;

/**
 * This Class holds static functions to call for excample either the facebook
 * App or the Browser with the given url
 * 
 * @author friedrda
 * 
 */
public class IntentHelper {
	private static final String TAG = "calendar IntentHelper ";

	private static final String FB_APP_BASE_URL = "fb://";
	private static final String FB_WEB_BASE_URL = "https://www.facebook.com/";

	private static final String FB_APP_PROFILE_URL = FB_APP_BASE_URL + "profile/";
	private static final String FB_APP_EVENT_URL = FB_APP_BASE_URL + "event/";

	private static final String FB_WEB_EVENT_URL = FB_WEB_BASE_URL + "events/";
	private static final String FB_WEB_PROFILE_URL = FB_WEB_BASE_URL;

	public static enum Facebook {
		EVENT, PROFILE
	}

	/**
	 * opens the WebBrowser with the given url
	 * 
	 * @param context
	 * @param url
	 */
	public static void openWebBrowser(Context context, String url) {
		Log.i(TAG, "open Browser with " + url);
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(url));
		context.startActivity(intent);
	}

	public static void openFacebook(Context context, String id, Facebook fb) {
		String url = "";

		if (isFacebookInstalled(context)) {
			//build FB url
			Log.d(TAG, "openFacebook() facebook is installed");
			switch (fb) {
			case EVENT:
				url = FB_APP_EVENT_URL + id;
				break;
			case PROFILE:
				url = FB_APP_PROFILE_URL + id;
				break;
			default:
				Log.e(TAG, "openFacebook() cannot open!");
				break;
			}
		}//build web URL 
		else {
			Log.d(TAG, "openFacebook() facebook is NOT installed");
			switch (fb) {
			case EVENT:
				url = FB_WEB_EVENT_URL + id;
				break;
			case PROFILE:
				url = FB_WEB_PROFILE_URL + id;
			default:
				Log.e(TAG, "openFacebook() cannot open!");
				break;
			}

		}
		
		Log.d(TAG, "openFacebook() url = " + url);
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(url));
		context.startActivity(intent);
	}

	private static boolean isFacebookInstalled(Context context) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		final String urlFb = "fb://event/";
		intent.setData(Uri.parse(urlFb));

		// If Facebook application is installed, use that else launch a browser
		final PackageManager packageManager = context.getPackageManager();

		List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		if (list != null && list.size() > 0) {
			return true;
		} else {
			return false;
		}
	}

}
