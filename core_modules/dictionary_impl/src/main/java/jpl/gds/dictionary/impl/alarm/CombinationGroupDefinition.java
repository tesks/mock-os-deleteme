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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import jpl.gds.dictionary.api.alarm.AlarmCombinationType;
import jpl.gds.dictionary.api.alarm.ICombinationAlarmDefinition;
import jpl.gds.dictionary.api.alarm.ICombinationGroup;
import jpl.gds.dictionary.api.alarm.ICombinationGroupMember;

/**
 * This class represents a combination group in combination alarm definition. A
 * group consists of a number of source alarms and nested groups, combined using
 * a boolean operator.
 * 
 *
 */
public class CombinationGroupDefinition implements ICombinationGroup {
	private final ICombinationAlarmDefinition parentComboAlarm;

	private final List<ICombinationGroupMember> operands = new LinkedList<ICombinationGroupMember>();
	private final AlarmCombinationType operator;
	private final String group;

	/**
	 * Constructor.
	 * 
	 * @param combinationAlarm
	 *            the parent combination alarm definition
	 * @param comboType
	 *            the combination type (operator) used to combine the states of
	 *            alarms in the group
	 * @param groupId
	 *            a unique ID for this group
	 */
	CombinationGroupDefinition(
			final ICombinationAlarmDefinition combinationAlarm,
			final AlarmCombinationType comboType, final String groupId) {

		this.parentComboAlarm = combinationAlarm;
		this.operator = comboType;
		this.group = groupId;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICombinationGroup#getGroupId()
	 */
	@Override
	public String getGroupId() {

		return group;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICombinationGroup#getOperator()
	 */
	@Override
	public AlarmCombinationType getOperator() {
		return this.operator;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICombinationGroup#getOperands()
	 */
	@Override
	public List<ICombinationGroupMember> getOperands() {
		return Collections.unmodifiableList(this.operands);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICombinationGroup#addOperand(jpl.gds.dictionary.impl.impl.api.alarm.ICombinationGroupMember)
	 */
	@Override
	public void addOperand(final ICombinationGroupMember operand) {
		this.operands.add(operand);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICombinationGroupMember#getParentCombinationAlarm()
	 */
	@Override
	public ICombinationAlarmDefinition getParentCombinationAlarm() {
		return this.parentComboAlarm;
	}
}
