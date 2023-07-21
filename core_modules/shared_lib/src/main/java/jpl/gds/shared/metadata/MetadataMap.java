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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.google.protobuf.InvalidProtocolBufferException;

import jpl.gds.serialization.metadata.Proto3MetadataMap;
import jpl.gds.serialization.primitives.metadata.Proto3MetadataMapElement;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.metadata.context.ContextKey;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.types.Pair;
import jpl.gds.shared.types.UnsignedLong;

/**
 * A class that implements a serializable metadata map of primitive values keyed
 * using the MetatadataContextKey enumeration.
 * 
 */
public class MetadataMap implements ISerializableMetadata {
    
    private static AtomicInteger nextId = new AtomicInteger(1);

    private static final int KEY_FFIELDS_NO = 6;

    /**
     * Map of metadata keys and values.
     */
    protected SortedMap<MetadataKey, Object> valueMap = new TreeMap<MetadataKey, Object>();
    
    /**
     *  Constructor. Context ID is automatically created.
     */
    public MetadataMap() {
        this((String)null);
    }
    
    @Override
	public boolean isIdOnly() {
    	return this.valueMap.size() == 1;
    }
    
    /**
     *  Constructor. Initializes context ID only.
     *  
     *  @param id Context ID string.
     */
    public MetadataMap(final IContextKey id) {
        valueMap.put(MetadataKey.CONTEXT_ID, id.getContextId());
    }
    
    @Override
	public IContextKey getContextKey() {
    	final ContextKey key = new ContextKey();
    	final String id = getContextId();
    	final String[] idPieces = id.split(IContextKey.ID_SEPARATOR);
    	if (idPieces.length != KEY_FFIELDS_NO) {
    		throw new IllegalStateException("Trying to create context key from context id " + id + " but it does not have the right components");
    	}
    	key.setNumber(Long.valueOf(idPieces[0]));
    	key.setHost(idPieces[1]);
    	key.setHostId(Integer.valueOf(idPieces[2]));
    	key.setFragment(Integer.valueOf(idPieces[3]));
    	key.setParentNumber(Long.valueOf(idPieces[4]));
    	key.setParentHostId(Integer.valueOf(idPieces[5]));
    	return key;
    }
    
    /**
     *  Constructor. Initializes context ID only.
     *  
     *  @param id Context ID string.
     */
    public MetadataMap(String id) {
        if (id == null) {
            id = String.valueOf(nextId.getAndIncrement() + IContextKey.ID_SEPARATOR + GdsSystemProperties.getIntegerPid());
        }
        valueMap.put(MetadataKey.CONTEXT_ID, id);
    }

    @Override
    public void setTemplateContext(final Map<String, Object> map) {
        for (final MetadataKey keyword :  MetadataKey.values()) {
            if (valueMap.get(keyword) != null) {
                map.put(keyword.getTemplateVariable(), valueMap.get(keyword).toString());
            }
        }
        
    }

    @Override
    public String getContextId() {
        return (String) valueMap.get(MetadataKey.CONTEXT_ID);
    }
   

    @Override
    public void load(final Proto3MetadataMap values) throws InvalidProtocolBufferException {
    	valueMap.clear();
    	
    	for(final Proto3MetadataMapElement entry : values.getMapEntryList()){
    		final Pair<MetadataKey, Object> toWrite = ISerializableMetadata.keyValueFromProtobuf(entry);
    		setValue(toWrite.getOne(), toWrite.getTwo());
    	}
    }
    

    @Override
    public int fromContextBinary(final byte[] content,
                                 final int inOffset)
            throws IOException {
    	final ByteArrayInputStream bais = new ByteArrayInputStream(content);
    	bais.skip(inOffset);
    	
    	Proto3MetadataMap values;
        values = Proto3MetadataMap.parseDelimitedFrom(bais);
        load(values);
    	
    	return (content.length - bais.available());
    	
    }
    
