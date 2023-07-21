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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jpl.gds.eha.api.channel.alarm.ICombinationAlarm;
import jpl.gds.eha.api.channel.alarm.ICombinationAlarmTargetProxy;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;


/**
 * <code>CombinationAlarmTable</code> maintains a keyed set of
 * <code>CombinationAlarm</code> objects, which are not placed into the standard
 * <code>AlarmDefinitionTable</code>.
 * 
 * 8/3/15 Changes to support the new combination alarm schema
 * 1/8/15. Definition objects split from alarm objects
 *          for alarm dictionary refactoring.
 *
 * @see ICombinationAlarm
 */
public final class CombinationAlarmTable {
    private static final Tracer                       comboAlarmsLogger = TraceManager
            .getTracer(Loggers.ALARM);


	    /**
     * The main table of combination alarms. Key = combination alarm ID, value =
     * actual combination alarm definition.
     */
	private final Map<String, ICombinationAlarm> comboIdMapping;

	/**
	 * A convenience table for looking up combination alarms definitions by
	 * their target channels. Key = channel ID, value = set of combination alarm
	 * definitions that target the channel. (Note: Whenever
	 * <code>comboIdMapping</code> is modified, this table should be updated
	 * accordingly.
	 */
	private final Map<String, Set<ICombinationAlarm>> targetChannelComboAlarmsMapping;

	/**
	 * Creates an instance of CombinationAlarmTable. Initialize all the mappings to empty.
	 */
	public CombinationAlarmTable() {
		comboIdMapping = new HashMap<String, ICombinationAlarm>();
		targetChannelComboAlarmsMapping = new HashMap<String, Set<ICombinationAlarm>>();
	}

	/**
	 * Add a new combination alarm to this table for the given ID. If there
	 * already exists a combination alarm of the same ID, the alarm being added
	 * will first be tested to see if it is identical to the existing one. If
	 * the two are identical, then the alarm will not be added, for the purpose
	 * of preserving the existing alarm's state.
	 * 
	 * @param comboId
	 *            the combination alarm ID
	 * @param combo
	 *            The combination alarm definition to add
	 */
	public synchronized void addCombinationAlarm(final String comboId, final ICombinationAlarm combo) {

		if (combo == null) {
			throw new IllegalArgumentException("Null combination alarm input not accepted");
		}

		if (comboId == null) {
			throw new IllegalArgumentException("Null combination alarm ID input not accepted");
		}

//		ICombinationAlarm existingComboAlarm = comboIdMapping.get(comboId);
//
//		if (existingComboAlarm != null) {
//			deleteCombinationAlarm(comboId);
//
//		}

		/*
		 * Note: All source and target proxies should already have been put in
		 * to the AlarmDefinitionTable because the alarm dictionary parser
		 * should have returned them as part of the alarm definitions list. So
		 * here we just put the combination alarm definition only.
		 */
		comboIdMapping.put(comboId, combo);
		addCombinationAlarmToTargetChannelMapping(combo);

	}

