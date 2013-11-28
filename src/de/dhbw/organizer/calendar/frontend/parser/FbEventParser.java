package de.dhbw.organizer.calendar.frontend.parser;

/**
 * 
 * @author Strittpa
 *
 */

public class FbEventParser {

	
	/**
	 * 
	 * @param id
	 * @param input
	 */
	/*private static void testIt(int id, String input)
	{
		// Ausgabe des Testfalls:
		System.out.println(id + ": " + input);
		
		// Test
		String result = parseFbEvent(input);
		
		if(result != null)
		{
			// Event-ID erfolgreich erkannt
			System.out.println("=> " + result);
		}
		
		System.out.println();
	}*/
	
	/**
	 * Parsing Logic for parsing a String with a FB Url to only the FB Url
	 * @param input The input which should be parsed
	 * @return The facebook URL
	 */
	public static String parseFbEvent(String input)
	{
		try
		{
			final String detection = "facebook.com/events/"; 
			int positionBegin = input.indexOf(detection);
			
			if (positionBegin < 0)
			{
				// input enth�lt kein Link zu einem Facebook-Event
				return null;							
			}
			else
			{
				// input enth�lt Link zu einem Facebook-Event
				String cutted = input.substring(positionBegin + detection.length());
				
				
				int positionEnd = cutted.indexOf('/');				
				if(positionEnd < 0)
				{
					// Verbleibender String beeinhaltet kein '/' => Ende der Event-ID nicht erkannt
					
					
					positionEnd = cutted.indexOf(' ');					
					if(positionEnd < 0)
					{
						// Verbleibender String beeinhaltet kein ' ' => Ende der Event-ID nicht erkannt
						
						
						if(cutted.matches(".\\d.")){
							 // Verbleibender String ist eine Zahl => wom�glich ist der gesamte String die Event-ID
							
							positionEnd = cutted.length();
						}
						else
						{
							// Verbleibender String ist irgendetwas, jedoch nicht die Event-ID, da diese ausschliesslich Ziffern beinhaltet
							return null;
						}
					}
				}
				
				// Ende der Event-ID erkannt
				//return "fb://event/" + cutted.substring(0, positionEnd);
				//return "event/" + cutted.substring(0, positionEnd);
				return cutted.substring(0, positionEnd);
			}
		}
		catch(Exception ex)
		{
			// Sollte ein Fehler beim Parsen entstehen
			return null;
		}
	}
}

