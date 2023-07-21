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


/**
 * An interface used by classes that serve as metadata keys in a metadata primitive
 * map. Not meant to apply to context keys that are associated with non-primitive
 * context values.
 * 
 */
public interface IMetadataKey {
    
    /**
     * The value used to indicate a variable serialization size for the
     * associated context value.
     */
    public static final int VARIABLE_SIZE = -1;

    /**
     * The name of this key. All keys in a given context must have unique
     * names.
     * 
     * @return key name
     */
    public String getName();
    
    /**
     * The serialization index associated with this key.  Used in 
     * binary serialization. All keys in a given context must have unique
     * serialization keys. Context keys for non-serialized objects 
     * may return 0. No context key may exceed 16 bit precision.
     * 
     * @return unique serialization key
     */
    default public short getSerializationKey() {
        return 0;
    }
    
    /**
     * Gets the name of the velocity template variable associated with this
     * key. 
     * 
     * @return template variable name; may be null
     */
    default public String getTemplateVariable() {
        return null;
    }
    
    /**
     * Gets the data type of the value associated with this context key.
     * 
     * @return MetadataDataType; may not be null
     */
    public MetadataDataType getDataType();
}
