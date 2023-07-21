/*
 * Copyright 2006-2018. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software is subject to U.S. export control laws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */

package jpl.gds.db.app.util;

import org.springframework.context.ApplicationContext;

/**
 * A factory used to create an appropriate OutputFormatter object based on the OutputFormatType.
 * These OutputFormatters are used by GetEverythingApp.
 * THIS IS NOW ONLY USED FOR THE SORTED EXCEL FILE OUTPUT.
 *
 */
public class OutputFormatterFactory {
	
	/**
	 * Returns the appropriate OutputFormatter object for the specified format.
	 * 
	 * @param format the requested OutputFormatType
	 * @return a new OutputFormatter object
	 */
	public static OutputFormatter getFormatter(final ApplicationContext appContext, OutputFormatType format) {
		
		switch (format) {
		//This is now ONLY used for Chill_get_everything excel sorted file.
		/*case TEXT_SESSION_REPORT:			
			return new TextSROutputFormatter(); 	// Session report text output format (ie the old format used by session report)
			
		case CSV_SESSION_REPORT:		
			return new CsvSROutputFormatter();		// Session report csv output format
			
		case TEXT:
			return new TextOutputFormatter();
			
		case CSV:
			return new CsvOutputFormatter();
*/
		case EXCEL:
			return new ExcelOutputFormatter(appContext);
			
		default:
			throw new IllegalArgumentException("Outout Format type " + format + " is not recognized");
		}	
	}

}
