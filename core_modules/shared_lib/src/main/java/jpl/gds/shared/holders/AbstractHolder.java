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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.CliUtility;
import jpl.gds.shared.database.BytesBuilder;
import jpl.gds.shared.sys.SystemUtilities;


/**
 * Abstract holder class. Values held are simple unitary values that are
 * usually written and read from a database. This class contains the basic
 * operations, and is subclassed to implement low-level operations specific to
 * the datatype.
 *
 * Formal generic parameter T is the type of the subclass representing the
 * final value (such as VcidHolder). It is used here to create common methods
 * such as equals and compareTo.
 *
 * Formal generic parameter U is the type of the primitive data, such as
 * Integer. It is Comparable, and therefore supports Number, String, and Enum.
 *
 * Formal generic parameter V is the type of the underlying data, such as
 * byte[]. It is needed when the "true" data is wrapped in another class to
 * make it Comparable. Most of the time it is the same as V. For example,
 * if U is ComparableByteArray, then V is byte[]. The user wants byte[], but
 * ComparableByteArray is used internally.
 *
 * Values may be cached and reused. This makes sense for some classes, but not
 * all. If value are known to be used repetitively, caching probably makes
 * sense. For example, relatively few unique station ids will be used in a
 * given application, and so they should be cached.
 *
 * Caching can only be done through "valueOf"-style methods, not constructors.
 * This can be enforced only with programmer discipline or tools.
 *
 * Special support is available for "unspecified" and "unsupported" values.
 *
 * "Unspecified" refers to a special value that stands for a non-specific
 * condition. The standard example for this is a station-id of zero. It means
 * essentially that there could have been a value, but there wasn't one for
 * some reason. The special value must lie within the bounds of the datatype
 * and the database column type.
 *
 * "Unsupported" is used for a null value. There are several ways to get an
 * unsupported. The database row column could have been NULL, or the table may
 * not even contain the column. The latter condition happens when a table is
 * UNIONed and the column value is set to a NULL constant.
 *
 * Generally, "unsupported" means that there cannot be a value.
 *
 * It may or may not be possible to write a NULL to the database. It is the
 * responsibility of the callers to determine whether or not a column exists
 * and whether or not a NULL may be written.
 *
 * The special values are written and read as specific string values.
 * Otherwise, instances of this class use the natural string form of their
 * values.
 *
 * The bottom level class may define a "clean" and/or "cleanString" and must
 * define a "validate" method.
 *
 * "clean" allows the user to transform otherwise illegal values into legal
 * ones. "cleanString" does the same for conversions from string. "cleanString"
 * does a trim by default, while "clean" does nothing. They do not need to test
 * for null and must not generate a null. For datatype String, the two are
 * essentially equivalent; see AbstractStringHolder. The clean methods do not
 * validate and should not throw.
 *
 * "validate" ensures that the value lies within bounds. For numeric types this
 * is usually a test that the value lies within a specific range. For string
 * types this is usually a length check. A test for null is not necessary.
 *
 * The "parse" method is for parsing values in the domain. It is not for
 * exposing to end users in the non-abstract classes. Ths end-user instead uses
 * a "valueOf" method which takes care of special values..
 *
 * Implementation details:
 *
 * Because this is a generic class with a generic implementation subclass
 * beneath it, I avoid static methods because they cannot be overridden. If the
 * ultimate method in the bottom level class must be static, an instance is
 * used to make the call. Any instance will do.
 *
 * The bottom level classes are final because subclassing doesn't really work
 * with this structure. Create a separate class instead.
 *
 * SQL Wrapper classes, such as PreparedStatement and ResultSet, may have
 * warnings. Oddly, getWarnings() is not in the Wrapper interface.
 *
 * Package visibility, not used directly by end classes.
 *
 *
 * @param <T> Subclass
 * @param <U> Inner value class (U and V may be the same)
 * @param <V> Value class
 */
