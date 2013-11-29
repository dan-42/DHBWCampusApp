package de.dhbw.organizer.startpage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import de.dhbw.organizer.R;
import de.dhbw.organizer.calendar.Constants;
import de.dhbw.organizer.calendar.frontend.activity.Vorlesungsplan;
import de.dhbw.organizer.calendar.helper.IntentHelper;
import de.dhbw.organizer.gebaudeplan.Gebaudeplan;

/**
 * @author riedings
 * 
 */
public class Startpage extends Activity {

	private static final String TAG = "Startpage";
	private TextView mTextViewTimeLeft;

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd ", Locale.getDefault());

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (Constants.TIME_END_TEST_VERSION > System.currentTimeMillis()) {
			setContentView(R.layout.startpage_activity);
			String text = getString(R.string.start_app_testversion_timeleft);
			mTextViewTimeLeft = (TextView) findViewById(R.id.start_app_testversion_timeleft_textview);
			mTextViewTimeLeft.setText(text + " " + sdf.format(new Date(Constants.TIME_END_TEST_VERSION)));
		} else {
			setContentView(R.layout.startpage_activity_deaktivated);

		}

	}

	@Override
	protected void onResume() {
		super.onResume();

		if (Constants.TIME_END_TEST_VERSION > System.currentTimeMillis()) {
			setContentView(R.layout.startpage_activity);
			String text = getString(R.string.start_app_testversion_timeleft);
			mTextViewTimeLeft = (TextView) findViewById(R.id.start_app_testversion_timeleft_textview);
			mTextViewTimeLeft.setText(text + " " + sdf.format(new Date(Constants.TIME_END_TEST_VERSION)));
		} else {
			setContentView(R.layout.startpage_activity_deaktivated);

		}

	};

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
			Toast.makeText(this, getString(R.string.mensa_error_not_installed), Toast.LENGTH_LONG).show();

		}
	}

	public void startOnlineFeedback(View v) {
		IntentHelper.openWebBrowser(this, Constants.ONLINE_FEEDBACK_URL);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(String.valueOf(item.getItemId()), String.valueOf(R.id.startpage_menu_info));
		switch (item.getItemId()) {
		case R.id.startpage_menu_info:
			getInfoDialog().show();
			break;
		default:
			break;

		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * erstellt ein Dialog, welches u.a. das Impressum enth�lt
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
		TextView versionNumber = (TextView) textEntryView.findViewById(R.id.start_app_version_string);
		String versionName = "";
		try {
			versionName = "Version: " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
		}

		versionNumber.setText(versionName);

		return new AlertDialog.Builder(this).setTitle("Info").setView(textEntryView).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.cancel();
			}
		}).setCancelable(false).create();

	}

	/**
	 * Method to open the FB page of information technic
	 * 
	 * @param v
	 *            View, which opens the function
	 */
	public void openSZIFacebookPage(View v) {
		// Open Facebook Page of "Informatik an der DHBW L�rrach"

		IntentHelper.openFacebook(this, this.getResources().getString(R.string.de_app_start_facebook_profile_id_szi), IntentHelper.Facebook.PROFILE);

	}

}
