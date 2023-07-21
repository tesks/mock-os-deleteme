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
package jpl.gds.dictionary.api;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import jpl.gds.shared.log.TraceManager;

/**
 * EnumerationDefinition is used to define a named lookup table that maps
 * integer values to string values. <br>
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <p>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <p>
 * <p>
 * 
 * EnumerationDefinition is used in several dictionary implementations where a
 * map of integer value (called the key or state value) to string state is
 * required. For instance, status telemetry channels map a raw DN number to a
 * symbolic state string for display. Each EnumerationDefinition also has a
 * name, which should be unique within a dictionary. This allows the re-use of
 * enumerations within the dictionaries, so care should be taken to assign a
 * unique name to each separate enumeration constructed by a given dictionary
 * parser.
 * <p>
 *
 * 
 */
public class EnumerationDefinition {
	
	/**
	 * The name of the enumeration.
	 */
    private String name; 
    
    /**
     * Description of the enumeration (optional)
     */
    private String description;
    
    /**
     * Map of long keys to string values.
     * 
     * Make this map sorted for predictable results with Java 8.
     */
    private final Map<Long, String> valueMap = new TreeMap<Long, String>();

    /**
     * Creates an instance of EnumerationDefinition with the given name.
     * 
     * @param enumName the name of the enumeration; should be non-null and unique within
     * a dictionary
     */
    public EnumerationDefinition(final String enumName) {
        name = enumName;
    }

	/**
	 * Adds a key-value pair to the enumeration.
	 * 
	 * @param key
	 *            the long key
	 * @param value
	 *            the symbolic string value
	 * 
	 *
	 * @since AMPCS R6
	 */
    public synchronized void addValue(final Long key, final String value) {
        valueMap.put(key, value);
    }
    
    /**
     * Adds a key-value pair to the enumeration.
     * 
     * @param key the key
     * @param value the symbolic string value
     */
    public synchronized void addValue(final Integer key, final String value) {
        valueMap.put(Long.valueOf(key), value);
    }

    /**
     * Gets the name of this enumeration.
     * 
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this enumeration.
     *
     * @param name the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets the optional description of this enumeration.
     * 
     * @return Returns the description; may be null
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the optional description of this enumeration.
     *
     * @param description the description to set
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * Gets the symbolic string value for the given numeric key.
     * 
     * @param key the long key
     * @return string value that maps to the long, or null if no mapping found
     */
    public String getValue(final long key) {
        return valueMap.get(key);
    }
    
    /**
     * Gets the key for the given symbolic string.
     *
     * @param symbol the string to find
     * @return the long key, or -1 if no such symbol exists in the enumeration
     */
    public synchronized long getKey (final String symbol) {
        Iterator<Long> myVeryOwnIterator = valueMap.keySet().iterator();
        long aKey = 0;

        if ( null != symbol && 0 < symbol.length() ) {
        	while(myVeryOwnIterator.hasNext()) {
        		aKey = myVeryOwnIterator.next();
        		if ( symbol.equalsIgnoreCase ( valueMap.get ( aKey ) ) )
        		{
        			return aKey;
        		}
        	} // end while

        }
        TraceManager.getDefaultTracer().error("Symbol: " + symbol + " not found as enumeration type value -1 returned.");
        return -1;
    } 
    
    /**
     * Gets the minimum key value from the table.
     * 
     * @return the minimum numeric key, or Long.MAX_VALUE if no entries defined
     */
    public synchronized long getMinValue() {
    	Set<Long> vals = valueMap.keySet();
    	long min = Long.MAX_VALUE;
    	for (Long curVal: vals) {
    		if (curVal < min) {
    			min = curVal;
    		}
    	}
    	return min;
    }
    
    /**
     * Gets a Map of all entries in the table (numeric key to string state value)
     * in a sorted Map. Sort order is by numeric key.
     * 
     * @return Map of key (long) to symbol (string)
     */
    public synchronized SortedMap<Long,String> getAllAsSortedMap() {
    	SortedMap<Long,String> sortedVals = new TreeMap<Long,String>(valueMap);
    	return Collections.unmodifiableSortedMap(sortedVals);  	
    }
    
    /**
     * Gets the maximum key value from the table.
     * 
     * @return the maximum numeric key, or Long.MIN_VALUE if no entries defined
     */
    public synchronized long getMaxValue() {
    	Set<Long> vals = valueMap.keySet();
    	long max = Long.MIN_VALUE;
    	for (Long curVal: vals) {
    		if (curVal > max) {
    			max = curVal;
    		}
    	}
    	return max;
    }
    
    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
	public synchronized String toString() {
    	Set<Long> keys = valueMap.keySet();
    	Long[] keyInts = new Long[keys.size()];
    	keys.toArray(keyInts);
        java.util.Arrays.sort(keyInts);
    	StringBuilder text = new StringBuilder();
    	for (Long key : keyInts) {
    		String value = valueMap.get(key);
    		String aLine = String.format("% 8d\t- %s", new Object[] {key, value});
    		text.append(aLine + "\n");
    	}
    	return text.toString();
    }

}
