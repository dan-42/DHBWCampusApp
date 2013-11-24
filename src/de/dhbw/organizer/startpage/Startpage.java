package de.dhbw.organizer.startpage;

import de.dhbw.organizer.R;
import de.dhbw.organizer.calendar.frontend.activity.Vorlesungsplan;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

/**
 * @author riedings
 * 
 */
public class Startpage extends Activity implements android.view.View.OnClickListener {

	Button b1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.startpage_activity);

		b1 = (Button) findViewById(R.id.start_button_calendar);

		b1.setOnClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.startpage_menu, menu);
		return true;
	}

	@Override
	public void onClick(View v) {

		Intent myIntent = new Intent(v.getContext(), Vorlesungsplan.class);
		startActivityForResult(myIntent, 0);

	}
	
	public void startMensaActivity(View v){
		
		Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage("de.dhbw.mensa");		
		startActivity(LaunchIntent);
		
		
	}

}
