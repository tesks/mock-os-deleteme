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
package jpl.gds.dictionary.impl.alarm;

import jpl.gds.dictionary.api.alarm.AlarmType;
import jpl.gds.dictionary.api.alarm.IAlarmDefinition;
import jpl.gds.dictionary.api.alarm.ICombinationAlarmDefinition;
import jpl.gds.dictionary.api.alarm.ICombinationSource;

/**
 * CombinationAlarmSourceProxyDefinition represents an alarm that is
 * used as a source alarm in an ICombinationAlarmDefinition. A source proxy
 * defines a simple alarm on one channel that is computed when the source
 * channel is received but NOT used to alarm the actual source channel.
 * Instead, it is used in the combination alarm that references it to 
 * compute alarm state on target channels. 
 * <p>
 * Note that a source proxy has no alarm level or DN/EU setting. The DN/EU
 * setting is in the actual alarm wrapped by this class. The level is 
 * irrelevant for combination source.
 * 
 */
public class CombinationAlarmSourceProxyDefinition extends
AlarmDefinition implements ICombinationSource {

	private final ICombinationAlarmDefinition parentCombinationAlarm;
	private final IAlarmDefinition actualAlarm;

	/**
	 * Constructs a new basic combination source proxy definition.
	 * 
	 * @param comboAlarm
	 *            the parent combination alarm definition to which this source
	 *            alarm belongs
	 * @param channelId
	 *            ID of the channel to which this source alarm belongs
	 * @param realAlarm
	 *            the real alarm definition for the source alarm
	 */
	CombinationAlarmSourceProxyDefinition(
			final ICombinationAlarmDefinition comboAlarm,
			final String channelId, final IAlarmDefinition realAlarm) {
		super(AlarmType.COMBINATION_SOURCE, null, channelId, null);

		if (comboAlarm == null) {
			throw new IllegalArgumentException(
					"combo alarm definition may not be null");
		}
		if (realAlarm == null) {
			throw new IllegalArgumentException(
					"source alarm definition may not be null");
		}

		this.parentCombinationAlarm = comboAlarm;
		this.actualAlarm = realAlarm;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.impl.alarm.AlarmDefinition#getUniqueAlarmDefinitionKey()
	 */
	@Override
	protected String getUniqueAlarmDefinitionKey() {

		StringBuilder sb = new StringBuilder(128);
		sb.append("ComboSrc_");
		appendAlarmUniqueKey(sb);
		return sb.toString();
	}


	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.impl.alarm.AlarmDefinition#appendAlarmUniqueKey(java.lang.StringBuilder)
	 */
	@Override
	protected StringBuilder appendAlarmUniqueKey(final StringBuilder sb) {

		sb.append(actualAlarm.getAlarmId());
		return sb;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.impl.alarm.AlarmDefinition#getDisplayString()
	 */
	@Override
	public String getDisplayString() {
		/*
		 * Source proxy alarms are "invisible." If this method is being called,
		 * there's something wrong.
		 */
		throw new UnsupportedOperationException(
				"This method should not be called for " + getClass().getName());
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICombinationGroupMember#getParentCombinationAlarm()
	 */
	@Override
	public ICombinationAlarmDefinition getParentCombinationAlarm() {
		return this.parentCombinationAlarm;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICombinationSource#getActualAlarmDefinition()
	 */
	@Override
	public IAlarmDefinition getActualAlarmDefinition() {
		return actualAlarm;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.impl.alarm.AlarmDefinition#toString()
	 */
	@Override
	public String toString() {
		return this.getAlarmType() + " for combo " + 
				this.parentCombinationAlarm.getAlarmId() + ": " + this.actualAlarm.toString(false);
	}

}
