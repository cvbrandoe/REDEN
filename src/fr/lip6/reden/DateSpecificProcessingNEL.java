package fr.lip6.reden;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class implements methods specific to date processing.
 * 
 * @author @author Brando & Frontini
 */
public class DateSpecificProcessingNEL {


	public static Double compareDate(Integer dateB1, Integer dateD1,
			Integer dateB2, Integer dateD2) {
		Double dateB1I = new Double(dateB1);
		Double dateB2I = new Double(dateB2);
		Double dateD1I = new Double(dateD1);
		Double dateD2I = new Double(dateD2);
		Double refBirth = Math.max(dateB1I, dateB2I);
		Double refDeath = Math.min(dateD1I, dateD2I);
		Double subs = (refDeath - refBirth);
		if (subs < 0.0) {
			subs = 0.0;
		}
		return subs;
	}

	public static boolean isValidDate(String input, SimpleDateFormat formatter) {
		try {
			formatter.parse(input);
			return true;
		} catch (ParseException e) {
			return false;
		}
	}

	public static Integer processDate(String dateS) {
		try {
			if (!dateS.contains(".")) {

				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
				if (isValidDate(dateS, formatter)) {
					Date date = formatter.parse(dateS);
					SimpleDateFormat df = new SimpleDateFormat("yyyy");
					String year = df.format(date);
					return Integer.parseInt(year);
				}
				SimpleDateFormat formatter2 = new SimpleDateFormat("yyyy");
				if (isValidDate(dateS, formatter2)) {
					Date date = formatter2.parse(dateS);
					SimpleDateFormat df = new SimpleDateFormat("yyyy");
					String year = df.format(date);
					return Integer.parseInt(year);
				}
			}

		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
}
