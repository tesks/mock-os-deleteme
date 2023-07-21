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
package jpl.gds.dictionary.api.client.alarm;

import java.util.Map;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.alarm.IAlarmDefinition;
import jpl.gds.dictionary.api.alarm.IAlarmDefinitionProvider;
import jpl.gds.dictionary.api.alarm.ICombinationAlarmDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.config.DictionaryProperties;

/**
 * An interface to be implemented by classes that manage multiple alarm
 * dictionaries and their automatic reload.
 * 
 *
 * @since R8
 *
 */
public interface IAlarmDictionaryManager extends IAlarmDefinitionProvider {

	/**
	 * System property, if set the FSW alarm configuration file will be
	 * specified by the value of this property.
	 * 
	 */
	public static final String ALARM_FILE_PROPERTY = "GdsUserAlarmFile";
	
	/**
	 * System property, if set the SSE alarm configuration file will be
	 * specified by the value of this property.
	 * 
	 */
	public static final String SSE_ALARM_FILE_PROPERTY = "GdsSseUserAlarmFile";

	/**
	 * Manually adds a simple alarm definition for a FSW channel. Should be used
	 * only by unit tests
	 * 
	 * @param ad the alarm definition to add
	 */
	public abstract void addSimpleFswAlarmDefinition(IAlarmDefinition ad);

	/**
	 * Manually adds a simple alarm definition for an SSE channel. Should be used
	 * only by unit tests
	 * 
	 * @param ad the alarm definition to add
	 */
	public abstract void addSimpleSseAlarmDefinition(IAlarmDefinition ad);

	/**
	 * Manually adds combination alarm definition for FSW channels. Should be used
	 * only by unit tests
	 * 
	 * @param ad the alarm definition to add
	 */
	public abstract void addFswCombinationAlarm(ICombinationAlarmDefinition ad);

	/**
	 * Manually adds combination alarm definition for SSE channels. Should be used
	 * only by unit tests
	 * 
	 * @param ad the alarm definition to add
	 */	
	public abstract void addSseCombinationAlarm(ICombinationAlarmDefinition ad);
	
	/**
	 * Loads all alarm dictionaries using the dictionary configuration
	 * established when the manager was constructed. This includes both system
	 * level and user level files. Once loaded, another invocation will not
	 * cause another load unless one of the clear methods is invoked. Note that
	 * alarm dictionaries are not required. Failure to find one will not
	 * result in an error.
	 * 
	 * @param chanMap
	 *            map of channel definitions, needed to load alarms
	 * @throws DictionaryException if there is a problem loading an alarm file
	 */
	public abstract void loadAll(Map<String, IChannelDefinition> chanMap) throws DictionaryException;

	/**
	 * Loads all alarm dictionaries using the supplied dictionary configuration,
	 * including both system level and user level files. Also causes the default
	 * dictionary configuration in the manager to be set to the supplied one.
	 * Once loaded, another invocation will not reload them unless one of the
	 * clear methods is used to clear one or more of the loaded dictionaries.
	 * Note that alarm dictionaries are not required. Failure to find one will
	 * not result in an error.
	 * 
	 * @param config DictionaryConfiguration to use; becomes the new default
	 * 
	 * @param chanMap
	 *            map of channel definitions, needed to load alarms
	 * @throws DictionaryException if there is a problem loading an alarm file
	 */
	public abstract void loadAll(DictionaryProperties config, Map<String, IChannelDefinition> chanMap) throws DictionaryException;

	/**
	 * Loads the specified flight alarm file on top of existing loaded definitions.
	 * This will replace any existing alarms with the same IDs as the newly loaded 
	 * alarms.
	 * 
	 * @param chanMap map of channel definitions, needed to load alarms
	 * @param filename path to alarm file to load
     * @throws DictionaryException if there is a problem loading an alarm file
	 */
	public abstract void addFswAlarmsFromFile(
			Map<String, IChannelDefinition> chanMap, String filename)
			throws DictionaryException;

	/**
	 * Loads the specified SSE alarm file on top of existing loaded definitions.
	 * This will replace any existing alarms with the same IDs as the newly loaded 
	 * alarms.
	 * 
	 * @param chanMap map of channel definitions, needed to load alarms
	 * @param filename path to alarm file to load
     * @throws DictionaryException if there is a problem loading an alarm file
	 */
	public abstract void addSseAlarmsFromFile(
			Map<String, IChannelDefinition> chanMap, String filename)
			throws DictionaryException;


	/**
	 * Loads all flight alarm dictionaries using the dictionary configuration
	 * established when the manager was constructed. This includes both system
	 * level and user level files. Once loaded, another invocation will not
	 * cause another load unless one of the clear methods is invoked. Note that
	 * alarm dictionaries are not required. Failure to find one will not
	 * result in an error.
	 * 
	 * @param chanMap
	 *            map of channel definitions, needed to load alarms
	 * @throws DictionaryException if there is a problem loading an alarm file
	 */
	public abstract void loadFsw(Map<String, IChannelDefinition> chanMap)
			throws DictionaryException;

	/**
	 * Loads all SSE alarm dictionaries using the dictionary configuration
	 * established when the manager was constructed. This includes both system
	 * level and user level files. Once loaded, another invocation will not
	 * cause another load unless one of the clear methods is invoked. Note that
	 * alarm dictionaries are not required. Failure to find one will not
	 * result in an error.
	 * 
	 * @param chanMap
	 *            map of channel definitions, needed to load alarms
	 * @throws DictionaryException if there is a problem loading an alarm file
	 */
	public abstract void loadSse(Map<String, IChannelDefinition> chanMap)
			throws DictionaryException;

	/**
	 * Clears all loaded alarm dictionaries and reload monitors.
	 */
	public abstract void clearAll();
	
	/**
     * Clears all loaded flight alarm dictionaries and reload monitors.
     */
    public abstract void clearFsw();
    
    /**
     * Clears all loaded SSE alarm dictionaries and reload monitors.
     */
    public abstract void clearSse();
}