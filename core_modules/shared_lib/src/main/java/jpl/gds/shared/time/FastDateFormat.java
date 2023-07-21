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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;



/**
 * Fast formatter of Date to our date/time format. All methods are static.
 *
 * Fixed issue where sol 999 would not be be displayed as 0999
 * 
 */
public class FastDateFormat extends Object
{
    /** GMT time zone */
    public static final TimeZone GMT_TIME_ZONE  = TimeZone.getTimeZone("GMT");

    private static final int FORMATTED_SIZE = 23; // Maximum

    // Prepared zero-padded integers.
    //     TWO_DIGITS covers the ranges [1,12], [1,31], [0,23], [0,59], and
    //     [0,61] to accomodate month, day, hours, minutes, and seconds
    //     (remember leap seconds.)
    //
    //     THREE_DIGITS covers the ranges [1,366] and [0,999] to accomodate
    //     day-of-year and milliseconds.
    private static final String[] TWO_DIGITS   = new String[62];
    private static final String[] THREE_DIGITS = new String[1000];
    private static final String[] FOUR_DIGITS = new String[10000];
    

    private static final ConcurrentLinkedQueue<Calendar> CALENDARS =
        new ConcurrentLinkedQueue<Calendar>();

    private static final ConcurrentLinkedQueue<StringBuilder> SBS =
        new ConcurrentLinkedQueue<StringBuilder>();

    static
    {
        final StringBuilder sb = new StringBuilder(3);

        for (int i = 0; i < TWO_DIGITS.length; ++i)
        {
            sb.setLength(0);

            if (i <= 9)
            {
                sb.append('0');
            }

            sb.append(i);

            TWO_DIGITS[i] = sb.toString();
        }

        for (int i = 0; i < THREE_DIGITS.length; ++i)
        {
            sb.setLength(0);

            if (i <= 9)
            {
                sb.append("00");
            }
            else if (i <= 99)
            {
                sb.append('0');
            }

            sb.append(i);

            THREE_DIGITS[i] = sb.toString();
        }
        
        for (int i = 0; i < FOUR_DIGITS.length; ++i)
        {
            sb.setLength(0);

            if (i <= 9)
            {
                sb.append("000");
            }
            else if (i <= 99)
            {
                sb.append("00");    
            }

            /**
             * LMST timestamp created with 3-digit sol number for Sol 999
             *
             *  Was previously i < 999 so 999 would be left with 3 digits.
             */
            else if ( i<= 999) 
            {
            	sb.append('0');
            }
            sb.append(i);

            FOUR_DIGITS[i] = sb.toString();
        }
    }


    /**
     * Constructor not available
     */
    private FastDateFormat()
    {
        super();
    }


    /**
     * Convenience method to get "standard" calendars.
     *
     * @return Calendar object
     */
    public static Calendar getStandardCalendar()
    {
        return new GregorianCalendar(GMT_TIME_ZONE);
    }


    /**
     * Return a standard calendar from the pool, or make a new one. In any case,
     * the calendar should be returned to the pool.
     *
     * @return A standard calendar
     */
    public static Calendar getStandardCalendarFromPool()
    {
        final Calendar calendar = CALENDARS.poll();

        return ((calendar != null) ? calendar : getStandardCalendar());
    }


    /**
     * Release a standard calendar back to the pool.
     *
     * @param calendar Calendar
     */
    public static void releaseStandardCalendarToPool(final Calendar calendar)
    {
        if (calendar != null)
        {
            CALENDARS.add(calendar);
        }
    }


    /**
     * Return a string builder from the pool, or make a new one. In any case,
     * it should be returned to the pool.
     *
     * @return A StringBuilder
     */
    private static StringBuilder getStringBuilderFromPool()
    {
        final StringBuilder sb = SBS.poll();

        return ((sb != null) ? sb : new StringBuilder(FORMATTED_SIZE));
    }


