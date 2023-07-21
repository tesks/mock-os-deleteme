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

import java.util.ArrayList;
import java.util.List;

import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.dictionary.api.alarm.AlarmCombinationType;
import jpl.gds.dictionary.api.alarm.ICombinationGroup;
import jpl.gds.dictionary.api.alarm.ICombinationGroupMember;
import jpl.gds.dictionary.api.alarm.ICombinationSource;
import jpl.gds.eha.api.channel.alarm.AlarmState;
import jpl.gds.eha.api.channel.alarm.IAlarmFactory;
import jpl.gds.eha.api.channel.alarm.ICombinationAlarm;
import jpl.gds.eha.api.channel.alarm.ICombinationAlarmBooleanGroup;
import jpl.gds.eha.api.channel.alarm.ICombinationAlarmSourceElement;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * <code>CombinationAlarmSourceBooleanLogicGroup</code> is the reference
 * implementation of the <code>ICombinationAlarmSourceElement</code> for boolean
 * combination of source (formerly called "trigger") alarms. This class groups
 * together one or more basic or complex source elements. (Basic source elements
 * are of the class <code>CombinationAlarmSourceProxy<code>.) This class also
 * carries a boolean operation that is to be applied to the alarms composing
 * this boolean logic group. It is possible to nest further
 * <code>CombinationAlarmSourceBooleanLogicGroup</code> levels underneath, in
 * which case the boolean operation will be applied only to the resulting,
 * overall boolean value (i.e. in alarm or not in alarm) of the nested boolean
 * logic group(s).
 * 
 * 1/8/15. Definition object split from this object
 *          for alarm dictionary refactoring.
 *
 * @since AMPCS R6.1
 * @see ICombinationAlarmSourceElement
 */
public class CombinationAlarmSourceBooleanLogicGroup implements ICombinationAlarmBooleanGroup {

	private static final Tracer tracer = TraceManager.getDefaultTracer();


	private final ICombinationGroup definition;
	private final ICombinationAlarm parentCombinationAlarm;
	private final List<ICombinationAlarmSourceElement> operands;
	private final AlarmCombinationType operator;

	private final IAlarmFactory alarmFactory;


	    /**
     * Default constructor.
     * 
     * @param comboAlarm
     *            the combination alarm that owns this boolean logic group
     * @param groupDef
     *            the dictionary definition of this group
     * @param alarmFactory
     *            the factory to use when creating alarm objects the boolean
     *            operation to be performed on this group's operands
     * @param timeStrategy
     *            the current time comparison strategy
     */
	public CombinationAlarmSourceBooleanLogicGroup(final ICombinationAlarm comboAlarm,
			final ICombinationGroup groupDef, 
			final IAlarmFactory alarmFactory,
			final TimeComparisonStrategyContextFlag timeStrategy) {
		definition = groupDef;
		operands = new ArrayList<ICombinationAlarmSourceElement>(groupDef.getOperands().size());
		parentCombinationAlarm = comboAlarm;
		this.operator = groupDef.getOperator();
		this.alarmFactory = alarmFactory;
		build(timeStrategy);
	}

	/**
	 * Add an operand to this boolean logic group.
	 * 
	 * @param operand
	 *            the operand to add
	 */
	private void addOperand(final ICombinationAlarmSourceElement operand) {
		operands.add(operand);
	}

	/**
	 * Builds all nested objects.
	 * @param timeStrategy 
	 */
	private void build(final TimeComparisonStrategyContextFlag timeStrategy) {
		buildSources(this, timeStrategy);
	}

