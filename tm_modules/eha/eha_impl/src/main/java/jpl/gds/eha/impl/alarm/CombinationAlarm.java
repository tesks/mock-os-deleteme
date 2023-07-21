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
import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.alarm.ICombinationAlarmDefinition;
import jpl.gds.dictionary.api.alarm.ICombinationGroup;
import jpl.gds.dictionary.api.alarm.ICombinationTarget;
import jpl.gds.eha.api.channel.alarm.AlarmState;
import jpl.gds.eha.api.channel.alarm.IAlarmFactory;
import jpl.gds.eha.api.channel.alarm.ICombinationAlarm;
import jpl.gds.eha.api.channel.alarm.ICombinationAlarmBooleanGroup;
import jpl.gds.eha.api.channel.alarm.ICombinationAlarmSourceElement;
import jpl.gds.eha.api.channel.alarm.ICombinationAlarmSourceProxy;
import jpl.gds.eha.api.channel.alarm.ICombinationAlarmTargetProxy;

/**
 * The combination alarm class. A combination alarm has two sets of components:
 * sources and targets. It "combines" the alarm conditions from the sources and
 * reflects the overall alarm condition to its targets.
 * 
 * For each <code>CombinationAlarm</code> object, these methods should be called
 * in proper order to ensure validity: (1) "setSource" method should be called
 * at least once to ensure that the combination alarm has source elements to
 * determine the alarm condition from; (2) "addTarget" method should be called 0
 * to many times to add target channels to the combination alarm; Note: The
 * order of calling (1) and (2) does not matter; (3) "build" method should be
 * called to ensure that all the proper source and target proxies are in place,
 * and if necessary, create target channels from the source elements; the
 * "build" call will "freeze" the member variables so that subsequent
 * modification attempts will throw exceptions. "build" will also enable
 * "getSourceProxies" and "getTargetProxies" methods. (4) "getSourceProxies" and
 * "getTargetProxies" should be called to obtain the list of alarm objects to be
 * included in the overall alarm list.
 * 
 * 1/8/15. Definition object split from alarm object
 *          for alarm dictionary refactoring.
 *
 * @since AMPCS 6.1.0
 */
public class CombinationAlarm implements ICombinationAlarm {
	private final ICombinationAlarmDefinition definition;
	/*
	 * For combination alarms, below "source" should always be an instance of
	 * CombinationAlarmSourceBooleanLogicGroup.
	 */
	private ICombinationAlarmSourceElement source;
	private final List<ICombinationAlarmSourceProxy> flatSources;
	private final List<ICombinationAlarmTargetProxy> targets;
	private final IAlarmFactory alarmFactory;

	    /**
     * Constructs a new combination alarm.
     * 
     * @param def
     *            the combination alarm definition
     * @param timeStrategy
     *            the current time comparison strategy
     * @param alarmFactory
     *            factory for creating alarm objects
     */
    public CombinationAlarm(final ICombinationAlarmDefinition def,
            final TimeComparisonStrategyContextFlag timeStrategy, final IAlarmFactory alarmFactory) {
		definition = def;
		flatSources = new ArrayList<ICombinationAlarmSourceProxy>(3);  // Default the source size to 3
		targets = new ArrayList<ICombinationAlarmTargetProxy>(3); // Default the target size to 3
		this.alarmFactory = alarmFactory;
		build(timeStrategy);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.ICombinationAlarm#getAlarmId()
     */
	@Override
    public String getAlarmId() {
		return definition.getAlarmId();
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.ICombinationAlarm#getAlarmLevel()
     */
	@Override
    public AlarmLevel getAlarmLevel() {
		return definition.getAlarmLevel();
	}

	/**
	 * Complete the definition of this combination alarm and register it to the
	 * alarms table. If no target proxies have been specified, build the target
	 * proxies from the source elements. Source element and targets will be
	 * frozen after this call and can no longer be modfiied. Successful
	 * completion of this method call gives the caller assurance that the
	 * combination alarm definition is validated to be safe, and can be added to
	 * the alarm tables.
	 * @param timeStrategy 
	 * 
	 * @throws IllegalStateException
	 *             thrown when the combination alarm defined thus far cannot
	 *             function properly as is (e.g. missing the source element)
	 */
	private void build(final TimeComparisonStrategyContextFlag timeStrategy) throws IllegalStateException {

		final ICombinationGroup sourceDef = this.definition.getSourceGroup();

		if (sourceDef == null) {
			throw new IllegalStateException("Combination alarm " + getAlarmId()
					+ " validation failed: No source element definition");
		}


		this.source = new CombinationAlarmSourceBooleanLogicGroup(this, sourceDef, alarmFactory, timeStrategy);

		buildFlatSources((CombinationAlarmSourceBooleanLogicGroup)this.source);

		buildTargetProxiesList();
	}

	/**
	 * Builds the target proxy objects from the target proxy definitions.
	 */
	private void buildTargetProxiesList() {
		final List<ICombinationTarget> targetDefs = definition.getTargets();
		for (final ICombinationTarget target: targetDefs) {
			final CombinationAlarmTargetProxy proxy = new CombinationAlarmTargetProxy(this, target, null);
			targets.add(proxy);
		}
	}

	/**
	 * Build a flat list of source proxies from a hierarchical source element.
	 * This method can be invoked recursively for the nested sources.
	 * 
	 * @param src
	 *            the source element to crawl
	 */
	private void buildFlatSources(
			final CombinationAlarmSourceBooleanLogicGroup src) {

		final List<ICombinationAlarmSourceElement> operands = src.getOperands();

		for (final ICombinationAlarmSourceElement operand : operands) {

			if (operand instanceof ICombinationAlarmSourceProxy) {

				/*
				 * Plain source proxy.
				 */
				flatSources.add((CombinationAlarmSourceProxy)operand);

			} else {
				/*
				 * Found a boolean group. Recursively crawl its operands.
				 */
				buildFlatSources((CombinationAlarmSourceBooleanLogicGroup) operand);

			}

		}

	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.ICombinationAlarm#getSourceProxies()
     */
	@Override
    public List<ICombinationAlarmSourceProxy> getSourceProxies() {

		return flatSources;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.ICombinationAlarm#getTargetProxies()
     */
	@Override
    public List<ICombinationAlarmTargetProxy> getTargetProxies() {

		return targets;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.ICombinationAlarm#getAlarmState(boolean)
     */
	@Override
    public AlarmState getAlarmState(final boolean forRealtime) {
		return forRealtime ? source.getRealtimeAlarmState() : source
				.getRecordedAlarmState();

	}
	
	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.ICombinationAlarm#getDefinition()
     */
	@Override
    public ICombinationAlarmDefinition getDefinition() {
		return this.definition;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.ICombinationAlarm#getSourceGroup()
     */
	@Override
    public ICombinationAlarmBooleanGroup getSourceGroup() {
		return (ICombinationAlarmBooleanGroup)this.source;
	}
}
