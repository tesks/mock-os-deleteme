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
package jpl.gds.dictionary.api.frame;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.config.DictionaryProperties;

/**
 * An interface to be implemented by transfer frame dictionary factories, which 
 * are used to create flight frame dictionary instances.
 * 
 *
 * @since R8
 */
public interface ITransferFrameDictionaryFactory {

    /**
     * Gets a unique transfer frame dictionary instance for the current mission by
     * parsing the default transfer frame dictionary file using the mission frame
     * dictionary parser defined in the supplied DictionaryConfiguration.
     * 
     * @param config
     *            DicitonaryConfiguration object defining parser classes for the
     *            current mission
     * @return the ITransferFrameDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     * 
     */
    ITransferFrameDictionary getNewInstance(DictionaryProperties config) throws DictionaryException;

    /**
     * Gets a unique transfer frame dictionary instance for the current mission by
     * parsing the given transfer frame dictionary file using the mission frame
     * dictionary parser defined in the supplied DictionaryConfiguration.
     * 
     * @param config
     *            DicitonaryConfiguration object defining parser classes for the
     *            current mission
     * @param filename
     *            the path to the frame file to load
     * @return the ITransferFrameDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     * 
     */
    ITransferFrameDictionary getNewInstance(DictionaryProperties config, String filename) throws DictionaryException;

}