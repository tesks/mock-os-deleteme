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
package jpl.gds.evr.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jpl.gds.shared.types.Pair;
import jpl.gds.serialization.evr.Proto3EvrMetadata;
import jpl.gds.serialization.evr.Proto3EvrMetadataElement;

/**
 * Holds EVR metadata keys and values. Used for interacting with EVR metadata in
 * the database.
 * 
 * class is serializable so we can store metadata in the global LAD
 */
public class EvrMetadata extends Object implements Serializable{
    
    private static final long serialVersionUID = -8612845734536824176L;
    private final List<Metadata> _metadata = new ArrayList<Metadata>();
    private final PairComparator _comparator = new PairComparator();

    /**
     * Constructor.
     */
    public EvrMetadata() {
        super();
    }

    /**
     * Copy constructor.
     * 
     * @param other
     *            Another EvrMetadata object to copy content from.
     */
    public EvrMetadata(final EvrMetadata other) {
        super();

        _metadata.addAll(other._metadata);
    }
    
    /**
     * Protobuf Constructor
     * @param msg an EvrMetadata protobuf message
     */
    public EvrMetadata(Proto3EvrMetadata msg){
        load(msg);
    }

    /**
     * Determines if this object is empty of metadata values.
     * 
     * @return True if empty
     */
    public boolean isEmpty() {
        return _metadata.isEmpty();
    }

    /**
     * Retrieves the number of metadata values in this object.
     * 
     * @return Count of metadata values
     */
    public int size() {
        return _metadata.size();
    }

    /**
     * Remove all metadata values from this object.
     */
    public void clear() {
        _metadata.clear();
    }

    /**
     * Sets the content of this metadata object from another one, clearing
     * existing contents first.
     * 
     * @param other
     *            EVR meta data
     */
    public void set(final EvrMetadata other) {
        _metadata.clear();

        _metadata.addAll(other._metadata);
    }

    /**
     * Add new key/value pair. Any previous value with the same key name will be
     * overwritten.
     * 
     * @param key
     *            the metadata key enumerated value
     * @param value
     *            the value of the metadata item
     */
    public void addKeyValue(final EvrMetadataKeywordEnum key, final String value) {
        _metadata.add(new Metadata(key, value));
    }

    /**
     * Add new key/value pair. Any previous value with the same key name will be
     * overwritten.
     * 
     * @param key
     *            the metadata key name
     * @param value
     *            the value of the metadata item
     */
    public void addKeyValue(final String key, final String value) {
        _metadata.add(new Metadata(EvrMetadataKeywordEnum.convert(key), value));
    }

    /**
     * Return a list of keys and values in string form, sorted by key name.
     * 
     * @return List of pairs of strings
     */
    public List<Pair<EvrMetadataKeywordEnum, String>> asStrings() {
        final List<Pair<EvrMetadataKeywordEnum, String>> result = new ArrayList<Pair<EvrMetadataKeywordEnum, String>>(
                _metadata.size());

        for (final Metadata m : _metadata) {
            result.add(new Pair<EvrMetadataKeywordEnum, String>(m.getKey(), m
                    .getValue()));
        }

        Collections.sort(result, _comparator);

        return result;
    }
    
    /**
     * Return a list of key/value Pairs in string form, not sorted.
     * 
     * @return List of pairs of strings
     * 
     */
    public List<Pair<EvrMetadataKeywordEnum, String>> asUnsortedStrings() {
        final List<Pair<EvrMetadataKeywordEnum, String>> result = new ArrayList<Pair<EvrMetadataKeywordEnum, String>>(
                _metadata.size());

        for (final Metadata m : _metadata) {
            result.add(new Pair<EvrMetadataKeywordEnum, String>(m.getKey(), m
                    .getValue()));
        }

        return result;
    }

    /**
     * Look up value for key.
     * 
     * @param key
     *            the metadata keyword enum
     * 
     * @return First value for key or null if the key does not exist
     */
    public String getMetadataValue(final EvrMetadataKeywordEnum key) {
        final EvrMetadataKeywordEnum useKey = (key != null) ? key
                : EvrMetadataKeywordEnum.UNKNOWN;

        for (final Metadata m : _metadata) {
            if (m.getKey() == useKey) {
                return m.getValue();
            }
        }

        return null;
    }

    @Override
    /**
     * @return Hash code
     */
    public int hashCode() {
        return _metadata.hashCode();
    }

    @Override
    /**
     * @return True if equals
     */
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof EvrMetadata)) {
            return false;
        }

        final EvrMetadata other = (EvrMetadata) o;

        return _metadata.equals(other._metadata);
    }

    /**
     * Holds a pair of key and value.
     */
    @SuppressWarnings("serial")
    private static class Metadata extends Pair<EvrMetadataKeywordEnum, String> {
        /**
         * Constructor.
         * 
         * @param key
         *            the metadata keyword enum
         * @param value
         *            the value for the metadata item
         */
        public Metadata(final EvrMetadataKeywordEnum key, final String value) {
            super((key != null) ? key : EvrMetadataKeywordEnum.UNKNOWN,
                    (value != null) ? value : "");
        }

        /**
         * Gets the key name.
         * 
         * @return Key
         */
        public EvrMetadataKeywordEnum getKey() {
            return getOne();
        }

        /**
         * Gets the value.
         * 
         * @return Value
         */
        public String getValue() {
            return getTwo();
        }
    }


    /**
     * Comparator to use when sorting results. We know values are non-null.
     */
    private static class PairComparator extends Object implements
            Comparator<Pair<EvrMetadataKeywordEnum, String>>, Serializable
    {
		private static final long serialVersionUID = 1L;


		/**
         * {@inheritDoc}
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare(final Pair<EvrMetadataKeywordEnum, String> o1,
                final Pair<EvrMetadataKeywordEnum, String> o2) {
            if (o1 == o2) {
                return 0;
            }

            // Compare as strings

            final int compareOne = o1.getOne().toString()
                    .compareTo(o2.getOne().toString());

            if (compareOne != 0) {
                return compareOne;
            }

            return o1.getTwo().compareTo(o2.getTwo());
        }


        @Override
        public boolean equals(final Object o) {
            return (this == o);
        }



        @Override
        public int hashCode()
        {
            return 0;
        }
    }
    
    /**
     * Convert this EvrMetadata into a protobuf message
     * 
     * @return this EvrMetadata as a protobuf message
     */
    public Proto3EvrMetadata build(){
    	Proto3EvrMetadata.Builder retVal = Proto3EvrMetadata.newBuilder();
    	for(Metadata element : this._metadata){
    		Proto3EvrMetadataElement.Builder entry = Proto3EvrMetadataElement.newBuilder();
    		entry.setKeyValue(element.getKey().ordinal());
    		entry.setValue(element.getValue());
    		retVal.addMetadataEntry(entry);
    	}
    	return retVal.build();
    }
    
    /**
     * Populate this EvrMetadata with the values from a protobuf message
     * 
     * @param msg
     *            an EvrMetadata object in protobuf message format
     */
    public void load(Proto3EvrMetadata msg){
    	this._metadata.clear();
    	
    	for(Proto3EvrMetadataElement entry : msg.getMetadataEntryList()){
    		Metadata element = new Metadata(EvrMetadataKeywordEnum.convert(entry.getKey().toString()), entry.getValue());
    		
    		this._metadata.add(element);
    	}
    }
}
