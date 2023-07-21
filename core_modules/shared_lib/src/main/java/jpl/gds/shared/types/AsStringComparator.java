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
package jpl.gds.shared.types;

import java.util.Comparator;

/**
 * Comparator that compares on the string form of the objects.
 *
 * @param <T> Type to be compared
 * 
 */
public class AsStringComparator<T> extends Object
    implements Comparator<T>
{
    /**
     * Constructor.
     */
    public AsStringComparator()
    {
        super();
    }


    /**
     * Convert to string and then compare.
     *
     * @param left  One object
     * @param right Other object
     *
     * @return Comparison state
     */
    @Override
    public int compare(final T left,
                       final T right)
    {
        return left.toString().compareTo(right.toString());
    }


    /**
     * Is object equal to this comparator? We say, only if the same.
     *
     * @param other Other object
     *
     * @return True if the same
     */
    @Override
    public boolean equals(final Object other)
    {
        return (this == other);
    }


    /**
     * Needed with equals.
     *
     * @return Hash
     */
    @Override
    public int hashCode()
    {

        return 0;
    }
}