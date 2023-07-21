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
package ammos.datagen.config;

import java.io.File;

import org.xml.sax.helpers.DefaultHandler;

/**
 * This interface is to be implemented by all XML configuration classes used by
 * the AMPCS data generators.
 * 
 *
 */
public interface IXmlConfiguration {

	/**
	 * Loads the configuration file.
	 * 
	 * @param uri
	 *            the path to the configuration file.
	 * @param handler
	 *            the SAX XML event parse handler for the configuration being
	 *            loaded
	 * 
	 * @return true if the file was successfully loaded, false if not.
	 */
	public boolean load(final String uri, final DefaultHandler handler);

	/**
	 * Resets member fields to their initial state so that load() can be called
	 * again.
	 */
	public void clear();

	/**
	 * Validates a configuration file against its RelaxNG Compact schema file.
	 * 
	 * @param configFile
	 *            the path the the XML configuration file to validate
	 * 
	 * @return true if the configuration file was valid, false if the validation
	 *         failed
	 */
	public boolean validate(final File configFile);

	/**
	 * Gets the value of a string configuration property.
	 * 
	 * @param propertyName
	 *            the name of the string property; this class defines the
	 *            available property names as constants.
	 * @param defaultVal
	 *            the default value to return if the configuration property is
	 *            not set
	 * @return the value of the property, or the default value if no value has
	 *         been loaded, or if the property is not a string
	 */
	public String getStringProperty(final String propertyName,
			final String defaultVal);

	/**
	 * Gets the value of an integer configuration property.
	 * 
	 * @param propertyName
	 *            the name of the integer property; the configuration class
	 *            defines the available property names as constants.
	 * @param defaultVal
	 *            the default value to return if the configuration property is
	 *            not set
	 * @return the value of the property, or the default value if no value has
	 *         been loaded, or if the property is not an integer
	 */
	public int getIntProperty(final String propertyName, final int defaultVal);

	/**
	 * Gets the value of a long configuration property.
	 * 
	 * @param propertyName
	 *            the name of the long property; the configuration class defines
	 *            the available property names as constants.
	 * @param defaultVal
	 *            the default value to return if the configuration property is
	 *            not set
	 * @return the value of the property, or the default value if no value has
	 *         been loaded, or if the property is not a long
	 */
	public long getLongProperty(final String propertyName, final long defaultVal);

	/**
	 * Gets the value of a boolean configuration property.
	 * 
	 * @param propertyName
	 *            the name of the boolean property; the configuration class
	 *            defines the available property names as constants.
	 * @param defaultVal
	 *            the default value to return if the configuration property is
	 *            not set
	 * @return the value of the property, or the default value if no value has
	 *         been loaded, or if the property is not a boolean
	 */
	public boolean getBooleanProperty(final String propertyName,
			final boolean defaultVal);

	/**
	 * Gets the value of a floating point configuration property.
	 * 
	 * @param propertyName
	 *            the name of the float property; the configuration class
	 *            defines the available property names as constants.
	 * @param defaultVal
	 *            the default value to return if the configuration property is
	 *            not set
	 * @return the value of the property, or the default value if no value has
	 *         been loaded, or if the property is not a float
	 */
	public float getFloatProperty(final String propertyName,
			final float defaultVal);

	/**
	 * Gets the value of a TraversalType configuration property.
	 * 
	 * @param propertyName
	 *            the name of the TraversalType property; the configuration
	 *            class defines the available property names as constants.
	 * @param defaultVal
	 *            the default value to return if the configuration property is
	 *            not set
	 * @return the value of the property, or the default value if no value has
	 *         been loaded, or if the property is not a float
	 */
	public TraversalType getTraversalTypeProperty(final String propertyName,
			final TraversalType defaultVal);

}
