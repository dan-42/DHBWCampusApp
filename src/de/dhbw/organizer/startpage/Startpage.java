package de.dhbw.organizer.startpage;

import java.util.List;

import de.dhbw.organizer.R;
import de.dhbw.organizer.calendar.frontend.activity.Vorlesungsplan;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

/**
 * @author riedings
 * 
 */
public class Startpage extends Activity {

	private static final String TAG = "Startpage";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.startpage_activity);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.startpage_menu, menu);
		return true;
	}

	public void startCalendarActivity(View v) {
		Intent myIntent = new Intent(v.getContext(), Vorlesungsplan.class);
		startActivityForResult(myIntent, 0);
	}

	public void startMensaActivity(View v) {
		PackageManager pm = getPackageManager();
		Intent intent = pm.getLaunchIntentForPackage("de.dhbw.mensa");

		if (intent != null) {

			List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
			if (list.size() > 0) {
				startActivity(intent);
			} else {
				Log.e(TAG, "startMensaActivity() cant start MensaApp, there is non");
			}

		} else {
			Log.e(TAG, "startMensaActivity() cant start MensaApp, there is non");
		}
	}

}
