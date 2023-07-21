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
package jpl.gds.dictionary.api.command;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.config.DictionaryProperties;

/**
 * An interface to be implemented by command dictionary factories, which 
 * are used to create flight command dictionary instances.
 * 
 *
 * @since R8
 */
public interface ICommandDictionaryFactory {

    /**
     * Gets a unique command dictionary instance for the current mission by
     * parsing the given command dictionary file using the mission command
     * parser defined in the given DictionaryConfiguration, but locating the
     * file itself through the search path mechanism.
     * 
     * @param config
     *            DictionaryConfiguration object defining parser classes for the
     *            current mission
     * @return the ICommandDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     * 
     * @since R8
     */
    ICommandDictionary getNewInstance(DictionaryProperties config) throws DictionaryException;

    /**
     * Gets a unique command dictionary instance for the current mission by
     * parsing the given command dictionary file using the mission command
     * parser defined in the given DictionaryConfiguration.
     * 
     * @param config
     *            DictionaryConfiguration object defining parser classes for the
     *            current mission
     * @param filename
     *            the path to the command file to load
     * @return the ICommandDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     * 
     */
    ICommandDictionary getNewInstance(DictionaryProperties config, String filename) throws DictionaryException;


}