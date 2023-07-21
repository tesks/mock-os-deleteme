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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;


/**
 * APID holder class.
 *
 * Note immutable.
 *
 */
public final class ApidHolder extends AbstractLongHolder<ApidHolder>
    implements Comparable<ApidHolder>
{
    /** Minimum ApidHolder */
    public static final long MIN_VALUE = 0L;

    /** Maximum ApidHolder */
    public static final long MAX_VALUE = (1L << Integer.SIZE) - 1L;

    /** Cache the instances */
    private static final Map<Long, ApidHolder> _cache =
        Collections.synchronizedMap(new HashMap<Long, ApidHolder>());

    /** Must be last of the statics */

    /** Minimum ApidHolder, also used as an "any" */
    public static final ApidHolder MINIMUM;

    static
    {
        try
        {
            // Cannot use valueOf here
            MINIMUM = new ApidHolder(MIN_VALUE);
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
     * @depreceted Use valueOf
     */
    @Deprecated
    public ApidHolder(final Long value) throws HolderException
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
    private ApidHolder(final Long    value,
                       final boolean isUnspecified,
                       final boolean isUnsupported)
        throws HolderException
    {
        super(ApidHolder.class, value, isUnspecified, isUnsupported, _cache);
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
            throw new HolderException(outOfRangeMessage(ApidHolder.class));
        }
    }


    /**
     * Non-static utility to call standard constructor.
     *
     * @param value Value
     *
     * @return ApidHolder
     *
     * @throws HolderException If invalid
     */
    @Override
    protected ApidHolder getNewInstance(final Long value)
        throws HolderException
    {
        return new ApidHolder(value);
    }


    /**
     * Get ApidHolder.
     *
     * @param value Value
     *
     * @return ApidHolder
     *
     * @throws HolderException If not a valid string
     */
    public static ApidHolder valueOfString(final String value)
        throws HolderException
    {
        // Can use any instance to call
        return MINIMUM.valueOfString(value, null, null, _cache);
    }


    /**
     * Get ApidHolder.
     *
     * @param value Value
     *
     * @return ApidHolder
     *
     * @throws HolderException If not a valid value
     */
    public static ApidHolder valueOf(final Long value)
        throws HolderException
    {
        // Can use any instance to call
        return MINIMUM.valueOf(value, null, null, _cache);
    }


    /**
     * Get ApidHolder from command line option.
     *
     * @param option   Parameter name (assumed long)
     * @param cl       Command line
     * @param required True if required
     *
     * @return ApidHolder or null
     *
     * @throws ParseException If anything wrong
     */
    public static ApidHolder getFromOption(final String      option,
                                           final CommandLine cl,
                                           final boolean     required)
        throws ParseException
    {
        // Can use any instance to call
        return MINIMUM.getFromOption(option,
                                     cl,
                                     required,
                                     null,
                                     null,
                                     _cache);
    }


    /**
     * Get ApidHolders from command line.
     *
     * @param option   Parameter name (assumed long)
     * @param cl       Command line
     * @param required True if required
     *
     * @return ApidHolder or null
     *
     * @throws ParseException If anything wrong
     */
    public static Set<ApidHolder> getSet(final String      option,
                                         final CommandLine cl,
                                         final boolean     required)
        throws ParseException
    {
        // Can use any instance to call
        return MINIMUM.getSet(option,
                              cl,
                              required,
                              null,
                              null,
                              _cache);
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
    public static ApidHolder getFromDb(final ResultSet        rs,
                                       final String           column,
                                       final List<SQLWarning> warnings)
        throws HolderException, SQLException
    {
        // Can use any instance to call
        return MINIMUM.getFromDb(rs, column, null, null, _cache, warnings);
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
    public static ApidHolder getFromDb(final ResultSet        rs,
                                       final int              column,
                                       final List<SQLWarning> warnings)
        throws HolderException, SQLException
    {
        // Can use any instance to call
        return MINIMUM.getFromDb(rs, column, null, null, _cache, warnings);
    }


    /**
     * Overridden to specify whether ranges are allowed on the command line.
     *
     * @return True if allowed
     */
    @Override
    protected boolean allowsRanges()
    {
        return true;
    }
}