    /**
     * Release a string builder back to the pool.
     *
     * @param sb
     */
    private static void releaseStringBuilderToPool(final StringBuilder sb)
    {
        if (sb != null)
        {
            SBS.add(sb);
        }
    }


    /**
     * Format date in our standard way, using supplied calendar and
     * string builder.
     *
     * @param date Date of interest or null for "now"
     * @param cal  Calendar to use or null to use one from the pool
     * @param sb   String builder to use or null to use one from the pool
     *
     * @return Formatted date
     */
    public static String format(final IAccurateDateTime date,
                                final Calendar      cal,
                                final StringBuilder sb)
    {
        final Calendar      useCal = (cal != null)
                                         ? cal
                                         : getStandardCalendarFromPool();
        final StringBuilder useSb  = (sb != null)
                                         ? sb
                                         : getStringBuilderFromPool();
        try
        {
            useCal.setTimeInMillis((date != null) ? date.getTime() : new AccurateDateTime().getTime());

            useSb.setLength(0);

            useSb.append(useCal.get(Calendar.YEAR));
            useSb.append('-');

            if (TimeProperties.getInstance().useDoyOutputFormat()) {
                useSb.append(THREE_DIGITS[useCal.get(Calendar.DAY_OF_YEAR)]);
            } else {
                useSb.append(TWO_DIGITS[useCal.get(Calendar.MONTH) + 1]);
                useSb.append('-');

                useSb.append(TWO_DIGITS[useCal.get(Calendar.DAY_OF_MONTH)]);
            }

            useSb.append('T');

            useSb.append(TWO_DIGITS[useCal.get(Calendar.HOUR_OF_DAY)]);
            useSb.append(':');

            useSb.append(TWO_DIGITS[useCal.get(Calendar.MINUTE)]);
            useSb.append(':');

            useSb.append(TWO_DIGITS[useCal.get(Calendar.SECOND)]);
            useSb.append('.');

            useSb.append(THREE_DIGITS[useCal.get(Calendar.MILLISECOND)]);

            return useSb.toString();
        }
        finally
        {
            if (cal == null)
            {
                releaseStandardCalendarToPool(useCal);
            }

            if (sb == null)
            {
                releaseStringBuilderToPool(useSb);
            }
        }
    }
    
    /**
     * Format date in our standard way, using supplied calendar and
     * string builder.
     *
     * @param solNumber SOL number
     * @param date      Date of interest or null for "now"
     * @param cal       Calendar to use or null to use one from the pool
     * @param sb        String builder to use or null to use one from the pool
     *
     * @return Formatted date
     */
    public static String formatSol(final int solNumber,
    		                       final Date          date,
                                   final Calendar      cal,
                                   final StringBuilder sb)
    {
        final Calendar      useCal = (cal != null)
                                         ? cal
                                         : getStandardCalendarFromPool();
        final StringBuilder useSb  = (sb != null)
                                         ? sb
                                         : getStringBuilderFromPool();
        try
        {
            useCal.setTime((date != null) ? date : new AccurateDateTime());

            useSb.setLength(0);
            
            useSb.append(TimeProperties.getInstance().getLstPrefix());
            useSb.append('-');           
            useSb.append(FOUR_DIGITS[solNumber]);
            
            useSb.append('M');

            useSb.append(TWO_DIGITS[useCal.get(Calendar.HOUR_OF_DAY)]);
            useSb.append(':');

            useSb.append(TWO_DIGITS[useCal.get(Calendar.MINUTE)]);
            useSb.append(':');

            useSb.append(TWO_DIGITS[useCal.get(Calendar.SECOND)]);
            useSb.append('.');

            useSb.append(THREE_DIGITS[useCal.get(Calendar.MILLISECOND)]);

            return useSb.toString();
        }
        finally
        {
            if (cal == null)
            {
                releaseStandardCalendarToPool(useCal);
            }

            if (sb == null)
            {
                releaseStringBuilderToPool(useSb);
            }
        }
    }
}
