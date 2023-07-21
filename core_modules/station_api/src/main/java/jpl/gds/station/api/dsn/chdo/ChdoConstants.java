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
package jpl.gds.station.api.dsn.chdo;

/**
 * This class defines constants used by CHDO processing and the CHDO dictionary.
 * 
 */
public class ChdoConstants
{
	/**
	 * Days to subtract of CHDO day fields to get 1958 EPOCH.
	 */
	public static final long JULIAN_DATE_1958 = 2436204;//.5;
	/**
	 * Days to subtract of CHDO day fields to get 1970 EPOCH.
	 */
	public static final long JULIAN_DATE_1970 = 2440587;//.5;
	/**
	 * Milliseconds in a day.
	 */
	public static final long MILLISECONDS_PER_DAY = 86400000; // = 1000* 60 * 60 * 24 = (msec/sec) * (sec/min) * (min/hour) * (hour/day)
	/**
	 * Byte length of year in CHDO fields.
	 */
	public static final int YEAR_BYTE_LENGTH = 2;
	/**
	 * Byte length of days of year in CHDO fields.
	 */
	public static final int DAY_OF_YEAR_BYTE_LENGTH = 2;
	/**
	 * Byte length of seconds of day in CHDO fields.
	 */
	public static final int SECONDS_OF_DAY_BYTE_LENGTH = 8;
	/**
	 * Byte length of days in CHDO fields.
	 */
	public static final int DAYS_BYTE_LENGTH = 2;
	/**
	 * Byte length of milliseconds in CHDO fields.
	 */
	public static final int MSECS_BYTE_LENGTH = 4;
	/**
	 * Default CHDO character set.
	 */
	public static final String CHARSET = "US-ASCII";
}
