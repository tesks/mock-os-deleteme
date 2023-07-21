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
package jpl.gds.shared.time;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.shared.types.EnumeratedType;

/**
 * Various ways of formatting time strings
 * 
 *
 */
@SuppressWarnings("MS_MUTABLE_ARRAY")
public class TimeFormatType extends EnumeratedType {
	private static final String GMT_TIME_ZONE = "GMT";
	private static final String MILLISECONDS_ENDING = ".SSS";

	// static integer values for the enumerated values

    /** ISO type integer value */
	public static final int ISO_TYPE = 0;

    /** Day-of-year time type integer value */
	public static final int DAY_OF_YEAR_TYPE = 1;

    /** Year-month-day time type integer value */
	public static final int YEAR_MONTH_DAY_TYPE = 2;

    /** ISO time type integer value */
	public static final int ISO_TIME_TYPE = 3;

	/** static string values for the enumerated values */
	@SuppressWarnings("MS_PKGPROTECT")
	public static final String[] formatTypeStrings = { "ISO", "DOY", "YMD",
			"ISO_TIME" };

    /** Regular expressions */
	@SuppressWarnings("MS_PKGPROTECT")
	public static final String[] regexpStrings = { "yyyy-MM-dd'T'HH:mm:ss",
			"yyyy-DDD'T'HH:mm:ss", "yyyy/MM/dd'T'HH:mm:ss", "HH:mm:ss" };

	// static instantiations of each of the possible values for this enumeration

    /** Formatter for ISO */
	public static final TimeFormatType ISO = new TimeFormatType(ISO_TYPE);

    /** Formatter for day-of-year */
	public static final TimeFormatType DAY_OF_YEAR = new TimeFormatType(
			DAY_OF_YEAR_TYPE);

    /** Formatter for year-month-day */
	public static final TimeFormatType YEAR_MONTH_DAY = new TimeFormatType(
			YEAR_MONTH_DAY_TYPE);

    /** Formatter for ISO TIME */
	public static final TimeFormatType ISO_TIME = new TimeFormatType(
			ISO_TIME_TYPE);

	/**
	 * Get a date formatter for the time format specified by this enumeration
	 * 
	 * @param includeMilliseconds
	 *            True if the format includes milliseconds, false otherwise
	 * 
	 * @return The date format object for the value of this enumeration. By
	 *         default, lenient parsing mode is enabled.
	 */
	public DateFormat getFormatter(final boolean includeMilliseconds) {
		String formatString = regexpStrings[getValueAsInt()];
		if (includeMilliseconds == true) {
			formatString += MILLISECONDS_ENDING;
		}

		final DateFormat df = new SimpleDateFormat(formatString);
		df.setTimeZone(TimeZone.getTimeZone(GMT_TIME_ZONE));
		df.setLenient(true);

		return (df);
	}

	/**
	 * 
	 * Creates an instance of VenueType.
	 * 
	 * @param strVal
	 *            The initial value of this enumerated type
	 */
	public TimeFormatType(final String strVal) {
		super(strVal);
	}

	/**
	 * 
	 * Creates an instance of VenueType.
	 * 
	 * @param intVal
	 *            The initial value of this enumerated type
	 */
	public TimeFormatType(final int intVal) {
		super(intVal);
	}

	/**
	 * Get the string value associated with the given index
	 * 
	 * @param index
	 *            Index of the string array (should probably be one of the
	 *            static integer values)
	 * 
	 * @return The string value associated with the index passed in
	 */
	@Override
	public String getStringValue(final int index) {
		if (index < 0 || index > getMaxIndex()) {
			throw new ArrayIndexOutOfBoundsException();
		}
		return (formatTypeStrings[index]);
	}

	/**
	 * Return the maximum index for this enumeration
	 * 
	 * @return The value of the maximum index for this enumeration
	 */
	@Override
	public int getMaxIndex() {
		return (formatTypeStrings.length - 1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jpl.gds.util.EnumeratedType#setValueFromString(java.lang.String)
	 */
	@Override
	protected void setValueFromString(final String strVal) {
		if (this.maxIndex == -1) {
			this.maxIndex = getMaxIndex();
		}

		for (int i = 0; i <= this.maxIndex; i++) {
			if (strVal.equals(formatTypeStrings[i])) {
				this.valIndex = i;
				this.valString = getStringValue(i);
				return;
			}
		}

		throw new IllegalArgumentException("Illegal input value " + strVal
				+ " to Time format enumeration");
	}
}
