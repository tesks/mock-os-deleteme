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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.google.protobuf.InvalidProtocolBufferException;

import jpl.gds.serialization.metadata.Proto3MetadataMap;
import jpl.gds.serialization.primitives.metadata.Proto3MetadataKey;
import jpl.gds.serialization.primitives.metadata.Proto3MetadataMapElement;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.types.Pair;
import jpl.gds.shared.types.UnsignedLong;

/**
 * An interface to be implemented by classes that contain a map of serializable
 * primitives keyed by MetadataContextKey.
 * 
 * @since R8
 */
public interface ISerializableMetadata extends IMetadata<MetadataKey> {
    
	/**
	 * Root XML tag for a context block.
	 */
    public static final String CONTEXT_ROOT_XML_TAG = "context";
    /**
     * XML attribute name for the context type.
     */
    public static final String CONTEXT_TYPE_ATTR_XML_TAG = "type";
    /**
     * XML attribute name for the context ID.
     */
    public static final String CONTEXT_ID_ATTR_XML_TAG = "id";
    /**
     * XML element name for the context key/value pair.
     */
    public static final String CONTEXT_KEYWORD_VALUE_XML_TAG = "keyword_value";
    /**
     * XML attribute name for the context key.
     */
    public static final String CONTEXT_KEY_ATTR_XML_TAG = "key"; 
    /**
     * XML attribute name for the context value.
     */
    public static final String CONTEXT_VALUE_ATTR_XML_TAG = "value"; 
    
    /**
     * Indicates if this metadata object contains a context ID as its only
     * metadata member.
     *  
     * @return true if object contains ID only, false if not
     */
    public boolean isIdOnly();
    
    /**
     * Parse the a metadata value from a single XML element.
     * 
     * @param elementName name of the XML element
     * @param attr SAX attributes of the XML element
     * @throws SAXException if there is a problem parsing the value
     */
    public void parseFromXmlElement(String elementName, Attributes attr) throws SAXException;
    
    /**
     * Adds the context values in the supplied serializable context object
     * to the current context object, less the context ID, which will not be 
     * copied.
     * 
     * @param context the context to copy values from
     */
    public void addContextValues(ISerializableMetadata context);
    
    /**
     * Creates a copy of this context and returns a new instance.
     * 
     * @return new ISerializableMetadata instance
     */
    public ISerializableMetadata copy();
    
    /**
     * Constructs and returns a context key object from the current context ID
     * in the metadata map.
     * 
     * @return IContextKey
     */
    public IContextKey getContextKey();
    
    /**
     * Constructs and returns a set of metadata values as a protobuf message
     * 
     * @return the metadata values as a protobuf message
     */
    public Proto3MetadataMap build();
      
    /**
     * Writes a single context value to a byte array.
     * 
     * @param buff the byte array to write context into
     * @param off the starting offset in the array
     * @param key the MetadataContextKey for the value to be written
     * @param value the context value to be written
     * @return next available offset in the input array
     */
    
    public static Proto3MetadataMapElement keyValueToProto(final MetadataKey key, final Object value){
    	
    	final Proto3MetadataMapElement.Builder entry = Proto3MetadataMapElement.newBuilder();
        final Proto3MetadataKey metaKey = Proto3MetadataKey.valueOf(key.getName());
    	
    	if(metaKey != null){
    		entry.setKey(metaKey);

    		if (value != null) {
    			switch (key.getDataType()) {
    				case BOOLEAN:
    					entry.setBoolValue(((Boolean)value).booleanValue());
    					break;
    				case BYTE:
    					entry.setNumberValue(((Number)value).byteValue());
    					break;
    				case SHORT:
    					entry.setNumberValue(((Number)value).shortValue());
    					break;
    				case INT:
    					entry.setNumberValue(((Number)value).intValue());
    					break;
    				case LONG:
    					entry.setLongValue(((Number)value).longValue());
    					break;
    				case DOUBLE:
    					entry.setDoubleValue(((Number)value).doubleValue());
    					break;
    				case FLOAT:
    					entry.setFloatValue(((Number)value).floatValue());
    					break;
    				case STRING:
    					entry.setStringValue((String)value);
    					break;
    				case UNSIGNED_BYTE:
    					entry.setUnsignedNumberValue(((Number)value).byteValue());
    					break;
    				case UNSIGNED_SHORT:
    					entry.setUnsignedNumberValue(((Number)value).shortValue());
    					break;
    				case UNSIGNED_INT:
    					entry.setUnsignedNumberValue(((Number)value).intValue());
    					break;
    				case UNSIGNED_LONG:
    					entry.setUnsignedLongValue(((Number)value).longValue());
    					break;
    				default:
    					throw new IllegalArgumentException("Unrecognized context key specified for serialization: " + key);

    			}
    		}
    	}
    	return entry.build();
    }
    
    /**
     * Converts a protobuf message to a MetadataKey and Object pair
     * 
     * @param entry the metadata value as a protobuf message
     * @return next available offset in the input array
     * @throws InvalidProtocolBufferException if there is a problem parsing the value
     */
    public static Pair<MetadataKey, Object> keyValueFromProtobuf(final Proto3MetadataMapElement entry) throws InvalidProtocolBufferException {
        
        final MetadataKey key = MetadataKey.valueOf(entry.getKey().name());
        
        if (key == null) {
            throw new InvalidProtocolBufferException("Unrecognized context key value in context: " + entry.getKeyValue() + " - " + entry.getKey().toString());
        }
        
        Object theVal = null;

        switch (key.getDataType()) {
        case BOOLEAN:
            theVal = entry.getBoolValue();
            break;
        case BYTE:
            theVal = Byte.valueOf((byte)entry.getNumberValue());
            break;
        case SHORT:
            theVal = Short.valueOf((short)entry.getNumberValue());
            break;
        case INT:
            theVal = Integer.valueOf(entry.getNumberValue());
            break;
        case LONG:
            theVal = Long.valueOf(entry.getLongValue());
            break;
        case DOUBLE:
            theVal = entry.getDoubleValue();
            break;
        case FLOAT:
            theVal = entry.getFloatValue();
            break;
        case STRING:
            theVal = entry.getStringValue();
            break;
        case UNSIGNED_BYTE:
            theVal = Short.valueOf((short)entry.getUnsignedNumberValue());
            break;
        case UNSIGNED_SHORT:
            theVal = Integer.valueOf(entry.getUnsignedNumberValue());
            break;
        case UNSIGNED_INT:
            theVal = Long.valueOf(entry.getUnsignedNumberValue());
            break;
        case UNSIGNED_LONG:
            theVal = UnsignedLong.valueOf(entry.getUnsignedLongValue());
            break;
        default:
            throw new InvalidProtocolBufferException("Unrecognized context key value in context: " + entry.getKeyValue() + " - " + entry.getKey().toString());
        
        }
        
        return new Pair<>(key, theVal);
               
    }

}
