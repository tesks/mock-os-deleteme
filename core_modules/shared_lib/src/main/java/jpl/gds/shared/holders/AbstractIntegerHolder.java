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


/**
 * Abstract Integer holder class.
 *
 *
 * @param <T> Subclass
 */
abstract public class AbstractIntegerHolder<T extends AbstractIntegerHolder<?>>
    extends AbstractHolder<T, Integer, Integer>
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
    protected AbstractIntegerHolder(final Class<T>        clss,
                                    final Integer         value,
                                    final boolean         isUnspecified,
                                    final boolean         isUnsupported,
                                    final Map<Integer, T> cache)
        throws HolderException
    {
        super(clss,
              Integer.class,
              value,
              isUnspecified,
              isUnsupported,
              cache);
    }


    /**
     * Parse from a string.
     *
     * @param v String value
     *
     * @return Integer value
     *
     * @throws HolderException If invalid
     */
    @Override
    protected Integer parse(final String v) throws HolderException
    {
        try
        {
            return Integer.valueOf(v);
        }
        catch (NumberFormatException nfe)
        {
            throw new HolderException(invalidMessage(getSubClass()), nfe);
        }
    }


    /**
     * Basic read of integer from database.
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
    protected Integer getFromDbRaw(final ResultSet        rs,
                                   final String           column,
                                   final List<SQLWarning> warnings)
        throws SQLException
    {
        final int     tInt    = rs.getInt(column);
        final boolean wasNull = rs.wasNull();

        addSqlWarnings(rs, warnings);

        return (wasNull ? null : Integer.valueOf(tInt));
    }


    /**
     * Basic read of integer from database.
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
    protected Integer getFromDbRaw(final ResultSet        rs,
                                   final int              column,
                                   final List<SQLWarning> warnings)
        throws SQLException
    {
        final int     tInt    = rs.getInt(column);
        final boolean wasNull = rs.wasNull();

        addSqlWarnings(rs, warnings);

        return (wasNull ? null : Integer.valueOf(tInt));
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
                               final Integer      value)
        throws SQLException
    {
        if (value != null)
        {
            bb.insert(value.intValue());
        }
        else
        {
            bb.insertNULL();
        }
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
                            final Integer           value,
                            final List<SQLWarning>  warnings)
        throws SQLException
    {
        if (value != null)
        {
            ps.setInt(index, value.intValue());
        }
        else
        {
            ps.setNull(index, Types.INTEGER);
        }

        addSqlWarnings(ps, warnings);
    }


    /**
     * Get value. Will be null for an unsupported.
     *
     * @return Value
     */
    @Override
    public final Integer getValue()
    {
        return getInnerValue();
    }
}
