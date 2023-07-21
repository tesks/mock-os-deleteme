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
package jpl.gds.shared.sys;

/**
 * Class SystemUtilities.
 *
 * Useful system and Java utilities.
 *
 */
public final class SystemUtilities extends Object
{
    /**
     * Constructor. Not used.
     */
    private SystemUtilities()
    {
        super();
    }


    /**
     * Perform a cast with a localized suppress warning.
     *
     * @param o Object to be cast
     *
     * @return Object cast to a T
     *
     * @param <T> Class to cast to
     */
    @SuppressWarnings("unchecked")
    public static <T> T castNoWarning(final Object o)
    {
        return (T) o;
    }

    // ignoreStatus and doNothing are new.
    // ignoreStatus references its parameter so as to beat any
    // static analysis tool that comes along. It isn't strictly
    // necessary for our current versions of Findbugs and PMD.
    // (I mean the if test inside.)

    /**
     * Ignore a returned status.
     *
     * @param o Value to be ignored
     */
    public static void ignoreStatus(final Object o)
    {
        if (o == null)
        {
            doNothing();
        }
    }


    /**
     * Ignore a returned status.
     *
     * @param o Value to be ignored
     */
    public static void ignoreStatus(final boolean o)
    {
        if (! o)
        {
            doNothing();
        }
    }

    /**
     * Does nothing.
     */
    public static void doNothing()
    {
        // Do nothing
    }


    /**
     * Binary null-safe equals.
     *
     * @param left  One object
     * @param right Other object
     *
     * @return True if equal
     *
     * @param <T> Value type
     */
    public static <T> boolean nullSafeEquals(final T left,
                                             final T right)
    {
        if (left == right)
        {
            // Takes care of case with both null

            return true;
        }

        if ((left == null) || (right == null))
        {
            return false;
        }

        return left.equals(right);
    }


    /**
     * Create a typed null.
     *
     * @return Null of type T
     *
     * @param <T> Type of null
     */
    public static <T> T typedNull()
    {
        return SystemUtilities.<T>castNoWarning(null);
    }
}
