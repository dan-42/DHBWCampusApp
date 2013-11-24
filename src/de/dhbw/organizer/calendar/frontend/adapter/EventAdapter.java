package de.dhbw.organizer.calendar.frontend.adapter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import de.dhbw.organizer.R;
import de.dhbw.organizer.calendar.frontend.activity.Vorlesungsplan;
import de.dhbw.organizer.calendar.frontend.parser.FbEventParser;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import de.dhbw.organizer.calendar.frontend.parser.FbEventParser;

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
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.calendar_activity_listview,
					null);
		}
		TextView tvTitle = (TextView) convertView.findViewById(R.id.name);
		tvTitle.setText(entry.getName());

		TextView tvTime = (TextView) convertView.findViewById(R.id.time);
		long startTime = entry.getStartTime();
		long endTime = entry.getEndTime();
		String startDate = getDate(startTime, "EEEE dd.MM.yy HH:mm");
		String endDate = getDate(endTime, "dd.MM.yy HH:mm");

		tvTime.setText(startDate + " - " + endDate);

		TextView tvLocation = (TextView) convertView
				.findViewById(R.id.location);
		tvLocation.setText(entry.getLocation());

		// set facebook icon if the description has a URL
		ImageView iv1 = (ImageView) convertView.findViewById(R.id.imageView1);
		iv1.setVisibility(View.INVISIBLE);

		String description = entry.getDescription();
		

		if (FbEventParser.parseFbEvent(description) != null) {
			String eventUrl = FbEventParser.parseFbEvent(description);
			iv1.setVisibility(View.VISIBLE);
			iv1.setTag(eventUrl);
			iv1.setOnClickListener(new OnClickListener() {
			
				@Override
				public void onClick(View v) {
					Log.d("FaceBook","Facebook App started");
					startActivity2(v);
				}
			});
		}

		return convertView;
	}

	private void startActivity2(View v){
		v.getContext().startActivity(getOpenFacebookIntent(v.getContext(),(((ImageView) v).getTag()).toString()));
		
	}
	
	private void showDialog(CalendarEvent entry) {
		// Create and show your dialog
		// Depending on the Dialogs button clicks delete it or do nothing
	}
	
	public static Intent getOpenFacebookIntent(Context context, String eventUrl) {

		   try {
		    context.getPackageManager().getPackageInfo("com.facebook.katana", 0);
		    //return new Intent(Intent.ACTION_VIEW, Uri.parse("fb://profile/<id_here>"));
		    return new Intent(Intent.ACTION_VIEW, Uri.parse(eventUrl));
		   } catch (Exception e) {
		    return new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/<user_name_here>"));
		   }
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