    @Override
    public byte[] toContextBinary(){
    	final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
			build().writeDelimitedTo(baos);
		} catch (final IOException e) {
			TraceManager.getDefaultTracer().warn(e.getMessage());
			return null;
		}
        return baos.toByteArray();
    }
    
    @Override
    public Proto3MetadataMap build(){
    	final Proto3MetadataMap.Builder retVal = Proto3MetadataMap.newBuilder();
    	
    	for(final Entry<MetadataKey, Object> mapEntry : valueMap.entrySet()){
    		
    		final Proto3MetadataMapElement entry = ISerializableMetadata.keyValueToProto(mapEntry.getKey(), mapEntry.getValue());
    		
    		if(entry != null){
    			retVal.addMapEntry(entry);
    		}
    	}
    	return retVal.build();
    }
    
    @Override
    public void toContextXml(final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(CONTEXT_ROOT_XML_TAG);
        writer.writeAttribute(CONTEXT_ID_ATTR_XML_TAG, getContextId());
        
        for (final MetadataKey keyword :  MetadataKey.values()) {
            if (keyword == MetadataKey.CONTEXT_ID) {
                continue;
            }
            if (valueMap.get(keyword) == null) {
                continue;
            }
            writer.writeStartElement(CONTEXT_KEYWORD_VALUE_XML_TAG);
            writer.writeAttribute(CONTEXT_KEY_ATTR_XML_TAG, keyword.toString());
            writer.writeAttribute(CONTEXT_VALUE_ATTR_XML_TAG, valueMap.get(keyword).toString());
            writer.writeEndElement();
        }
        
        writer.writeEndElement();
        
    }

    @Override
    public void setValue(final MetadataKey key, final Object val) throws ClassCastException {
        if (val == null) {
            this.valueMap.remove(key);
            return;
        }
        Object realVal = null;
        
        if (val.getClass().equals(key.getDataType().getValueClass())) {
            realVal = val;  
        } else if (key.getDataType() == MetadataDataType.STRING) {
            realVal = val.toString();
        } else {
            try {
                realVal = key.getDataType().getValueClass().cast(val);
            } catch (final ClassCastException e) {
                switch(key.getDataType()) {

                case BOOLEAN:
                    if (val instanceof String) {
                        realVal = Boolean.valueOf((String)val);
                    } else {
                        realVal = Boolean.valueOf((boolean)val);
                    }
                    break;
                case BYTE:
                    if (val instanceof String) {
                        realVal = Byte.valueOf((String)val);
                    } else {
                        realVal = Byte.valueOf(((Number)val).byteValue());
                    }
                    break;
                case DOUBLE:
                    if (val instanceof String) {
                        realVal = Double.valueOf((String)val);
                    } else {
                        realVal = Double.valueOf(((Number)val).doubleValue());
                    }
                    break;
                case FLOAT:
                    if (val instanceof String) {
                        realVal = Double.valueOf((String)val);
                    } else {
                        realVal = Float.valueOf(((Number)val).floatValue());
                    }
                    break;
                case INT:
                case UNSIGNED_SHORT:
                    if (val instanceof String) {
                        realVal = Integer.valueOf((String)val);
                    } else {
                        realVal = Integer.valueOf(((Number)val).intValue());
                    }
                    break;
                case UNSIGNED_BYTE:
                case SHORT:
                    if (val instanceof String) {
                        realVal = Short.valueOf((String)val);
                    } else {
                        realVal = Short.valueOf(((Number)val).shortValue());
                    }
                    break;
                case LONG:
                case UNSIGNED_INT:
                    if (val instanceof String) {
                        realVal = Long.valueOf((String)val);
                    } else {
                        realVal = Long.valueOf(((Number)val).longValue());
                    }
                    break;
                case UNSIGNED_LONG:
                    if (val instanceof String) {
                        realVal = UnsignedLong.valueOf((String)val);
                    } else {
                        realVal = UnsignedLong.valueOf(((Number)val).longValue());
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Class of context value for " + key + " cannot be cast or converted to " + key.getDataType().getClass());

                }

            }
        }
        this.valueMap.put(key, realVal);
    }
    
    @Override
    public void parseFromXmlElement(final String elementName, final Attributes attr)
            throws SAXException {
        if (elementName.equals(ISerializableMetadata.CONTEXT_ROOT_XML_TAG)) {
            valueMap.clear();
            final String id = attr.getValue(ISerializableMetadata.CONTEXT_ID_ATTR_XML_TAG);
            valueMap.put(MetadataKey.CONTEXT_ID, id);
           
        } else if (elementName.equals(ISerializableMetadata.CONTEXT_KEYWORD_VALUE_XML_TAG)) {
            final String keyStr = attr.getValue(ISerializableMetadata.CONTEXT_KEY_ATTR_XML_TAG);
            final String val = attr.getValue(ISerializableMetadata.CONTEXT_VALUE_ATTR_XML_TAG);
            try {
                final MetadataKey key = MetadataKey.valueOf(keyStr); 
                setValue(key, val);
            } catch (final IllegalArgumentException e) {
                e.printStackTrace();
                throw new SAXException("Unrecognized context key in XML context: " + keyStr, e);
            }
        }
        
    }
    
    @Override
    public boolean containsKey(final MetadataKey key) {
        return valueMap.get(key) != null;
    }
    
    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof MetadataMap)) {
            return false;
        }
        return getContextId().equals(((MetadataMap)o).getContextId());
    }

    @Override
    public int hashCode() {
        return getContextId().hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder();
        for (final Entry<MetadataKey, Object> entry : valueMap.entrySet()) {
            if (entry.getValue() != null) {
                s.append(entry.getKey() + "=" + entry.getValue().toString() + "\n");
            }
        }
        return s.toString();
    }

    @Override
    public String getValueAsString(final MetadataKey key, final String defaultValue) {
        final Object o = getValue(key, defaultValue);
        if (o == null) {
            return null;
        }
        return o.toString();
    }
    
    @Override
    public String getRequiredValueAsString(final MetadataKey key) throws InvalidMetadataException {
        final Object o = getRequiredValue(key);
        return o.toString();
    }

    @Override
    public Long getValueAsLong(final MetadataKey key, final Long defaultValue) {
        if (key.getDataType().isLongCompatible()) {
            
            final Number temp = ((Number)getValue(key, defaultValue));
            if (temp != null) {
                return temp.longValue();
            }
            return null;
        }
        throw new IllegalStateException("Attempting to fetch context value for key " + key + " as a long, but the value is not compatible");
    }
    
    @Override
    public Long getRequiredValueAsLong(final MetadataKey key) throws InvalidMetadataException {
        if (key.getDataType().isLongCompatible()) {
            return ((Number)getRequiredValue(key)).longValue();
        }
        throw new IllegalStateException("Attempting to fetch context value for key " + key + " as a long, but the value is not compatible");
    }
    
    @Override
    public Integer getValueAsInteger(final MetadataKey key, final Integer defaultValue) {
        if (key.getDataType().isIntegerCompatible()) {
            final Number temp = ((Number)getValue(key, defaultValue));
            if (temp != null) {
                return temp.intValue();
            }
            return null;
        }
        throw new IllegalStateException("Attempting to fetch context value for key " + key + " as a long, but the value is not compatible");
    }
    
    @Override
    public Integer getRequiredValueAsInteger(final MetadataKey key) throws InvalidMetadataException {
        if (key.getDataType().isIntegerCompatible()) {
            return ((Number)getRequiredValue(key)).intValue();
        }
        throw new IllegalStateException("Attempting to fetch context value for key " + key + " as a long, but the value is not compatible");
    }
    

    @Override
    public Double getValueAsDouble(final MetadataKey key, final Double defaultValue) {
        if (key.getDataType().isDoubleCompatible()) {
            final Number temp = ((Number)getValue(key, defaultValue));
            if (temp != null) {
                return temp.doubleValue();
            }
            return null;
        }
        throw new IllegalStateException("Attempting to fetch context value for key " + key + " as a double, but the value is not compatible");
    }
    
    @Override
    public Double getRequiredValueAsDouble(final MetadataKey key) throws InvalidMetadataException {
        if (key.getDataType().isDoubleCompatible()) {
            return ((Number)getRequiredValue(key)).doubleValue();
        }
        throw new IllegalStateException("Attempting to fetch context value for key " + key + " as a double, but the value is not compatible");
    }

    @Override
    public Boolean getValueAsBoolean(final MetadataKey key, final Boolean defaultValue) {
        if (key.getDataType().isBooleanCompatible()) {
            return ((Boolean)getValue(key, defaultValue));
        }
        throw new IllegalStateException("Attempting to fetch context value for key " + key + " as a boolean, but the value is not compatible");
    }
    
    @Override
    public Boolean getRequiredValueAsBoolean(final MetadataKey key) throws InvalidMetadataException {
        if (key.getDataType().isBooleanCompatible()) {
            return ((Boolean)getRequiredValue(key));
        }
        throw new IllegalStateException("Attempting to fetch context value for key " + key + " as a boolean, but the value is not compatible");
    }

    @Override
    public Object getValue(final MetadataKey key, final Object defaultValue) {
        final Object o = this.valueMap.get(key);
        return (o == null ? defaultValue : o);
    }
    
    @Override
    public Object getRequiredValue(final MetadataKey key) throws InvalidMetadataException {
        final Object o = this.valueMap.get(key);
        if (o == null) {
            throw new InvalidMetadataException("Required value for key " + key + " is not present in the context");
        }
        return o;
    }

    @Override
    public <T extends Enum<T>> T getValueAsEnum(final MetadataKey key, final Class<T> enumClass,
            final T defaultValue) {
        final String s = (String) getValue(key, defaultValue);
        return enumClass.cast(Enum.valueOf(enumClass, s));
    }

    @Override
    public <T extends Enum<T>> T getRequiredValueAsEnum(final MetadataKey key, final Class<T> enumClass)
            throws InvalidMetadataException {
        final String s = (String) getRequiredValue(key);
        return enumClass.cast(Enum.valueOf(enumClass, s));
    }

    @Override
    public Set<MetadataKey> getKeys() {
        return new TreeSet<MetadataKey>(valueMap.keySet());
    }

    @Override
    public Map<MetadataKey, Object> getMap() {
        return Collections.unmodifiableSortedMap(this.valueMap);
    }

    @Override
    public void addContextValues(final ISerializableMetadata context) {
        if (context == null) {
            return;
        }

        for (final Entry<MetadataKey, Object> entry : context.getMap().entrySet()) {
              if (entry.getKey() != MetadataKey.CONTEXT_ID) {
                  this.valueMap.put(entry.getKey(), entry.getValue());
              }
        }
       
    }

    @Override
    public ISerializableMetadata copy() {
        final ISerializableMetadata result = new MetadataMap(this.getContextId());
        result.addContextValues(this);
        return result;
    }
   
}
