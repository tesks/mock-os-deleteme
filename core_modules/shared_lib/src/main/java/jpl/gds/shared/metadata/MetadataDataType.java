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
package jpl.gds.shared.metadata;

import jpl.gds.shared.types.UnsignedLong;

/**
 * An enumeration that defines the types of primitive data types supported
 * within a metadata object. These are the data types that can be placed into 
 * a metadata map that must be serialized.
 * 
 *
 */
public enum MetadataDataType {
    /** Boolean context type */
    BOOLEAN(1, Boolean.class),
    /** Byte context type */
    BYTE(1, Byte.class),
    /** Short context type */
    SHORT(2, Short.class),
    /** Integer context type */
    INT(4, Integer.class),
    /** Long context type */
    LONG(8, Long.class),
    /** Unsigned byte context type. Note the actual class used is Short, but the serialization size is that of byte. */
    UNSIGNED_BYTE(1, Short.class),
    /** Unsigned short context type. Note the actual class used is Integer, but the serialization size is that of short. */
    UNSIGNED_SHORT(2, Integer.class),
    /** Unsigned integer context type. Note, the actual class used is Long, but the serialization size is that of int. */
    UNSIGNED_INT(4, Long.class),
    /** Unsigned long context type. */
    UNSIGNED_LONG(8, UnsignedLong.class),
    /** String context type */
    STRING(IMetadataKey.VARIABLE_SIZE, String.class),
    /** Floating point context type */
    FLOAT(4, Float.class),
    /** Double floating point context type */
    DOUBLE(8, Double.class);
    
    private int serializationSize;
    private Class<?> valueClass;
    
    private MetadataDataType(int size, Class<?> repClass) {
        this.serializationSize = size;
        this.valueClass = repClass;
    }
    
    /**
     * Gets the size in bytes of the serialized version of a context value with
     * the current type.
     * 
     * @return byte size; may be IMetadataKey.VARIABLE if the size may vary based
     *         upon current value
     */
    public int getSerializationSize() {
        return this.serializationSize;
    }
    
    /**
     * Indicates whether the current type is compatible with a BOOLEAN context type.
     * @return true if compatible, false if not
     */
    public boolean isBooleanCompatible() {
        return this == BOOLEAN;
    }
    
    /**
     * Indicates whether the current type is compatible with a DOUBLE context type.
     * @return true if compatible, false if not
     */
    public boolean isDoubleCompatible() {
        return this == FLOAT || this == DOUBLE;
    }
    

    /**
     * Indicates whether the current type is compatible with a LONG context type.
     * @return true if compatible, false if not
     */
    public boolean isLongCompatible() {
        switch(this) {
        case BOOLEAN:
        case STRING:
        case DOUBLE:
        case FLOAT:
            return false;
        default:
            return true;
        
        }
    }
    
    /**
     * Indicates whether the current type is compatible with an INTEGER context type.
     * @return true if compatible, false if not
     */
    public boolean isIntegerCompatible() {
        switch(this) {
        case BYTE:
        case INT: 
        case SHORT:  
        case UNSIGNED_BYTE:
        case UNSIGNED_INT:
        case UNSIGNED_SHORT:
            return true;
        default:
            return false;
        
        }
    }

    /**
     * Indicates whether the current type applies to values with variable serialization
     * size.
     * 
     * @return true if variable length, false if fixed
     */
    public boolean isVariableLength() {
        return this.serializationSize == IMetadataKey.VARIABLE_SIZE;
    }
    
    /**
     * Gets the class of a context value with the current type.
     * 
     * @return Class object
     */
    public Class<?> getValueClass() {
        return this.valueClass;
    }
}