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
 * Virtual channel id holder class.
 *
 * Note immutable.
 *
 */
public final class VcidHolder extends AbstractLongHolder<VcidHolder>
    implements Comparable<VcidHolder>
{
    /** Minimum VcidHolder */
    public static final long MIN_VALUE = 0L;

    /** Maximum VcidHolder */
    public static final long MAX_VALUE = (1L << Integer.SIZE) - 1L;

    /** Special for unspecified */
    public static final long UNSPECIFIED_VALUE = MAX_VALUE;

    /** Cache the instances */
    private static final Map<Long, VcidHolder> _cache =
        Collections.synchronizedMap(new HashMap<Long, VcidHolder>());

    /** Must be last of the statics */

    /** Unspecified VcidHolder */
    public static final VcidHolder UNSPECIFIED;

    /** Unsupported VcidHolder */
    public static final VcidHolder UNSUPPORTED;

    /** VcidHolder used as an "any" */
    private static final VcidHolder ANY;

    static
    {
        try
        {
            UNSPECIFIED = new VcidHolder(UNSPECIFIED_VALUE, true,  false);
            UNSUPPORTED = new VcidHolder(null, false, true);
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
    public VcidHolder(final Long value)
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
    private VcidHolder(final Long    value,
                       final boolean isUnspecified,
                       final boolean isUnsupported)
        throws HolderException
    {
        super(VcidHolder.class,
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
    protected void validate(final Long value)
        throws HolderException
    {
        final long v = value.longValue();

        if ((v < MIN_VALUE) || (v > MAX_VALUE))
        {
            throw new HolderException(outOfRangeMessage(VcidHolder.class));
        }
    }


    /**
     * Non-static utility to call standard constructor.
     *
     * @param value Value
     *
     * @return VcidHolder
     *
     * @throws HolderException If invalid
     */
    @Override
    protected VcidHolder getNewInstance(final Long value)
        throws HolderException
    {
        return new VcidHolder(value);
    }


    /**
     * Get VcidHolder from string.
     *
     * @param value Value
     *
     * @return VcidHolder
     *
     * @throws HolderException If not a valid string
     */
    public static VcidHolder valueOfString(final String value)
        throws HolderException
    {
        // Can use any instance to call
        return ANY.valueOfString(value, UNSPECIFIED, UNSUPPORTED, _cache);
    }


    /**
     * Get VcidHolder.
     *
     * @param value Value
     *
     * @return VcidHolder
     *
     * @throws HolderException If not a valid value
     */
    public static VcidHolder valueOf(final Long value)
        throws HolderException
    {
        // Can use any instance to call
        return ANY.valueOf(value, UNSPECIFIED, UNSUPPORTED, _cache);
    }


    /**
     * Get VcidHolder from command line option.
     *
     * @param option   Parameter name (assumed long)
     * @param cl       Command line
     * @param required True if required
     *
     * @return VcidHolder or null
     *
     * @throws ParseException If anything wrong
     */
    public static VcidHolder getFromOption(final String      option,
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
     * Get VcidHolders from command line.
     *
     * @param option   Parameter name (assumed long)
     * @param cl       Command line
     * @param required True if required
     *
     * @return Set of VcidHolder or null
     *
     * @throws ParseException If anything wrong
     */
    public static Set<VcidHolder> getSet(final String      option,
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
    public static VcidHolder getFromDb(final ResultSet        rs,
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
    public static VcidHolder getFromDb(final ResultSet       rs,
                                       final int             column,
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
     * Utility to truncate a VCID to a positive integer. This method should
     * be removed when there is a better fix.
     *
     * @param vcid   VCID as a Long
     * @param nullOK True if a null may be returned
     *
     * @return VCID as a positive Integer or null
     */
    public static Integer restrictSfduVcid(final Long    vcid,
                                           final boolean nullOK)
    {

        if (vcid == null)
        {
            return (nullOK ? null : 0);
        }

        final long temp = vcid.longValue();

        if (temp < 0L)
        {
            throw new IllegalArgumentException("Too-small VCID");
        }

        // NB: Not the local MAX_VALUE
        if (temp > Integer.MAX_VALUE)
        {
            throw new IllegalArgumentException("Too-large VCID");
        }

        return vcid.intValue();
    }
}
