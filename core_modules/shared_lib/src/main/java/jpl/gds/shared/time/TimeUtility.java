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
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * Methods for converting times to specific formats
 * 
 */
public class TimeUtility
{
	/** Pool of preallocated ISO DateFormat objects. **/
	private static final ConcurrentLinkedQueue<DateFormat> ISO_FORMATTERS = new ConcurrentLinkedQueue<DateFormat>();

	/** Pool of preallocated DOY DateFormat objects. **/
	private static final ConcurrentLinkedQueue<DateFormat> DOY_FORMATTERS = new ConcurrentLinkedQueue<DateFormat>();


	/**
	 * Get a DateFormat object for the ISO date/time format
	 * 
	 * @return The DateFormat object for ISO date/times (includes milliseconds)
	 */
	public static DateFormat getIsoFormatter() {
		return (TimeFormatType.ISO.getFormatter(true));
	}

	
	/**
	 * Get a DateFormat object for the DOY time format
	 * 
	 * @return The DateFormat object for DOY times (includes milliseconds)
	 */

	public static DateFormat getDayOfYearFormatter() {
		return (TimeFormatType.DAY_OF_YEAR.getFormatter(true));
	}

	/**
	 * Return an ISO formatter to the preallocated pool.
	 * 
	 * @param df
	 *            the formatter to release
	 */
	public static void releaseISOFormatterToPool(final DateFormat df) {
		ISO_FORMATTERS.add(df);
	}

	/**
	 * Get a DateFormat object for the ISO time format from a pool of
	 * pre-initialized DateFormat objects. Using this method improves
	 * performance over allocating a new one each time, but to prevent memory
	 * leaks users of this method must call the release method to return the
	 * formatter to the pool when done.
	 * 
	 * @see #releaseISOFormatterToPool(DateFormat)
	 * 
	 * @return The DateFormat object for ISO times (includes milliseconds)
	 */
	public static DateFormat getISOFormatterFromPool() {
		final DateFormat df = ISO_FORMATTERS.poll();
		if (df == null) {
			return (TimeFormatType.ISO.getFormatter(true));
		}
		return df;
	}

	/**
	 * Get a DateFormat object for the DOY time format from a pool of
	 * pre-initialized DateFormat objects. Using this method improves
	 * performance over allocating a new one each time, but to prevent memory
	 * leaks users of this method must call the release method to return the
	 * formatter to the pool when done.
	 * 
	 * @see #releaseISOFormatterToPool(DateFormat)
	 * 
	 * @return The DateFormat object for ISO times (includes milliseconds)
	 */
	public static DateFormat getDoyFormatterFromPool() {
		final DateFormat df = DOY_FORMATTERS.poll();
		if (df == null) {
			return (TimeFormatType.DAY_OF_YEAR.getFormatter(true));
		}
		return df;
	}

	/**
	 * Return an DOY formatter to the preallocated pool.
	 * 
	 * @param df
	 *            the formatter to release
	 */
	public static void releaseDoyFormatterToPool(final DateFormat df) {
		DOY_FORMATTERS.add(df);
	}

	/**
	 * Get a DateFormat object for the project-configured date/time format,
	 * which may be ISO or DOY.
	 * 
     * @param useDoyFormat True for DOY format
	 * 
	 * @return The DateFormat object for date/times (includes milliseconds)
	 */
	public static DateFormat getFormatter(final boolean useDoyFormat) {

		if (useDoyFormat) {
			return (getDayOfYearFormatter());
		} else {
			return (getIsoFormatter());
		}
	}

	/**
	 * Get a DateFormat object for the project-configured time format from a
	 * pool of pre-initialized DateFormat objects. Using this method improves
	 * performance over allocating a new one each time, but to prevent memory
	 * leaks users of this method must call the release method to return the
	 * formatter to the pool when done.
	 * 
	 * @see #releaseFormatterToPool(DateFormat)
	 * 
     * @param useDoyFormat True for DOY format
     *
	 * @return The DateFormat object for ISO times (includes milliseconds)
	 */
	public static DateFormat getFormatterFromPool(final boolean useDoyFormat) {

		if (useDoyFormat) {
			return getDoyFormatterFromPool();
		} else {
			return getISOFormatterFromPool();
		}
	}

