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
package jpl.gds.eha.impl.alarm;

import java.util.Iterator;
import java.util.List;

import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.dictionary.api.alarm.AlarmCombinationType;
import jpl.gds.dictionary.api.alarm.AlarmType;
import jpl.gds.dictionary.api.alarm.IAlarmDefinition;
import jpl.gds.dictionary.api.alarm.ICompoundAlarmDefinition;
import jpl.gds.eha.api.channel.IAlarmValue;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.channel.alarm.IAlarmFactory;
import jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider;
import jpl.gds.eha.api.channel.alarm.IChannelAlarm;

/**
 * Run-time class corresponding to a compound alarm definition, which combines
 * child alarms on a single channel using a boolean operator.
 * 
 * @since AMPCS R4
 *
 */
public class CompoundAlarm extends AbstractAlarm {
	
	private final TimeComparisonStrategyContextFlag timeStrategy;
	private final IAlarmFactory alarmFactory;

	    /**
     * Constructor.
     * 
     * @param def
     *            the compound alarm definition object that defines this alarm.
     * @param timeStrategy
     *            the CurrentTimeComparisonStrategy object, dictating how
     *            channel samples are compared in time
     * @param alarmFactory
     *            the alarm factory to use when creating alarm objects
     */
	public CompoundAlarm(final ICompoundAlarmDefinition def, final TimeComparisonStrategyContextFlag timeStrategy, final IAlarmFactory alarmFactory) {
		super(def, timeStrategy);
		this.timeStrategy = timeStrategy;
		this.alarmFactory = alarmFactory;
	}

	@Override
	protected IAlarmValue calculateAlarm(final IAlarmHistoryProvider history, final IServiceChannelValue channelVal)
	{
		final ICompoundAlarmDefinition mydef = (ICompoundAlarmDefinition)this.definition;

		final List<IAlarmDefinition> alarmDefList = mydef.getChildAlarms(); 
		if (alarmDefList == null || alarmDefList.size() == 0) {
			return null;
		}

		final IAlarmValue result = new AlarmValue();
		final Iterator<IAlarmDefinition> it = alarmDefList.iterator();
		StringBuffer buffer = null;
		boolean first = true;
		while (it.hasNext()) {
			final IAlarmDefinition def = it.next();
			//Need to get an Alarm run-time object corresponding to def
			final IChannelAlarm alarm = alarmFactory.createAlarm(def, timeStrategy); 
			final IAlarmValue check = alarm.check(history, channelVal);
			if (check != null) {
				if (mydef.getCombinationOperator().equals(AlarmCombinationType.OR)) {
					result.setInAlarm(true);
				} else if (mydef.getCombinationOperator().equals(AlarmCombinationType.XOR)) {
					if (result.isInAlarm()) {
						result.setInAlarm(false);
						break;
					} else {
						result.setInAlarm(true);
					}
				} else {
					if (first) {
						result.setInAlarm(true);
					} else {
						if (!result.isInAlarm()) {
							break;
						}
						result.setInAlarm(true);
					}
				}
				first = false;
				if (result.isInAlarm()) {
					result.setType(result.getType().equals(AlarmType.NO_TYPE) ? def.getAlarmType() : AlarmType.COMPOUND);
					result.setLevel((def.getAlarmLevel().ordinal() > result.getLevel().ordinal()) ? def.getAlarmLevel() : result.getLevel());
					if (buffer == null) {
						buffer = new StringBuffer(check.getState());
					} else {
						buffer.append("-" + check.getState());
					}
				}
			} else {
				if (mydef.getCombinationOperator().equals(AlarmCombinationType.AND)) {
					result.setInAlarm(false);
				}
			}
		}
		if (result.isInAlarm()) {
			result.setState(buffer.toString());
			result.setOnEu(this.definition.isCheckOnEu());
			return result;
		} else {
			return null;
		}
	}

}
