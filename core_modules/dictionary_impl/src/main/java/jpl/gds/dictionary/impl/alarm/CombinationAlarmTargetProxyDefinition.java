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
import jpl.gds.dictionary.api.alarm.ICombinationAlarmDefinition;
import jpl.gds.dictionary.api.alarm.ICombinationTarget;

/**
 * CombinationAlarmTargetProxyDefinition represents a target alarm in an
 * ICombinationAlarmDefinition. The target proxy definition really just
 * identifies the target channel ID, whether to alarm DN or EU on the target
 * channel, and the parent combination alarm definition. There is n actual
 * alarm calculation associated with it.
 * 
 *
 */
public class CombinationAlarmTargetProxyDefinition extends
AlarmDefinition implements ICombinationTarget {

	private final ICombinationAlarmDefinition parentCombinationAlarm;

	/**
	 * Constructs a new target alarm proxy.
	 * 
	 * @param comboAlarm
	 *            the parent combination alarm to which this target alarm
	 *            belongs to
	 * @param channelId
	 *            ID of the channel to which this target alarm belongs to
	 * @param isDn
	 *            true if this alarm is on the DN, false if on EU
	 * 
	 */
	CombinationAlarmTargetProxyDefinition(
			final ICombinationAlarmDefinition comboAlarm,
			final String channelId, final boolean isDn) {
		super(AlarmType.COMBINATION_TARGET, null, channelId, comboAlarm.getAlarmLevel());
		this.setCheckOnEu(!isDn);

		this.parentCombinationAlarm = comboAlarm;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.impl.alarm.AlarmDefinition#getUniqueAlarmDefinitionKey()
	 */
	@Override
	protected String getUniqueAlarmDefinitionKey() {

		StringBuilder sb = new StringBuilder(128);

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

		sb.append("ComboTgt_");
		sb.append(this.parentCombinationAlarm.getAlarmId());
		sb.append("_");
		sb.append(getChannelId());
		sb.append("_");
		sb.append(getAlarmLevel().toString());
		sb.append("_");


		if (isDn) {
			sb.append("DN");
		} else {
			sb.append("EU");
		}
		return sb;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.impl.alarm.AlarmDefinition#getDisplayString()
	 */
	@Override
	public String getDisplayString() {
		return parentCombinationAlarm.getAlarmId();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICombinationTarget#getParentCombinationAlarm()
	 */
	@Override
	public ICombinationAlarmDefinition getParentCombinationAlarm() {
		return this.parentCombinationAlarm;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.impl.alarm.AlarmDefinition#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(128);
		sb.append(this.getAlarmType() + " for combo " + this.parentCombinationAlarm.getAlarmId());
		sb.append(", Channel= " + getChannelId());
		sb.append(", Level= " + getAlarmLevel().toString() + ", ");
		if (isDn) {
			sb.append("on DN");
		} else {
			sb.append("on EU");
		}
		return sb.toString();
	}

}
