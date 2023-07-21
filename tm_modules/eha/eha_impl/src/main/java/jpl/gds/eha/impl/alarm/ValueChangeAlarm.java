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
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.eha.api.channel.IAlarmValue;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider;

/**
 *
 * Run-time class corresponding to an alarm definition object for
 * a change alarm, in which a value is considered in alarm if
 * it changes from the previous value in any way.
 * 
 */
public class ValueChangeAlarm extends AbstractAlarm {

	    /**
     * Constructor
     * 
     * @param def
     *            the definition object for the alarm
     * @param timeStrategy
     *            the current time comparison strategy
     */
	public ValueChangeAlarm(final IAlarmDefinition def, final TimeComparisonStrategyContextFlag timeStrategy) {
		super(def, timeStrategy);
	}

	@Override
	protected IAlarmValue calculateAlarm(final IAlarmHistoryProvider history, final IServiceChannelValue channelVal)
	{
		if (channelVal.getChannelType().isStringType() &&
				! this.definition.isCheckOnDn()) {
			return null;
		}

		/*
		 * Added the new parameters
		 * required for fetching station monitor data if applies.
		 */
		final IServiceChannelValue lastValue = history
				.getMostRecentValue(
						this,
						channelVal.isRealtime(),
						channelVal.getDefinitionType() == ChannelDefinitionType.M,
						channelVal.getDssId());

		/*
		 * If there is a time regression
		 * according to the configured time system, do not calculate the alarm
		 * nor save the new value in the history.
		 */
		if (lastValue != null && !timeCompare.timestampIsLater(lastValue, channelVal)) {
			return null;
		}

		if (lastValue == null) {
			history.addNewValue(this, channelVal);
			return null;
		}
		final Object checkValue = this.definition.isCheckOnDn() ? channelVal.getDn() : channelVal.getEu();
		final Object compareValue = this.definition.isCheckOnDn() ? lastValue.getDn() : lastValue.getEu();

		if (!checkValue.equals(compareValue)) {
			history.addNewValue(this, channelVal);
			return new AlarmValue(this.definition, true, this.definition.getDisplayString());
		}

		return null;
	}

}
