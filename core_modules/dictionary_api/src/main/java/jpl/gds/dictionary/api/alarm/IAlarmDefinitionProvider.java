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

import java.util.List;
import java.util.Map;

import jpl.gds.dictionary.api.IDefinitionProviderLoadStatus;

/**
 * An interface to be implemented by classes that serve up alarm
 * definitions.
 * 
 *
 * @since R8
 */
public interface IAlarmDefinitionProvider extends IDefinitionProviderLoadStatus {

    /**
     * Returns a list of parsed single-channel IAlarmDefinitions applicable to
     * the specified channel ID. Does not include combination (multi-channel)
     * alarm definitions.
     * 
     * @param id the channel ID
     * 
     * @return List of IAlarmDefinition objects, which are keyed by the IDs of
     *         alarms and channels; will be null if there are no alarms for the
     *         specified channel
     */
    public List<IAlarmDefinition> getSingleChannelAlarmDefinitions(String id);

    /**
     * Retrieves a Map of parsed combination (multi-channel) alarm definitions
     * from the IAlarmDictionary. The map key is the combination alarm ID.
     * 
     * @return non-modifiable map of ICombinationAlarmDefinition objects; map
     *         should be empty if no combination alarms exist, but not null
     *         
     */
    public Map<String, ICombinationAlarmDefinition> getCombinationAlarmMap();

    /**
     * Retrieves a map of parsed single-channel alarm definitions, keyed
     * by channel ID. The value in the map is actually a list of
     * IAlarmDefinition objects that apply to the channel. No objects related
     * to combination (multi-channel) alarms are returned. 
     * 
     * @return Map of Lists of IAlarmDefinition objects, keyed by channel ID
     */
    public Map<String, List<IAlarmDefinition>> getSingleChannelAlarmMapByChannel();
    
    /**
     * Adds a listener for alarm definition reload events.
     * 
     * @param l listener to add
     */
    public void addReloadListener(IAlarmReloadListener l);
    
    /**
     * Removes a listener for alarm definition reload events.
     * 
     * @param l listener to remove
     */
    public void removeReloadListener(IAlarmReloadListener l);

}