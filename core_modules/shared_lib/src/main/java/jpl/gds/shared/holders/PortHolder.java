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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;


/**
 * Port holder class.
 *
 * Note immutable.
 *
 */
public final class PortHolder
    extends AbstractIntegerHolder<PortHolder>
    implements Comparable<PortHolder>
{
    /** Minimum PortHolder */
    public static final int MIN_VALUE = 1;

    public static final int MAX_VALUE = 65535;

    /** Cache the instances */
    private static final Map<Integer, PortHolder> _cache =
        Collections.synchronizedMap(new HashMap<Integer, PortHolder>());

    /** Must be last of the statics */

    /** Unsupported PortHolder */
    public static final PortHolder UNSUPPORTED;

    /** PortHolder used as an "any" */
    private static final PortHolder ANY;

    static
    {
        try
        {
            UNSUPPORTED = new PortHolder(null, false, true);
            ANY         = UNSUPPORTED;
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
    public PortHolder(final Integer value)
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
    private PortHolder(final Integer value,
                       final boolean isUnspecified,
                       final boolean isUnsupported)
        throws HolderException
    {
        super(PortHolder.class,
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
            throw new HolderException(outOfRangeMessage(PortHolder.class));
        }
    }


    /**
     * Non-static utility to call standard constructor.
     *
     * @param value Value
     *
     * @return PortHolder
     *
     * @throws HolderException If invalid
     */
    @Override
    protected PortHolder getNewInstance(final Integer value)
        throws HolderException
    {
        return new PortHolder(value);
    }


    /**
     * Get PortHolder.
     *
     * @param value Value
     *
     * @return PortHolder
     *
     * @throws HolderException If not a valid string
     */
    public static PortHolder valueOfString(final String value)
        throws HolderException
    {
        // Can use any instance to call
        return ANY.valueOfString(value, null, UNSUPPORTED, _cache);
    }


    /**
     * Get PortHolder.
     *
     * @param value Value
     *
     * @return PortHolder
     *
     * @throws HolderException If not a valid value
     */
    public static PortHolder valueOf(final Integer value)
        throws HolderException
    {
        // Can use any instance to call
        return ANY.valueOf(value, null, UNSUPPORTED, _cache);
    }


    /**
     * Get PortHolder from command line option.
     *
     * @param option   Parameter name (assumed long)
     * @param cl       Command line
     * @param required True if required
     *
     * @return PortHolder or null
     *
     * @throws ParseException If anything wrong
     */
    public static PortHolder getFromOption(final String      option,
                                           final CommandLine cl,
                                           final boolean     required)
        throws ParseException
    {
        // Can use any instance to call
        return ANY.getFromOption(option,
                                 cl,
                                 required,
                                 null,
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
    public static PortHolder getFromDb(final ResultSet        rs,
                                       final String           column,
                                       final List<SQLWarning> warnings)
        throws HolderException, SQLException
    {
        // Can use any instance to call
        return ANY.getFromDb(rs, column, null, UNSUPPORTED, _cache, warnings);
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
    public static PortHolder getFromDb(final ResultSet        rs,
                                       final int              column,
                                       final List<SQLWarning> warnings)
        throws HolderException, SQLException
    {
        // Can use any instance to call
        return ANY.getFromDb(rs, column, null, UNSUPPORTED, _cache, warnings);
    }
}
