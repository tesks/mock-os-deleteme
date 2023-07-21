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

import java.util.Arrays;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;


/**
 * Primitive class to make a byte array comparable. Intended for use with
 * the AbstractHolder hierarchy only.
 *
 * Immutable. Byte array is copied going in and out.
 *
 * To be consistent with the other types, this class cannot hold a null. The
 * idea is that unsupported is represented by a null value, which means a null
 * OF ComparableByteArray, not a null IN ComparableByteArray. Since other base
 * types such as Integer do not contain nulls, neither should
 * ComparableByteArray.
 *
 * For internal uses that need the array, there is a getValueTrusted that can
 * be called whenever the recipient can be trusted to not modify the array.
 * That eliminates the copy. Examples are PreparedStatement and BytesBuilder.
 *
 */
public final class ComparableByteArray extends Object
    implements Comparable<ComparableByteArray>
{
    private final byte[] _value;
    private final int    _hashCode;

    /** Computed when needed */
    private String _stringValue = null;

    /** Common value */
    public static final ComparableByteArray ZERO_ARRAY;

    static
    {
        try
        {
            ZERO_ARRAY = new ComparableByteArray(new byte[0], 0, 0);
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
     * @param value  Byte array, not null
     * @param offset Offset into byte array
     * @param length Number of bytes to take
     *
     * @throws HolderException On null value
     */
    private ComparableByteArray(final byte[] value,
                                final int    offset,
                                final int    length)
        throws HolderException
    {
        super();

        if (value == null)
        {
            throw new HolderException("ComparableByteArray cannot be null");
        }

        _value = new byte[length];

        System.arraycopy(value, offset, _value, 0, length);

        _hashCode = Arrays.hashCode(_value);
    }


    /**
     * Getter for value.
     *
     * @return Byte array
     */
    public byte[] getValue()
    {
        return Arrays.copyOf(_value, _value.length);
    }


    /**
     * Getter for value. Used only for internal calls where the recipient
     * can be trusted to not modify the array.
     *
     * @return Byte array
     */
    @SuppressWarnings("EI_EXPOSE_REP")
    public byte[] getValueTrusted()
    {
        return _value;
    }


    /**
     * Getter for length of value.
     *
     * @return Byte array length
     */
    public int getLength()
    {
        return _value.length;
    }


    /**
     * Get hash code.
     *
     * @return Hash code
     */
    @Override
    public int hashCode()
    {
        return _hashCode;
    }


    /**
     * Check equality against another.
     *
     * @param other The value to compare against
     *
     * @return Result of comparison
     */
    @Override
    public boolean equals(final Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (! ComparableByteArray.class.isInstance(other))
        {
            // Takes care of null as well
            return false;
        }

        final ComparableByteArray o = ComparableByteArray.class.cast(other);

        return Arrays.equals(_value, o._value);
    }


    /**
     * Compare to another. "other" cannot be null.
     *
     * @param other The value to compare against
     *
     * @return Result of comparison
     */
    @Override
    public int compareTo(final ComparableByteArray other)
    {
        if (other == this)
        {
            return 0;
        }

        final int leftSize  = _value.length;
        final int rightSize = other._value.length;
        final int minSize   = Math.min(leftSize, rightSize);

        for (int i = 0; i < minSize; ++i)
        {
            final byte left  = _value[i];
            final byte right = other._value[i];

            if (left > right)
            {
                return 1;
            }

            if (left < right)
            {
                return -1;
            }
        }

        if (leftSize > rightSize)
        {
            return 1;
        }

        if (leftSize < rightSize)
        {
            return -1;
        }

        return 0;
    }


    /**
     * Get value as string, lazily.
     *
     * @return Value as string
     */
    @Override
    public synchronized String toString()
    {
        if (_stringValue == null)
        {
            _stringValue = Arrays.toString(_value);
        }

        return _stringValue;
    }


    /**
     * Parse from a string. This the inverse of toString, except that we
     * trim things at each step.
     *
     * @param v String to parse
     *
     * @return ComparableByteArray
     *
     * @throws HolderException If unable to parse
     */
    public static ComparableByteArray valueOf(final String v)
        throws HolderException
    {
        String useV = v.trim();

        final int length = useV.length();

        if ((length < 2)                     ||
            (useV.charAt(0)          != '[') ||
            (useV.charAt(length - 1) != ']'))
        {
            throw new HolderException("Unable to parse: '" + useV + "'");
        }

        useV = useV.substring(1, length - 1).trim();

        final String[] values = useV.split(",", -1);
        final byte[]   bytes  = new byte[values.length];
        int            i      = 0;

        for (final String s : values)
        {
            bytes[i] = Byte.parseByte(s.trim());

            ++i;
        }

        return valueOf(bytes);
    }


    /**
     * One way to construct an instance.
     *
     * @param value Byte array or null
     *
     * @return Instance of this class or null
     *
     * @throws HolderException On error
     */
    public static ComparableByteArray valueOf(final byte[] value)
        throws HolderException
    {
        return ((value != null) ? valueOf(value, 0, value.length) : null);
    }


    /**
     * One way to construct an instance. Never returns null. It makes no sense
     * to take a portion of a null.
     *
     * @param value  Byte array
     * @param offset Offset into byte array
     * @param length Number of bytes to take
     *
     * @return Instance of this class
     *
     * @throws HolderException On error
     */
    public static ComparableByteArray valueOf(final byte[] value,
                                              final int    offset,
                                              final int    length)
        throws HolderException
    {
        // If the length is zero and the offset and length are also,
        // we return the special value. Otherwise let them be validated.

        if ((value.length == 0) &&
            (offset       == 0) &&
            (length       == 0))
        {
            return ZERO_ARRAY;
        }

        return new ComparableByteArray(value, offset, length);
    }
}
