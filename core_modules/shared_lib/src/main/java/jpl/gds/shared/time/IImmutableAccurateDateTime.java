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

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import jpl.gds.serialization.primitives.time.Proto3Adt;
import jpl.gds.shared.annotation.AssumesAccurateDateTimeIsDateObject;
import jpl.gds.shared.annotation.CustomerAccessible;

/**
 * An immutable interface for AccurateDateTime objects, which supports dates/times
 * that have more precision than a basic Java time.
 * 
 *
 * @since R8
 */
@CustomerAccessible(immutable = true)
public interface IImmutableAccurateDateTime extends Comparable<Date> {
    
    /**
     * Compares two Dates for ordering.
     *
     * @param anotherAccurateDateTime
     *            the <code>IAccurateDateTime</code> to be compared.
     * @return the value <code>0</code> if the argument Date is equal to
     *         this Date; a value less than <code>0</code> if this Date
     *         is before the Date argument; and a value greater than
     *         <code>0</code> if this Date is after the Date argument.
     * @exception NullPointerException
     *                if <code>anotherDate</code> is null.
     */
    public int compareTo(final IAccurateDateTime anotherAccurateDateTime);

    /**
     * Tests if this date is before the specified date.
     *
     * @param when
     *            a date.
     * @return <code>true</code> if and only if the instant of time
     *         represented by this <tt>Date</tt> object is strictly
     *         earlier than the instant represented by <tt>when</tt>;
     *         <code>false</code> otherwise.
     * @exception NullPointerException
     *                if <code>when</code> is null.
     */
    @AssumesAccurateDateTimeIsDateObject
    public boolean before(IAccurateDateTime when);

    /**
     * Tests if this date is after the specified date.
     *
     * @param when
     *            a date.
     * @return <code>true</code> if and only if the instant represented
     *         by this <tt>Date</tt> object is strictly later than the
     *         instant represented by <tt>when</tt>;
     *         <code>false</code> otherwise.
     * @exception NullPointerException
     *                if <code>when</code> is null.
     */
    @AssumesAccurateDateTimeIsDateObject
    public boolean after(IAccurateDateTime when);

    /**
     * Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT
     * represented by this <tt>Date</tt> object.
     *
     * @return the number of milliseconds since January 1, 1970, 00:00:00 GMT
     *         represented by this date.
     */
    @AssumesAccurateDateTimeIsDateObject
    long getTime();

    /**
     * Returns the number of seconds past the minute represented by this date.
     * The value returned is between <code>0</code> and <code>61</code>. The
     * values <code>60</code> and <code>61</code> can only occur on those
     * Java Virtual Machines that take leap seconds into account.
     *
     * @return the number of seconds past the minute represented by this date.
     * @see java.util.Calendar
     * @deprecated As of JDK version 1.1,
     *             replaced by <code>Calendar.get(Calendar.SECOND)</code>.
     */
    @Deprecated
    @AssumesAccurateDateTimeIsDateObject
    int getSeconds();

    /**
     * Gets the extended portion of the date, beyond the milliseconds, as nanoseconds. Note
     * this includes the microseconds.
     * 
     * @return the nanoseconds
     */
    long getNanoseconds();

    /**
     * Gets the extended portion of the date as microseconds, truncating any nanoseconds. 
     * @return the microseconds
     */
    long getMicros();

    /**
     * Gets the extended portion of the date as tenths of microseconds, truncating any nanoseconds 
     * beyond that. 
     * @return tenths of microseconds
     */
    long getMicroTenths();

    /**
     * Formats the accurate date time as an ERT string of the format YYYY-MM-DDTHH:mm:ss.ttt[mmm[n]]
     * 
     * @param localCal              Calendar to use
     * @param builder               String builder to use (or null)
     * @param suppressTrailingZeros True if trailing 0s beyond milliseconds
     *                                  should be stripped
     *
     * @return the formatted string
     */
    String getFormattedErtSafe(Calendar localCal, StringBuilder builder, boolean suppressTrailingZeros);

