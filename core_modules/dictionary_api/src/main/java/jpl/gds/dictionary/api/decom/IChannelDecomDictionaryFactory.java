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
package jpl.gds.dictionary.api.decom;

import java.util.Map;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.config.DictionaryProperties;

/**
 * An interface to be implemented by decom dictionary factories, which 
 * are used to create flight and SSE decom dictionary instances.
 * 
 *
 * @since R8
 */
public interface IChannelDecomDictionaryFactory {

    /**
     * Returns an empty flight IChannelDecomDictionary object. This object
     * doesn't parse any decom XML, nor set any channel map. Uses the supplied
     * DictionaryConfiguration to determine which class to use.
     * 
     * @param dictConfig
     *            the DictionaryConfiguration to use
     * 
     * @return IChannelDecomDictionary
     * @throws DictionaryException  if the instance cannot be created
     * 
     */
    IChannelDecomDictionary getEmptyInstance(DictionaryProperties dictConfig) throws DictionaryException;

    /**
     * Returns an empty SSE IChannelDecomDictionary object. This object
     * doesn't parse any decom XML, nor set any channel map. Uses the supplied
     * DictionaryConfiguration to determine which class to use.
     * 
     * @param dictConfig
     *            the DictionaryConfiguration to use
     * 
     * @return IChannelDecomDictionary
     * @throws DictionaryException  if the instance cannot be created
     * 
     */
    IChannelDecomDictionary getEmptySseInstance(DictionaryProperties dictConfig) throws DictionaryException;

    /**
     * Gets a channel decom dictionary parser instance for the current mission
     * and uses it to parse the given channel top-level decom file, which must
     * be an APID map file. This method always returns a new instance of
     * IChannelDecomDictionary, since it does not parse the default channel
     * file. Clearing the resulting dictionary object is the responsibility of
     * the caller. The class used to parse the dictionary and the dictionary
     * search configuration must be defined in the supplied
     * DictionaryConfiguration. The input map of channel definitions is required
     * to parse decom maps that contain references to channels.
     * 
     * @param config
     *            the DictionaryConfiguration defining dictionary adaptor and
     *            search configuration
     * @param decomFile
     *            the path to the channel file to parse
     * @param chanMap
     *            a Map of String channel IDs to IChannelDefinition objects
     * @return the IChannelDecomDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     */
    IChannelDecomDictionary getNewInstance(DictionaryProperties config, String decomFile,
            Map<String, IChannelDefinition> chanMap) throws DictionaryException;

    /**
     * Gets a channel decom dictionary parser instance for the current mission
     * and uses it to parse the default top-level decom file, which must be an
     * APID map file. This method always returns a new instance of
     * IChannelDecomDictionary. Clearing the resulting dictionary object is the
     * responsibility of the caller. The class used to parse the dictionary and
     * the dictionary search configuration must be defined in the supplied
     * DictionaryConfiguration. The input map of channel definitions is required
     * to parse decom maps that contain references to channels.
     * 
     * @param config
     *            the DictionaryConfiguration defining dictionary adaptor and
     *            search configuration
     * @param chanMap
     *            a Map of String channel IDs to IChannelDefinition objects
     * @return the IChannelDecomDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     */
    IChannelDecomDictionary getNewInstance(DictionaryProperties config, Map<String, IChannelDefinition> chanMap)
            throws DictionaryException;

    /**
     * Gets a channel decom dictionary parser instance for the current mission
     * SSE and uses it to parse the default top-level decom file, which must be an
     * APID map file. This method always returns a new instance of
     * IChannelDecomDictionary. Clearing the resulting dictionary object is the
     * responsibility of the caller. The class used to parse the dictionary and
     * the dictionary search configuration must be defined in the supplied
     * DictionaryConfiguration. The input map of channel definitions is required
     * to parse decom maps that contain references to channels.
     * 
     * @param config
     *            the DictionaryConfiguration defining dictionary adaptor and
     *            search configuration
     * @param chanMap
     *            a Map of String channel IDs to IChannelDefinition objects
     * @return the IChannelDecomDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     */
    IChannelDecomDictionary getNewSseInstance(DictionaryProperties config, Map<String, IChannelDefinition> chanMap)
            throws DictionaryException;

    /**
     * Gets an SSE Channel Decom dictionary instance for the current mission and
     * uses it to parse the given channel decom file, which must be an APID map
     * file. This methods always returns a new instance of
     * IChannelDecomDictionary, since it does not parse the default channel
     * file. Clearing the resulting dictionary object is the responsibility of
     * the caller. The class used to parse the dictionary must and the
     * dictionary search configuration must be defined in the supplied
     * DictionaryConfiguration.
     * 
     * @param config
     *            the DictionaryConfiguration defining dictionary adaptor and
     *            search configuration
     * @param decomFile
     *            the path to the channel decom file to parse
     * @param chanMap
     *            a Map of String channel IDs to IChannelDefinition objects
     * @return the IChannelDecomDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     */
    IChannelDecomDictionary getNewSseInstance(DictionaryProperties config, String decomFile,
            Map<String, IChannelDefinition> chanMap) throws DictionaryException;

    /**
     * Gets a Flight Channel Decom dictionary instance for the current mission and
     * uses it to parse the given channel decom file, which must be an APID map
     * file. This methods always returns a new instance of
     * IChannelDecomDictionary, since it does not parse the default channel
     * file. Clearing the resulting dictionary object is the responsibility of
     * the caller. The class used to parse the dictionary must and the
     * dictionary search configuration must be defined in the supplied
     * DictionaryConfiguration.
     * 
     * @param config
     *            the DictionaryConfiguration defining dictionary adaptor and
     *            search configuration
     * @param decomFile
     *            the path to the channel decom file to parse
     * @param chanMap
     *            a Map of String channel IDs to IChannelDefinition objects
     * @return the IChannelDecomDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     */
    IChannelDecomDictionary getNewFlightInstance(DictionaryProperties config, String decomFile,
                                                 Map<String, IChannelDefinition> chanMap)
            throws DictionaryException;

}