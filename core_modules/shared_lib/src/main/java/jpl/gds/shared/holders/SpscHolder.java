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
package jpl.gds.shared.holders;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.List;


/**
 * SPSC holder class.
 *
 * Note immutable.
 *
 */
public final class SpscHolder extends AbstractLongHolder<SpscHolder>
    implements Comparable<SpscHolder>
{
    /** Minimum SpscHolder */
    public static final long MIN_VALUE = 0L;

    /** Maximum SpscHolder */
    public static final long MAX_VALUE = (1L << Integer.SIZE) - 1L;

    /** Must be last of the statics */

    /** Minimum SpscHolder used as an "any" */
    public static final SpscHolder MINIMUM;

    static
    {
        try
        {
            // Cannot use valueOf here
            MINIMUM = new SpscHolder(MIN_VALUE);
        }
        catch (HolderException he)
        {
            // Won't happen
            throw new HolderRuntimeException(he);
        }
    }


    /**
     * Constructor.
     *
     * @param value Value
     *
     * @throws HolderException If invalid
     *
     * @deprecated Use valueOf
     */
    @Deprecated
    public SpscHolder(final Long value) throws HolderException
    {
        this(value, false, false);
    }


    /**
     * Private constructor that can handle special values.
     *
     * @param value         Value
     * @param isUnspecified True if creating unspecified
     * @param isUnsupported True if creating unsupported
     *
     * @throws HolderException If invalid
     */
    private SpscHolder(final Long    value,
                       final boolean isUnspecified,
                       final boolean isUnsupported)
        throws HolderException
    {
        super(SpscHolder.class, value, isUnspecified, isUnsupported, null);
    }


    /**
     * Method to validate cleaned value.
     *
     * @param value Value to be validated
     *
     * @throws HolderException If value not valid
     */
    @Override
    protected void validate(final Long value)
        throws HolderException
    {
        final long v = value.longValue();

        if ((v < MIN_VALUE) || (v > MAX_VALUE))
        {
            throw new HolderException(outOfRangeMessage(SpscHolder.class));
        }
    }


    /**
     * Non-static utility to call standard constructor.
     *
     * @param value Value
     *
     * @return SpscHolder
     *
     * @throws HolderException If invalid
     */
    @Override
    protected SpscHolder getNewInstance(final Long value)
        throws HolderException
    {
        return new SpscHolder(value);
    }


    /**
     * Get SpscHolder.
     *
     * @param value Value
     *
     * @return SpscHolder
     *
     * @throws HolderException If not a valid string
     */
    public static SpscHolder valueOfString(final String value)
        throws HolderException
    {
        // Can use any instance to call
        return MINIMUM.valueOfString(value, null, null, null);
    }


    /**
     * Get SpscHolder.
     *
     * @param value Value
     *
     * @return SpscHolder
     *
     * @throws HolderException If not a valid value
     */
    public static SpscHolder valueOf(final Long value)
        throws HolderException
    {
        // Can use any instance to call
        return MINIMUM.valueOf(value, null, null, null);
    }


    /**
     * Get from SQL result set.
     *
     * @param rs       Result set
     * @param column   Column name
     * @param warnings List to stash warnings
     *
     * @return New instance
     *
     * @throws HolderException If value out of range
     * @throws SQLException    SQL error
     */
    public static SpscHolder getFromDb(final ResultSet        rs,
                                       final String           column,
                                       final List<SQLWarning> warnings)
        throws HolderException, SQLException
    {
        // Can use any instance to call
        return MINIMUM.getFromDb(rs, column, null, null, null, warnings);
    }


    /**
     * Get from SQL result set.
     *
     * @param rs       Result set
     * @param column   Column index
     * @param warnings List to stash warnings
     *
     * @return New instance
     *
     * @throws HolderException If value out of range
     * @throws SQLException    SQL error
     */
    public static SpscHolder getFromDb(final ResultSet        rs,
                                       final int              column,
                                       final List<SQLWarning> warnings)
        throws HolderException, SQLException
    {
        // Can use any instance to call
        return MINIMUM.getFromDb(rs, column, null, null, null, warnings);
    }
}
