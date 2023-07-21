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

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.google.protobuf.InvalidProtocolBufferException;

import jpl.gds.serialization.metadata.Proto3MetadataMap;
import jpl.gds.shared.template.Templatable;

/**
 * An interface to be implemented by all context objects. This basic interface
 * assumes that the context supports a map of values with the data types defined
 * by MetadataDataType, keyed by an IMetadataKey. It includes serialization
 * interfaces, but the implementations are defaulted so context classes that are
 * not serialized do not have to implement them.
 * 
 *
 * @param <U>
 *            the key class, any type that extends IMetadataKey
 * 
 */
public interface IMetadata<U extends IMetadataKey> extends Templatable {

    /**
     * Gets the unique identifier of this context object
     * 
     * @return string identifier
     */
    public String getContextId();

    /**
     * Populates the context object by deserializing content from an array of
     * bytes.
     * 
     * @param content
     *            the byte array containing context content
     * @param inOffset
     *            the starting offset of the context data in the array
     * @return the new offset, i.e., the offset in the array immediately
     *         following the context data
     * @throws IOException
     *             if an error occurs during parsing
     */
    public default int fromContextBinary(final byte[] content, final int inOffset) throws IOException {
        return inOffset;
    }

    /**
     * Returns the contents of the context object in serialized binary format.
     * 
     * @return byte array containing serialized content
     * 
     * @throws UnsupportedOperationException
     *             if the current class does not support binary serialization
     */
    public default byte[] toContextBinary() {
        throw new UnsupportedOperationException("Binary serialization is not supported by this class");
    }