	/**
	 * Return an project-configured date formatter to the preallocated pool.
	 * 
	 * @param df           Formatter to release
     * @param useDoyFormat True for DOY format
	 */
	public static void releaseFormatterToPool(final DateFormat df,
			final boolean useDoyFormat) {
		if (useDoyFormat) {
			releaseDoyFormatterToPool(df);
		} else {
			releaseISOFormatterToPool(df);
		}
	}

	/**
	 * This method should be called to determine if the GDS configuration
	 * specifies whether or not DOY date format should be used. If not, ISO
	 * format is used.
	 * 
	 * @return true if DOY date format is to be used, false otherwise
	 */
	public static boolean isConfiguredToUseDoyFormat() {
        return TimeProperties.getInstance().useDoyOutputFormat();
	}

	/**
	 * Get a DateFormat object for the project-configured date/time format,
	 * which may be ISO or DOY.
	 * 
	 * @return The DateFormat object for date/times (includes milliseconds)
	 */
	public static DateFormat getFormatter() {
        return getFormatter(TimeProperties.getInstance().useDoyOutputFormat());
	}

	/**
	 * Get a DateFormat object for the project-configured time format from a
	 * pool of pre-initialized DateFormat objects. Using this method improves
	 * performance over allocating a new one each time, but to prevent memory
	 * leaks users of this method must call the release method to return the
	 * formatter to the pool when done.
	 * 
	 * @see #releaseFormatterToPool(DateFormat)
	 * 
	 * @return The DateFormat object for times (includes milliseconds)
	 */
	public static DateFormat getFormatterFromPool() {
        return getFormatterFromPool(TimeProperties.getInstance().useDoyOutputFormat());
	}

	/**
	 * Return an project-configured date formatter to the preallocated pool.
	 * 
	 * @param df
	 *            the formatter to release
	 */
	public static void releaseFormatterToPool(final DateFormat df) {
        releaseFormatterToPool(df, TimeProperties.getInstance().useDoyOutputFormat());
	}


    /**
     * Fetch a formatter from the pool, format, and release.
     *
     * @param date   Date-time to format
     * @param useDoy If true use DOY else ISO
     * @return the formatted string
     */
    public static String format(final Date    date,
                                final boolean useDoy)
    {
        DateFormat format = null;
        String     result = null;

        try
        {
            format = getFormatterFromPool(useDoy);
            result = format.format(date);
        }
        finally
        {
            if (format != null)
            {
                releaseFormatterToPool(format, useDoy);
            }
        }

        return result;
    }


    /**
     * Fetch a default formatter from the pool, format, and release.
     *
     * @param date Date-time to format
     * @return the formatted string
     */
    public static String format(final IAccurateDateTime date)
    {
        DateFormat format = null;
        String     result = null;

        try
        {
            format = getFormatterFromPool();
            result = format.format(date);
        }
        finally
        {
            if (format != null)
            {
                releaseFormatterToPool(format);
            }
        }

        return result;
    }


    /**
     * Fetch a ISO formatter from the pool, format, and release.
     *
     * @param date Date-time to format
     * @return the formatted string
     */
    public static String formatISO(final Date date)
    {
        DateFormat format = null;
        String     result = null;

        try
        {
            format = getISOFormatterFromPool();
            result = format.format(date);
        }
        finally
        {
            if (format != null)
            {
                releaseISOFormatterToPool(format);
            }
        }

        return result;
    }


    /**
     * Fetch a DOY formatter from the pool, format, and release.
     *
     * @param date Date-time to format
     * @return the formatted string
     */
    public static String formatDOY(final IAccurateDateTime date)
    {
        DateFormat format = null;
        String     result = null;

        try
        {
            format = getDoyFormatterFromPool();
            result = format.format(date);
        }
        finally
        {
            if (format != null)
            {
                releaseDoyFormatterToPool(format);
            }
        }

        return result;
    }
    
    /**
     * Gets the current size of the DOY formatter pool.
     * @return number of formatters in pool
     * 
     */
    public static int getDoyFormatterPoolSize() {
        return DOY_FORMATTERS.size();
    }
    
    /**
     * Gets the current size of the ISO formatter pool.
     * @return number of formatters in pool
     * 
     */
    public static int getIsoFormatterPoolSize() {
        return ISO_FORMATTERS.size();
    }
}
