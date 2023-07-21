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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import jpl.gds.shared.database.BytesBuilder;
import jpl.gds.shared.string.StringUtil;


/**
 * Abstract String holder class.
 *
 * @param <T> Subclass
 */
abstract public class AbstractStringHolder<T extends AbstractStringHolder<?>>
    extends AbstractHolder<T, String, String>
{
    /**
     * Constructor.
     *
     * @param clss          Subclass
     * @param value         Value
     * @param isUnspecified True if unspecified value
     * @param isUnsupported True if unsupported value
     * @param cache         Instance cache or null
     *
     * @throws HolderException If invalid
     */
    protected AbstractStringHolder(final Class<T>       clss,
                                   final String         value,
                                   final boolean        isUnspecified,
                                   final boolean        isUnsupported,
                                   final Map<String, T> cache)
        throws HolderException
    {
        super(clss,
              String.class,
              value,
              isUnspecified,
              isUnsupported,
              cache);
    }


    /**
     * By default, clean does trim for string types.
     *
     * @param value Value to be cleaned
     *
     * @return Cleaned value
     */
    @Override
    protected String clean(final String value)
    {
        return value.trim();
    }


    /**
     * By default, cleanString same as clean for string types.
     *
     * @param value Value to be cleaned
     *
     * @return Cleaned value
     */
    @Override
    protected String cleanString(final String value)
    {
        return clean(value);
    }


    /**
     * Parse from a string.
     *
     * @param v String value
     *
     * @return String value
     *
     * @throws HolderException If invalid
     */
    @Override
    protected String parse(final String v) throws HolderException
    {
        return v;
    }


    /**
     * Basic read of string from database.
     *
     * @param rs       Result set
     * @param column   Column name
     * @param warnings List to stash warnings
     *
     * @return Value or null
     *
     * @throws SQLException SQL error
     */
    @Override
    protected String getFromDbRaw(final ResultSet        rs,
                                  final String           column,
                                  final List<SQLWarning> warnings)
        throws SQLException
    {
        final String str = rs.getString(column);

        addSqlWarnings(rs, warnings);

        return str;
    }


    /**
     * Basic read of string from database.
     *
     * @param rs       Result set
     * @param column   Column index
     * @param warnings List to stash warnings
     *
     * @return Value or null
     *
     * @throws SQLException SQL error
     */
    @Override
    protected String getFromDbRaw(final ResultSet        rs,
                                  final int              column,
                                  final List<SQLWarning> warnings)
        throws SQLException
    {
        final String str = rs.getString(column);

        addSqlWarnings(rs, warnings);

        return str;
    }


    /**
     * Insert value into bytes-builder.
     *
     * @param bb    Bytes builder
     * @param value Value to insert or null
     *
     * @throws SQLException SQL error
     */
    @Override
    protected void insertDbRaw(final BytesBuilder bb,
                               final String       value)
        throws SQLException
    {

        // I assume that any subclasses that use this are not holding
        // strings that generally can contain anything, but rather
        // stuff like names, hosts, etc.

        bb.insertTextOrNullComplainReplace(value);
    }


    /**
     * Insert value into prepared statement.
     *
     * @param ps       Prepared statement
     * @param index    Index of prepared statement
     * @param value    Value to insert or null
     * @param warnings List to stash warnings
     *
     * @throws SQLException SQL error
     */
    @Override
    protected void setDbRaw(final PreparedStatement ps,
                            final int               index,
                            final String            value,
                            final List<SQLWarning>  warnings)
        throws SQLException
    {
        if (value != null)
        {

            // I assume that any subclasses that use this are not holding
            // strings that generally can contain anything, but rather
            // stuff like names, hosts, etc.

            ps.setString(index, StringUtil.checkSqlText(value));
        }
        else
        {
            ps.setNull(index, Types.VARCHAR);
        }

        addSqlWarnings(ps, warnings);
    }


    /**
     * Ranges make no sense for strings.
     *
     * @return True if allowed
     */
    @Override
    protected final boolean allowsRanges()
    {
        return false;
    }


    /**
     * Get value. Will be null for an unsupported.
     *
     * @return Value
     */
    @Override
    public final String getValue()
    {
        return getInnerValue();
    }
}