abstract /* package */ class AbstractHolder<T extends AbstractHolder<?, ?, ?>,
                                            U extends Comparable<U>,
                                            V>
    extends Object implements Comparable<T>
{
    /** String form for unspecified values */
    private static final String UNSPECIFIED_STRING = "UNSPECIFIED";

    /** String form for unsupported values */
    private static final String UNSUPPORTED_STRING = "UNSUPPORTED";

    private final boolean  _isUnspecified;
    private final boolean  _isUnsupported;
    private final Class<T> _tClss;
    private final Class<U> _uClss;

    /** Internal value, null ONLY for unsupported */
    private final U _value;


    /**
     * Constructor.
     *
     * @param tClss         Subclass
     * @param uClss         Value class
     * @param value         Value
     * @param isUnspecified True if unspecified value
     * @param isUnsupported True if unsupported value
     * @param cache         Instance cache or null
     *
     * @throws HolderException If invalid
     *
     */
    protected AbstractHolder(final Class<T>  tClss,
                             final Class<U>  uClss,
                             final U         value,
                             final boolean   isUnspecified,
                             final boolean   isUnsupported,
                             final Map<U, T> cache)
        throws HolderException
    {
        super();

        if (tClss == null)
        {
            throw new HolderException("AbstractHolder" +
                                      " requires a subclass object");
        }

        if (uClss == null)
        {
            throw new HolderException(tClss.getSimpleName() +
                                      " requires a data class object");
        }

        if (! isUnsupported && (value == null))
        {
            throw new HolderException(tClss.getSimpleName()            +
                                      " does not support null values " +
                                      "except for "                    +
                                      UNSUPPORTED_STRING);
        }

        _tClss         = tClss;
        _uClss         = uClss;
        _value         = (! isUnsupported ? clean(value) : null);
        _isUnspecified = isUnspecified;
        _isUnsupported = isUnsupported;

        if (! _isUnsupported)
        {
            validate(_value);

            if ((cache != null) && ! cache.containsKey(_value))
            {
                cache.put(_value, _tClss.cast(this));
            }
        }
    }


    /**
     * Utility to call standard constructor.
     *
     * @param value Value
     *
     * @return New instance
     *
     * @throws HolderException If bad value
     */
    abstract protected T getNewInstance(final U value)
        throws HolderException;


    /**
     * Get subclass object.
     *
     * @return Subclass object
     */
    protected final Class<T> getSubClass()
    {
        return _tClss;
    }


    /**
     * Get data class object.
     *
     * @return Data class object
     */
    protected final Class<U> getDataClass()
    {
        return _uClss;
    }


    /**
     * Get value. Will be null for an unsupported.
     *
     * @return Value
     */
    protected final U getInnerValue()
    {
        return _value;
    }


    /**
     * Get value. Will be null for an unsupported.
     *
     * @return Value
     */
    abstract public V getValue();


    /**
     * Return true if unspecified.
     *
     * @return True if unspecified
     */
    public final boolean isUnspecified()
    {
        return _isUnspecified;
    }


    /**
     * Return true if unsupported.
     *
     * @return True if unsupported
     */
    public final boolean isUnsupported()
    {
        return _isUnsupported;
    }


    /**
     * Return true if unspecified or unsupported.
     *
     * @return True if unspecified or unsupported
     */
    public final boolean isSpecial()
    {
        return (_isUnspecified || _isUnsupported);
    }


    /**
     * Method to "clean" value in whatever fashion.
     * Does not throw, because it is not a validator.
     *
     * This is a dummy that does nothing unless overridden.
     *
     * Clean should not expect nor create a null.
     *
     * @param value Value to be cleaned
     *
     * @return Cleaned value
     */
    protected U clean(final U value)
    {
        return value;
    }


    /**
     * Method to "clean" string value in whatever fashion.
     * Does not throw, because it is not a validator. It does not
     * expect a null.
     *
     * This is a dummy that just does a trim unless overridden.
     *
     * @param value Value to be cleaned
     *
     * @return Cleaned value
     */
    protected String cleanString(final String value)
    {
        return value.trim();
    }


    /**
     * Method to validate cleaned value. It does not expect a null.
     *
     * @param value Value to be validated
     *
     * @throws HolderException If value not valid
     */
    abstract protected void validate(final U value)
        throws HolderException;


    /**
     * Compare to another.
     *
     * @param other The value to compare against
     *
     * @return Result of comparison
     */
    @Override
    public final int compareTo(final T other)
    {

        if (other == this)
        {
            return 0;
        }

        final U       otherValue = _uClss.cast(other.getInnerValue());
        final boolean otherNull  = (otherValue == null);

        if (_value == null)
        {
            return (otherNull ? 0 : -1);
        }

        if (otherNull)
        {
            return 1;
        }

        return _value.compareTo(otherValue);
    }


    /**
     * Convert to a string.
     *
     * @return Value converted to a string.
     *
     */
    @Override
    public final String toString()
    {
        String stringValue = null;

        if (_isUnspecified)
        {
            stringValue = UNSPECIFIED_STRING;
        }
        else if (_isUnsupported)
        {
            stringValue = UNSUPPORTED_STRING;
        }
        else
        {
            stringValue = String.valueOf(_value);
        }

        return stringValue;
    }


    /**
     * Get hash code.
     *
     * @return Hash code
     *
     */
    @Override
    public final int hashCode()
    {
        return ((_value != null) ? _value.hashCode() : 0);
    }


    /**
     * Check equality against another.
     *
     * @param other The value to compare against
     *
     * @return Result of comparison
     */
    @Override
    public final boolean equals(final Object other)
    {

        if (other == this)
        {
            return true;
        }

        if (! _tClss.isInstance(other))
        {
            // Also takes care of null
            return false;
        }

        final T       o      = _tClss.cast(other);
        final U       oValue = _uClss.cast(o.getInnerValue());
        final boolean oNull  = (oValue == null);

        if (_value == null)
        {
            return oNull;
        }

        if (oNull)
        {
            return false;
        }

        return _value.equals(oValue);
    }


    /**
     * Get from string.
     *
     * @param value       Value
     * @param unspecified Unspecified instance
     * @param unsupported Unsupported instance
     * @param cache       Cache or null
     *
     * @return New object
     *
     * @throws HolderException If not a valid string
     */
    protected T valueOfString(final String    value,
                              final T         unspecified,
                              final T         unsupported,
                              final Map<U, T> cache)
        throws HolderException
    {
        if (value == null)
        {
            throw new HolderException(_tClss.getSimpleName() +
                                      " does not support null string values");
        }

        final String vString = cleanString(value);

        if ((unsupported != null) &&
            vString.equalsIgnoreCase(UNSUPPORTED_STRING))
        {
            return unsupported;
        }

        if ((unspecified != null) &&
            vString.equalsIgnoreCase(UNSPECIFIED_STRING))
        {
            return unspecified;
        }

        return valueOf(parse(vString), unspecified, unsupported, cache);
    }


    /**
     * Parse from a string.
     *
     * @param v String value
     *
     * @return Value
     *
     * @throws HolderException If invalid
     */
    abstract protected U parse(final String v)
        throws HolderException;


    /**
     * Get from U.
     *
     * @param value       Value
     * @param unspecified Unspecified instance or null
     * @param unsupported Unsupported instance or null
     * @param cache       Cached instances or null
     *
     * @return New object
     *
     * @throws HolderException If not a valid value
     */
    protected T valueOf(final U         value,
                        final T         unspecified,
                        final T         unsupported,
                        final Map<U, T> cache)
        throws HolderException
    {

        if (value == null)
        {
            if (unsupported != null)
            {
                return unsupported;
            }

            throw new HolderException(_tClss.getSimpleName() +
                                      " does not support null values");
        }

        final U cvalue = clean(value);

        if ((unspecified != null) &&
            cvalue.equals(unspecified.getInnerValue()))
        {
            return unspecified;
        }

        if (cache != null)
        {
            final T cached = cache.get(cvalue);

            if (cached != null)
            {
                return cached;
            }
        }

        return _tClss.cast(getNewInstance(cvalue));
    }


    /**
     * Get from command line option.
     *
     * @param option      Parameter name (assumed long)
     * @param cl          Command line
     * @param required    True if required
     * @param unspecified Unspecified
     * @param unsupported Unsupported
     * @param cache       Value cache
     *
     * @return New instance or null
     *
     * @throws MissingOptionException   If required option missing
     * @throws MissingArgumentException If option argument missing
     * @throws ParseException           If anything else wrong
     */
    protected T getFromOption(final String      option,
                              final CommandLine cl,
                              final boolean     required,
                              final T           unspecified,
                              final T           unsupported,
                              final Map<U, T>   cache)
        throws MissingOptionException,
               MissingArgumentException,
               ParseException
    {
        final String value = cleanString(getOptionValue(option, cl, required));

        if (value == null)
        {
            return null;
        }

        T obj = null;

        try
        {
            obj = _tClss.cast(valueOfString(value,
                                            unspecified,
                                            unsupported,
                                            cache));
        }
        catch (final HolderException he)
        {
            throw new ParseException("Illegal value for --" +
                                     option                 +
                                     ": "                   +
                                     he.getMessage());
        }

        return obj;
    }


    /**
     * Get set from command line. Values are separated by commas, and
     * may be ranges separated by ".." if allowed.
     *
     * @param option      Parameter name (assumed long)
     * @param cl          Command line
     * @param required    True if required
     * @param unspecified Unspecified
     * @param unsupported Unsupported
     * @param cache       Value cache
     *
     * @return Set of T or null
     *
     * @throws MissingOptionException   If required option missing
     * @throws MissingArgumentException If option argument missing
     * @throws ParseException           If anything else wrong
     */
    protected Set<T> getSet(final String      option,
                            final CommandLine cl,
                            final boolean     required,
                            final T           unspecified,
                            final T           unsupported,
                            final Map<U, T>   cache)
        throws MissingOptionException,
               MissingArgumentException,
               ParseException
    {
        String value = getOptionValue(option, cl, required);

        if (value == null)
        {
            return null;
        }

        value = cleanString(value);

        final Set<T>  set    = new TreeSet<T>();
        final boolean expand = allowsRanges();

        try
        {
            for (final String s : value.split(",", -1))
            {
                if (expand)
                {
                    for (final String r : CliUtility.expandRange(s.trim()))
                    {
                        set.add(valueOfString(r,
                                              unspecified,
                                              unsupported,
                                              cache));
                    }
                }
                else
                {
                    set.add(valueOfString(s.trim(),
                                          unspecified,
                                          unsupported,
                                          cache));
                }
            }
        }
        catch (final HolderException he)
        {
            throw new ParseException("Illegal value for --" +
                                     option                 +
                                     ": "                   +
                                     he.getMessage());
        }

        return set;
    }


    /**
     * Insert value into bytes-builder for LDI. Note that unsupported goes
     * in as a NULL.
     *
     * @param bb Bytes builder
     *
     * @throws SQLException If cannot insert
     */
    public void insert(final BytesBuilder bb)
        throws SQLException
    {
        if (_isUnsupported)
        {
            insertDbRaw(bb, null);
        }
        else
        {
            insertDbRaw(bb, _value);
        }
    }


    /**
     * Insert value into bytes-builder.
     *
     * @param bb    Bytes builder
     * @param value Value to insert
     *
     * @throws SQLException SQL error
     */
    abstract protected void insertDbRaw(final BytesBuilder bb,
                                        final U            value)
        throws SQLException;


    /**
     * Insert value into prepared statement. Note that unsupported goes
     * in as a NULL.
     *
     * @param ps       Prepared statement
     * @param index    Index in prepared statement
     * @param warnings List to stash warnings
     *
     * @throws SQLException SQL error
     */
    public void insert(final PreparedStatement ps,
                       final int               index,
                       final List<SQLWarning>  warnings)
        throws SQLException
    {
        if (_isUnsupported)
        {
            setDbRaw(ps, index, null, warnings);
        }
        else
        {
            setDbRaw(ps, index, _value, warnings);
        }
    }


    /**
     * Insert value into prepared statement.
     *
     * @param ps       Prepared statement
     * @param index    Index of prepared statement
     * @param value    Value to insert or null
     * @param warning List to stash warnings
     *
     * @throws SQLException SQL error
     */
    abstract protected void setDbRaw(final PreparedStatement ps,
                                     final int               index,
                                     final U                 value,
                                     final List<SQLWarning>  warning)
        throws SQLException;


    /**
     * Get from SQL result set.
     *
     * @param tU          Value from result set
     * @param unspecified Unspecified instance or null
     * @param unsupported Unsupported instance or null
     * @param column      Column name or integer as string
     * @param cache       Cache or null
     *
     * @return New instance
     *
     * @throws HolderException If value out of range
     * @throws SQLException    SQL error
     *
     */
    private T getFromDbInner(final U         tU,
                             final T         unspecified,
                             final T         unsupported,
                             final String    column,
                             final Map<U, T> cache)
        throws HolderException, SQLException
    {
        if (tU == null)
        {
            if (unsupported != null)
            {
                return unsupported;
            }

            throw new SQLException("Unexpected NULL for " +
                                   _tClss.getSimpleName() +
                                   " column "             +
                                   column);
        }

        if ((unspecified != null) &&
            clean(tU).equals(unspecified.getInnerValue()))
        {
            return unspecified;
        }

        return valueOf(tU, unspecified, unsupported, cache);
    }


    /**
     * Get from SQL result set.
     *
     * @param rs          Result set
     * @param column      Column name
     * @param unspecified Unspecified instance or null
     * @param unsupported Unsupported instance or null
     * @param cache       Cache
     * @param warnings    List to stash warnings
     *
     * @return New instance
     *
     * @throws HolderException If value out of range
     * @throws SQLException    SQL error
     *
     */
    protected T getFromDb(final ResultSet        rs,
                          final String           column,
                          final T                unspecified,
                          final T                unsupported,
                          final Map<U, T>        cache,
                          final List<SQLWarning> warnings)
        throws HolderException, SQLException
    {
        return getFromDbInner(getFromDbRaw(rs, column, warnings),
                              unspecified,
                              unsupported,
                              column,
                              cache);
    }


    /**
     * Get from SQL result set.
     *
     * @param rs          Result set
     * @param column      Column integer
     * @param unspecified Unspecified instance or null
     * @param unsupported Unsupported instance or null
     * @param cache       Cache
     * @param warnings    List to stash warnings
     *
     * @return New instance
     *
     * @throws HolderException If value out of range
     * @throws SQLException    SQL error
     *
     */
    protected T getFromDb(final ResultSet        rs,
                          final int              column,
                          final T                unspecified,
                          final T                unsupported,
                          final Map<U, T>        cache,
                          final List<SQLWarning> warnings)
        throws HolderException, SQLException
    {
        return getFromDbInner(getFromDbRaw(rs, column, warnings),
                              unspecified,
                              unsupported,
                              String.valueOf(column),
                              cache);
    }


    /**
     * Get from SQL result set, and handle HolderException.
     *
     * @param rs          Result set
     * @param column      Column name
     * @param unspecified Unspecified instance or null
     * @param unsupported Unsupported instance or null
     * @param cache       Cache
     * @param warnings    List to stash warnings
     *
     * @return New instance
     *
     * @throws SQLException SQL error
     */
    protected T getFromDbRethrow(final ResultSet        rs,
                                 final String           column,
                                 final T                unspecified,
                                 final T                unsupported,
                                 final Map<U, T>        cache,
                                 final List<SQLWarning> warnings)
        throws SQLException
    {
        try
        {
            return getFromDb(rs,
                             column,
                             unspecified,
                             unsupported,
                             cache,
                             warnings);
        }
        catch (final HolderException hc)
        {
            throw new SQLException(column + " out of range from database", hc);
        }
    }


    /**
     * Get from SQL result set, and handle HolderException.
     *
     * @param rs          Result set
     * @param column      Column index
     * @param unspecified Unspecified instance or null
     * @param unsupported Unsupported instance or null
     * @param cache       Cache
     * @param warnings    List to stash warnings
     *
     * @return New instance
     *
     * @throws SQLException SQL error
     *
     */
    protected T getFromDbRethrow(final ResultSet        rs,
                                 final int              column,
                                 final T                unspecified,
                                 final T                unsupported,
                                 final Map<U, T>        cache,
                                 final List<SQLWarning> warnings)
        throws SQLException
    {
        try
        {
            return getFromDb(rs,
                             column,
                             unspecified,
                             unsupported,
                             cache,
                             warnings);
        }
        catch (final HolderException hc)
        {
            throw new SQLException(column + " out of range from database", hc);
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
     * @throws HolderException Invalid value
     * @throws SQLException    SQL error
     */
    abstract protected U getFromDbRaw(final ResultSet        rs,
                                      final String           column,
                                      final List<SQLWarning> warnings)
        throws HolderException, SQLException;


    /**
     * Basic read from database.
     *
     * @param rs       Result set
     * @param column   Column index
     * @param warnings List to stash warnings
     *
     * @return Value or null
     *
     * @throws HolderException Invalid value
     * @throws SQLException    SQL error
     *
     */
    abstract protected U getFromDbRaw(final ResultSet        rs,
                                      final int              column,
                                      final List<SQLWarning> warnings)
        throws HolderException, SQLException;


    /**
     * Get option value from command line.
     *
     * @param option   Parameter name (assumed long)
     * @param cl       Command line
     * @param required True if required
     *
     * @return Value or null
     *
     * @throws MissingOptionException   If required option missing
     * @throws MissingArgumentException If option argument missing
     */
    private static String getOptionValue(final String      option,
                                         final CommandLine cl,
                                         final boolean     required)
        throws MissingOptionException, MissingArgumentException
    {
        if (! cl.hasOption(option))
        {
            if (required)
            {
                throw new MissingOptionException("You must supply --" +
                                                 option);
            }

            return null;
        }

        final String value = cl.getOptionValue(option);

        if (value == null)
        {
            throw new MissingArgumentException("You must supply a value " +
                                               "for --"                   +
                                               option);
        }

        return value;
    }


    /**
     * Write as a simple XML element.
     *
     * @param writer  XML writer
     * @param element Element name
     *
     * @throws XMLStreamException If XML error
     *
     */
    public void writeSimpleElement(final XMLStreamWriter writer,
                                   final String          element)
        throws XMLStreamException
    {
        writer.writeStartElement(element);

        writer.writeCharacters(toString());

        writer.writeEndElement();
    }


    /**
     * Return string for out-of-range condition. Since this may be called
     * inside a constructor, it does not trust the local value of the class
     * object.
     *
     * @param clss Subclass type object
     *
     * @return Message
     *
     * @param <TT> Subclass
     */
    protected <TT extends AbstractHolder<?, ?, ?>>
        String outOfRangeMessage(final Class<TT> clss)
    {
        return ("Out of range for " + clss.getSimpleName());
    }


    /**
     * Return string for invalid condition. Since this mey be called inside
     * a constructor, it does not trust the local value of the class object.
     *
     * @param clss Subclass type object
     *
     * @return Message
     *
     * @param <TT> Subclass
     *
     */
    protected static final <TT extends AbstractHolder<?, ?, ?>>
        String invalidMessage(final Class<TT> clss)
    {
        return ("Not valid for " + clss.getSimpleName());
    }


    /**
     * Overridden to specify whether ranges are allowed on the command line.
     * Only integer types may do that.
     *
     * @return True if allowed
     */
    protected boolean allowsRanges()
    {
        return false;
    }


    /**
     * Add SQL warning chain to a list.
     *
     * @param warning Initial SQL warning
     * @param list    List to add to
     */
    private static void addSqlWarnings(final SQLWarning       warning,
                                       final List<SQLWarning> list)
    {
        if (list == null)
        {
            return;
        }

        for (SQLWarning next = warning;
             next != null;
             next = next.getNextWarning())
        {
            list.add(next);
        }
    }


    /**
     * Add SQL warnings from a PreparedStatement to a list.
     *
     * @param wrapper Wrapper object
     * @param list    List to add to
     */
    protected final static void addSqlWarnings(final PreparedStatement wrapper,
                                               final List<SQLWarning>  list)
    {
        try
        {
            addSqlWarnings(wrapper.getWarnings(), list);

            wrapper.clearWarnings();
        }
        catch (final SQLException sqle)
        {
        }
    }


    /**
     * Add SQL warnings from a ResultSet to a list.
     *
     * @param wrapper Wrapper object
     * @param list    List to add to
     *
     */
    protected final static void addSqlWarnings(final ResultSet        wrapper,
                                               final List<SQLWarning> list)
    {
        try
        {
            addSqlWarnings(wrapper.getWarnings(), list);

            wrapper.clearWarnings();
        }
        catch (final SQLException sqle)
        {
            SystemUtilities.doNothing();
        }
    }
}
