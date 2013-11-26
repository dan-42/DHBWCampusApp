package de.dhbw.organizer.calendar.frontend.adapter;




public class CalendarEvent {

	private String name;
	private long startTime; // Time in Millis
	private long endTime; // Time in Millis
	private String location;
	private String description;
	private boolean color;
	private int backgroundColor;

	public CalendarEvent(String name, long startTime, long endTime,
			String location, String description, boolean color) {
		super();
		this.name = name;
		this.startTime = startTime;
		this.endTime = endTime;
		this.location = location;
		this.description = description;
		this.color = color;
		this.backgroundColor = 0;
	}

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

	public void setLocation(String location) {
		this.location = location;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	public boolean getColor() {
		return color;
	}

	public void setColor(boolean color) {
		this.color = color;
	}

	public void setBackgroundColor(int backgroundColor) {
		this.backgroundColor = backgroundColor;
		
	}
	
	public int getBackgroundColor() {
		return backgroundColor;
		
	}
}
