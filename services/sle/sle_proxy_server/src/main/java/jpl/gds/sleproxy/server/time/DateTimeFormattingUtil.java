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
package jpl.gds.sleproxy.server.time;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.lsespace.sle.user.util.JavaTimeTag;

/**
 * Utility singleton for handling date/time formatting.
 * 
 */
public enum DateTimeFormattingUtil {

	/**
	 * The singleton object.
	 */
	INSTANCE;

	/**
	 * Formatter to convert date/time to the standard AMPCS format.
	 */
	private final DateTimeFormatter ampcsDateTimeFormatter;

	/**
	 * Default constructor.
	 */
	DateTimeFormattingUtil() {
		ampcsDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-DDD'T'HH:mm:ss[.SSS]");
	}

	/**
	 * Get the AMPCS date/time formatter.
	 * 
	 * @return The AMPCS date/time formatter
	 */
	public DateTimeFormatter getAMPCSDateTimeFormatter() {
		return ampcsDateTimeFormatter;
	}

	/**
	 * Generates a date-time string from a JavaTimeTag object, formatted to
	 * AMPCS's standard date-time format.
	 * 
	 * @param javaTimeTag
	 *            The time tag to generate the date-time string from
	 * @return Date-time string in AMPCS's standard format
	 */
	public String toAMPCSDateTimeString(final JavaTimeTag javaTimeTag) {
		return ZonedDateTime.ofInstant(Instant.ofEpochMilli(javaTimeTag.getMilliseconds()), ZoneOffset.UTC)
				.format(ampcsDateTimeFormatter);
	}

}