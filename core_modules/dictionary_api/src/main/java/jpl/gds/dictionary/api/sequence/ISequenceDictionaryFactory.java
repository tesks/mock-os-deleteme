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
package jpl.gds.dictionary.api.sequence;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.config.DictionaryProperties;

/**
 * An interface to be implemented by sequence dictionary factories, which 
 * are used to create flight sequence dictionary instances.
 * 
 *
 * @since R8
 */
public interface ISequenceDictionaryFactory {

    /**
     * Gets a Sequence dictionary instance for the current mission by parsing
     * the default sequence file. This class will create a new ISequenceDictionary
     * instance and parse the default Sequence Definition file using the mission
     * Sequence parser defined in the supplied DictionaryConfiguration object.
     * 
     * @param config DictionaryConfiguration object to get parser class from
     * 
     * @return a unique ISequenceDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, if the
     *             mission-specific dictionary class cannot be found or is not
     *             configured in the DictionaryConfiguration
     */
    ISequenceDictionary getNewInstance(DictionaryProperties config) throws DictionaryException;

    /**
     * Gets a Sequence dictionary instance for the current mission by parsing
     * the supplied file. This class will create a new ISequenceDictionary
     * instance and parse the default Sequence Definition file using the mission
     * Sequence parser defined in the supplied DictionaryConfiguration object.
     * 
     * @param config DictionaryConfiguration object to get parser class from
     * @param filename
     *            path to the sequence file to parse
     * 
     * @return a unique ISequenceDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, if the
     *             mission-specific dictionary class cannot be found or is not
     *             configured in the DictionaryConfiguration
     */
    ISequenceDictionary getNewInstance(DictionaryProperties config, String filename) throws DictionaryException;

}