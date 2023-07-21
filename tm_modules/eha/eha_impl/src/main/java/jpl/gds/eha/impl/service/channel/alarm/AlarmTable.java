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
package jpl.gds.eha.impl.service.channel.alarm;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.dictionary.api.alarm.IAlarmDefinition;
import jpl.gds.dictionary.api.alarm.IAlarmDefinitionProvider;
import jpl.gds.dictionary.api.alarm.ICombinationAlarmDefinition;
import jpl.gds.eha.api.channel.alarm.IAlarmFactory;
import jpl.gds.eha.api.channel.alarm.IChannelAlarm;
import jpl.gds.eha.api.channel.alarm.ICombinationAlarm;
import jpl.gds.eha.api.channel.alarm.ICombinationAlarmSourceProxy;
import jpl.gds.eha.api.channel.alarm.ICombinationAlarmTargetProxy;

/**
 * This class stores all single-channel alarm calculation objects, as created
 * from multiple alarm files. It will parse alarm dictionary files, get the
 * alarm definitions, and create alarm objects from those definitions. It keeps
 * maps of alarm objects by both channel ID and alarm ID. Combination source and
 * target proxy alarms, which also apply to a single channel, are also kept
 * here. The combination alarms themselves are kept in the
 * CombinationAlarmTable, which interacts with this AlarmTable to manage
 * proxy alarms.  This class also works relies upon a Channel Definition Map
 * which must contain the definitions for channels to be alarmed before any
 * alarm file is parsed by this class.
 * 
 *
 * 1/15/15. Major work for the alarm vs alarm
 *          definition split. Renamed class from AlarmDefinitionTable and made
 *          it track alarms rather than definitions of alarms. Use new
 *          combination interfaces. Use new strategy for turning off alarms.
 * 
 * 10/29/15 - getUserAlarmfile function moved here,
 * 			from GdsConfiguration, and added getSseUserAlarm function.
 * 
 */
public class AlarmTable {

	/** Mapping from channel ID to alarm list. */
	private final Map<String, List<IChannelAlarm>> channelIdMapping;

	/** Mapping from Alarm ID to alarm list. */
	private final Map<String, IChannelAlarm> alarmIdMapping;
	
	private final CombinationAlarmTable comboTable;
	
	private final TimeComparisonStrategyContextFlag timeStrategy;

	private final IAlarmFactory alarmFactory;

	    /**
     * Creates an instance of AlarmTable. Initialize all the mappings to empty.
     * 
     * @param alarmFactory
     *            factory for creating alarm objects
     * @param inTimeStrategy
     *            current time comparison strategy
     */
	public AlarmTable(final IAlarmFactory alarmFactory, final TimeComparisonStrategyContextFlag inTimeStrategy) {
		channelIdMapping = new ConcurrentHashMap<String, List<IChannelAlarm>>();
		alarmIdMapping = new ConcurrentHashMap<String, IChannelAlarm>();
		comboTable = new CombinationAlarmTable();
		timeStrategy = inTimeStrategy;
		this.alarmFactory = alarmFactory;
	}

    /**
     * Populates the alarm table from alarm definitions.
     * 
     * @param provider
     *            the alarm definition provider
     */
	public synchronized void populateFromDefinitionProvider(final IAlarmDefinitionProvider provider) {
	    if (provider == null) {
	        throw new IllegalArgumentException("alarm definition provider is null");
	    }
	    
	    clear();
	    
	    provider.getSingleChannelAlarmMapByChannel().forEach((k,v)->{
	        v.forEach(def->addFromSimpleAlarmDefinition(def));
	    });
	    
	    provider.getCombinationAlarmMap().forEach((k,v)->addFromCombinationAlarmDefinition(v));
	}
	
	/**
	 * Given a Channel ID, get its associated alarms. The return list will
	 * include combination proxy alarms as well a other single channel
	 * alarms.
	 * 
	 * @param id the channel ID
	 * 
	 * @return The list of alarm objects corresponding to the input channel ID or
	 *         null if none exist
	 */
	public List<IChannelAlarm> getAlarmsFromChannelId(
			final String id) {

		if (id == null) {
			throw new IllegalArgumentException("Input channel ID was null!");
		}

		final List<IChannelAlarm> current = channelIdMapping.get(id);

		if ((current == null) || current.isEmpty()) {
			return null;
		}

		return current;
	}

    	/**
    	 * Gets the simple alarm object with the given ID, excluding combination proxies.
    	 * 
    	 * @param alarmId the ID of the alarm to fetch
    	 * @return the alarm object, or null if no matching, non-proxy simply alarm
    	 * 
    	 */
    	public IChannelAlarm getAlarmFromAlarmId(final String alarmId) {
    		return (this.alarmIdMapping.get(alarmId));
    	}
    	
    /**
     * Gets the combination alarm table.
     * 
     * @return combination table
     */
    	public CombinationAlarmTable getCombinationAlarmTable() {
    	    return this.comboTable;
    	}
    	

    	/**
    	 * Creates and adds a combination alarm, given its definition. This actually translates
    	 * to adding the combination alarm to the CombinationAlarmTable, and the 
    	 * combination source and target proxies definitions to this table.
    	 * 
    	 * @param comboDef the combination alarm definition
    	 * 
    	 */
    	private void addFromCombinationAlarmDefinition(final ICombinationAlarmDefinition comboDef) {
    		
    		if (comboDef == null) {
    			throw new IllegalArgumentException("Null combo alarm definition input!");
    		}
    		
    		final ICombinationAlarm combo = alarmFactory.createCombinationAlarm(comboDef, timeStrategy);
    		final List<ICombinationAlarmSourceProxy> sources = combo.getSourceProxies();
    		for (final ICombinationAlarmSourceProxy proxy: sources) {
    			addAlarmToChannel(proxy);
    		}
    		final List<ICombinationAlarmTargetProxy> targets = combo.getTargetProxies();
    		for (final ICombinationAlarmTargetProxy proxy: targets) {
    			addAlarmToChannel(proxy);
    		}
    		comboTable.addCombinationAlarm(
    				comboDef.getAlarmId(), combo);
    	}

    /**
	 * Clear all alarm definitions. Also clears the combination alarm table,
	 * and stops monitoring of all alarm files.
	 */
	private synchronized void clear() {
	    comboTable.clear();
	    channelIdMapping.clear();
	    alarmIdMapping.clear();
	}


	/**
	 * Creates and adds a new simple alarm to this table, given its definition.
	 * 
	 * @param def
	 *            The alarm definition to create an alarm for
	 * 
	 */
	private void addFromSimpleAlarmDefinition(final IAlarmDefinition def) {
		if (def == null) {
			throw new IllegalArgumentException("Null alarm definition input!");
		}

		
		final IChannelAlarm actualAlarm = addAlarmToChannel(alarmFactory.createAlarm(def, timeStrategy));
		if (def.getAlarmId() != null) {
		    this.alarmIdMapping.put(def.getAlarmId(), actualAlarm);
		}
	}


	
	/**
	 * Adds the given alarm to the table, for the channel the alarm
	 * object references. 
	 * 
	 * @param alarm the alarm object to add
	 * @return the same alarm object supplied as input
	 */
	private IChannelAlarm addAlarmToChannel(final IChannelAlarm alarm) {
		final String chanId = alarm.getDefinition().getChannelId();
		List<IChannelAlarm> list = channelIdMapping.get(chanId);

		if (list == null) {
			list = new CopyOnWriteArrayList<IChannelAlarm>();
			channelIdMapping.put(chanId, list);
		}
		list.add(alarm);
		return alarm;
	}

}
