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
 * Station id holder class.
 *
 * Note immutable.
 *
 */
public final class StationIdHolder
    extends AbstractIntegerHolder<StationIdHolder>
    implements Comparable<StationIdHolder>
{
    /**
     * Minimum StationIdHolder
     */
    public static final int MIN_VALUE = 0;

    /** Maximum StationIdHolder */
    public static final int MAX_VALUE = 9999;

    /** Unspecified station id */
    public static final int UNSPECIFIED_VALUE = MIN_VALUE;

    /** Cache the instances */
    private static final Map<Integer, StationIdHolder> _cache =
        Collections.synchronizedMap(new HashMap<Integer, StationIdHolder>());

    /** Must be last of the statics */

    /** Unspecified StationIdHolder */
    public static final StationIdHolder UNSPECIFIED;

    /** Unsupported StationIdHolder */
    public static final StationIdHolder UNSUPPORTED;

    /** StationIdHolder used as an "any" */
    private static final StationIdHolder ANY;

    static
    {
        try
        {
            UNSPECIFIED =
                new StationIdHolder(UNSPECIFIED_VALUE, true, false);
            UNSUPPORTED = new StationIdHolder(null, false, true);
            ANY         = UNSPECIFIED;
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
    public StationIdHolder(final Integer value)
        throws HolderException
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
    private StationIdHolder(final Integer value,
                            final boolean isUnspecified,
                            final boolean isUnsupported)
        throws HolderException
    {
        super(StationIdHolder.class,
              value,
              isUnspecified,
              isUnsupported,
              _cache);
    }


    /**
     * Method to validate cleaned value.
     *
     * @param value Value to be validated
     *
     * @throws HolderException If value not valid
     */
    @Override
    protected void validate(final Integer value)
        throws HolderException
    {
        final int v = value.intValue();

        if ((v < MIN_VALUE) || (v > MAX_VALUE))
        {
            throw new HolderException(
                          outOfRangeMessage(StationIdHolder.class));
        }
    }


    /**
     * Non-static utility to call standard constructor.
     *
     * @param value Value
     *
     * @return StationIdHolder
     *
     * @throws HolderException If invalid
     */
    @Override
    protected StationIdHolder getNewInstance(final Integer value)
        throws HolderException
    {
        return new StationIdHolder(value);
    }


    /**
     * Get StationIdHolder.
     *
     * @param value Value
     *
     * @return StationIdHolder
     *
     * @throws HolderException If not a valid string
     */
    public static StationIdHolder valueOfString(final String value)
        throws HolderException
    {
        // Can use any instance to call
        return ANY.valueOfString(value, UNSPECIFIED, UNSUPPORTED, _cache);
    }


    /**
     * Get StationIdHolder.
     *
     * @param value Value
     *
     * @return StationIdHolder
     *
     * @throws HolderException If not a valid value
     */
    public static StationIdHolder valueOf(final Integer value)
        throws HolderException
    {
        // Can use any instance to call
        return ANY.valueOf(value, UNSPECIFIED, UNSUPPORTED, _cache);
    }


    /**
     * Get StationIdHolder from command line option.
     *
     * @param option   Parameter name (assumed long)
     * @param cl       Command line
     * @param required True if required
     *
     * @return StationIdHolder or null
     *
     * @throws ParseException If anything wrong
     */
    public static StationIdHolder getFromOption(final String      option,
                                                final CommandLine cl,
                                                final boolean     required)
        throws ParseException
    {
        // Can use any instance to call
        return ANY.getFromOption(option,
                                 cl,
                                 required,
                                 UNSPECIFIED,
                                 UNSUPPORTED,
                                 _cache);
    }


    /**
     * Get StationIdHolders from command line.
     *
     * @param option   Parameter name (assumed long)
     * @param cl       Command line
     * @param required True if required
     *
     * @return Set of StationIdHolder or null
     *
     * @throws ParseException If anything wrong
     */
    public static Set<StationIdHolder> getSet(final String      option,
                                              final CommandLine cl,
                                              final boolean     required)
        throws ParseException
    {
        // Can use any instance to call
        return ANY.getSet(option,
                          cl,
                          required,
                          UNSPECIFIED,
                          UNSUPPORTED,
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
    public static StationIdHolder getFromDb(final ResultSet        rs,
                                            final String           column,
                                            final List<SQLWarning> warnings)
        throws HolderException, SQLException
    {
        // Can use any instance to call
        return ANY.getFromDb(rs,
                             column,
                             UNSPECIFIED,
                             UNSUPPORTED,
                             _cache,
                             warnings);
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
    public static StationIdHolder getFromDb(final ResultSet        rs,
                                            final int              column,
                                            final List<SQLWarning> warnings)
        throws HolderException, SQLException
    {
        // Can use any instance to call
        return ANY.getFromDb(rs,
                             column,
                             UNSPECIFIED,
                             UNSUPPORTED,
                             _cache,
                             warnings);
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
