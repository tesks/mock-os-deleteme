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

import jpl.gds.serialization.primitives.time.Proto3Lst;
import jpl.gds.shared.annotation.CustomerAccessible;

@CustomerAccessible(immutable=true)
public interface IImmutableLocalSolarTime extends IImmutableAccurateDateTime {

    /**
     * Formats a sol into a string using the project standard format.
     * 
     * @param suppressTrailingZeros
     *            true to suppress trailing zeros in nanosecond values
     * @return formatted string
     */
    String getFormattedSol(boolean suppressTrailingZeros);

    /**
     * Formats a sol into a string using the project standard format, using a
     * fast, NON THREAD SAFE algorithm. Use getFormattedSol() for thread safe
     * formatting.
     * 
     * @param suppressTrailingZeros
     *            true to suppress trailing zeros in nanosecond values
     * @return formatted string
     */
    String getFormattedSolFast(boolean suppressTrailingZeros);

    /**
     * Gets the solar day number.
     * 
     * @return sol number
     */
    int getSolNumber();

    /**
     * Gets the leap seconds for this sol.
     * 
     * @return Leap seconds
     */
    double getLeapSeconds();

    /**
     * Calculates Local Solar Time based on given SCLK
     * 
     * @param iSclk
     *            is ISclk object that will be converted to Local Solar Time
     *
     * @return Local solar time
     */
    IImmutableLocalSolarTime sclkToSol(ISclk iSclk);

    /**
     * Calculates Local Solar Time based on given SCET
     * 
     * @param scet
     *            is the Date object that will be converted to Local Solar Time
     *
     * @return Local solar time
     */
    IImmutableLocalSolarTime scetToSol(IAccurateDateTime scet);

    /**
     * SOL to SCLK conversion
     * 
     * @return sclk is the value of this sol
     */
    ISclk toSclk();

    /**
     * SOL to SCET conversion
     * 
     * @return scet is the value of this sol
     */
    IImmutableAccurateDateTime toScet();

    /**
     * Determine if this object and the given ILocalSolarTime objects are equal.
     * <p><br>
     * THIS METHOD IS NOT INTENDED TO SPECIFICALLY OVERRIDE THE GENERAL
     * Object.equals(Object o)! Do not change the signature (like I tried to - duh). 
     * 
     * @param anotherDate the ILocalSolarTime object to compare to
     * @return true if equal, false if not
     */
    boolean equals(IImmutableLocalSolarTime anotherDate);

    /**
     * Compares two local solar time objects
     * 
     * @param anotherDate Date to compare against
     * @return -1 if this date is before another date 0 if this date is equal to
     *         another date 1 if this date is after another date
     * @throws ClassCastException If of wrong type
     */
    int compareTo(IImmutableLocalSolarTime anotherDate) throws ClassCastException;

    /**
     * Returns the local solar time in milliseconds. Uses the original
     * Date.getTime() to return hrs, mins, secs and ms portion and adds that to
     * the sol number.
     * 
     * @return local solar time representation in milliseconds
     */
    long getSolExact();

    /**
     * Get decimal portion.
     *
     * @return Decimal portion
     */
    double getDecimalPortion();

    /**
     * Convert to a local solar time protobuf message
     * 
     * @return this IImmutableLocalSolarTime as an LST protobuf message
     */
    public Proto3Lst buildLocalSolarTime();

}
