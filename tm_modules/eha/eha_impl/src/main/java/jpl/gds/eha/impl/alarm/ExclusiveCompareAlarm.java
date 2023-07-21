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

import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.dictionary.api.alarm.IAlarmDefinition;
import jpl.gds.eha.api.channel.IAlarmValue;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider;

/**
 * 
 * Run-time alarm calculation class corresponding to an alarm definition object
 * for an exclusive range alarm, in which a channel is considered to be in alarm
 * if its value is outside of both the lower and upper thresholds (inclusive).
 * This alarm works only for numeric channels.
 * 
 * 
 */
public class ExclusiveCompareAlarm extends AbstractAlarm {

	    /**
     * Constructor
     * 
     * @param def
     *            the definition object for the alarm
     * @param timeStrategy
     *            the current time comparison strategy
     */
	public ExclusiveCompareAlarm(final IAlarmDefinition def, final TimeComparisonStrategyContextFlag timeStrategy) {
		super(def, timeStrategy);
	}

	@Override
	protected IAlarmValue calculateAlarm(final IAlarmHistoryProvider history, final IServiceChannelValue channelVal)
	{
		if (!channelVal.getChannelType().isNumberType()) {
			return null;
		}

		final double checkValue = definition.isCheckOnDn() ? channelVal.doubleValue() : channelVal.getEu();
		if (checkValue <= definition.getLowerLimit() || checkValue >= definition.getUpperLimit()) {
			return new AlarmValue(definition, true, definition.getDisplayString());
		}

		return null;

	}

}
