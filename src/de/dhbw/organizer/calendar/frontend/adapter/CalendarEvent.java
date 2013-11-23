package de.dhbw.organizer.calendar.frontend.adapter;

public class CalendarEvent {

	private String name;
	private long startTime; // Time in Millis
	private long endTime; // Time in Millis
	private String location;

	// Constructor for the Event list Class
	public CalendarEvent(String name, long startTime, long endTime,
			String location) {
		super();
		this.name = name;
		this.startTime = startTime;
		this.endTime = endTime;
		this.location = location;
	}

	// Getter and setter methods for all the fields.
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String mail) {
		this.location = mail;
	}
}
