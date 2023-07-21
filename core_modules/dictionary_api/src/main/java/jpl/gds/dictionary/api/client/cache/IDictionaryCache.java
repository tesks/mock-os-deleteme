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
package jpl.gds.dictionary.api.client.cache;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.alarm.IAlarmDictionary;
import jpl.gds.dictionary.api.apid.IApidDictionary;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDictionary;
import jpl.gds.dictionary.api.command.ICommandDictionary;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.decom.IChannelDecomDefinitionProvider;
import jpl.gds.dictionary.api.evr.IEvrDictionary;
import jpl.gds.dictionary.api.frame.ITransferFrameDictionary;
import jpl.gds.dictionary.api.sequence.ISequenceDictionary;

import java.util.Map;

/**
 * Interface for dictionary cache implementations
 * 
 *
 */
public interface IDictionaryCache {

    /**
     * Clear the cache
     */
    public void clearCache();

    /**
     * Retrieve a flight channel dictionary
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @param  filepath
     * @return                     flight channel dictionary
     * @throws DictionaryException
     */
    IChannelDictionary getFlightChannelDictionary(DictionaryProperties dictConfig, String filepath)
            throws DictionaryException;

    /**
     * Retrieve a SSE channel dictionary
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @param  filepath
     * @return                     SSE channel dictionary
     * @throws DictionaryException
     */
    IChannelDictionary getSseChannelDictionary(DictionaryProperties dictConfig, String filepath)
            throws DictionaryException;

    /**
     * Retrieve a header channel dictionary
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @param  filepath
     * @return                     header channel dictionary
     * @throws DictionaryException
     */
    IChannelDictionary getHeaderChannelDictionary(DictionaryProperties dictConfig, String filepath)
            throws DictionaryException;

    /**
     * Retrieve an SSE header channel dictionary
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @param  filepath
     * @return                     SSE header channel dictionary
     * @throws DictionaryException
     */
    IChannelDictionary getSseHeaderChannelDictionary(DictionaryProperties dictConfig, String filepath)
            throws DictionaryException;

    /**
     * Retrieve a monitor dictionary
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @param  filepath
     * @return                     monitor dictionary
     * @throws DictionaryException
     */
    IChannelDictionary getMonitorDictionary(DictionaryProperties dictConfig, String filepath)
            throws DictionaryException;

    /**
     * Retrieves an APID dictionary
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @param  isSse               if requesting an sse apid dictionary.
     * @param  filepath
     * @return                     configured dictionary
     * @throws DictionaryException
     */
    IApidDictionary getApidDictionary(DictionaryProperties dictConfig, boolean isSse, String filepath)
            throws DictionaryException;

    /**
     * Retrieves an EVR dictionary
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @param  isSse               if requesting an sse apid dictionary.
     * @param  filepath
     * @return                     EVR dictionary
     * @throws DictionaryException
     */
    IEvrDictionary getEvrDictionary(DictionaryProperties dictConfig, boolean isSse, String filepath)
            throws DictionaryException;

    /**
     * Retrieves a channel decom dictionary
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @param  channelIdMapping    channel definition map
     * @param  isSse               if requesting an sse apid dictionary.
     * @param  filePath
     * @return                     channel decom dictionary
     * @throws DictionaryException
     */
    IChannelDecomDefinitionProvider getChannelDecomDictionary(DictionaryProperties dictConfig,
            Map<String, IChannelDefinition> channelIdMapping, boolean isSse, String filePath)
            throws DictionaryException;

    /**
     * Retrieve a transfer frame dictionary
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @param  filePath
     * @return                     transfer frame dictionary
     * @throws DictionaryException
     */
    ITransferFrameDictionary getTransferFrameDictionary(DictionaryProperties dictConfig, String filePath)
            throws DictionaryException;

    /**
     * Retrieve a sequence dictionary
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @param  filePath
     * @return                     sequence dictionary
     * @throws DictionaryException
     */
    ISequenceDictionary getSequenceDictionary(DictionaryProperties dictConfig, String filePath)
            throws DictionaryException;

    /**
     * Retrieve a command dictionary
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @param  filePath
     * @return                     command dictionary
     * @throws DictionaryException
     */
    ICommandDictionary getCommandDictionary(DictionaryProperties dictConfig, String filePath)
            throws DictionaryException;

    /**
     * Retrieve a flight channel dictionary
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @return                     flight channel dictionary
     * @throws DictionaryException
     */
    IChannelDictionary getFlightChannelDictionary(DictionaryProperties dictConfig) throws DictionaryException;

