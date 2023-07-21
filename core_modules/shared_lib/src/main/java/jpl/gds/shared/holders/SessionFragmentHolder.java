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
 * Session fragment holder class.
 *
 * Note immutable.
 *
 */
public final class SessionFragmentHolder
    extends AbstractIntegerHolder<SessionFragmentHolder>
    implements Comparable<SessionFragmentHolder>
{
    /**
     * Developer debug flag to display throughout.
     */
    public static final boolean DISPLAY_FRAGMENT = false;

    /** Minimum SessionFragmentHolder */
    public static final int MIN_VALUE = 1;

    /** Maximum SessionFragmentHolder */
    public static final int MAX_VALUE = (1 << Short.SIZE) - 1;

    /** Cache the instances */
    private static final Map<Integer, SessionFragmentHolder> _cache =
        Collections.synchronizedMap(new HashMap<Integer,
                                                SessionFragmentHolder>());

    /** Must be last of the statics */

    /** Minimum SessionFragmentHolder used as an "any" */
    public static final SessionFragmentHolder MINIMUM;

    static
    {
        try
        {
            // Cannot use valueOf here
            MINIMUM = new SessionFragmentHolder(MIN_VALUE);
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
    public SessionFragmentHolder(final Integer value)
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
    private SessionFragmentHolder(final Integer value,
                                  final boolean isUnspecified,
                                  final boolean isUnsupported)
        throws HolderException
    {
        super(SessionFragmentHolder.class,
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
                          outOfRangeMessage(SessionFragmentHolder.class));
        }
    }


    /**
     * Non-static utility to call standard constructor.
     *
     * @param value Value
     *
     * @return SessionFragmentHolder
     *
     * @throws HolderException If invalid
     */
    @Override
    protected SessionFragmentHolder getNewInstance(final Integer value)
        throws HolderException
    {
        return new SessionFragmentHolder(value);
    }


    /**
     * Get SessionFragmentHolder.
     *
     * @param value Value
     *
     * @return SessionFragmentHolder
     *
     * @throws HolderException If not a valid string
     */
    public static SessionFragmentHolder valueOfString(final String value)
        throws HolderException
    {
        // Can use any instance to call
        return MINIMUM.valueOfString(value, null, null, _cache);
    }


    /**
     * Get SessionFragmentHolder.
     *
     * @param value Value
     *
     * @return SessionFragmentHolder
     *
     * @throws HolderException If not a valid value
     */
    public static SessionFragmentHolder valueOf(final Integer value)
        throws HolderException
    {
        // Can use any instance to call
        return MINIMUM.valueOf(value, null, null, _cache);
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
    public static SessionFragmentHolder getFromDb(
                                            final ResultSet        rs,
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
    public static SessionFragmentHolder getFromDb(
                                            final ResultSet        rs,
                                            final int              column,
                                            final List<SQLWarning> warnings)
        throws HolderException, SQLException
    {
        // Can use any instance to call
        return MINIMUM.getFromDb(rs, column, null, null, _cache, warnings);
    }


    /**
     * Get from SQL result set and handle HolderException.
     *
     * @param rs       Result set
     * @param column   Column name
     * @param warnings List to stash warnings
     *
     * @return New instance
     *
     * @throws SQLException SQL error or holder error
     */
    public static SessionFragmentHolder getFromDbRethrow(
                                            final ResultSet        rs,
                                            final String           column,
                                            final List<SQLWarning> warnings)
        throws SQLException
    {
        // Can use any instance to call
        return MINIMUM.getFromDbRethrow(rs,
                                        column,
                                        null,
                                        null,
                                        _cache,
                                        warnings);
    }


    /**
     * Get from SQL result set and handle HolderException.
     *
     * @param rs       Result set
     * @param column   Column index
     * @param warnings List to stash warnings
     *
     * @return New instance
     *
     * @throws SQLException SQL error or holder error
     */
    public static SessionFragmentHolder getFromDbRethrow(
                                            final ResultSet        rs,
                                            final int              column,
                                            final List<SQLWarning> warnings)
        throws SQLException
    {
        // Can use any instance to call
        return MINIMUM.getFromDbRethrow(rs,
                                        column,
                                        null,
                                        null,
                                        _cache,
                                        warnings);
    }
}
