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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import jpl.gds.shared.sys.SystemUtilities;


/**
 * Host name holder class.
 *
 * Note immutable.
 *
 */
public final class HostNameHolder extends AbstractStringHolder<HostNameHolder>
    implements Comparable<HostNameHolder>
{
    /** Minimum HostNameHolder length (>= 1) */
    public static final int MIN_LENGTH = 1;

    /** Maximum HostNameHolder length */
    public static final int MAX_LENGTH = 64;

    /** Cache the instances */
    private static final Map<String, HostNameHolder> _cache =
        Collections.synchronizedMap(new HashMap<String, HostNameHolder>());

    /** Must be last of the statics */

    /** HostNameHolder for unsupported */
    public static final HostNameHolder UNSUPPORTED;

    /** HostNameHolder used as an "any" */
    private static final HostNameHolder ANY;

    static
    {
        try
        {
            UNSUPPORTED = new HostNameHolder(null, false, true);
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
    public HostNameHolder(final String value)
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
    private HostNameHolder(final String  value,
                           final boolean isUnspecified,
                           final boolean isUnsupported)
        throws HolderException
    {
        super(HostNameHolder.class,
              value,
              isUnspecified,
              isUnsupported,
              _cache);
    }


    /**
     * Method to clean value before validation.
     *
     * @param value Value to be cleaned
     *
     * @return Cleaned value
     */
    @Override
    protected String clean(final String value)
    {
        String clean = super.clean(value);

        if (clean.startsWith("[") && clean.endsWith("]"))
        {
            // Remove bracketing (sometimes used with IP addresses)

            clean = clean.substring(1, clean.length() - 1);
        }

        return clean;
    }


    /**
     * Method to validate cleaned value. Will be trimmed and debracketed
     * on entry.
     *
     * @param value Value to be validated
     *
     * @throws HolderException If value not valid
     */
    @Override
    protected void validate(final String value)
        throws HolderException
    {

        final int length = value.length();

        if ((length < MIN_LENGTH) || (length > MAX_LENGTH))
        {
            throw new HolderException(
                          outOfRangeMessage(HostNameHolder.class));
        }

        // This will check for a proper host name and see if it exists.
        // It will also check for a plain IP address, but will not check
        // that for existence. Performing a really good check on the value
        // here would be very elaborate; let the system do it.

        try
        {
            SystemUtilities.ignoreStatus(InetAddress.getByName(value));
        }
        catch (UnknownHostException uhe)
        {
            throw new HolderException(uhe);
        }
    }


    /**
     * Non-static utility to call standard constructor.
     *
     * @param value Value
     *
     * @return HostNameHolder
     *
     * @throws HolderException If invalid
     */
    @Override
    protected HostNameHolder getNewInstance(final String value)
        throws HolderException
    {
        return new HostNameHolder(value);
    }


    /**
     * Get HostNameHolder.
     *
     * @param value Value
     *
     * @return HostNameHolder
     *
     * @throws HolderException If not a valid string
     */
    public static HostNameHolder valueOf(final String value)
        throws HolderException
    {
        // Can use any instance to call
        return ANY.valueOf(value, null, UNSUPPORTED, _cache);
    }


    /**
     * Get HostNameHolder.
     *
     * @param value Value
     *
     * @return HostNameHolder
     *
     * @throws HolderException If not a valid string
     */
    public static HostNameHolder valueOfString(final String value)
        throws HolderException
    {
        return valueOf(value);
    }


    /**
     * Get HostNameHolder from command line option.
     *
     * @param option   Parameter name (assumed long)
     * @param cl       Command line
     * @param required True if required
     *
     * @return HostNameHolder or null
     *
     * @throws ParseException If anything wrong
     */
    public static HostNameHolder getFromOption(final String      option,
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
    public static HostNameHolder getFromDb(final ResultSet        rs,
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
    public static HostNameHolder getFromDb(final ResultSet        rs,
                                           final int              column,
                                           final List<SQLWarning> warnings)
        throws HolderException, SQLException
    {
        // Can use any instance to call
        return ANY.getFromDb(rs, column, null, UNSUPPORTED, _cache, warnings);
    }
}
