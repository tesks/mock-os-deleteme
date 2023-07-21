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

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import jpl.gds.shared.database.BytesBuilder;
import jpl.gds.shared.types.UnsignedLong;


/**
 * Abstract UnsignedLong holder class.
 *
 *
 * @param <T> Subclass
 */
abstract public class AbstractUnsignedLongHolder<
                          T extends AbstractUnsignedLongHolder<?>>
    extends AbstractHolder<T, UnsignedLong, UnsignedLong>
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
    protected AbstractUnsignedLongHolder(
                  final Class<T>             clss,
                  final UnsignedLong         value,
                  final boolean              isUnspecified,
                  final boolean              isUnsupported,
                  final Map<UnsignedLong, T> cache)
        throws HolderException
    {
        super(clss,
              UnsignedLong.class,
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
     * @return UnsignedLong value
     *
     * @throws HolderException If invalid
     */
    @Override
    protected UnsignedLong parse(final String v)
        throws HolderException
    {
        try
        {
            return UnsignedLong.valueOf(v);
        }
        catch (final NumberFormatException nfe)
        {
            throw new HolderException(invalidMessage(getSubClass()), nfe);
        }
    }


    /**
     * Basic read of unsigned long from database.
     *
     * @param rs       Result set
     * @param value    BigDecimal or null
     ^ @param column   Column
     * @param warnings List to stash warnings
     *
     * @return UnsignedLong or null
     *
     * @throws SQLException Any error
     *
     */
    private static UnsignedLong getFromDbRawInner(
                                    final ResultSet        rs,
                                    final BigDecimal       value,
                                    final Object           column,
                                    final List<SQLWarning> warnings)
        throws SQLException
    {
        try
        {
            addSqlWarnings(rs, warnings);

            return ((value != null)
                        ? UnsignedLong.valueOf(value.toBigIntegerExact())
                        : null);
        }
        catch (final ArithmeticException | NumberFormatException rte)
        {
            // Should not happen unless column is decimal and fractional
            // or too large
            throw new SQLException("Column "  +
                                       column +
                                       " is not BIGINT UNSIGNED",
                                   rte);
        }
    }


    /**
     * Basic read of unsigned long from database.
     *
     * @param rs       Result set
     * @param column   Column name
     * @param warnings List to stash warnings
     *
     * @return Value or null
     *
     * @throws SQLException SQL error
     *
     */
    @Override
    protected UnsignedLong getFromDbRaw(final ResultSet        rs,
                                        final String           column,
                                        final List<SQLWarning> warnings)
        throws SQLException
    {
        return getFromDbRawInner(rs,
                                 rs.getBigDecimal(column),
                                 column,
                                 warnings);
    }


    /**
     * Basic read of unsigned long from database.
     *
     * @param rs       Result set
     * @param column   Column index
     * @param warnings List to stash warnings
     *
     * @return Value or null
     *
     * @throws SQLException SQL error
     *
     */
    @Override
    protected UnsignedLong getFromDbRaw(final ResultSet        rs,
                                        final int              column,
                                        final List<SQLWarning> warnings)
        throws SQLException
    {
        return getFromDbRawInner(rs,
                                 rs.getBigDecimal(column),
                                 Integer.valueOf(column),
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
                               final UnsignedLong value)
        throws SQLException
    {
        if (value != null)
        {
            bb.insert(value);
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
                            final UnsignedLong      value,
                            final List<SQLWarning>  warnings)
        throws SQLException
    {
        if (value != null)
        {
            ps.setBigDecimal(index, new BigDecimal(value.bigIntegerValue()));
        }
        else
        {
            ps.setNull(index, Types.NUMERIC);
        }

        addSqlWarnings(ps,warnings);
    }


    /**
     * Get value. Will be null for an unsupported.
     *
     * @return Value
     */
    @Override
    public final UnsignedLong getValue()
    {
        return getInnerValue();
    }


    /**
     * Extract as a GDR byte array. The surrogate is the value to use if
     * unsupported.
     *
     * @param surrogate Value to use if unsupported
     *
     * @return GDR byte array
     *
     */
    protected final byte[] toGdrByteArray(final UnsignedLong surrogate)
    {
        if (isUnsupported())
        {
            return surrogate.toGdrByteArray();
        }

        return getValue().toGdrByteArray();
    }


    /**
     * Construct from a GDR byte array. If there is a surrogate and the value
     * matches the surrogate value, return null indicating unsupported.
     *
     * @param source    Byte array as unsigned long
     * @param surrogate Value that was used to mark unsupported
     * @param clss      Class for error messages
     *
     * @return UnsignedLong or null
     *
     * @throws HolderException If bad byte array
     *
     * @param <TT> Subclass
     *
     */
    protected static final <TT extends AbstractHolder<?, ?, ?>>
        UnsignedLong fromGdrByteArray(final byte[]       source,
                                      final UnsignedLong surrogate,
                                      final Class<TT>    clss)
        throws HolderException
    {
        UnsignedLong result = null;

        try
        {
            result = UnsignedLong.fromGdrByteArray(source);
        }
        catch (final IllegalArgumentException iae)
        {
            throw new HolderException(invalidMessage(clss), iae);
        }
        
        if ((surrogate != null) && result.equals(surrogate))
        {
            return null;
        }

        return result;
    }
}