    /**
     * Serializes the current contents of the context object to the given
     * XMLStreamWriter.
     * 
     * @param writer
     *            the writer to write XML contents to
     * @throws XMLStreamException
     *             if there is a problem writing the XML
     * @throws UnsupportedOperationException
     *             if the current class does not support XML serialization
     */
    public default void toContextXml(final XMLStreamWriter writer)
            throws XMLStreamException, UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "XML serialization is not supported by this class");
    }

    /**
     * Gets a context value as a string.
     * 
     * @param key
     *            the IMetadataKey for the value to fetch
     * @param defaultValue
     *            the value to be returned if the requested value is not present
     *            in the context
     * @return context value, or the defaultValue
     */
    public String getValueAsString(U key, String defaultValue);

    /**
     * Gets a context value as a string and throws if it is not present in the
     * context.
     * 
     * @param key
     *            the IMetadataKey for the value to fetch
     * @return context value
     * @throws InvalidMetadataException
     *             if the value is not present
     */
    public String getRequiredValueAsString(U key)
            throws InvalidMetadataException;

    /**
     * Gets a context value as a Long.
     * 
     * @param key
     *            the IMetadataKey for the value to fetch
     * @param defaultValue
     *            the value to be returned if the requested value is not present
     *            in the context
     * @return context value, or the defaultValue
     * 
     * @throws IllegalStateException
     *             if the value is not compatible with Long
     */
    public Long getValueAsLong(U key, Long defaultValue)
            throws IllegalStateException;

    /**
     * Gets a context value as a Long and throws if it is not present in the
     * context.
     * 
     * @param key
     *            the IMetadataKey for the value to fetch
     * @return context value
     * @throws InvalidMetadataException
     *             if the value is not present
     * @throws IllegalStateException
     *             if the value is not compatible with Long
     */
    public Long getRequiredValueAsLong(U key) throws InvalidMetadataException,
            IllegalStateException;

    /**
     * Gets a context value as an Integer.
     * 
     * @param key
     *            the IMetadataKey for the value to fetch
     * @param defaultValue
     *            the value to be returned if the requested value is not present
     *            in the context
     * @return context value, or the defaultValue
     * @throws IllegalStateException
     *             if the value is not compatible with Integer
     */
    public Integer getValueAsInteger(U key, Integer defaultValue)
            throws IllegalStateException;

    /**
     * Gets a context value as an Integer and throws if it is not present in the
     * context.
     * 
     * @param key
     *            the IMetadataKey for the value to fetch
     * @return context value
     * @throws InvalidMetadataException
     *             if the value is not present
     * @throws IllegalStateException
     *             if the value is not compatible with Integer
     */
    public Integer getRequiredValueAsInteger(U key)
            throws InvalidMetadataException, IllegalStateException;

    /**
     * Gets a context value as a Double.
     * 
     * @param key
     *            the IMetadataKey for the value to fetch
     * @param defaultValue
     *            the value to be returned if the requested value is not present
     *            in the context
     * @return context value, or the defaultValue
     * @throws IllegalStateException
     *             if the value is not compatible with Double
     */
    public Double getValueAsDouble(U key, Double defaultValue)
            throws IllegalStateException;

    /**
     * Gets a context value as a Double and throws if it is not present in the
     * context.
     * 
     * @param key
     *            the IMetadataKey for the value to fetch
     * @return context value
     * @throws InvalidMetadataException
     *             if the value is not present
     * @throws IllegalStateException
     *             if the value is not compatible with Double
     */
    public Double getRequiredValueAsDouble(U key)
            throws InvalidMetadataException, IllegalStateException;

    /**
     * Gets a context value as a Boolean.
     * 
     * @param key
     *            the IMetadataKey for the value to fetch
     * @param defaultValue
     *            the value to be returned if the requested value is not present
     *            in the context
     * @return context value, or the defaultValue
     * @throws IllegalStateException
     *             if the value is not compatible with Boolean
     */
    public Boolean getValueAsBoolean(U key, Boolean defaultValue)
            throws IllegalStateException;

    /**
     * Gets a context value as a Boolean and throws if it is not present in the
     * context.
     * 
     * @param key
     *            the IMetadataKey for the value to fetch
     * @return context value
     * @throws InvalidMetadataException
     *             if the value is not present
     * @throws IllegalStateException
     *             if the value is not compatible with Boolean
     */
    public Boolean getRequiredValueAsBoolean(U key)
            throws InvalidMetadataException, IllegalStateException;

    /**
     * Gets a context value as a an enum value of the specified enum class.
     * 
     * @param <T>
     *            an enum class
     * @param key
     *            the IMetadataKey for the value to fetch
     * @param enumClass
     *            the expected enum class of the value
     * @param defaultValue
     *            the value to be returned if the requested value is not present
     *            in the context
     * @return context value, or the defaultValue
     * @throws ClassCastException
     *             if the value is not compatible with the supplied enum class
     */
    public <T extends Enum<T>> T getValueAsEnum(U key, Class<T> enumClass,
            T defaultValue) throws ClassCastException;

    /**
     * Gets a context value as a an enum value of the specified enum class and
     * throws if it is not present in the context.
     * 
     * @param <T>
     *            an enum class
     * @param key
     *            the IMetadataKey for the value to fetch
     * @param enumClass
     *            the expected enum class of the value
     * @return context value, or the defaultValue
     * @throws InvalidMetadataException
     *             if the value is not present
     * @throws ClassCastException
     *             if the value is not compatible with the supplied enum class
     */
    public <T extends Enum<T>> T getRequiredValueAsEnum(U key,
            Class<T> enumClass) throws InvalidMetadataException;

    /**
     * Gets a context value as an Object.
     * 
     * @param key
     *            the IMetadataKey for the value to fetch
     * @param defaultValue
     *            the value to be returned if the requested value is not present
     *            in the context
     * @return context value, or the defaultValue
     */
    public Object getValue(U key, Object defaultValue);

    /**
     * Gets a context value as an Object and throws if it is not present in the
     * context.
     * 
     * @param key
     *            the IMetadataKey for the value to fetch
     * @return context value
     * @throws InvalidMetadataException
     *             if the value is not present
     */
    public Object getRequiredValue(U key) throws InvalidMetadataException;

    /**
     * Sets a context value. Will overwrite any previous value for the same key.
     * 
     * @param key
     *            the IMetadataKey for the value
     * @param val
     *            the value to set
     */
    public void setValue(U key, Object val);

    /**
     * Indicates if the request value is in the context.
     * 
     * @param key
     *            the IMetadataKey for the value to look for
     * @return true if present, false if not
     */
    public boolean containsKey(U key);

    /**
     * Gets the whole set of context values as a map.
     * 
     * @return Map of IMetadataKey to value
     */
    public Map<U, Object> getMap();

    /**
     * Gets the set of the context keys present in the context.
     * 
     * @return Set of IMetadataKey, never null
     */
    public Set<U> getKeys();

    /**
     * Restores the values in the provided protobuf message to this IMetadata
     * object
     * 
     * @param values
     *            the protobuf to be restored
     * @throws InvalidProtocolBufferException
     *             an error occurred while reading the protobuf message
     */
    void load(Proto3MetadataMap values) throws InvalidProtocolBufferException;

}
