package de.dhbw.organizer.calendar.frontend.adapter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import de.dhbw.organizer.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

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
            convertView = inflater.inflate(R.layout.calendar_activity_listview, null);
        }
        TextView tvTitle = (TextView) convertView.findViewById(R.id.name);
        tvTitle.setText(entry.getName());

        TextView tvTime = (TextView) convertView.findViewById(R.id.time);
        long startTime = entry.getStartTime();
        long endTime = entry.getEndTime();
        String startDate = getDate(startTime, "EEEE dd.MM.yy HH:mm");
        String endDate = getDate(endTime, "dd.MM.yy HH:mm");
        
        tvTime.setText(startDate + " - " + endDate);

        TextView tvLocation = (TextView) convertView.findViewById(R.id.location);
        tvLocation.setText(entry.getLocation());

        // Set the onClick Listener on this button
        
        //Button btnRemove = (Button) convertView.findViewById(R.id.btnRemove);
        //btnRemove.setFocusableInTouchMode(false);
        //btnRemove.setFocusable(false);
        
        //btnRemove.setOnClickListener(this);
        // Set the entry, so that you can capture which item was clicked and
        // then remove it
        // As an alternative, you can use the id/position of the item to capture
        // the item
        // that was clicked.
        
        //btnRemove.setTag(entry);

        // btnRemove.setId(position);
        

        return convertView;
    }


    private void showDialog(CalendarEvent entry) {
        // Create and show your dialog
        // Depending on the Dialogs button clicks delete it or do nothing
    }
    
	/**
	 * Return date in specified format.
	 * @param milliSeconds Date in milliseconds
	 * @param dateFormat Date format 
	 * @return String representing date in specified format
	 */
	public static String getDate(long milliSeconds, String dateFormat)
	{
	    // Create a DateFormatter object for displaying date in specified format.
	    DateFormat formatter = new SimpleDateFormat(dateFormat);

	    // Create a calendar object that will convert the date and time value in milliseconds to date. 
	     Calendar calendar = Calendar.getInstance();
	     calendar.setTimeInMillis(milliSeconds);
	     return formatter.format(calendar.getTime());
	}

}
