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
package jpl.gds.product.api.decom;

import jpl.gds.dictionary.api.channel.ChannelType;

/**
 * DecomDataType is used to represent the data type (primitive type and length)
 * of a data field for decommutation.
 * <p>
 * <b>MULTI-MISSION CORE ADAPTATION CLASS
 * <p>
 * This is a controlled class. It may not be updated without applicable change requests
 * being filed, and approval of project management. A new version tag must be added below 
 * with each revision, and both ECR number and author must be included with the version number.</b>
 * 
 *
 * @see IProductDecomField
 */
public final class DecomDataType {
    
    /**
     * The default/undefined bit-length value.
     */
    public static final int UNDEFINED_BIT_LENGTH = -1;

    private int bitLength = UNDEFINED_BIT_LENGTH;

    private final BaseDecomDataType baseType;
    

    // convenient statics with defined bit lengths
    /** Pre-defined U8 decom data type definition */
    public static final DecomDataType UNSIGNED_INT_8 = new DecomDataType(
            BaseDecomDataType.UNSIGNED_INT, 8);
    /** Pre-defined U16 decom data type definition */
    public static final DecomDataType UNSIGNED_INT_16 = new DecomDataType(
            BaseDecomDataType.UNSIGNED_INT, 16);
    /** Pre-defined U24 decom data type definition */
    public static final DecomDataType UNSIGNED_INT_24 = new DecomDataType(
            BaseDecomDataType.UNSIGNED_INT, 24);
    /** Pre-defined U32 decom data type definition */
    public static final DecomDataType UNSIGNED_INT_32 = new DecomDataType(
            BaseDecomDataType.UNSIGNED_INT, 32);
    /** Pre-defined U64 decom data type definition */
    public static final DecomDataType UNSIGNED_INT_64 = new DecomDataType(
            BaseDecomDataType.UNSIGNED_INT, 64);
    /** Pre-defined I8 decom data type definition */
    public static final DecomDataType SIGNED_INT_8 = new DecomDataType(
            BaseDecomDataType.SIGNED_INT, 8);
    /** Pre-defined I16 decom data type definition */
    public static final DecomDataType SIGNED_INT_16 = new DecomDataType(
            BaseDecomDataType.SIGNED_INT, 16);
    /** Pre-defined I24 decom data type definition */
    public static final DecomDataType SIGNED_INT_24 = new DecomDataType(
            BaseDecomDataType.SIGNED_INT, 24);
    /** Pre-defined I32 decom data type definition */
    public static final DecomDataType SIGNED_INT_32 = new DecomDataType(
            BaseDecomDataType.SIGNED_INT, 32);
    /** Pre-defined I64 decom data type definition */
    public static final DecomDataType SIGNED_INT_64 = new DecomDataType(
            BaseDecomDataType.SIGNED_INT, 64);
    /** Pre-defined F32 decom data type definition */
    public static final DecomDataType FLOAT_32 = new DecomDataType(BaseDecomDataType.FLOAT, 32);
    /** Pre-defined F64 decom data type definition */
    public static final DecomDataType FLOAT_64 = new DecomDataType(BaseDecomDataType.FLOAT, 64);
    /** Pre-defined enum 8 decom data type definition */
    public static final DecomDataType ENUM_8 = new DecomDataType(BaseDecomDataType.ENUMERATION, 8);
    /** Pre-defined enum 16 decom data type definition */
    public static final DecomDataType ENUM_16 = new DecomDataType(BaseDecomDataType.ENUMERATION, 16);
    /** Pre-defined enum 24 decom data type definition */
    public static final DecomDataType ENUM_24 = new DecomDataType(BaseDecomDataType.ENUMERATION, 24);
    /** Pre-defined enum 32 decom data type definition */
    public static final DecomDataType ENUM_32 = new DecomDataType(BaseDecomDataType.ENUMERATION, 32);
    // TODO - why do we have both enum and status types defined?
    /** Pre-defined status 8 decom data type definition */
    public static final DecomDataType STATUS_8 = new DecomDataType(BaseDecomDataType.ENUMERATION, 8);
    /** Pre-defined status 16 decom data type definition */
    public static final DecomDataType STATUS_16 = new DecomDataType(BaseDecomDataType.ENUMERATION, 16);
    /** Pre-defined status 24 decom data type definition */
    public static final DecomDataType STATUS_24 = new DecomDataType(BaseDecomDataType.ENUMERATION, 24);
    /** Pre-defined status 32 decom data type definition */
    public static final DecomDataType STATUS_32 = new DecomDataType(BaseDecomDataType.ENUMERATION, 32);
    // TODO - determine if we need digital any more and get rid of it if not
    /** Pre-defined digital 8 decom data type definition */
    public static final DecomDataType DIGITAL_8 = new DecomDataType(BaseDecomDataType.DIGITAL, 8);
    /** Pre-defined digital 16 decom data type definition */
    public static final DecomDataType DIGITAL_16 = new DecomDataType(BaseDecomDataType.DIGITAL, 16);
    /** Pre-defined digital 24 decom data type definition */
    public static final DecomDataType DIGITAL_24 = new DecomDataType(BaseDecomDataType.DIGITAL, 24);
    /** Pre-defined digital 32 decom data type definition */
    public static final DecomDataType DIGITAL_32 = new DecomDataType(BaseDecomDataType.DIGITAL, 32);
    /** Pre-defined T32 decom data type definition */
    public static final DecomDataType TIME_32 = new DecomDataType(BaseDecomDataType.TIME, 32);
    /** Pre-defined T62 decom data type definition */
    public static final DecomDataType TIME_64 = new DecomDataType(BaseDecomDataType.TIME, 64);


