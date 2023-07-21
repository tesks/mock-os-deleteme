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

import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.types.UnsignedLong;


/**
 * VCFC holder class.
 *
 * Note immutable.
 *
 */
public final class VcfcHolder
    extends AbstractUnsignedLongHolder<VcfcHolder>
    implements Comparable<VcfcHolder>
{
    /** Minimum VcfcHolder value */
    public static final UnsignedLong MIN_VALUE = UnsignedLong.MIN_VALUE;

    /** Maximum VcfcHolder value */
    public static final UnsignedLong MAX_VALUE = UnsignedLong.MAX_VALUE;

    /** Minimum VCFC */
    public static final VcfcHolder MINIMUM;

    /** Must be last of the statics */

    /** VcfcHolder for unsupported */
    public static final VcfcHolder UNSUPPORTED;

    /** VcfcHolder used as an "any" */
    private static final VcfcHolder ANY;

    static
    {
        try
        {
            MINIMUM     = new VcfcHolder(UnsignedLong.MIN_VALUE);
            UNSUPPORTED = new VcfcHolder(null, false, true);
            ANY         = UNSUPPORTED;
        }
        catch (final HolderException he)
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
    public VcfcHolder(final UnsignedLong value)
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
    private VcfcHolder(final UnsignedLong value,
                       final boolean      isUnspecified,
                       final boolean      isUnsupported)
        throws HolderException
    {
        super(VcfcHolder.class,
              value,
              isUnspecified,
              isUnsupported,
              null);
    }


    /**
     * Method to validate cleaned value.
     *
     * @param value Value to be validated
     *
     * @throws HolderException If value not valid
     */
    @Override
    protected void validate(final UnsignedLong value)
        throws HolderException
    {
        SystemUtilities.doNothing();
    }


    /**
     * Non-static utility to call standard constructor.
     *
     * @param value Value
     *
     * @return VcfcHolder
     *
     * @throws HolderException If invalid
     */
    @Override
    protected VcfcHolder getNewInstance(final UnsignedLong value)
        throws HolderException
    {
        return new VcfcHolder(value);
    }


    /**
     * Get VcfcHolder from string.
     *
     * @param value Value
     *
     * @return VcfcHolder
     *
     * @throws HolderException If not a valid string
     */
    public static VcfcHolder valueOfString(final String value)
        throws HolderException
    {
        // Can use any instance to call
        return ANY.valueOfString(value, null, UNSUPPORTED, null);
    }


    /**
     * Get VcfcHolder from UnsignedLong.
     *
     * @param value Value
     *
     * @return VcfcHolder
     *
     * @throws HolderException If not a valid value
     */
    public static VcfcHolder valueOf(final UnsignedLong value)
        throws HolderException
    {
        // Can use any instance to call
        return ANY.valueOf(value, null, UNSUPPORTED, null);
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
    public static VcfcHolder getFromDb(final ResultSet        rs,
                                       final String           column,
                                       final List<SQLWarning> warnings)
        throws HolderException, SQLException
    {
        // Can use any instance to call
        return ANY.getFromDb(rs, column, null, UNSUPPORTED, null, warnings);
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
    public static VcfcHolder getFromDb(final ResultSet        rs,
                                       final int              column,
                                       final List<SQLWarning> warnings)
        throws HolderException, SQLException
    {
        // Can use any instance to call
        return ANY.getFromDb(rs, column, null, UNSUPPORTED, null, warnings);
    }
}
