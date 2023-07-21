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

import java.io.Serializable;


/**
 * Class NanoTime.
 *
 * Holds and compares times in nanoseconds. Used for timing things.
 *
 */
public final class NanoTime extends Object
        implements Serializable, Comparable<INanoTime>, INanoTime
{
    private static final long serialVersionUID = 0L;

    private static final long CONVERT_NANO = 1000000L;
    private static final long ROUND_NANO   =  500000L;

    private final long _nano;
    private final int  _hash;


    /**
     * Constructs with current nanotime.
     */
    public NanoTime()
    {
        this(System.nanoTime());
    }


    /**
     * Constructs with a value in nanoseconds.
     *
     * @param nano Nanoseconds
     */
    public NanoTime(final long nano)
    {
        super();

        _nano = nano;
        _hash = (int) (_nano ^ (_nano >>> 32)); // The way Long does it
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long longValue()
    {
        return _nano;
    }


    /**
     * Compare as nanotimes, taking wrapping into account
     *
     * @param other Other INanoTime
     *
     * @return -1, 0, 1
     */
    @Override
    public int compareTo(final INanoTime other)
    {
        final long diff = elapsed(other);

        if (diff == 0L)
        {
            return 0;
        }

        if (diff < 0L)
        {
            return -1;
        }

        return 1;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long elapsed(final INanoTime other)
    {
        return (_nano - other.longValue());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long elapsedMillis(final INanoTime other)
    {
        final long diff = elapsed(other);

        if (diff < 0L)
        {
            return ((diff - ROUND_NANO) / CONVERT_NANO);
        }

        return ((diff + ROUND_NANO) / CONVERT_NANO);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return _hash;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof INanoTime))
        {
            return false;
        }

        final INanoTime o = (INanoTime) other;

        return (_nano == o.longValue());
    }
}
