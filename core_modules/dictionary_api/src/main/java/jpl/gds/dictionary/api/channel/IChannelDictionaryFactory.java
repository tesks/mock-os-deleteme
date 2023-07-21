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
package jpl.gds.dictionary.api.channel;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.config.DictionaryProperties;

/**
 * An interface to be implemented by channel dictionary factories, which 
 * are used to create flight and SSE channel dictionary instances, as flight
 * and SSE header channel dictionary instances, and station monitor dictionary
 * instances.
 * 
 *
 * @since R8
 */
public interface IChannelDictionaryFactory {

    /**
     * Gets a channel dictionary parser instance for the current mission and
     * uses it to parse the given channel file. This method always returns a new
     * instance of IChannelDictionary, since it does not parse the default
     * channel file. Clearing the resulting dictionary object is the
     * responsibility of the caller. The class used to parse the dictionary must
     * be defined in the supplied DictionaryConfiguration.
     * 
     * @param config
     *            the DictionaryConfiguration to get parser class names from
     * @param filename
     *            the path to the channel file to parse
     * @return the IChannelDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     * 
     */
    IChannelDictionary getNewInstance(DictionaryProperties config, String filename) throws DictionaryException;


    /**
     * Gets a channel dictionary parser instance for the current mission and
     * uses it to parse the default channel file. This method always returns a
     * new instance of IChannelDictionary, since it does not parse the default
     * channel file. Clearing the resulting dictionary object is the
     * responsibility of the caller. The class used to parse the dictionary must
     * be defined in the supplied DictionaryConfiguration.
     * 
     * @param config
     *            the DictionaryConfiguration to get parser class names from
     * @return the IChannelDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     * 
     */
    IChannelDictionary getNewInstance(DictionaryProperties config) throws DictionaryException;

    /**
     * Gets an SSE Channel dictionary instance for the current mission and uses
     * it to parse the given channel file. This method always returns a new
     * instance of IChannelDictionary, since it does not parse the default
     * channel file. Clearing the resulting dictionary object is the
     * responsibility of the caller. The class used to parse the dictionary must
     * be defined in the supplied DictionaryConfiguration.
     * 
     * @param config
     *            the DictionaryConfiguration to get parser class names from
     * @param filename
     *            the path to the channel file to parse
     * @return the IChannelDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     */
    IChannelDictionary getNewSseInstance(DictionaryProperties config, String filename) throws DictionaryException;

    /**
     * Gets an SSE Channel dictionary instance for the current mission and uses
     * it to parse the default channel file. This method always returns a new
     * instance of IChannelDictionary, since it does not parse the default
     * channel file. Clearing the resulting dictionary object is the
     * responsibility of the caller. The class used to parse the dictionary must
     * be defined in the supplied DictionaryConfiguration.
     * 
     * @param config
     *            the DictionaryConfiguration to get parser class names from
     * @return the IChannelDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     */
    IChannelDictionary getNewSseInstance(DictionaryProperties config) throws DictionaryException;

    /**
     * Gets a monitor channel dictionary parser instance for the current mission
     * and uses it to parse the given monitor channel file. This method always
     * returns a new instance of IChannelDictionary, since it does not parse the
     * default monitor channel file. Clearing the resulting dictionary object is the
     * responsibility of the caller. The class used to parse the dictionary must
     * be defined in the supplied DictionaryConfiguration.
     * 
     * @param config
     *            the DictionaryConfiguration to get parser class names from
     * @param filename
     *            the path to the channel file to parse
     * @return the IChannelDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     * 
     */
    IChannelDictionary getNewMonitorInstance(DictionaryProperties config, String filename) throws DictionaryException;

    /**
     * Gets a monitor channel dictionary parser instance for the current mission
     * and uses it to parse the default monitor channel file. This method always
     * returns a new instance of IChannelDictionary, since it does not parse the
     * default monitor channel file. Clearing the resulting dictionary object is
     * the responsibility of the caller. The class used to parse the dictionary
     * must be defined in the supplied DictionaryConfiguration.
     * 
     * @param config
     *            the DictionaryConfiguration to get parser class names from
     * @return the IChannelDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     * 
     */
    IChannelDictionary getNewMonitorInstance(DictionaryProperties config) throws DictionaryException;


    /**
     * Gets a header channel dictionary parser instance for the current mission
     * and uses it to parse the given header channel file. This method always
     * returns a new instance of IChannelDictionary, since it does not parse the
     * default header channel file. Clearing the resulting dictionary object is the
     * responsibility of the caller. The class used to parse the dictionary must
     * be defined in the supplied DictionaryConfiguration.
     * 
     * @param config
     *            the DictionaryConfiguration to get parser class names from
     * @param filename
     *            the path to the channel file to parse
     * @return the IChannelDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     * 
     */
    IChannelDictionary getNewHeaderInstance(DictionaryProperties config, String filename) throws DictionaryException;

    /**
     * Gets a header channel dictionary parser instance for the current mission
     * and uses it to parse the default header channel file. This method always
     * returns a new instance of IChannelDictionary, since it does not parse the
     * default header channel file. Clearing the resulting dictionary object is the
     * responsibility of the caller. The class used to parse the dictionary must
     * be defined in the supplied DictionaryConfiguration.
     * 
     * @param config
     *            the DictionaryConfiguration to get parser class names from
     * @return the IChannelDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     * 
     */
    IChannelDictionary getNewHeaderInstance(DictionaryProperties config) throws DictionaryException;


    /**
     * Gets an SSE header Channel dictionary instance for the current mission and uses
     * it to parse the given header channel file. This method always returns a new
     * instance of IChannelDictionary, since it does not parse the default
     * header channel file. Clearing the resulting dictionary object is the
     * responsibility of the caller. The class used to parse the dictionary must
     * be defined in the supplied DictionaryConfiguration.
     * 
     * @param config
     *            the DictionaryConfiguration to get parser class names from
     * @param filename
     *            the path to the header channel file to parse
     * @return the IChannelDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     */
    IChannelDictionary getNewSseHeaderInstance(DictionaryProperties config, String filename) throws DictionaryException;

    /**
     * Gets an SSE header Channel dictionary instance for the current mission
     * and uses it to parse the default header channel file. This method always
     * returns a new instance of IChannelDictionary, since it does not parse the
     * default header channel file. Clearing the resulting dictionary object is
     * the responsibility of the caller. The class used to parse the dictionary
     * must be defined in the supplied DictionaryConfiguration.
     * 
     * @param config
     *            the DictionaryConfiguration to get parser class names from
     * @return the IChannelDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     */
    IChannelDictionary getNewSseHeaderInstance(DictionaryProperties config) throws DictionaryException;

    /**
     * Gets an Flight header Channel dictionary instance for the current mission
     * and uses it to parse the default header channel file. This method always
     * returns a new instance of IChannelDictionary, since it does not parse the
     * default header channel file. Clearing the resulting dictionary object is
     * the responsibility of the caller. The class used to parse the dictionary
     * must be defined in the supplied DictionaryConfiguration.
     * 
     * @param config
     *            the DictionaryConfiguration to get parser class names from
     * @return the IChannelDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     */
    IChannelDictionary getNewFlightInstance(DictionaryProperties config, String filename) throws DictionaryException;

}