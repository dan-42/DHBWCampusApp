package de.dhbw.organizer.startpage;

import java.util.List;

import de.dhbw.organizer.R;
import de.dhbw.organizer.calendar.backend.activity.AuthenticatorActivityTabed;
import de.dhbw.organizer.calendar.frontend.activity.Settings;
import de.dhbw.organizer.calendar.frontend.activity.Vorlesungsplan;
import de.dhbw.organizer.gebaudeplan.Gebaudeplan;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

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

	public void startGebaudeActivity(View v) {
		Intent myIntent = new Intent(v.getContext(), Gebaudeplan.class);
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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Toast.makeText(Startpage.this, item.getTitle(), Toast.LENGTH_LONG)
				.show();
		Log.d(String.valueOf(item.getItemId()), String.valueOf(R.id.startpage_menu_info));
		switch (item.getItemId()) {
		case R.id.startpage_menu_info:
			getInfoDialog().show();
			break;
		case R.id.startpage_menu_settings:
			break;
		default:
			break;

		}

		return super.onOptionsItemSelected(item);
	}
	
	
	/**
	 * erstellt ein Dialog, welches u.a. das Impressum enthält
	 * 
	 * @return
	 */
	private AlertDialog getInfoDialog() {
		LayoutInflater factory = LayoutInflater.from(this);
		final View textEntryView = factory.inflate(R.layout.info_dialog, null);
		final ViewFlipper flipper = (ViewFlipper) textEntryView.findViewById(R.id.flipper);
		flipper.startFlipping();
		flipper.setInAnimation((AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)));
		flipper.setOutAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right));

		return new AlertDialog.Builder(this).setTitle("Info").setView(textEntryView)
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.cancel();
					}
				}).setCancelable(false).create();

	}

}
