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


/**
 * APID name holder class.
 *
 * Note immutable.
 *
 */
public final class ApidNameHolder extends AbstractStringHolder<ApidNameHolder>
    implements Comparable<ApidNameHolder>
{
    /** Minimum ApidNameHolder length */
    public static final int MIN_LENGTH = 1;

    /** Maximum ApidNameHolder length */
    public static final int MAX_LENGTH = 64;

    /** Cache the instances */
    private static final Map<String, ApidNameHolder> _cache =
        Collections.synchronizedMap(new HashMap<String, ApidNameHolder>());

    /** Must be last of the statics */

    /** ApidNameHolder for unsupported */
    public static final ApidNameHolder UNSUPPORTED;

    /** ApidNameHolder used as an "any" */
    private static final ApidNameHolder ANY;

    static
    {
        try
        {
            UNSUPPORTED = new ApidNameHolder(null, false, true);
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
    public ApidNameHolder(final String value) throws HolderException
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
    private ApidNameHolder(final String  value,
                           final boolean isUnspecified,
                           final boolean isUnsupported)
        throws HolderException
    {
        super(ApidNameHolder.class,
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
    protected void validate(final String value)
        throws HolderException
    {
        final int size = value.length();

        if ((size < MIN_LENGTH) || (size > MAX_LENGTH))
        {
            throw new HolderException(
                          outOfRangeMessage(ApidNameHolder.class));
        }
    }


    /**
     * Non-static utility to call standard constructor.
     *
     * @param value Value
     *
     * @return ApidNameHolder
     *
     * @throws HolderException If invalid
     */
    @Override
    protected ApidNameHolder getNewInstance(final String value)
        throws HolderException
    {
        return new ApidNameHolder(value);
    }


    /**
     * Get ApidNameHolder.
     *
     * @param value Value
     *
     * @return ApidNameHolder
     *
     * @throws HolderException If not a valid string
     */
    public static ApidNameHolder valueOf(final String value)
        throws HolderException
    {
        // Can use any instance to call
        return ANY.valueOf(value, null, UNSUPPORTED, _cache);
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
    public static ApidNameHolder getFromDb(final ResultSet        rs,
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
    public static ApidNameHolder getFromDb(final ResultSet        rs,
                                           final int              column,
                                           final List<SQLWarning> warnings)
        throws HolderException, SQLException
    {
        // Can use any instance to call
        return ANY.getFromDb(rs, column, null, UNSUPPORTED, _cache, warnings);
    }
}
