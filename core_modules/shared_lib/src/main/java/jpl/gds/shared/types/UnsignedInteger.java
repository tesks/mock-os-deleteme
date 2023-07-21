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
 * Utility class for unsigned integer values. Note that this class is NOT
 ^ a holder and does NOT hold a null value. Instead, references may be null.
 *
 * Note immutable.
 *
 */
public final class UnsignedInteger extends Number
    implements Comparable<UnsignedInteger>
{
    private static final long serialVersionUID = 0L;

    private static final long MAX_VALUE_AS_LONG = (1L << Integer.SIZE) - 1L;
    private static final long MIN_VALUE_AS_LONG = 0L;
    private static final int  MAX_VALUE_AS_INT  = (int) MAX_VALUE_AS_LONG;
    private static final int  MIN_VALUE_AS_INT  = (int) MIN_VALUE_AS_LONG;

    /** Largest value as a big integer */
    private static final BigInteger BIG_MAX_VALUE =
        BigInteger.valueOf(MAX_VALUE_AS_LONG);

    private static final BigInteger BIG_MIN_VALUE =
        BigInteger.valueOf(MIN_VALUE_AS_LONG);

    /** Maximum value */
    public static final UnsignedInteger MAX_VALUE =
        new UnsignedInteger(MAX_VALUE_AS_INT);

    /** Minimum value */
    public static final UnsignedInteger MIN_VALUE =
        new UnsignedInteger(MIN_VALUE_AS_INT);

    /** Size in bits */
    public static final int SIZE = Integer.SIZE;

    /** Size in bytes */
    public static final int BYTES = Integer.BYTES;

    /** Class object */
    public static final Class<UnsignedInteger> TYPE = UnsignedInteger.class;

    /** Internal value held as int, presumed unsigned */
    private final int _value;


    /**
     * Constructor. This constructor presumea that all checking has
     * been done and just accepts the value.
     *
     * @param value Value as int
     */
    private UnsignedInteger(final int value)
    {
        super();

        _value = value;
    }


    /**
     * Convert to double. We need an unsigned value here.
     *
     * @return Value converted to a double
     */
    @Override
    public double doubleValue()
    {
        return longValue();
    }


    /**
     * Convert to float. We need an unsigned value here.
     *
     * @return Value converted to a float
     */
    @Override
    public float floatValue()
    {
        return longValue();
    }


    /**
     * Convert to long.
     *
     * @return Value converted to a long
     */
    @Override
    public long longValue()
    {
        return Integer.toUnsignedLong(_value);
    }


    /**
     * Convert to int.
     *
     * @return Value converted to a int
     */
    @Override
    public int intValue()
    {
        return _value;
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
     * Convert to big integer. We need an unsigned value here.
     *
     * @return Value converted to a big integer
     */
    public BigInteger bigIntegerValue()
    {
        return BigInteger.valueOf(longValue());
    }


    /**
     * Compare to another unsigned integer.
     *
     * @param other The value to compare against
     *
     * @return Result of comparison
     */
    @Override
	public int compareTo(final UnsignedInteger other)
    {
        return Integer.compareUnsigned(_value, other._value);
    }


    /**
     * Convert to a string.
     *
     * @return Value converted to a string.
     */
    @Override
    public String toString()
    {
        return Integer.toUnsignedString(_value);
    }


    /**
     * Get hash code.
     *
     * @return Hash code
     */
    @Override
    public int hashCode()
    {
        return _value;
    }


    /**
     * Check equality against another unsigned int.
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
     * @return Result as unsigned int
     *
     * @throws NumberFormatException Value out of range
     */
    public static UnsignedInteger valueOf(final long value)
        throws NumberFormatException
    {
        if ((value < MIN_VALUE_AS_LONG) ||
            (value > MAX_VALUE_AS_LONG))    
        {
            throw new NumberFormatException(
                          "Out-of-range value to UnsignedInteger");
        }

        return valueOfIntegerAsUnsigned((int) value);
    }


    /**
     * Create from big integer.
     *
     * @param value Value as big integer
     *
     * @return Value as unsigned int
     *
     * @throws NumberFormatException Value out of range
     */
    public static UnsignedInteger valueOf(final BigInteger value)
        throws NumberFormatException
    {
        if ((value.compareTo(BIG_MAX_VALUE) > 0) ||
            (value.compareTo(BIG_MIN_VALUE) < 0))
        {
            throw new NumberFormatException(
                          "Out-of-range value to UnsignedInteger");
        }

        return valueOfIntegerAsUnsigned(value.intValue());
    }


    /**
     * Create from string interpreted as unsigned.
     *
     * @param value Value as string
     *
     * @return Value as UnsignedInteger
     *
     * @throws NumberFormatException Value out of range
     */
    public static UnsignedInteger valueOf(final String value)
        throws NumberFormatException
    {
        return valueOfIntegerAsUnsigned(Integer.parseUnsignedInt(value));
    }


    /**
     * Create from int interpreted as unsigned. This is the only
     * call to the constructor except for the statics.
     *
     * @param value Value as int
     *
     * @return Result as unsigned int
     */
    public static UnsignedInteger valueOfIntegerAsUnsigned(final int value)
    {
        return new UnsignedInteger(value);
    }


    /**
     * Increment by one. A new instance is created.
     *
     * @return New instance with incremented value
     *
     * @throws NumberFormatException Value out of range
     */
    public UnsignedInteger increment() throws NumberFormatException
    {
        if (_value == MAX_VALUE_AS_INT)
        {
            throw new NumberFormatException("Cannot increment UnsignedInteger");
        }

        return valueOfIntegerAsUnsigned(_value + 1);
    }


    /**
     * Extract as a GDR byte array.
     *
     * @return GDR byte array as unsigned int
     */
    public byte[] toGdrByteArray()
    {
        final byte[] bytes = new byte[GDR.GDR_32_SIZE];

        GDR.set_u32(bytes, 0, _value);

        return bytes;
    }


    /**
     * Create from a GDR byte array.
     *
     * @param source Byte array as unsigned int
     * @param offset Offset into byte array
     *
     * @return UnsignedInteger
     */
    public static UnsignedInteger fromGdrByteArray(final byte[] source,
                                                   final int    offset)
    {
        return valueOfIntegerAsUnsigned((int) GDR.get_u32(source, offset));
    }


    /**
     * Create from a GDR byte array.
     *
     * @param source Byte array as unsigned int
     *
     * @return UnsignedInteger
     */
    public static UnsignedInteger fromGdrByteArray(final byte[] source)
    {
        return fromGdrByteArray(source, 0);
    }
}
