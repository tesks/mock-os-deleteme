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
package jpl.gds.dictionary.api.alarm;

import java.util.Map;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.config.DictionaryProperties;

/**
 * An interface to be implemented by alarm dictionary factories, which 
 * are used to create flight and SSE alarm dictionary instances, including
 * user alarm dictionary instances.
 * 
 *
 * @since R8
 */
public interface IAlarmDictionaryFactory {

    /**
     * Gets a alarm dictionary parser instance for the current mission and uses
     * it to parse the default alarm file. Clearing the resulting dictionary
     * object is the responsibility of the caller. The class used to parse the
     * dictionary must be defined in the supplied DictionaryConfiguration. This
     * method should be supplied a list of parsed IChannelDefinitions from an
     * IChannelDictionary object. Alarms will only be parsed for a channel if
     * there is a channel definition in this map.
     * 
     * @param config
     *            the DictionaryConfiguration to get parser class names from
     * @param channelIdMapping
     *            map of String channel IDs to IChannelDefinition objects
     * @return the IAlarmDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     * 
     */
    IAlarmDictionary getNewInstance(DictionaryProperties config, Map<String, IChannelDefinition> channelIdMapping)
            throws DictionaryException;

    /**
     * Gets a alarm dictionary parser instance for the current mission and uses
     * it to parse the given alarm file. This method always returns a new
     * instance of IAlarmDictionary, since it does not parse the default alarm
     * file. Clearing the resulting dictionary object is the responsibility of
     * the caller. The class used to parse the dictionary must be defined in the
     * supplied DictionaryConfiguration. This method should be supplied a list
     * of parsed IChannelDefinitions from an IChannelDictionary object. Alarms
     * will only be parsed for a channel if there is a channel definition in
     * this map.
     * 
     * @param config
     *            the DictionaryConfiguration to get parser class names from
     * @param filename
     *            the path to the alarm file to parse
     * @param channelIdMapping
     *            map of String channel IDs to IChannelDefinition objects
     * @return the IAlarmDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     * 
     */
    IAlarmDictionary getNewInstance(DictionaryProperties config, String filename,
            Map<String, IChannelDefinition> channelIdMapping) throws DictionaryException;

    /**
     * Gets a alarm dictionary parser instance for the current mission SSE and
     * uses it to parse the default SSE alarm file. This method always returns a
     * new instance of IAlarmDictionary. Clearing the resulting dictionary
     * object is the responsibility of the caller. The class used to parse the
     * dictionary must be defined in the supplied DictionaryConfiguration. This
     * method should be supplied a list of parsed IChannelDefinitions from an
     * IChannelDictionary object. Alarms will only be parsed for a channel if
     * there is a channel definition in this map.
     * 
     * @param config
     *            the DictionaryConfiguration to get parser class names from
     * @param channelIdMapping
     *            map of String channel IDs to IChannelDefinition objects
     * @return the IAlarmDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     * 
     */
    IAlarmDictionary getNewSseInstance(DictionaryProperties config, Map<String, IChannelDefinition> channelIdMapping)
            throws DictionaryException;

    /**
     * Gets an SSE Alarm dictionary instance for the current mission and uses it
     * to parse the given alarm file. This method always returns a new instance
     * of IAlarmDictionary, since it does not parse the default alarm file.
     * Clearing the resulting dictionary object is the responsibility of the
     * caller. The class used to parse the dictionary must be defined in the
     * supplied DictionaryConfiguration. This method should be supplied a list
     * of parsed IChannelDefinitions from an IChannelDictionary object. Alarms
     * will only be parsed for a channel if there is a channel definition in
     * this map.
     * 
     * 
     * @param config
     *            the DictionaryConfiguration to get parser class names from
     * @param filename
     *            the path to the alarm file to parse
     * @param channelIdMapping
     *            map of String channel IDs to IChannelDefinition objects
     * @return the IAlarmDictionary object
     * 
     * @throws DictionaryException
     *             if the dictionary cannot be found or parsed, or if the
     *             mission-specific dictionary class cannot be found
     */
    IAlarmDictionary getNewSseInstance(DictionaryProperties config, String filename,
            Map<String, IChannelDefinition> channelIdMapping) throws DictionaryException;


    /**
     * Gets an FSW Alarm dictionary instance for the current mission. This method always returns a new instance
     * of IAlarmDictionary, since it does not parse the default alarm file.
     * Clearing the resulting dictionary object is the responsibility of the
     * caller. The class used to parse the dictionary must be defined in the
     * supplied DictionaryConfiguration. This method should be supplied a list
     * of parsed IChannelDefinitions from an IChannelDictionary object. Alarms
     * will only be parsed for a channel if there is a channel definition in
     * this map.
     * 
     * @param config
     * @param filename
     * @param channelIdMapping
     * @return IAlarmDictionary instance
     * @throws DictionaryException
     */
    IAlarmDictionary getNewFlightInstance(DictionaryProperties config, String filename,
                                          Map<String, IChannelDefinition> channelIdMapping)
            throws DictionaryException;

}