    /**
     * 
     * Creates an instance of DecomDataType with the given bit length.
     * 
     * @param type the BaseDecomDataType indicating the primitive data type
     * @param bitLen The length in bits of this data type
     */
    public DecomDataType(final BaseDecomDataType type, final int bitLen) {
        baseType = type;
        this.bitLength = bitLen;
    }

    /**
     * 
     * Creates an instance of DecomDataType with undefined bit length.
     * 
     * @param type the BaseDecomDataType indicating the primitive data type
     */
    public DecomDataType(final BaseDecomDataType type) {
        baseType = type;
    }

    /**
     * Gets the BaseDecomDataType enumeration value that defines the basic primitive data type.
     * @return BasedecomDataType for this object
     */
    public BaseDecomDataType getBaseType() {
        return baseType;
    }

    /**
     * Returns the size in bits of this data type.
     * 
     * @return the length of the data type in bits
     */
    public int getBitLength() {
        return bitLength;
    }

    /**
     * Returns the size in bytes of this data type. If the value is not an even
     * number of bytes, the return value is rounded up to the next whole byte.
     * 
     * @return the length of the data type in bits
     */
    public int getByteLength() {
        if (bitLength == -1) {
            return -1;
        }
        int bytes = bitLength / 8;
        if (bitLength % 8 != 0) {
            bytes++;
        }
        return bytes;
    }

    /**
     * Sets the length in bits.
     * 
     * @param bitLen the number of bits to set.
     */
    public void setBitLength(final int bitLen) {
        bitLength = bitLen;
    }

    /**
     * Sets the length in bytes.
     * 
     * @param byteLen the number of bytes to set.
     */
    public void setByteLength(final int byteLen) {
        if (byteLen == -1) {
            bitLength = -1;
        } else {
            bitLength = byteLen * 8;
        }
    }
    
    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof DecomDataType)) {
        	return false;
        }
        boolean match = baseType.equals(((DecomDataType)o).getBaseType());
        return match;
    }
    
    @Override
    public int hashCode() {
        return baseType.toString().hashCode();
    }

    /**
     * The default equals method on EnumeratedType just compares the integer
     * value of the enumerations. This method also accounts for the bit length
     * in the comparison
     * 
     * @param type the DictionaryDataType to compare
     * @return true if the parameter is equal to this
     */
    public boolean equalsWithSize(final DecomDataType type) {
        if (type == null) {
            return false;
        }
        if (type.baseType.equals(baseType)
                && type.getBitLength() == this.getBitLength()) {
            return true;
        }
        return false;
    }

    /**
     * Indicates whether this data dictionary type can be used as the type for a
     * record or array length field.
     * 
     * @return true if data type can be used for length fields; false if not
     */
    // - We want the switch cases to fall through here
    public boolean isValueFieldLengthType() {
        switch (baseType) {
            case UNSIGNED_INT:
            case SIGNED_INT:
            case FLOAT:
            case DIGITAL:
            case ENUMERATION:
                return bitLength <= 32;
            default:
                return false;
        }
    }
    
    /**
     * Indicates whether this data dictionary type can be used as the type for a
     * channel time.
     * 
     * @return true if data type can be used for length fields; false if not
     */
    public boolean isChannelTimeType() {
        return baseType.isChannelTimeType();
    }

    /**
     * Overrides the default implementation of toString(), which normally just
     * returns the string value of the enumeration. This method also returns the
     * bit length as part of the response string.
     * 
     * @return the string representation of this type
     * @see jpl.gds.shared.types.EnumeratedType#getValueAsString()
     */
    @Override
    public String toString() {
        return baseType.toString() + ","
                + (bitLength == -1 ? "variable" : bitLength);
    }
    
    /**
     * Gets the channel data type that is compatible with this decom field
     * type.
     * 
     * @return the channel type
     */
    protected ChannelType getChannelType() {
        return baseType.getChannelType();
    }
}
