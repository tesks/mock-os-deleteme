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

import java.util.ArrayList;
import java.util.List;

import jpl.gds.dictionary.api.alarm.AlarmCombinationType;
import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.alarm.AlarmType;
import jpl.gds.dictionary.api.alarm.IAlarmDefinition;
import jpl.gds.dictionary.api.alarm.ICompoundAlarmDefinition;

/**
 * CompoundAlarmDefinition is used to define alarms that are actually 
 * groups of alarms on the same channel meant to be associated together.  
 * The child alarms are combined using a logical OR, AND, or XOR to determine 
 * the compound alarm state. A Compound Alarm does not have a level. The
 * level is set to the most severe level found in the child alarms 
 * that have been triggered. A compound alarm may also not have a null ID.
 *
 *
 */
public class CompoundAlarmDefinition extends AlarmDefinition implements ICompoundAlarmDefinition {


	/**
	 * The list of alarm definitions that make up this compound alarm.
	 */
	private List<IAlarmDefinition> alarmList;

	/**
	 * The type of logical operation used in the combination of alarms.
	 */
	private AlarmCombinationType operator = AlarmCombinationType.OR;

	/**
	 * Creates a new compound alarm definition.
	 * 
	 * @param alarmId
	 *            a unique ID for the alarm; if null, will be computed
	 * @param chanId
	 *            the ID of the channel the alarm applies to
	 */
	CompoundAlarmDefinition(final String alarmId, final String chanId) {
		super(AlarmType.COMPOUND, alarmId, chanId, null);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICompoundAlarmDefinition#addChildAlarmDefinition(jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition)
	 */
	@Override
	public void addChildAlarmDefinition(final IAlarmDefinition alarm) {
		if (alarmList == null) {
			alarmList = new ArrayList<IAlarmDefinition>();
		}
		alarmList.add(alarm);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICompoundAlarmDefinition#getChildAlarms()
	 */
	@Override
	public List<IAlarmDefinition> getChildAlarms() {
		return alarmList;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.impl.alarm.AlarmDefinition#getAlarmType()
	 */
	@Override
	public AlarmType getAlarmType() {
		return AlarmType.COMPOUND;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.impl.alarm.AlarmDefinition#getDisplayString()
	 */
	@Override
	public String getDisplayString() {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder("Compound (" + operator + ")\n");
		for (IAlarmDefinition alarm : alarmList) {
			result.append(alarm.toString() + "\n");
		}
		return result.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.impl.alarm.AlarmDefinition#appendAlarmUniqueKey(java.lang.StringBuilder)
	 */
	@Override
	protected StringBuilder appendAlarmUniqueKey(final StringBuilder sb) {

		if (alarmList == null || alarmList.size() < 1) {
			throw new IllegalStateException("Tried to create unique key for compound alarm but it has null/empty alarm list");
		}

		sb.append("ANON_COMPOUND");

		for (IAlarmDefinition ad : alarmList) {
			sb.append("_");
			sb.append(ad.getAlarmId());
		}

		return sb;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICompoundAlarmDefinition#getCombinationOperator()
	 */
	@Override
	public AlarmCombinationType getCombinationOperator() {
		return operator;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICompoundAlarmDefinition#setCombinationOperator(jpl.gds.dictionary.impl.impl.api.alarm.AlarmCombinationType)
	 */
	@Override
	public void setCombinationOperator(final AlarmCombinationType operator) {
		this.operator = operator;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#setAlarmLevel(jpl.gds.dictionary.impl.impl.api.alarm.AlarmLevel)
	 */
	@Override
	public void setAlarmLevel(final AlarmLevel alarmLevel) {
		throw new UnsupportedOperationException("Compound alarms do not support a level");
	}
}
