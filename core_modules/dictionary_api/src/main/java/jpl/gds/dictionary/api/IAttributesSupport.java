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


/**
 * The IAttributeSupport interface is implemented by dictionary definition
 * classes that use a KeyValueAttributes object, which is a container to store
 * project-specific information as a map of key-value pairs. IxxxDefinition
 * interfaces that support attributes should extend IAttributesSupport. <br>
 * 
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <p>
 * <p>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <p>
 * <p>
 * 
 * @see KeyValueAttributes
 * 
 */

public interface IAttributesSupport {

	/**
	 * Adds a key-value pair to the key-value attributes map.
	 * 
	 * @param key  the attribute keyword name
	 * @param value  the attribute keyword value
	 */
	public void setKeyValueAttribute(String key, String value);
	
	/**
	 * Gets the value of an attribute given the keyword name.
	 * 
	 * @param key  the keyword name
	 * @return the value to which the specified key is mapped
	 */
	public String getKeyValueAttribute(String key);
	
	/**
	 * Sets the key-value attributes map in the current object.
	 * 
	 * @param toSet the key-value attributes object to set
	 */
	public void setKeyValueAttributes(KeyValueAttributes toSet);
	
	/**
	 * Gets the key-value attributes map from the current object.
	 * 
	 * @return the key-value attribute map.
	 */
	public KeyValueAttributes getKeyValueAttributes();
	
	/**
	 * Clears the contents of the current key-value attributes map.
	 */
	public void clearKeyValue();




}
