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
 * for a digital alarm.
 * 
 */
public class DigitalCompareAlarm extends AbstractAlarm {

    /**
     * Constructor
     * 
     * @param def
     *            the definition object for the alarm
     * @param timeStrategy
     *            the current time comparison strategy
     */
	public DigitalCompareAlarm(final IAlarmDefinition def, final TimeComparisonStrategyContextFlag timeStrategy) {
		super(def, timeStrategy);
	}

	/**
	 * alarmCheck
	 * Purpose: This function will "and" the two masks together and then "and" the result with the IAlarmValue.
	 *          If the resulting element is non-zero the Alarm condition is assumed to be set.
	 *          Otherwise, the Alarm condition is assumed to be not set.
	 *
	 * Algorithm: The Alarm State is set by the following formulae:
	 *            alarmState = ( dn_value ^ ( ~mask2 ) & mask1 ) | valid_bit_mask;
	 *            Where:
	 *              dn_value is here IAlarmValue
	 *              mask1    is here valueMask
	 *              mask2    is here validMask
	 *              valid_bit_mask is a legacy value and is only needed if MPCS is ported to Voyager.
	 *              I quote from May Tran here:
	 *              "The valid bit mask is there because of the backward compatibility reason.
	 *               Voyager is the only mission that uses this valid bit mask field (sometimes).
	 *               No other JPL mission uses this mask."
	 *
	 * Corrected Formula:  (NOT (chanVal XOR validMask)) AND valueMask
	 *
	 * @return true: alarm value set or false: alarm value not set
	 */
	private boolean alarmCheck ( final long valueMask, final long validMask, final long theIAlarmValue)
	{
		final long alarm = (~(theIAlarmValue ^ validMask)) & valueMask;

		return 0 != alarm;
	}

	@Override
	protected IAlarmValue calculateAlarm(final IAlarmHistoryProvider history, final IServiceChannelValue channelVal)
	{

		if (!channelVal.getChannelType().isIntegralType()) {
			return null;
		}

		if (this.definition.isCheckOnEu()) {
			return null;
		}

		// Get values.
		final long val = channelVal.longValue();
		final long valueMask = this.definition.getValueMask();
		final long validMask = this.definition.getDigitalValidMask();

		if (valueMask != 0) {
			final boolean isInAlarm = this.alarmCheck(valueMask, validMask, val);
			if (isInAlarm) {
				return new AlarmValue(this.definition, true, this.definition.getDisplayString());
			}
		}

		return null;

	}

}