	private void addCombinationAlarmToTargetChannelMapping(final ICombinationAlarm comboAlarm) {

		for (final ICombinationAlarmTargetProxy targetProxy : comboAlarm.getTargetProxies()) {
			final String chanId = targetProxy.getDefinition().getChannelId();
			Set<ICombinationAlarm> comboAlarmsTargettingOnChannel = targetChannelComboAlarmsMapping.get(chanId);

			if (comboAlarmsTargettingOnChannel == null) {
				comboAlarmsTargettingOnChannel = new HashSet<ICombinationAlarm>(1);
				targetChannelComboAlarmsMapping.put(chanId, comboAlarmsTargettingOnChannel);
			}

			comboAlarmsTargettingOnChannel.add(comboAlarm);

		}

	}

//	private void deleteCombinationAlarmFromTargetChannelMapping(final ICombinationAlarm comboAlarm) {
//
//		for (ICombinationAlarmTargetProxy targetProxy : comboAlarm.getTargetProxies()) {
//			String chanId = targetProxy.getDefinition().getChannelId();
//			Set<ICombinationAlarm> comboAlarmsTargettingOnChannel = targetChannelComboAlarmsMapping.get(chanId);
//			comboAlarmsTargettingOnChannel.remove(comboAlarm);
//		}
//
//	}

//	/**
//	 * Deletes a combination alarm definition from the table. This method will
//	 * also remove all proxy alarms from the <code>AlarmDefinitionTable</code>.
//	 * 
//	 * @param comboId
//	 *            unique identifier of the combination alarm definition to
//	 *            delete
//	 */
//	private synchronized void deleteCombinationAlarm(final String comboId) {
//
//		if (comboId == null) {
//			throw new IllegalArgumentException("Null combination alarm ID input not accepted");
//		}
//
//		ICombinationAlarm comboAlarmToClear = comboIdMapping.get(comboId);
//
//		if (comboAlarmToClear == null) {
//			comboAlarmsLogger
//			.debug("CombinationAlarmTable: deleteCombinationAlarm("
//					+ comboId
//					+ ") called but the ID is not mapped; returning");
//			return;
//
//		}

//		/*
//		 * Source proxies and target proxies have to be deleted from the
//		 * AlarmDefinitionTable.
//		 */
//		List<ICombinationAlarmSourceProxy> sources = comboAlarmToClear
//				.getSourceProxies();
//		List<ICombinationAlarmTargetProxy> targets = comboAlarmToClear
//				.getTargetProxies();
//
//		for (ICombinationAlarmSourceProxy sourceProxy : sources) {
//			AlarmTable.getInstance().removeCombinationAlarmProxyFromChannelId(
//					sourceProxy, sourceProxy.getDefinition().getChannelId());
//
//		}
//
//		for (ICombinationAlarmTargetProxy targetProxy : targets) {
//			AlarmTable.getInstance().removeCombinationAlarmProxyFromChannelId(
//					targetProxy, targetProxy.getDefinition().getChannelId());
//
//		}
//
//		comboIdMapping.remove(comboId);
//		deleteCombinationAlarmFromTargetChannelMapping(comboAlarmToClear);
//
//	}

	/**
	 * Checks if the channel ID is being targeted by any of the combination
	 * alarms (i.e. the channel has target proxies).
	 * 
	 * @param cid
	 *            the channel ID to check for combination alarms that target it
	 * @return true if there exists combination alarms that target the channel,
	 *         false if the channel is free of targetting combination alarms
	 */
	public synchronized boolean hasCombinationAlarmsTargettingOnChannel(final String cid)
	{
		final Set<ICombinationAlarm> comboAlarmsTargettingChannel = targetChannelComboAlarmsMapping.get(cid);

		if (comboAlarmsTargettingChannel == null || comboAlarmsTargettingChannel.isEmpty()) {
			return false;
		}

		return true;
	}

	/**
	 * Clears this table and removes all associated proxies from the
	 * <code>AlarmTable</code>.
	 */
	public synchronized void clear()
	{
//		/*
//		 * Avoid ConcurrentModificationException.
//		 */
//		Set<String> comboAlarmIdsToDelete = new HashSet<String>(
//				comboIdMapping.keySet());
//
//		for (String comboAlarmIdToDelete : comboAlarmIdsToDelete) {
//			deleteCombinationAlarm(comboAlarmIdToDelete);
//		}

		comboIdMapping.clear();
		targetChannelComboAlarmsMapping.clear();
	}
	
	/**
	 * Gets the combination alarm object with the given alarm ID.
	 * 
	 * @param id the alarm ID
	 * @return the matching CombinationAlarm object, or null if no such alarm found.
	 * 
	 */
	public ICombinationAlarm getCombinationAlarmFromId(final String id) {
		return this.comboIdMapping.get(id);
	}

}
