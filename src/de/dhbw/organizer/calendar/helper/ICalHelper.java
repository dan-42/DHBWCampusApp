/**
 * 
 */
package de.dhbw.organizer.calendar.helper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import mf.javax.xml.transform.Source;
import mf.javax.xml.transform.stream.StreamSource;
import mf.javax.xml.validation.Schema;
import mf.javax.xml.validation.SchemaFactory;
import mf.javax.xml.validation.Validator;
import mf.org.apache.xerces.jaxp.validation.XMLSchemaFactory;

import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Base64;
import android.util.Log;
import biweekly.component.VEvent;
import biweekly.property.Classification;
import biweekly.property.DateEnd;
import biweekly.property.DateStart;
import biweekly.property.ExceptionDates;
import biweekly.property.Priority;
import biweekly.property.Status;
import biweekly.property.Transparency;
import biweekly.util.Duration;
import biweekly.util.Recurrence;
import biweekly.util.Recurrence.DayOfWeek;

import com.google.ical.compat.javautil.DateIterator;
import com.google.ical.compat.javautil.DateIteratorFactory;

import de.dhbw.organizer.calendar.backend.objects.RecurringVEvent;
import de.dhbw.organizer.calendar.backend.objects.SpinnerItem;

/**
 * @author friedrda
 * 
 */
public class ICalHelper {

	private static final String TAG = "ICalHelper";

	/**
	 * TIME OFFSET from to day, this is used, when a Recurring event has no
	 * COUNT or UNTIL so we don't try to add ad infinti amount of events
	 */
	private static final int EVENT_MAX_UNTIL_OFFSET_IN_YEAR = 5;

	private static final String XML_SCHEMA_VERSION = "1.0";