    /**
     * Formats the accurate date time as an ERT string of the format YYYY-MM-DDTHH:mm:ss.ttt[mmm[n]]
     *
     * @param suppressTrailingZeros True if trailing 0s beyond milliseconds
     *                                  should be stripped
     *
     * @return the formatted string
     */
    String getFormattedErt(boolean suppressTrailingZeros);

    /**
     * Formats the accurate date time as an ERT string of format YYYY-MM-DDTHH:mm:ss.ttt[mmm[n]] using a faster,
     * method than getDsnFormat(). However, this makes this method NOT THREAD SAFE. It
     * should not be used except when multiple threads are not involved.
     * 
     * @param suppressTrailingZeros true if trailing 0s beyond milliseconds should be stripped
     *
     * @return the formatted string
     */
    String getFormattedErtFast(boolean suppressTrailingZeros);

    /**
     * Formats the accurate date time as a SCET string of format YYYY-MM-DDTHH:mm:ss.ttt[mmm[n]]
     *
     * @param suppressTrailingZeros True if trailing 0s beyond milliseconds
     *                                  should be stripped
     *
     * @return the formatted string
     */
    String getFormattedScet(boolean suppressTrailingZeros);

    /**
     * Formats the accurate date time as as SCET of format YYYY-MM-DDTHH:mm:ss.ttt[mmm[n]] using a faster,
     * method than getDsnFormat().
     * adding synchronization. since the introduction of TI/TP, this method
     * can be used within threaded contexts unpredictably. Benchmarks have shown that adding synchronization
     * causes a minimal performance hit in exchange for a safer method call.
     * 
     * @param suppressTrailingZeros true if trailing 0s beyond milliseconds should be stripped
     *
     * @return the formatted string
     */
    String getFormattedScetFast(boolean suppressTrailingZeros);

    /**
     * Performs addition and subtraction arithmetic of an
     * IAccurateDateTime instance. <code>forward</code> flag indicates
     * whether to add to (<code>true</code>) or subtract from
     * (<code>negative</code>) the instance. The term accepted is in
     * the form of milliseconds and nanosecond fraction.
     * 
     * Nanosecond argument will be rounded to the nearest 100,
     * consistent with the supported granularity of this class.
     * 
     * @param termInMillis milliseconds to add/subtract
     * @param nanoTerm nanoseconds to add/subtract in addition to milliseconds
     * @param isAdd add if <code>true</code>, subtract if <code>false</code>
     * @return new IAccurateDateTime object containing the result time instance
     * @throws IllegalArgumentException If time values out of range
     */
    IAccurateDateTime roll(long termInMillis, long nanoTerm, boolean isAdd) throws IllegalArgumentException;

    /**
     * Formats this IAccurateDateTime using a custom format string. This method allows 'e' to be appended to the 
     * basic java date/time format string to indicate how many extended digits (beyond milliseconds) are desired.
     * @param formatString custom format pattern ALA SimpleDateFormat
     * @return formatted date/time
     */
    String formatCustom(String formatString);

    /**
     * Get is-dummy state.
     *
     * @return True if value is a dummy
     */
    boolean isDummy();

    /**
     * Compare with another IAccurateDateTime.
     *
     * @param anotherDate The other guy
     *
     * @return Compare result
     *
     */
    int fastCompareTo(IAccurateDateTime anotherDate);

    /**
     * Convert to fractional seconds.
     *
     * @return BigDecimal
     *
     */
    BigDecimal asFractionalSeconds();

    /**
     * Return time as milliseconds, rounding up nanoseconds.
     *
     * @return Time as milliseconds, rounded
     *
     */
    long getRoundedTimeAsMillis();
    
    /**
     * Convert to a protobuf message
     * 
     * @return this IImmutableAccurateDateTime as a protobuf message
     */
    public Proto3Adt buildAccurateDateTime();
}
