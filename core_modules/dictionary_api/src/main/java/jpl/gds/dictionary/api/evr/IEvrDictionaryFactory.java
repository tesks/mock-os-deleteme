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
package jpl.gds.dictionary.api.evr;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.config.DictionaryProperties;

/**
 * An interface to be implemented by EVR dictionary factories, which 
 * are used to create flight and SSE EVR dictionary instances, as well
 * as EVR definition instances.
 * 
 *
 * @since R8
 */
public interface IEvrDictionaryFactory {

    /**
     * Gets an EVR dictionary parser instance for the current mission and uses
     * it to parse the given EVR file. This method always returns a new instance
     * of IEvrDictionary. Clearing
     * the resulting dictionary object is the responsibility of the caller. The
     * class used to parse the dictionary must be defined in the supplied
     * DictionaryProperties.
     * 
     * @param config
     *            the DictionaryProperties to get parser class names from
     * @param filename
     *            the path to the EVR file to parse
     * @return the IEVRDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     * 
     */
    IEvrDictionary getNewInstance(DictionaryProperties config, String filename) throws DictionaryException;

    /**
     * Gets an EVR dictionary parser instance for the current mission and uses
     * it to parse the default EVR file. This method always returns a new instance
     * of IEvrDictionary. Clearing
     * the resulting dictionary object is the responsibility of the caller. The
     * class used to parse the dictionary must be defined in the supplied
     * DictionaryProperties.
     * 
     * @param config
     *            the DictionaryProperties to get parser class names from
     * @return the IEVRDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     * 
     */
    IEvrDictionary getNewInstance(DictionaryProperties config) throws DictionaryException;


    /**
     * Gets an SSE EVR dictionary instance for the current mission and uses it
     * to parse the given EVR file. This method always returns a new instance of
     * IEvrDictionary. Clearing
     * the resulting dictionary object is the responsibility of the caller. The
     * class used to parse the dictionary must be defined in the supplied
     * DictionaryProperties.
     * 
     * @param config
     *            the DictionaryProperties to get parser class names from
     * @param filename
     *            the path to the EVR file to parse
     * @return the IEvrDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     * 
     */
    IEvrDictionary getNewSseInstance(DictionaryProperties config, String filename) throws DictionaryException;


    /**
     * Gets an SSE EVR dictionary instance for the current mission and uses it
     * to parse the default EVR File. This method always returns a new instance of
     * IEvrDictionarye. Clearing
     * the resulting dictionary object is the responsibility of the caller. The
     * class used to parse the dictionary must be defined in the supplied
     * DictionaryProperties.
     * 
     * @param config
     *            the DictionaryProperties to get parser class names from
     * @return the IEvrDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     * 
     */
    IEvrDictionary getNewSseInstance(DictionaryProperties config) throws DictionaryException;
    
    /**
     * Gets a new instance of the multimission EVR definition object.
     * 
     * @return evr definition instance
     */
    IEvrDefinition getMultimissionEvrDefinition();
    
    /**
     * Gets a new instance of the JPL SSE EVR definition object.
     * 
     * @return evr definition instance
     */
    IEvrDefinition getJplSseEvrDefinition();

    /**
     * Gets an Flight EVR dictionary instance for the current mission. This method always returns a new instance
     * of IEvrDictionary. Clearing the resulting dictionary object is the responsibility of the caller. The
     * class used to parse the dictionary must be defined in the supplied DictionaryProperties.
     * 
     * @param config
     *            DictionaryProperties
     * @param filename
     *            EVR dictionary filename
     * @return IEvrDictionary instance
     * @throws DictionaryException
     */
    IEvrDictionary getNewFlightInstance(DictionaryProperties config, String filename) throws DictionaryException;

}