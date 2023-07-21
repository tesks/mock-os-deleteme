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
/**
 * Project:	AMMOS Mission Data Processing and Control System (MPCS)
 * Package:	jpl.gds.eha.impl.alarm.dictionary.adaptation
 * File:	CombinationAlarmSourceProxy.java
 *
 * Author:	Josh Choi (joshchoi)
 * Created:	Jul 1, 2013
 *
 */
package jpl.gds.eha.impl.alarm;

import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.dictionary.api.alarm.IAlarmDefinition;
import jpl.gds.dictionary.api.alarm.ICombinationSource;
import jpl.gds.eha.api.channel.IAlarmValue;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.channel.alarm.AlarmState;
import jpl.gds.eha.api.channel.alarm.IAlarmFactory;
import jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider;
import jpl.gds.eha.api.channel.alarm.IChannelAlarm;
import jpl.gds.eha.api.channel.alarm.ICombinationAlarm;
import jpl.gds.eha.api.channel.alarm.ICombinationAlarmSourceElement;
import jpl.gds.eha.api.channel.alarm.ICombinationAlarmSourceProxy;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * <code>CombinationAlarmSourceProxy</code> is the reference implementation of
 * the <code>ICombinationAlarmSourceElement</code> for singular source (formerly
 * called "trigger") alarms. This class is the most basic source element. More
 * complex source elements, such as
 * <code>CombinationAlarmSourceBooleanLogicGroup</code> will be composed of one
 * or more of these class objects.
 * 
 * 1/8/15. Definition object split from alarm object
 *          for alarm dictionary refactoring.
 *
 * @since AMPCS R6.1
 * @see ICombinationAlarmSourceElement
 */
public class CombinationAlarmSourceProxy extends AbstractAlarm
implements ICombinationAlarmSourceProxy {
    private static final Tracer      comboAlarmsLogger = TraceManager.getTracer(Loggers.ALARM);

	private final StringBuilder sb;

	private final ICombinationAlarm parentCombinationAlarm;
	private final IChannelAlarm actualAlarm;
	private AlarmState realtimeAlarmState;
	private AlarmState recordedAlarmState;
	private final ICombinationSource sourceDefinition;

	    /**
     * Constructs a new basic source alarm.
     * 
     * @param comboAlarm
     *            the parent combination alarm to which this source alarm
     *            belongs to
     * @param def
     *            definition object for the source proxy alarm
     * @param timeStrategy
     *            the current time comparison strategy
     * @param alarmFactory
     *            factory to use when creating alarm objects
     */
	public CombinationAlarmSourceProxy(final ICombinationAlarm comboAlarm,
			final ICombinationSource def, final TimeComparisonStrategyContextFlag timeStrategy, final IAlarmFactory alarmFactory) {
		super(def.getActualAlarmDefinition(), timeStrategy);

		parentCombinationAlarm = comboAlarm;
		actualAlarm = alarmFactory.createAlarm(def.getActualAlarmDefinition(), timeStrategy);
		realtimeAlarmState = AlarmState.UNKNOWN;
		recordedAlarmState = AlarmState.UNKNOWN;
		sb = new StringBuilder(1024);
		this.sourceDefinition = def;
	}

	@Override
	public IAlarmValue check(final IAlarmHistoryProvider history, final IServiceChannelValue value) {
		/*
		 * We override the "check" method in this class because the parent's
		 * "check" method includes hysteresis calculation, which is unnecessary
		 * for a combination alarm source proxy class.
		 */

		/*
		 * Although the alarm calculation logic can be entirely implemented
		 * inside this method, call the method below to follow the same pattern
		 * as that of other (regular) alarms.
		 */
		final IAlarmValue actualAlarmValue = actualAlarm.check(history, value);

		if (value.isRealtime()) {
			realtimeAlarmState = actualAlarmValue != null ? AlarmState.IN_ALARM
					: AlarmState.NOT_IN_ALARM;

			sb.setLength(0); // For debug text
			comboAlarmsLogger.debug("CombinationAlarmSourceProxy: realtime check; parent="
					+ parentCombinationAlarm.getAlarmId() + " " + getDefinition().getChannelId()
					+ " " + getDefinition().getAlarmId() + " -> realtime alarm state = " + realtimeAlarmState);

		} else {
			recordedAlarmState = actualAlarmValue != null ? AlarmState.IN_ALARM
					: AlarmState.NOT_IN_ALARM;

			sb.setLength(0); // For debug text
			comboAlarmsLogger.debug("CombinationAlarmSourceProxy: recorded check; parent="
					+ parentCombinationAlarm.getAlarmId() + " " + getDefinition().getChannelId()
					+ " " + getDefinition().getAlarmId() + " -> recorded alarm state = " + recordedAlarmState);

		}

		return null; // Source proxies should always return null.
	}

	@Override
	protected IAlarmValue calculateAlarm(final IAlarmHistoryProvider history, final IServiceChannelValue value) {
		throw new UnsupportedOperationException(
				"This method should not be called for " + getClass().getName());
	}

	@Override
	public AlarmState getRealtimeAlarmState() {
		return realtimeAlarmState;
	}

	@Override
	public AlarmState getRecordedAlarmState() {
		return recordedAlarmState;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.impl.alarm.AbstractAlarm#toString()
	 */
	@Override
	public String toString() {
		return this.sourceDefinition.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.impl.alarm.AbstractAlarm#getDefinition()
	 */
	@Override
	public IAlarmDefinition getDefinition() {
		return this.sourceDefinition;
	}
	
	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.ICombinationAlarmSourceProxy#getActualAlarm()
     */
	@Override
    public IChannelAlarm getActualAlarm() {
		return this.actualAlarm;
	}
}