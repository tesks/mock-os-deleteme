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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class tracks information categories attached to dictionary objects, such
 * as associated flight module or subsystem. Standard category names recognized
 * by AMPCS are defined as constants in the ICategorySupport interface. Some
 * dictionary schemas allow custom categories to be defined as well, though
 * these are ignored by AMPCS.
 * 
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <p>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <p>
 * <p>
 * 
 * @see ICategorySupport
 * 
 */

public class Categories {
	private Map<String, String> categoryMap = new HashMap<String, String>();
	

	/**
	 * Constructor.
	 * 
	 */
	public Categories() {
	    // do nothing
	}
	
	/**
	 * Copy Constructor
	 * @param copy Categories object to copy
	 */
	public Categories(final Categories copy) {
	    categoryMap.putAll(copy.categoryMap);
	}
	/**
	 * Retrieves the set of keys (category names) from the category map.
	 *
	 * @return sets of keys in the map
	 */	
	public Set <String> getKeys() {
		return categoryMap.keySet();
	}
	
	/**
	 * Indicates whether the category map is empty.
	 *  
	 * @return true if the category map is empty
	 */
	public boolean isEmpty() {
		return categoryMap.isEmpty();
	}
	
	/**
	 * Sets a category name and value pair. Any previous value
	 * with the same name will be overwritten.
	 * 
	 * @param name  the category name
	 * @param value  the category value
	 */	 
	public void setCategory(final String name, final String value) {
		categoryMap.put(name, value);
	}
	
	/**
	 * Retrieves the category value given the category name.
	 * 
	 * @param name  the category name
	 *
	 * @return the category value associated with the category name
	 */

	public String getCategory(final String name) {
		return categoryMap.get(name);
	}
		
	/**
	 * Clears the contents of the category map.
	 *
	 */	
	public void clearMap() {
		categoryMap.clear();
	}
	
	/**
	 * Makes a copy of the category map.
	 * 
	 * @param cat  the category name to copy from
	 *
	 */		
	public void copyFrom(final Categories cat) {
		categoryMap = new ConcurrentHashMap<String, String> (cat.categoryMap);
	}	

}
