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
 * Abstract Long holder class.
 *
 *
 * @param <T> Subclass
 */
abstract public class AbstractLongHolder<T extends AbstractLongHolder<?>>
    extends AbstractHolder<T, Long, Long>
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
    protected AbstractLongHolder(final Class<T>     clss,
                                 final Long         value,
                                 final boolean      isUnspecified,
                                 final boolean      isUnsupported,
                                 final Map<Long, T> cache)
        throws HolderException
    {
        super(clss,
              Long.class,
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
     * @return Long value
     *
     * @throws HolderException If invalid
     */
    @Override
    protected Long parse(final String v) throws HolderException
    {
        try
        {
            return Long.valueOf(v);
        }
        catch (NumberFormatException nfe)
        {
            throw new HolderException(invalidMessage(getSubClass()), nfe);
        }
    }


    /**
     * Basic read from database.
     *
     * @param tLong    Value from database
     * @param column   Column name
     * @param rs       Result set
     * @param warnings List to stash warnngs
     *
     * @return Value or null
     *
     * @throws SQLException SQL error
     */
    private Long getFromDbRawInner(final long             tLong,
                                   final String           column,
                                   final ResultSet        rs,
                                   final List<SQLWarning> warnings)
        throws SQLException
    {
        final boolean wasNull = rs.wasNull();

        addSqlWarnings(rs, warnings);

        return (wasNull ? null : Long.valueOf(tLong));
    }


    /**
     * Basic read from database.
     *
     * @param rs       Result set
     * @param column   Column name
     * @param warnings List to stash warnngs
     *
     * @return Value or null
     *
     * @throws SQLException SQL error
     */
    @Override
    protected Long getFromDbRaw(final ResultSet        rs,
                                final String           column,
                                final List<SQLWarning> warnings)
        throws SQLException
    {
        return getFromDbRawInner(rs.getLong(column), column, rs, warnings);
    }


    /**
     * Basic read from database.
     *
     * @param rs       Result set
     * @param column   Column index
     * @param warnings List to stash warnngs
     *
     * @return Value or null
     *
     * @throws SQLException SQL error
     */
    @Override
    protected Long getFromDbRaw(final ResultSet        rs,
                                final int              column,
                                final List<SQLWarning> warnings)
        throws SQLException
    {
        return getFromDbRawInner(rs.getLong(column),
                                 String.valueOf(column),
                                 rs,
                                 warnings);
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
                               final Long         value)
        throws SQLException
    {
        if (value != null)
        {
            bb.insert(value.longValue());
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
     * @param warnings List to stash warnngs
     *
     * @throws SQLException SQL error
     */
    @Override
    protected void setDbRaw(final PreparedStatement ps,
                            final int               index,
                            final Long              value,
                            final List<SQLWarning>  warnings)
        throws SQLException
    {
        if (value != null)
        {
            ps.setLong(index, value.longValue());
        }
        else
        {
            ps.setNull(index, Types.BIGINT);
        }

        addSqlWarnings(ps, warnings);
    }


    /**
     * Get value. Will be null for an unsupported.
     *
     * @return Value
     */
    @Override
    public final Long getValue()
    {
        return getInnerValue();
    }
}
