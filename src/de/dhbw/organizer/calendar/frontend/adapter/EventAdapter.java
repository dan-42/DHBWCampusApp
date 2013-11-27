package de.dhbw.organizer.calendar.frontend.adapter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.dhbw.organizer.R;
import de.dhbw.organizer.calendar.frontend.parser.FbEventParser;
import de.dhbw.organizer.calendar.helper.IntentHelper;

public class EventAdapter extends BaseAdapter {

	private Context context;

	private List<CalendarEvent> listEvents;

	public EventAdapter(Context context, List<CalendarEvent> listEvents) {
		this.context = context;
		this.listEvents = listEvents;
	}

	public int getCount() {
		return listEvents.size();
	}

	public Object getItem(int position) {
		return listEvents.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup viewGroup) {
		CalendarEvent entry = listEvents.get(position);
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.calendar_activity_listview, null);
		}
		TextView tvTitle = (TextView) convertView.findViewById(R.id.name);
		tvTitle.setText(entry.getName());

		TextView tvTime = (TextView) convertView.findViewById(R.id.time);
		long startTime = entry.getStartTime();
		long endTime = entry.getEndTime();
		String startDate = getDate(startTime, "EEEE dd.MM.yy HH:mm");
		String endDate = getDate(endTime, "HH:mm");

		tvTime.setText(startDate + " - " + endDate);

		TextView tvLocation = (TextView) convertView.findViewById(R.id.location);
		tvLocation.setText(entry.getLocation());

		// set facebook icon if the description has a URL
		ImageView iv1 = (ImageView) convertView.findViewById(R.id.imageView1);
		iv1.setVisibility(View.INVISIBLE);

		String description = entry.getDescription();

		LinearLayout linearLayout = (LinearLayout) convertView.findViewById(R.id.linearLayout2);
		LinearLayout linearLayout1 = (LinearLayout) convertView.findViewById(R.id.linearLayout1);

		// setze die Hintergrundfarbe

		if (startTime < System.currentTimeMillis()) {
			// linearLayout.setBackgroundColor(Color.LTGRAY);
			linearLayout1.setBackgroundColor(Color.LTGRAY);
		} else if (entry.getColor()) {
			// linearLayout.setBackgroundColor(Constants.CALENDAR_COLORS[0]);
			// linearLayout.setBackground(context.getResources().getDrawable(R.drawable.farbverlauf));
			linearLayout1.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.farbverlauf));
		} else {
			linearLayout1.setBackgroundColor(entry.getBackgroundColor());

		}

		if (FbEventParser.parseFbEvent(description) != null) {
			String eventUrl = FbEventParser.parseFbEvent(description);
			iv1.setVisibility(View.VISIBLE);
			iv1.setTag(eventUrl);
			iv1.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Log.d("FaceBook", "Facebook App started");
					startActivity2(v);
				}
			});
		}

		return convertView;
	}

	private void startActivity2(View v) {
		Context context = v.getContext();
		String path = (((ImageView) v).getTag()).toString();

		IntentHelper.openFacebook(context, path, IntentHelper.Facebook.EVENT);

		openFacebookWithPath(context, path);

	}

	private void showDialog(CalendarEvent entry) {
		// Create and show your dialog
		// Depending on the Dialogs button clicks delete it or do nothing
	}

	/**
	 * opens the Facebook App or the WebBrowser when no FB App is available thx
	 * to Mayank Saini
	 * http://stackoverflow.com/questions/10299777/open-a-facebook
	 * -page-from-android-app
	 * 
	 * @param Context
	 *            , current Context
	 * @param String
	 *            path, musst be like "events/43219384371892" or
	 *            "pages/4237894923"
	 */
	private void openFacebookWithPath(Context context, String path) {
		final String TAG = "openFacebookWithPath() ";

		final String urlFb = "fb://event/" + path;
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(urlFb));

		// If Facebook application is installed, use that else launch a browser
		final PackageManager packageManager = context.getPackageManager();

		List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		if (list.size() == 0) {
			final String urlBrowser = "https://www.facebook.com/events/" + path;
			Log.i(TAG, " urlBrowser " + urlBrowser);
			intent.setData(Uri.parse(urlBrowser));
		}

		context.startActivity(intent);
	}

	/**
	 * Return date in specified format.
	 * 
	 * @param milliSeconds
	 *            Date in milliseconds
	 * @param dateFormat
	 *            Date format
	 * @return String representing date in specified format
	 */
	public static String getDate(long milliSeconds, String dateFormat) {
		// Create a DateFormatter object for displaying date in specified
		// format.
		DateFormat formatter = new SimpleDateFormat(dateFormat);

		// Create a calendar object that will convert the date and time value in
		// milliseconds to date.
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(milliSeconds);
		return formatter.format(calendar.getTime());
	}

}
