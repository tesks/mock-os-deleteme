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
package jpl.gds.dictionary.api.apid;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.config.DictionaryProperties;

/**
 * An interface to be implemented by APID dictionary factories, which 
 * are used to create flight and SSE APID dictionary instances.
 * 
 *
 * @since R8
 */
public interface IApidDictionaryFactory {

    /**
     * Gets a unique APID dictionary instance for the current mission by parsing
     * the given APID dictionary file using the mission APID parser defined in
     * the given DictionaryConfiguration, but locating the file using the
     * standard search mechanism.
     * 
     * @param dictConfig
     *            DicitonaryConfiguration object defining parser classes for the
     *            current mission
     * @return the IApidDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, if the
     *             mission-specific dictionary class cannot be found or is not
     *             configured in the DictionaryConfiguration
     * 
     * @since R8
     */
    IApidDictionary getNewInstance(DictionaryProperties dictConfig) throws DictionaryException;

    /**
     * Gets a unique APID dictionary instance for the current mission by parsing
     * the given APID dictionary file using the mission APID parser defined in
     * the given DictionaryConfiguration.
     * 
     * @param dictConfig
     *            DicitonaryConfiguration object defining parser classes for the
     *            current mission
     * @param filePath
     *            full path to APID dictionary file
     * @return the IApidDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, if the
     *             mission-specific dictionary class cannot be found or is not
     *             configured in the DictionaryConfiguration
     */
    IApidDictionary getNewInstance(DictionaryProperties dictConfig, String filePath) throws DictionaryException;

    /**
     * Gets an SSE APID dictionary instance for the current mission and uses
     * it to parse the given APID file. This method always returns a new
     * instance of IApidDictionary, since it does not parse the default
     * APID file. Clearing the resulting dictionary object is the
     * responsibility of the caller. The class used to parse the dictionary must
     * be defined in the supplied DictionaryConfiguration.
     * 
     * @param config
     *            the DictionaryConfiguration to get parser class names from
     * @param filename
     *            the path to the APID file to parse
     * @return the IApidDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     *             
     */
    IApidDictionary getNewSseInstance(DictionaryProperties config, String filename) throws DictionaryException;

    /**
     * Gets a unique SSE APID dictionary instance for the current mission by
     * parsing the given APID dictionary file using the mission APID parser
     * defined in the given DictionaryConfiguration, but locating the file using
     * the standard search mechanism.
     * 
     * @param dictConfig
     *            DicitonaryConfiguration object defining parser classes for the
     *            current mission
     * @return the IApidDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, if the
     *             mission-specific dictionary class cannot be found or is not
     *             configured in the DictionaryConfiguration
     * 
     * @since R8
     */
    IApidDictionary getSseNonStaticInstance(DictionaryProperties dictConfig) throws DictionaryException;

    /**
     * Gets a unique FSW APID dictionary instance for the current mission by
     * parsing the given APID dictionary file using the mission APID parser
     * defined in the given DictionaryConfiguration, but locating the file using
     * the standard search mechanism.
     * 
     * @param dictConfig
     *            DicitonaryConfiguration object defining parser classes for the
     *            current mission
     * @param filePath
     *            The dictionary file path
     * @return the IApidDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, if the
     *             mission-specific dictionary class cannot be found or is not
     *             configured in the DictionaryConfiguration
     * 
     * @since R8
     */
    IApidDictionary getNewFlightInstance(DictionaryProperties dictConfig, String filePath)
			throws DictionaryException;

}