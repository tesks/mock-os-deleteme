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

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * This class implements a map of key-value attributes. It is a container
 * to store project specific information attached to dictionary objects.
 * 
 * <p>
 * <p><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <p>
 * <p>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <p><p>
 * 
 * @see IAttributesSupport
 * 
 */

public class KeyValueAttributes {
    
    /* Make this map sorted for predictable results with Java 8.*/

	private Map<String,String> attributeMap = new TreeMap<String, String>();

	/**
	 * Sets a key-value pair into the attribute map.
	 * 
	 * @param key  the attribute name
	 * @param value  the attribute value
	 */
	public void setKeyValue(String key, String value) {
		attributeMap.put(key, value);
	}

	/**
	 * Retrieves an attribute value for the specified key.
	 * 
	 * @param key  the attribute name
	 * @return the attribute value to which the specified key is mapped
	 */
	public String getValueForKey(String key) {
		return attributeMap.get(key);
	}

	/**
	 * Retrieves the set of keys from the key-value attribute map.
	 * 
	 * @return the sets of keys in the map.
	 */
	public Set<String> getKeys() {
		return attributeMap.keySet();
	}
	
	/**
	 * Makes a copy of the key-value attribute map and assigns
	 * it as the new map for this object.
	 * 
	 * @param attr  the attribute map to copy from
	 */
	public void copyFrom(KeyValueAttributes attr) {
		attributeMap = new TreeMap<String, String> (attr.attributeMap);
	}
	
	/**
	 * Returns true if the key-value attribute map is empty.
	 * @return true if the key-value attribute map contains no key-value mappings
	 */
	public boolean isEmpty() {
		return attributeMap.isEmpty();
	}
	
	/**
	 * Clears the contents of the key-value attribute map.
	 */
	public void clearKeyValue() {
		attributeMap.clear();
	}

}
