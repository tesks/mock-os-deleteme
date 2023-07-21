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
package jpl.gds.dictionary.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jpl.gds.dictionary.api.config.DictionaryType;

/**
 * Tracks and checks the required starting elements for an XML dictionary
 * schema. If set, The XML element names on the required list are those that
 * MUST be seen prior to any other element in the XML for there to be match to
 * the schema the parser corresponds to. (Note that this is a sanity check for
 * schema only, because at this time, we do not validate the XML against the
 * schema.) The source schema name and dictionary type name are needed by this
 * class for use in error messages. The XML parser should invoke the
 * checkState() method as the top of its startElement() event handler. If the state
 * check returns false, the error message the parser should emit can be obtained
 * using getLastMessage().
 * 
 */
public class StartingRequiredElementTracker {
	private Map <String, Boolean> requiredElementMap;
	private String schemaName = "Unknown";
	
	private final DictionaryType dictionaryTypeName;

	/**
	 * Constructor.
	 * 
	 * @param schemaName name of the source schema for the dictionary; used for error reporting
	 * @param dictionaryTypeName type of the dictionary; used for error reporting
	 * @param elementNames list of required starting XML element names
	 */
	public StartingRequiredElementTracker(final String schemaName, final DictionaryType dictionaryTypeName, final List<String> elementNames) {
		if (schemaName == null) {
			throw new IllegalArgumentException("schema name cannot be null");
		}
		if (dictionaryTypeName == null) {
			throw new IllegalArgumentException("dictionary type name cannot be null");
		}
		if (elementNames == null) {
			throw new IllegalArgumentException("elementNames list cannot be null");
		}
		this.schemaName = schemaName;
		this.dictionaryTypeName = dictionaryTypeName;
		setRequiredElements(elementNames);
	}

	/**
	 * Initialize the required starting elements for the current schema and set
	 * their states to "not seen".
	 * 
	 * @param elementNames List of element names required at the start of the XML
	 */
	private void setRequiredElements(final List<String> elementNames) {
		requiredElementMap = new HashMap<String, Boolean>();
		for (String name: elementNames) {
			requiredElementMap.put(name, false);
		}
	}

	/**
	 * Resets the state of all required elements to "not seen".
	 */
	public void clearState() {
		requiredElementMap = new HashMap<String, Boolean>();
		for (String name: requiredElementMap.keySet()) {
			requiredElementMap.put(name, false);
		}
	}

	/**
	 * Checks the given XML element name against the list of XML elements required
	 * at start of parsing.  If this is one of the required elements, that element
	 * is marked "seen". If the element is NOT one of the required starting
	 * elements, and all the required elements are not marked "seen", then this
	 * method will throw. In all other cases it will just return.
	 * 
	 * @param elementName the name of the XML element being parsed.
	 * 
	 * @throws WrongSchemaException if a required element is missing
	 */
	public void checkState(final String elementName) throws WrongSchemaException {
		if (requiredElementMap == null) {
			return;
		}
		if (requiredElementMap.containsKey(elementName)) {
			requiredElementMap.put(elementName, true);
		} else {
			for (Map.Entry<String,Boolean> present: requiredElementMap.entrySet()) {
				if (!present.getValue()) {
					StringBuilder message = new StringBuilder("This does not appear to be a " + 
							schemaName + "-formatted " + this.dictionaryTypeName + " dictionary, ");
					message.append("because it is missing one of the required starting elements: ");
					int count = requiredElementMap.size();
					for (String name: requiredElementMap.keySet()) {
						message.append(name);
						if (count > 1) {
							message.append(", ");
						}
						count--;
					}
					throw new WrongSchemaException(message.toString());
				}
			}
		}
	}
}
