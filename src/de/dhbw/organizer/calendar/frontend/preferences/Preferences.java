package de.dhbw.organizer.calendar.frontend.preferences;

import de.dhbw.organizer.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * 
 * @author riedings
 *
 */
public class Preferences extends PreferenceActivity{
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.vorlesungsplan_preferences);
	}

}
