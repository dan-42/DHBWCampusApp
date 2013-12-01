package de.dhbw.organizer.calendar.frontend.adapter;

/**
 * 
 * @author riedings
 * 
 */
public class CalendarEvent {

	private String name;
	private long startTime; // Time in Millis
	private long endTime; // Time in Millis
	private String location;
	private String description;
	private boolean color;
	private int backgroundColor;

	public CalendarEvent(String name, long startTime, long endTime, String location, String description, boolean color) {
		super();
		this.name = name;
		this.startTime = startTime;
		this.endTime = endTime;
		this.location = location;
		this.description = description;
		this.color = color;
		this.backgroundColor = 0;
	}

	/**
	 * get the Name of the event
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * set the name
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * get the start time
	 * 
	 * @return
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * set the start time
	 * 
	 * @param startTime
	 */
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	/**
	 * get the end time
	 * 
	 * @return
	 */
	public long getEndTime() {
		return endTime;
	}

	/**
	 * set the end time
	 * 
	 * @param endTime
	 */
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	/**
	 * get the location
	 * 
	 * @return
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * set the location
	 * 
	 * @param location
	 */
	public void setLocation(String location) {
		this.location = location;
	}

	/**
	 * get the description
	 * 
	 * @return
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * set the description
	 * 
	 * @param description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * get the color flagg
	 * 
	 * @return
	 */
	public boolean getColor() {
		return color;
	}

	/**
	 * set the color flagg
	 * 
	 * @param color
	 */
	public void setColor(boolean color) {
		this.color = color;
	}

	/**
	 * set the background color
	 * 
	 * @param backgroundColor
	 */
	public void setBackgroundColor(int backgroundColor) {
		this.backgroundColor = backgroundColor;

	}

	/**
	 * get the background color
	 * 
	 * @return
	 */
	public int getBackgroundColor() {
		return backgroundColor;

	}
}
