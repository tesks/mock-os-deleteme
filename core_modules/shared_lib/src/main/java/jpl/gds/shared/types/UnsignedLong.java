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
package jpl.gds.shared.types;

import java.math.BigInteger;

import jpl.gds.shared.gdr.GDR;


/**
 * Utility class for unsigned long values. Note that this class is NOT
 ^ a holder and does NOT hold a null value. Instead, references may be null.
 * Rewritten to use Java 8 unsigned support.
 *
 * Note immutable.
 *
 */
public final class UnsignedLong extends Number
    implements Comparable<UnsignedLong>
{
    private static final long serialVersionUID = 0L;

    /** Largest value as a big integer; also serves as a mask */
    private static final BigInteger BIG_MAX_VALUE =
        BigInteger.ONE.shiftLeft(Long.SIZE).subtract(BigInteger.ONE);

    private static final BigInteger BIG_MIN_VALUE = BigInteger.ZERO;

    private static final long MAX_VALUE_AS_LONG = BIG_MAX_VALUE.longValue();
    private static final long MIN_VALUE_AS_LONG = BIG_MIN_VALUE.longValue();

    /** Maximum value */
    public static final UnsignedLong MAX_VALUE =
        new UnsignedLong(MAX_VALUE_AS_LONG);

    /** Minimum value */
    public static final UnsignedLong MIN_VALUE =
        new UnsignedLong(MIN_VALUE_AS_LONG);

    /** Size in bits */
    public static final int SIZE = Long.SIZE;

    /** Size in bytes */
    public static final int BYTES = Long.BYTES;

    /** Class object */
    public static final Class<UnsignedLong> TYPE = UnsignedLong.class;

    /** Internal value held as long, presumed unsigned */
    private final long _value;


    /**
     * Constructor. This constructor presumea that all checking has
     * been done and just accepts the value.
     *
     * @param value Value as long
     */
    private UnsignedLong(final long value)
    {
        super();

        _value = value;
    }


    /**
     * Convert to double. We have to make it somehow unsigned, and a string
     * is the easiest way to do that here.
     *
     * @return Value converted to a double
     */
    @Override
    public double doubleValue()
    {
        return Double.parseDouble(toString());
    }


    /**
     * Convert to float. We have to make it somehow unsigned, and a string
     * is the easiest way to do that here.
     *
     * @return Value converted to a float
     */
    @Override
    public float floatValue()
    {
        return Float.parseFloat(toString());
    }


    /**
     * Convert to long.
     *
     * @return Value converted to a long
     */
    @Override
    public long longValue()
    {
        return _value;
    }


    /**
     * Convert to int.
     *
     * @return Value converted to a int
     */
    @Override
    public int intValue()
    {
        return (int) _value;
    }


    /**
     * Convert to short.
     *
     * @return Value converted to a short
     */
    @Override
    public short shortValue()
    {
        return (short) _value;
    }


    /**
     * Convert to byte.
     *
     * @return Value converted to a byte
     */
    @Override
    public byte byteValue()
    {
        return (byte) _value;
    }


    /**
     * Convert to big integer.
     *
     * @return Value converted to a big integer
     */
    public BigInteger bigIntegerValue()
    {
        return BigInteger.valueOf(_value).and(BIG_MAX_VALUE);
    }


    /**
     * Compare to another unsigned long.
     *
     * @param other The value to compare against
     *
     * @return Result of comparison
     */
    public int compareTo(final UnsignedLong other)
    {
        return Long.compareUnsigned(_value, other._value);
    }


    /**
     * Convert to a string.
     *
     * @return Value converted to a string.
     */
    @Override
    public String toString()
    {
        return Long.toUnsignedString(_value);
    }


    /**
     * Get hash code.
     *
     * @return Hash code
     */
    @Override
    public int hashCode()
    {
        return (int) ((_value >>> Integer.SIZE) ^ _value);
    }


    /**
     * Check equality against another unsigned long.
     *
     * @param other The value to compare against
     *
     * @return Result of comparison
     */
    @Override
    public boolean equals(final Object other)
    {
        if (other == this)
        {
            return true;
        }

        if (! TYPE.isInstance(other))
        {
            // Also takes care of null
            return false;
        }

        return (_value == TYPE.cast(other)._value);
    }


    /**
     * Create from long interpreted as signed.
     *
     * @param value Value as long
     *
     * @return Result as unsigned long
     *
     * @throws NumberFormatException Value out of range
     */
    public static UnsignedLong valueOf(final long value)
        throws NumberFormatException
    {
        if (value < MIN_VALUE_AS_LONG)
        {
            throw new NumberFormatException(
                          "Out-of-range value to UnsignedLong");
        }

        return valueOfLongAsUnsigned(value);
    }


    /**
     * Create from big integer.
     *
     * @param value Value as big integer
     *
     * @return Value as unsigned long
     *
     * @throws NumberFormatException Value out of range
     */
    public static UnsignedLong valueOf(final BigInteger value)
        throws NumberFormatException
    {
        if ((value.compareTo(BIG_MIN_VALUE) < 0) ||
            (value.compareTo(BIG_MAX_VALUE) > 0))
        {
            throw new NumberFormatException(
                          "Out-of-range value to UnsignedLong");
        }

        return valueOfLongAsUnsigned(value.longValue());
    }


    /**
     * Create from string interpreted as unsigned.
     *
     * @param value Value as string
     *
     * @return Value as UnsignedLong
     *
     * @throws NumberFormatException Value out of range
     */
    public static UnsignedLong valueOf(final String value)
        throws NumberFormatException
    {
        return valueOfLongAsUnsigned(Long.parseUnsignedLong(value));
    }


    /**
     * Create from long interpreted as unsigned. This is the only
     * call to the constructor except the statics.
     *
     * @param value Value as long
     *
     * @return Result as unsigned long
     */
    public static UnsignedLong valueOfLongAsUnsigned(final long value)
    {
        return new UnsignedLong(value);
    }


    /**
     * Create from int interpreted as unsigned.
     *
     * @param value Value as int
     *
     * @return Result as unsigned long
     */
    public static UnsignedLong valueOfIntAsUnsigned(final int value)
    {
        return valueOfLongAsUnsigned(Integer.toUnsignedLong(value));
    }


    /**
     * Increment by one. A new instance is created.
     *
     * @return New instance with incremented value
     *
     * @throws NumberFormatException Value out of range
     */
    public UnsignedLong increment() throws NumberFormatException
    {
        if (_value == MAX_VALUE_AS_LONG)
        {
            throw new NumberFormatException("Cannot increment UnsignedLong");
        }

        return valueOfLongAsUnsigned(_value + 1L);
    }


    /**
     * Extract as a GDR byte array.
     *
     * @return GDR byte array as unsigned long
     */
    public byte[] toGdrByteArray()
    {
        final byte[] bytes = new byte[GDR.GDR_64_SIZE];

        GDR.set_u64(bytes, 0, _value);

        return bytes;
    }


    /**
     * Create from a GDR byte array.
     *
     * @param source Byte array as unsigned long
     * @param offset Offset into byte array
     *
     * @return UnsignedLong
     */
    public static UnsignedLong fromGdrByteArray(final byte[] source,
                                                final int    offset)
    {
        return valueOfLongAsUnsigned(GDR.get_u64(source, offset));
    }


    /**
     * Create from a GDR byte array.
     *
     * @param source Byte array as unsigned long
     *
     * @return UnsignedLong
     */
    public static UnsignedLong fromGdrByteArray(final byte[] source)
    {
        return fromGdrByteArray(source, 0);
    }
}