	/**
	 * validates a xml file with and xmlSchema Thanks to: James Oravec
	 * http://stackoverflow.com/questions/801144/android-schema-validation
	 * 
	 * @param xmlFilePath
	 * @param xmlSchemaFilePath
	 * @return true if valid, false otherwise
	 */
	public static boolean validate(InputStream xmlInputStream, InputStream xmlSchemaInputStream) {

		// Try the validation, we assume that if there are any issues with the
		// validation process that the input is invalid.
		try {
			SchemaFactory factory = new XMLSchemaFactory();
			Source schemaFile = new StreamSource(xmlSchemaInputStream);
			Source xmlSource = new StreamSource(xmlInputStream);
			Schema schema = factory.newSchema(schemaFile);
			Validator validator = schema.newValidator();
			validator.validate(xmlSource);
		} catch (SAXException e) {
			return false;
		} catch (IOException e) {
			return false;
		} catch (Exception e) {
			// Catches everything beyond: SAXException, and IOException.
			e.printStackTrace();
			return false;
		} catch (Error e) {
			// Needed this for debugging when I was having issues with my 1st
			// set of code.
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * seperates an event list into single events, but first
	 * 
	 * @param eventList
	 * @return
	 */
	public static ArrayList<VEvent> seperateAllEvents(ArrayList<VEvent> eventList, boolean ingnorePrivateEvents) {

		ArrayList<RecurringVEvent> recurringEvents = new ArrayList<RecurringVEvent>();
		ArrayList<VEvent> tempEventList = new ArrayList<VEvent>();

		ArrayList<VEvent> atomarEventList = new ArrayList<VEvent>();

		/**
		 * split events into recurring events and regular events
		 */
		for (VEvent e : eventList) {

			if (ingnorePrivateEvents) {
				Classification classification = e.getClassification();
				if (classification.isPrivate()) {
					continue;
				}
			}

			// no RRULE or nor RECURRENCE-ID
			// we keep those seperate, to avoid unneeded processing
			// regular events can be inserted without any problems
			if (e.getRecurrenceRule() == null && e.getRecurrenceId() == null) {
				atomarEventList.add(e);
			}

			else {

				if (e.getRecurrenceRule() != null) {
					// this event is a recurring one,
					// it has an RRULE so we need to seperate these
					recurringEvents.add(new RecurringVEvent(e));

				} else if (e.getRecurrenceId() != null) {
					// this event is an exception of an recurring event
					// e.g.Event is evey monday at 9:00 but this time its at
					// 13:00
					// so we need to find the fitting recurring Event and add id
					// for later processing

					boolean isInserted = false;
					for (RecurringVEvent re : recurringEvents) {
						if (re.e.getUid().getValue().equals(e.getUid().getValue())) {
							re.addException(e);
							isInserted = true;
							break;
						}
					}
					if (isInserted == false) {
						// if the exception was bevor the recurring event, we
						// check later again
						tempEventList.add(e);
					}
				}
			}
		}

		// add last exceptions to recurring event,
		for (VEvent e : tempEventList) {
			boolean isInserted = false;
			for (RecurringVEvent re : recurringEvents) {
				if (re.e.getUid().getValue().equals(e.getUid().getValue())) {
					re.addException(e);
					isInserted = true;
					break;
				}
			}
			if (isInserted == false) {
				Log.e(TAG, "ERRO cannot find fitting Recurring event: evenUID = " + e.getUid().getValue());
			}
		}

		// Seperate recurring events
		// check and replece single event with exception, if it fits
		// add them to teh DB
		Log.d(TAG, "insert recurring Events ");
		for (RecurringVEvent re : recurringEvents) {
			atomarEventList.addAll(seperateEvents(re));

		}

		return atomarEventList;

	}

	/**
	 * calcs hashvalue of the concatinated values of Summary Description
	 * Location DateStart DateEnd
	 * 
	 * @param e
	 * @return sha1 hash as String
	 */
	public static String calcEventHash(VEvent e) {
		StringBuilder sb = new StringBuilder();

		sb.append(e.getSummary().getValue());
		if (e.getDescription() != null)
			sb.append(e.getDescription().getValue());
		if (e.getLocation() != null)
			sb.append(e.getLocation().getValue());
		sb.append(e.getDateStart().getValue().toString());
		sb.append(e.getDateEnd().getValue().toString());

		byte[] eventByteString = sb.toString().getBytes(Charset.defaultCharset());

		MessageDigest md = null;

		try {
			md = MessageDigest.getInstance("SHA1");

			byte[] sha1 = Base64.encode(md.digest(eventByteString), Base64.DEFAULT);

			return new String(sha1);
		} catch (NoSuchAlgorithmException e1) {

			e1.printStackTrace();
		}

		return null;
	}

	/**
	 * extract the exception dates
	 * 
	 * @param e
	 * @return
	 */
	private static List<Date> extractExceptionDates(VEvent e) {
		List<Date> dateList = new ArrayList<Date>();

		List<ExceptionDates> exceptionDates = e.getExceptionDates();
		if (exceptionDates != null) {
			for (ExceptionDates ed : exceptionDates) {
				dateList.addAll(ed.getValues());
			}
		}
		return dateList;
	}

	/**
	 * @param VEvent
	 *            event
	 * @param TimeZone
	 *            tz
	 * @return
	 */
	private static String buildExdate(VEvent event, TimeZone tz) {

		List<ExceptionDates> exceptionDates = event.getExceptionDates();
		if (exceptionDates != null) {

			StringBuilder sb = new StringBuilder();
			List<Date> dates = extractExceptionDates(event);

			if (tz != null) {
				sb.append("TZID=").append(tz.getID()).append(':');
			}

			for (Iterator<Date> iterator = dates.iterator(); iterator.hasNext();) {
				Date date = (Date) iterator.next();

				sb.append(parseIcalDateToString(date, tz));

				if (iterator.hasNext()) {
					sb.append(",");
				}
			}
			return sb.toString();
		}

		return "";
	}

	/**
	 * RRULE:FREQ=WEEKLY;UNTIL=20131106T080000Z;INTERVAL=1;BYDAY=TU,WE;WKST=MO
	 * 
	 * @param e
	 * @return
	 */
	private static String buildRrule(VEvent e) {

		if (e.getRecurrenceRule() != null) {
			Recurrence r = e.getRecurrenceRule().getValue();
			StringBuilder sb = new StringBuilder();

			sb.append("FREQ=").append(r.getFrequency().toString());
			if (r.getUntil() != null)
				sb.append(";UNTIL=").append(parseIcalDateToString(r.getUntil(), null));
			else if (r.getCount() != null)
				sb.append(";COUNT=").append(r.getCount());
			else {
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(System.currentTimeMillis());

				int year = cal.get(Calendar.YEAR);
				int month = cal.get(Calendar.MONTH);
				int day = cal.get(Calendar.DAY_OF_MONTH);

				cal.set(year + EVENT_MAX_UNTIL_OFFSET_IN_YEAR, month, day);
				Date untilDate = cal.getTime();

				String until = parseIcalDateToString(untilDate, null);
				// Log.i(TAG, "buildRrule() no UNTIL or COUNT, so set UNTIL to "
				// + untilDate.toString() + "   ICAL:" + until);
				sb.append(";UNTIL=").append(until);
			}

			if (r.getWorkweekStarts() != null)
				sb.append(";WKST=").append(r.getWorkweekStarts().getAbbr());

			if (r.getInterval() != null)
				sb.append(";INTERVAL=").append(r.getInterval());
			else
				sb.append(";INTERVAL=").append(1);

			// BYSECOND
			if (!r.getBySecond().isEmpty()) {
				sb.append(";BYSECOND=");
				int idx = 0;
				for (int s : r.getBySecond()) {
					sb.append(s);
					idx++;
					if (idx < r.getByDay().size()) {
						sb.append(",");
					}
				}
			}

			// BYMINUTE
			if (!r.getByMinute().isEmpty()) {
				sb.append(";BYMINUTE=");
				int idx = 0;
				for (int s : r.getBySecond()) {
					sb.append(s);
					idx++;
					if (idx < r.getByDay().size()) {
						sb.append(",");
					}
				}
			}

			// BYHOURE
			if (!r.getByHour().isEmpty()) {
				sb.append(";BYHOURE=");
				int idx = 0;
				for (int s : r.getBySecond()) {
					sb.append(s);
					idx++;
					if (idx < r.getByDay().size()) {
						sb.append(",");
					}
				}
			}

			// BYDAY
			if (!r.getByDay().isEmpty()) {
				sb.append(";BYDAY=");
				int idx = 0;

				for (DayOfWeek dow : r.getByDay()) {
					sb.append(dow.getAbbr());
					idx++;
					if (idx < r.getByDay().size()) {
						sb.append(",");
					}
				}
			}

			// BYMONTHDAY
			if (!r.getByMonthDay().isEmpty()) {
				sb.append(";BYMONTHDAY=");
				int idx = 0;

				for (int i : r.getByMonthDay()) {
					sb.append(i);
					idx++;
					if (idx < r.getByDay().size()) {
						sb.append(",");
					}
				}
			}

			// BYYEARDAY
			if (!r.getByYearDay().isEmpty()) {
				sb.append(";BYYEARDAY=");
				int idx = 0;

				for (int i : r.getByYearDay()) {
					sb.append(i);
					idx++;
					if (idx < r.getByDay().size()) {
						sb.append(",");
					}
				}
			}

			// BYWEEKNO
			if (!r.getByWeekNo().isEmpty()) {
				sb.append(";BYWEEKNO=");
				int idx = 0;

				for (int i : r.getByWeekNo()) {
					sb.append(i);
					idx++;
					if (idx < r.getByDay().size()) {
						sb.append(",");
					}
				}
			}

			// BYMONTH
			if (!r.getByMonth().isEmpty()) {
				sb.append(";BYMONTH=");
				int idx = 0;

				for (int i : r.getByMonth()) {
					sb.append(i);
					idx++;
					if (idx < r.getByDay().size()) {
						sb.append(",");
					}
				}
			}

			// BYSETPOS
			if (!r.getBySetPos().isEmpty()) {
				sb.append(";BYSETPOS=");
				int idx = 0;

				for (int i : r.getBySetPos()) {
					sb.append(i);
					idx++;
					if (idx < r.getByDay().size()) {
						sb.append(",");
					}
				}
			}

			return sb.toString();
		} else {
			return "";
		}

	}

	/**
	 * seperates an Recurring event into a list of single events, and also
	 * includes the exceptions in the RecuringVEvent
	 * 
	 * @param rve
	 * @return
	 */
	private static ArrayList<VEvent> seperateEvents(RecurringVEvent rve) {

		ArrayList<VEvent> cleanEventList = new ArrayList<VEvent>();

		if (rve != null && rve.e != null && rve.e.getRecurrenceRule() != null) {

			// we have two lists, the list of atomar events, but with no
			// exceptions
			ArrayList<VEvent> atomarEvents = splitRecurringEvent(rve.e);
			// and a list of Events which are the exception
			ArrayList<VEvent> exceptions = rve.getExceptions();

			/**
			 * we need to find the event, for which the exception is an
			 * exception as an RECURRING-ID which represenst the DateTime or
			 * only Date on which the event should have been. the MS-Exchange
			 * exports only a Date not a Date Time, so we check only if Year,
			 * Month and Day fist
			 */
			Calendar cal = Calendar.getInstance();
			for (VEvent atom : atomarEvents) {

				cal.setTime(atom.getDateStart().getValue());
				int atomYear = cal.get(Calendar.YEAR);
				int atomMonth = cal.get(Calendar.MONTH);
				int atomDay = cal.get(Calendar.DAY_OF_MONTH);

				boolean inserted = false;

				for (VEvent ex : exceptions) {
					if (ex.getRecurrenceId() == null) {
						Log.e(TAG, "MISSING RecurrenceID HELP! in seperateEvents() " + ex.getSummary().getValue() + " on Date "
								+ ex.getDateStart().getValue().toString());
						break;
					}
					Date recId = ex.getRecurrenceId().getValue();

					cal.setTime(recId);
					int exYear = cal.get(Calendar.YEAR);
					int exMonth = cal.get(Calendar.MONTH);
					int exDay = cal.get(Calendar.DAY_OF_MONTH);
					// int exDstOff = cal.get(Calendar.DST_OFFSET);

					// this here should always be the case!
					if (ex.getUid().getValue().equals(atom.getUid().getValue())) {

						// found atomar Event of reccuring, for which this
						// exception fits
						if (atomYear == exYear && atomMonth == exMonth && atomDay == exDay) {
							// add the exception to the list, not the "normal"
							// recurring event
							cleanEventList.add(ex);
							inserted = true;
							break;
						}

					} else {
						Log.e(TAG, "Missmatching UID in seperateEvents o0 WTF?");
					}
				}// inner for

				// if there is no exception for tis event, we use this event
				if (!inserted) {
					cleanEventList.add(atom);
				}
			}

		} else {
			Log.e(TAG, "ERROR rve != null && rve.e != null && rve.e.getRecurrenceRule() != null");
			// add single Event?
		}
		/*
		 * for (VEvent e : cleanEventList) { Log.d(TAG, "RECURRING EVENT : " +
		 * e.getSummary().getValue() + "  " +
		 * e.getDateStart().getValue().toString()); }
		 */
		return cleanEventList;
	}

	/**
	 * splits an recurring Event with an RRULE and EXDATE into a List of
	 * SingleEvents
	 * 
	 * @author friedrda
	 * @param recurringEvent
	 * @return List of events
	 */
	private static ArrayList<VEvent> splitRecurringEvent(VEvent recurringEvent) {
		TimeZone tz = TimeZone.getDefault();
		TimeZone.setDefault(tz);

		ArrayList<VEvent> atomarEvents = new ArrayList<VEvent>();
		boolean hasRec = false;

		// avoid nullPointer by checking if needed attributes are set
		if (recurringEvent != null && recurringEvent.getRecurrenceRule() != null && recurringEvent.getRecurrenceId() == null) {

			// collect data from event, to but into new atomar events
			String title = recurringEvent.getSummary().getValue();
			String description = "";
			if (recurringEvent.getDescription() != null) {
				description = recurringEvent.getDescription().getValue();
			}
			String location = "";
			if (recurringEvent.getLocation() != null) {
				location = recurringEvent.getLocation().getValue();
			}

			String uid = recurringEvent.getUid().getValue();

			Status status = recurringEvent.getStatus();

			Classification eventCalss = recurringEvent.getClassification();

			Priority prio = recurringEvent.getPriority();

			int sequence = 0;
			if (recurringEvent.getSequence() != null) {
				sequence = recurringEvent.getSequence().getValue();
			}

			Transparency transp = recurringEvent.getTransparency();

			List<String> categories = new ArrayList<String>();

			if (recurringEvent.getCategories() != null && !recurringEvent.getCategories().isEmpty()) {
				categories = recurringEvent.getCategories().get(0).getValues();
			}

			// handle DATE stuff
			Date startDate = recurringEvent.getDateStart().getValue();
			// String timeZoneIdFromStartDate =
			// recurringEvent.getDateStart().getTimezoneId();

			// Log.d(TAG, "--------------------------------------");
			// Log.d(TAG, "RECURRING EVENT");
			// Log.d(TAG, "\tSTARTDATE: toString()\t" + startDate.toString());
			// Log.d(TAG, "\tSTARTDATE: timeZone \t" + timeZoneIdFromStartDate);
			// Log.d(TAG, "\tSTARTDATE: timeZoneConv \t" +
			// TimeZone.getTimeZone("timeZoneIdFromStartDate").getID());

			SimpleDateFormat sdf = new SimpleDateFormat("EEE dd.MM.yyyy G   HH:mm:ss z", Locale.getDefault());
			sdf.setTimeZone(TimeZone.getDefault());

			Date endDate = recurringEvent.getDateEnd().getValue();

			Duration duration = Duration.diff(startDate, endDate);

			// get RRULE
			StringBuilder rdata = new StringBuilder();
			rdata.append("RRULE:").append(buildRrule(recurringEvent));

			// get EXDATE
			if (recurringEvent.getExceptionDates() != null && recurringEvent.getExceptionDates().size() > 0) {
				hasRec = true;
				String exdate = buildExdate(recurringEvent, null);
				rdata.append("\nEXDATE:").append(exdate);

			}

			// Log.d(TAG, "RDATA: ");
			// Log.d(TAG, "\t --------------");
			// Log.d(TAG, rdata.toString());
			// Log.d(TAG, "\t --------------");

			try {
				Calendar cal = Calendar.getInstance();
				cal.setTime(startDate);
				int startDstOffset = cal.get(Calendar.DST_OFFSET);

				DateIterator dif = DateIteratorFactory.createDateIterator(rdata.toString(), startDate, null, true);

				/*
				 * if (hasRec) { Log.d(TAG, "timeZoneId " +
				 * timeZoneIdFromStartDate); Log.d(TAG, "timeZoneId in tz" +
				 * tz.getDisplayName()); Log.d(TAG, "RDATA : " + rdata);
				 * Log.d(TAG, "RDATA TZ: " + tz.toString()); Log.d(TAG,
				 * "RDATA startDate: " + startDate.toString()); }
				 */

				// + " Recurrs on: ");

				List<Date> exceptionDates = extractExceptionDates(recurringEvent);

				while (dif.hasNext()) {
					Date d = dif.next();

					cal.setTime(d);
					cal.setTimeZone(tz);
					int dstOffset = cal.get(Calendar.DST_OFFSET);
					// create VEvent with this date
					VEvent e = new VEvent();
					e.setUid(uid);
					e.setSummary(title);
					e.setDescription(description);
					e.setLocation(location);

					e.setStatus(status);
					e.setClassification(eventCalss);
					e.setPriority(prio);
					e.setDateTimeStamp(new Date());
					e.setSequence(sequence);
					e.setTransparency(transp);
					e.addCategories(categories);

					long offset = startDstOffset - dstOffset;

					/**
					 * the google rfc lib propably can't handle EXDATE when the
					 * start date is in summer time but the EXDATE is in winter
					 * time so we check manualy, pretty ugly i know :(
					 */
					boolean skip = false;
					if (startDstOffset != dstOffset) {
						// Log.i(TAG, "startDstOffset = " + startDstOffset +
						// "\t dstOffset = " + dstOffset);
						// Log.d(TAG, "dif = \t\t" + d.getTime());

						for (Date date : exceptionDates) {
							long check = d.getTime() - date.getTime() + offset;
							if (check == 0) {
								skip = true;
							}
						}

					}
					if (skip) {
						continue;
					}

					// offset = 0;

					// set StartDate
					DateStart ds = new DateStart(new Date(d.getTime() + offset));
					ds.setTimezoneId(tz.getID());
					e.setDateStart(ds);

					// calc and set EndDate
					Date end = new Date(d.getTime() + duration.toMillis() + offset);
					DateEnd de = new DateEnd(end);
					de.setTimezoneId(tz.getID());
					e.setDateEnd(de);

					// Date end = new Date(d.getTime() + duration.toMillis());
					// Log.d(TAG, "Date FROM " + d.toString() + "  to: " +
					// end.toString());

					if (hasRec == true) {
						sdf.setTimeZone(TimeZone.getTimeZone(e.getDateStart().getTimezoneId()));
						// Log.d(TAG, "has EXDATE: " + title + "  -- " +
						// sdf.format(e.getDateStart().getValue()));
					}

					atomarEvents.add(e);

				}
			} catch (ParseException e1) {
				e1.printStackTrace();
			}

		} else {
			Log.e(TAG, "NULL ERROR");
		}
		return atomarEvents;

	}

	/**
	 * Pase the Ical Date to a String
	 * 
	 * @param date
	 *            the date
	 * @param tz
	 *            timezone
	 * @return the parsed Date as a String
	 */
	private static String parseIcalDateToString(Date date, TimeZone tz) {

		StringBuilder sb = new StringBuilder();
		SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm", Locale.getDefault());
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

		if (tz != null) {
			timeFormat.setTimeZone(tz);
			dateFormat.setTimeZone(tz);
		} else {
			timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		}

		sb.append(dateFormat.format(date));
		sb.append("T");
		sb.append(timeFormat.format(date));
		sb.append("00");

		if (tz == null)
			sb.append('Z');

		return sb.toString();
	}

	/**
	 * 
	 * @param is
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	public static ArrayList<SpinnerItem> parseXmlFromInputStream(InputStream is) throws IOException, XmlPullParserException {

		CalendarXmlParser cxp = new CalendarXmlParser(is);
		return cxp.readCalendars();

	}

	/**
	 * 
	 * @author friedrda
	 * 
	 */

	static class CalendarXmlParser {

		private static final String XML_ATTR_VERSION = "Version";
		private static final String XML_ATTR_LAST_UPDATE = "LastUpdate";

		private static final String XML_TAG_CALENDARS = "Calendars";
		private static final String XML_TAG_DISPLAY_NAME = "DisplayName";
		private static final String XML_TAG_ICAL_URL = "iCalUrl";
		private static final String XML_TAG_CALENDAR = "Calendar";

		private String mNameSpace = "";

		private XmlPullParserFactory mParserfactory = null;
		private XmlPullParser mParser = null;

		/**
		 * 
		 * @param is
		 *            the input stream
		 * @throws IOException
		 */
		public CalendarXmlParser(InputStream is) throws IOException {
			try {
				mParserfactory = XmlPullParserFactory.newInstance();
				mParser = mParserfactory.newPullParser();
				mParser.setInput(new InputStreamReader(is));
				mParser.next();
			} catch (XmlPullParserException e) {
				Log.e(TAG, "CalendarXmlParser() ERROR " + e.getMessage());
			}

			this.mNameSpace = mParser.getNamespace();

			String xmlVersion = mParser.getAttributeValue(mNameSpace, XML_ATTR_VERSION);
			String xmlLastUpdate = mParser.getAttributeValue(mNameSpace, XML_ATTR_LAST_UPDATE);

			Log.i(TAG, "XMLversion = " + xmlVersion + "  xmlLastUpdate = " + xmlLastUpdate + " Namsespace = " + mNameSpace);

			if (!xmlVersion.equals(XML_SCHEMA_VERSION)) {
				Log.e(TAG, "XML VERSION don't match, error!");
				throw new IOException();
			}

		}

		/**
		 * 
		 * @return
		 * @throws XmlPullParserException
		 * @throws IOException
		 */

		public ArrayList<SpinnerItem> readCalendars() throws XmlPullParserException, IOException {
			ArrayList<SpinnerItem> cals = new ArrayList<SpinnerItem>();

			mParser.require(XmlPullParser.START_TAG, mNameSpace, XML_TAG_CALENDARS);
			while (mParser.next() != XmlPullParser.END_TAG) {
				if (mParser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String name = mParser.getName();
				// Starts by looking for the entry tag
				if (name.equals(XML_TAG_CALENDAR)) {
					cals.add(readCalendar(mParser));
				} else {
					skip(mParser);
				}
			}

			return cals;

		}

		/**
		 * 
		 * @param parser
		 * @return
		 * @throws XmlPullParserException
		 * @throws IOException
		 */
		private SpinnerItem readCalendar(XmlPullParser parser) throws XmlPullParserException, IOException {

			parser.require(XmlPullParser.START_TAG, mNameSpace, XML_TAG_CALENDAR);
			String displayName = null;
			String iCalUrl = null;

			while (parser.next() != XmlPullParser.END_TAG) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String name = parser.getName();
				if (name.equals(XML_TAG_DISPLAY_NAME)) {
					displayName = readDisplayName(parser);
				} else if (name.equals(XML_TAG_ICAL_URL)) {
					iCalUrl = readICalUrl(parser);

				} else {
					skip(parser);
				}
			}
			return new SpinnerItem(displayName, iCalUrl);
		}

		/**
		 * 
		 * @param parser
		 * @throws XmlPullParserException
		 * @throws IOException
		 */
		private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				throw new IllegalStateException();
			}
			int depth = 1;
			while (depth != 0) {
				switch (parser.next()) {
				case XmlPullParser.END_TAG:
					depth--;
					break;
				case XmlPullParser.START_TAG:
					depth++;
					break;
				}
			}
		}

		/**
		 * 
		 * @param parser
		 * @return
		 * @throws XmlPullParserException
		 * @throws IOException
		 */
		private String readICalUrl(XmlPullParser parser) throws XmlPullParserException, IOException {

			parser.require(XmlPullParser.START_TAG, mNameSpace, XML_TAG_ICAL_URL);
			String iCalUrl = readText(parser);
			parser.require(XmlPullParser.END_TAG, mNameSpace, XML_TAG_ICAL_URL);
			return iCalUrl;
		}

		/**
		 * 
		 * @param parser
		 * @return
		 * @throws XmlPullParserException
		 * @throws IOException
		 */
		private String readDisplayName(XmlPullParser parser) throws XmlPullParserException, IOException {

			parser.require(XmlPullParser.START_TAG, mNameSpace, XML_TAG_DISPLAY_NAME);
			String displayName = readText(parser);
			parser.require(XmlPullParser.END_TAG, mNameSpace, XML_TAG_DISPLAY_NAME);
			return displayName;
		}

		/**
		 * For the tags title and summary, extracts their text values.
		 * @param parser
		 * @return
		 * @throws IOException
		 * @throws XmlPullParserException
		 */
		private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
			String result = "";
			if (parser.next() == XmlPullParser.TEXT) {
				result = parser.getText();
				parser.nextTag();
			}
			return result;
		}
	}

}