	/**
	 * Builds nested source proxy and boolean group objects, starting from the given group,
	 * and recursing until all operands and nested groups are built.
	 *
	 * @param srcGroup the boolean logic group to crawl
	 * @param timeStrategy
	 *
	 * 3/4/15. Changed to take runtime group object rather than definition object
	 */
	private void buildSources(
			final CombinationAlarmSourceBooleanLogicGroup srcGroup, final TimeComparisonStrategyContextFlag timeStrategy) {

		final List<ICombinationGroupMember> operands = srcGroup.getDefinition().getOperands();

		for (final ICombinationGroupMember operand : operands) {

			if (operand instanceof ICombinationSource) {
				/*
				 * Found a source proxy definition. Create a source proxy for it
				 * and add it as a simple operand to the current source group.
				 */
				final CombinationAlarmSourceProxy sourceProxy = 
						new CombinationAlarmSourceProxy(parentCombinationAlarm, (ICombinationSource)operand, timeStrategy, alarmFactory);
				srcGroup.addOperand(sourceProxy);

			} else {
				/*
				 * Found a boolean group definition. Create a new boolean group object
				 * for it, add it as an operand to the current source group. The constructor 
				 * will recursively crawl the operands the new group contains and build
				 * its internal objects.
				 * 
				 * Removed recursive call. The boolean group
				 * constructor already does the recursion
				 */
				final CombinationAlarmSourceBooleanLogicGroup nestedGroup = 
						new CombinationAlarmSourceBooleanLogicGroup(this.parentCombinationAlarm, (ICombinationGroup)operand, alarmFactory, timeStrategy);
				srcGroup.addOperand(nestedGroup);

			}

		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AlarmState getRealtimeAlarmState() {
		checkOperandsCount();
		final List<AlarmState> alarmStates = new ArrayList<AlarmState>(
				operands.size());

		for (final ICombinationAlarmSourceElement op : operands) {
			alarmStates.add(op.getRealtimeAlarmState());
		}

		return calculateOverallState(alarmStates);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AlarmState getRecordedAlarmState() {
		checkOperandsCount();
		final List<AlarmState> alarmStates = new ArrayList<AlarmState>(
				operands.size());

		for (final ICombinationAlarmSourceElement op : operands) {
			alarmStates.add(op.getRecordedAlarmState());
		}

		return calculateOverallState(alarmStates);
	}

	private void checkOperandsCount() {

		if (operands.size() < 1) {
			throw new IllegalStateException(
					"Boolean logic group in combination alarm "
							+ parentCombinationAlarm.getAlarmId()
							+ " has no operand to perform boolean operation");

		} else if (operands.size() < 2) {
			tracer.warn("Boolean logic group in combination alarm "
					+ parentCombinationAlarm.getAlarmId()
					+ " has only one operand");

		}

	}

	private AlarmState calculateOverallState(final List<AlarmState> states) {
		/*
		 * Because the AlarmState is defined so that we must use three-valued
		 * logic, we follow the Kleene's logic convention.
		 * 
		 * Find the truth table here:
		 * http://en.wikipedia.org/wiki/Three-valued_logic#Kleene_logic
		 */

		int inAlarmCount = 0;
		int notInAlarmCount = 0;
		int unknownCount = 0;

		for (final AlarmState st : states) {
			if (st == AlarmState.IN_ALARM) {
				inAlarmCount++;
			} else if (st == AlarmState.NOT_IN_ALARM) {
				notInAlarmCount++;
			} else if (st == AlarmState.UNKNOWN) {
				unknownCount++;
			} else {
				throw new IllegalStateException("Unrecognized enum value for " + st.getClass().getName() + " encountered");
			}
		}

		switch (operator) {
		case AND:
			/*
			 * Kleene's logic:
			 * 
			 * (1) Result is false if there is at least one false operand.
			 * (2) If all operands are unknown, the result is unknown.
			 * (3) If there are no false operands but there is at least one
			 *     unknown, then the result is unknown.
			 * (4) Result is true if and only if all operands are true.
			 */

			if (notInAlarmCount > 0) {
				return AlarmState.NOT_IN_ALARM;
			} else if (unknownCount > 0) {
				return AlarmState.UNKNOWN;
			} else {
				return AlarmState.IN_ALARM;
			}

		case OR:
			/*
			 * Kleene's logic:
			 * 
			 * (1) Result is true if there is at least one true operand.
			 * (2) If all operands are unknown, the result is unknown.
			 * (3) If there are no true operands but there is at least one
			 *     unknown, then the result is unknown.
			 * (4) Result is false if any only if all operands are false.
			 */

			if (inAlarmCount > 0) {
				return AlarmState.IN_ALARM;
			} else if (unknownCount > 0) {
				return AlarmState.UNKNOWN;
			} else {
				return AlarmState.NOT_IN_ALARM;
			}

		case XOR:
			/*
			 * Convention:
			 * 
			 * (1) If there exists any unknown, the result is unknown.
			 * (2) Result is true if odd number of operands are true.
			 * (3) Result if false if even number of operands are true.
			 */

			if (unknownCount > 0) {
				return AlarmState.UNKNOWN;
			} else if ((inAlarmCount & 1) == 0) {
				// inAlarmCount is even
				return AlarmState.NOT_IN_ALARM;
			} else {
				return AlarmState.IN_ALARM;
			}

		default:
			throw new IllegalStateException("Unrecognized enum value for "
					+ operator.getClass().getName() + " encountered");
		}

	}

	/**
	 * Returns the boolean logic group's operands
	 * 
	 * @return the operands of this group
	 */
	@Override
	public List<ICombinationAlarmSourceElement> getOperands() {
		return operands;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.ICombinationAlarmBooleanGroup#getGroupId()
     */
	@Override
    public String getGroupId() {
		return this.definition.getGroupId();
	}
	
	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.ICombinationAlarmBooleanGroup#getDefinition()
     */
	@Override
    public ICombinationGroup getDefinition() {
		return this.definition;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.ICombinationAlarmBooleanGroup#getOperator()
     */
	@Override
    public AlarmCombinationType getOperator() {
		return this.definition.getOperator();
	}

}
