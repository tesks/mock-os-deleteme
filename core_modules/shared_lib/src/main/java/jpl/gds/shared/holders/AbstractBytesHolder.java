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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.shared.database.BytesBuilder;


/**
 * Abstract byte array holder class. Methods added to support the length of
 * the array. Because a byte array is not Comparable, the array is wrapped in
 * a simple class that implements Comparable. The wrapper class never holds
 * a null.
 *
 * For internal uses that need the array, there is a getValueTrusted that can
 * be called whenever the recipient can be trusted to not modify the array.
 * That eliminates the copy. Examples are PreparedStatement and BytesBuilder.
 *
 *
 * @param <T> Subclass
 */
abstract public class AbstractBytesHolder<T extends AbstractBytesHolder<?>>
    extends AbstractHolder<T, ComparableByteArray, byte[]>
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
    protected AbstractBytesHolder(
                  final Class<T>                    clss,
                  final ComparableByteArray         value,
                  final boolean                     isUnspecified,
                  final boolean                     isUnsupported,
                  final Map<ComparableByteArray, T> cache)
        throws HolderException
    {
        super(clss,
              ComparableByteArray.class,
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
     * @return ComparableByteArray value
     *
     * @throws HolderException If invalid
     */
    @Override
    protected ComparableByteArray parse(final String v) throws HolderException
    {
        try
        {
            return ComparableByteArray.valueOf(v);
        }
        catch (HolderException he)
        {
            throw new HolderException(invalidMessage(getSubClass()), he);
        }
    }


    /**
     * Basic read from database.
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
    protected ComparableByteArray getFromDbRaw(final ResultSet        rs,
                                               final String           column,
                                               final List<SQLWarning> warnings)
        throws SQLException
    {
        final byte[] bytes = rs.getBytes(column);

        addSqlWarnings(rs, warnings);

        try
        {
            return ComparableByteArray.valueOf(bytes);
        }
        catch (HolderException he)
        {
            throw new SQLException(he);
        }
    }


    /**
     * Basic read from database.
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
    protected ComparableByteArray getFromDbRaw(
                                      final ResultSet        rs,
                                      final int              column,
                                      final List<SQLWarning> warnings)
        throws SQLException
    {
        final byte[] bytes = rs.getBytes(column);

        addSqlWarnings(rs, warnings);

        try
        {
            return ComparableByteArray.valueOf(bytes);
        }
        catch (HolderException he)
        {
            throw new SQLException(he);
        }
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
    protected void insertDbRaw(final BytesBuilder        bb,
                               final ComparableByteArray value)
        throws SQLException
    {
        if (value != null)
        {
            bb.insertBlob(value.getValueTrusted());
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
    protected void setDbRaw(final PreparedStatement   ps,
                            final int                 index,
                            final ComparableByteArray value,
                            final List<SQLWarning>    warnings)
        throws SQLException
    {
        if (value != null)
        {
            ps.setBytes(index, value.getValueTrusted());
        }
        else
        {
            ps.setNull(index, Types.ARRAY);
        }

        addSqlWarnings(ps, warnings);
    }


    /**
     * Write as a simple XML element. Overridden because an array is not a
     * simple element. You could still write out the array as a string, but if
     * the user truly wants that, he can do it himself.
     *
     * @param writer  XML writer
     * @param element Element name
     *
     * @throws XMLStreamException If XML error
     */
    @Override
    public void writeSimpleElement(final XMLStreamWriter writer,
                                   final String          element)
        throws XMLStreamException
    {
        throw new UnsupportedOperationException(
                      "AbstractBytesHolder: Not a simple element");
    }


    /**
     * Get value. Will be null for an unsupported.
     *
     * @return Value
     */
    @Override
    public final byte[] getValue()
    {
        final ComparableByteArray inner = getInnerValue();

        return ((inner != null) ? inner.getValue() : null);
    }


    /**
     * Get length. Will be zero for an unsupported.
     *
     * @return Length
     */
    public int getLength()
    {
        final ComparableByteArray inner = getInnerValue();

        return ((inner != null) ? inner.getLength() : 0);
    }


    /**
     * Basic read of length from database. This is just a utility method. The
     * length is not directly usable to make the proper holder.
     *
     * @param rs       Result set
     * @param column   Column name
     * @param warnings List to stash warnings
     *
     * @return Length or null
     *
     * @throws SQLException SQL error
     */
    public static Integer getLengthFromDb(final ResultSet        rs,
                                          final String           column,
                                          final List<SQLWarning> warnings)
        throws SQLException
    {
        final int     value   = rs.getInt(column);
        final boolean wasNull = rs.wasNull();

        addSqlWarnings(rs, warnings);

        if (wasNull)
        {
            return null;
        }

        return Integer.valueOf(value);
    }


    /**
     * Insert length into prepared statement. Unsupported inserts a NULL.
     *
     * @param ps       Prepared statement
     * @param index    Index of prepared statement
     * @param warnings List to stash warnings
     *
     * @throws SQLException SQL error
     */
    public void insertLength(final PreparedStatement ps,
                             final int               index,
                             final List<SQLWarning>  warnings)
        throws SQLException
    {
        if (! isUnsupported())
        {
            ps.setInt(index, getLength());
        }
        else
        {
            ps.setNull(index, Types.ARRAY);
        }

        addSqlWarnings(ps, warnings);
    }


    /**
     * Insert length into bytes builder. Unsupported inserts a NULL.
     *
     * @param bb Bytes builder
     *
     * @throws SQLException SQL error
     */
    public void insertLength(final BytesBuilder bb)
        throws SQLException
    {
        if (! isUnsupported())
        {
            bb.insert(getLength());
        }
        else
        {
            bb.insertNULL();
        }
    }
}
