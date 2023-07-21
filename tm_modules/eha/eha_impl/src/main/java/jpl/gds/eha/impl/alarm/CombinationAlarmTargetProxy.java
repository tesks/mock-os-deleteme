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
import jpl.gds.dictionary.api.alarm.ICombinationTarget;
import jpl.gds.eha.api.channel.IAlarmValue;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.channel.alarm.AlarmState;
import jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider;
import jpl.gds.eha.api.channel.alarm.ICombinationAlarm;
import jpl.gds.eha.api.channel.alarm.ICombinationAlarmTargetProxy;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * <code>CombinationAlarmTargetProxy</code> is the reference implementation of
 * the target proxy for combination alarms.
 * 
 * 1/8/15. Definition object split from alarm object
 *          for alarm dictionary refactoring.
 *
 * @since AMPCS R6.1
 */
public class CombinationAlarmTargetProxy extends AbstractAlarm implements ICombinationAlarmTargetProxy {
    private static final Tracer     comboAlarmsLogger = TraceManager.getTracer(Loggers.ALARM);


	private final ICombinationAlarm parentCombinationAlarm;

	    /**
     * Constructs a new target alarm.
     * 
     * @param comboAlarm
     *            the parent combination alarm to which this target alarm
     *            belongs to
     * @param def
     *            the target proxy alarm definition
     * @param timeStrategy
     *            the current time comparison strategy
     */
    public CombinationAlarmTargetProxy(final ICombinationAlarm comboAlarm, final ICombinationTarget def,
            final TimeComparisonStrategyContextFlag timeStrategy) {
		super(def, timeStrategy);

		parentCombinationAlarm = comboAlarm;
	}

	@Override
	public IAlarmValue check(final IAlarmHistoryProvider history, final IServiceChannelValue value) {
		/*
		 * We override the "check" method in this class because the parent's
		 * "check" method includes hysteresis calculation, which is unnecessary
		 * for a combination alarm target proxy class.
		 */

		final AlarmState alarmState = parentCombinationAlarm.getAlarmState(value.isRealtime());
		comboAlarmsLogger.debug("CombinationAlarmTargetProxy: check; parent="
				+ parentCombinationAlarm.getAlarmId() + " " + getDefinition().getChannelId()
				+ (getDefinition().isCheckOnDn() ? " DN" : " EU") + " -> alarm state = " + alarmState);

		if (alarmState == AlarmState.IN_ALARM) {
			/* 
			 * changed second argument from value.isRealtime() to true.
			 * Second argument is if this AlarmValue is in alarm or not, not the realtime/recorded state.
			 */
			return new AlarmValue(this.getDefinition(), true,
					getDefinition().getDisplayString());

		}

		return null;
	}

	@Override
	protected IAlarmValue calculateAlarm(final IAlarmHistoryProvider history, final IServiceChannelValue value) {
		throw new UnsupportedOperationException(
				"This method should not be called for " + getClass().getName());
	}	

}