    /**
     * Retrieve an SSE channel dictionary
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @return                     SSE channel dictionary
     * @throws DictionaryException
     */
    IChannelDictionary getSseChannelDictionary(DictionaryProperties dictConfig) throws DictionaryException;

    /**
     * Retrieve a header channel dictionary
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @return                     header channel dictionary
     * @throws DictionaryException
     */
    IChannelDictionary getHeaderChannelDictionary(DictionaryProperties dictConfig) throws DictionaryException;

    /**
     * Retrieves an SSE header channel dictionary
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @return                     SSE header channel dictionary
     * @throws DictionaryException
     */
    IChannelDictionary getSseHeaderChannelDictionary(DictionaryProperties dictConfig) throws DictionaryException;

    /**
     * Retrieve a monitor dictionary
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @return                     monitor dictionary
     * @throws DictionaryException
     */
    IChannelDictionary getMonitorDictionary(DictionaryProperties dictConfig) throws DictionaryException;

    /**
     * Returns the cached apid dictionary if there is one, or it will create a new
     * one, cache it and return the new copy.
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @param  isSse               if requesting an sse apid dictionary.
     * @return                     configured dictionary
     * @throws DictionaryException
     */
    IApidDictionary getApidDictionary(DictionaryProperties dictConfig, boolean isSse) throws DictionaryException;

    /**
     * Retrieve an EVR dictionary
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @param  isSse               if requesting an sse apid dictionary.
     * @return                     EVR dictionary
     * @throws DictionaryException
     */
    IEvrDictionary getEvrDictionary(DictionaryProperties dictConfig, boolean isSse) throws DictionaryException;

    /**
     * Retrieve an alarm dictionary
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @param  channelIdMapping    channel definition map
     * @param  isSse               if requesting an sse apid dictionary.
     * @return                     alarm dictionary
     * @throws DictionaryException
     */
    IAlarmDictionary getAlarmDictionary(DictionaryProperties dictConfig,
            Map<String, IChannelDefinition> channelIdMapping, boolean isSse) throws DictionaryException;

    /**
     * Reload an alarm dictionary
     *
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @param  channelIdMapping    channel definition map
     * @param  isSse               if requesting an sse apid dictionary.
     * @return                     alarm dictionary
     * @throws DictionaryException
     */
    IAlarmDictionary reloadAlarmDictionary(DictionaryProperties dictConfig,
                                        Map<String, IChannelDefinition> channelIdMapping, boolean isSse) throws DictionaryException;

    /**
     * Retrieve a channel decom dictionary
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @param  channelIdMapping    channel definition map
     * @param  isSse               if requesting an sse apid dictionary.
     * @return                     channel decom dictionary
     * @throws DictionaryException
     */
    IChannelDecomDefinitionProvider getChannelDecomDictionary(DictionaryProperties dictConfig,
            Map<String, IChannelDefinition> channelIdMapping, boolean isSse) throws DictionaryException;

    /**
     * Retrieve a transfer frame dictionary
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @return                     transfer frame dictionary
     * @throws DictionaryException
     */
    ITransferFrameDictionary getTransferFrameDictionary(DictionaryProperties dictConfig) throws DictionaryException;

    /**
     * Retrieve a sequence dictionary
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @return                     sequence dictionary
     * @throws DictionaryException
     */
    ISequenceDictionary getSequenceDictionary(DictionaryProperties dictConfig) throws DictionaryException;

    /**
     * Retrieve a command dictionary
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @return                     command dictionary
     * @throws DictionaryException
     */
    ICommandDictionary getCommandDictionary(DictionaryProperties dictConfig) throws DictionaryException;

    /**
     * Retrieve an alarm dictionary
     * 
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @param  channelIdMapping    channel definition map
     * @param  alarmFile
     * @param  isSse               if requesting an sse apid dictionary.
     * @return                     alarm dictionary
     * @throws DictionaryException
     */
    IAlarmDictionary getAlarmDictionary(DictionaryProperties dictConfig,
            Map<String, IChannelDefinition> channelIdMapping, String alarmFile, boolean isSse)
            throws DictionaryException;

    /**
     * Reload an alarm dictionary
     *
     * @param  dictConfig          Dictionary configuration set up with the version
     *                             and dictionary directory for the required
     *                             dictionary object.
     * @param  channelIdMapping    channel definition map
     * @param  alarmFile
     * @param  isSse               if requesting an sse apid dictionary.
     * @return                     alarm dictionary
     * @throws DictionaryException
     */
    IAlarmDictionary reloadAlarmDictionary(DictionaryProperties dictConfig,
                                           Map<String, IChannelDefinition> channelIdMapping, String alarmFile,
                                           boolean isSse) throws DictionaryException;

    /**
     * Print cache statistics
     */
    void printStats();

}