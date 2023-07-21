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
package jpl.gds.eha.api.channel.alarm;

import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.dictionary.api.alarm.IAlarmDefinition;
import jpl.gds.dictionary.api.alarm.ICombinationAlarmDefinition;

/**
 * An interface to be implemented by EHA alarm factories.
 * 
 * @since R8
 */
public interface IAlarmFactory {

	    /**
     * Creates an instance of a single channel alarm object given its dictionary
     * definition. Can also be passed an ICompoundAlarmDefinition.
     * 
     * @param def
     *            the IAlarmDefinition object for the alarm
     * @param timeStrategy
     *            current time comparison strategy
     * @return alarm object
     */
	IChannelAlarm createAlarm(IAlarmDefinition def, TimeComparisonStrategyContextFlag timeStrategy);

	    /**
     * Creates an instance of a combination (multi-channel) alarm object given
     * its dictionary definition.
     * 
     * @param def
     *            the definition object for the combination alarm
     * @param timeStrategy
     *            current time comparison strategy
     * @return a combination alarm object
     */
	ICombinationAlarm createCombinationAlarm(ICombinationAlarmDefinition def,
			TimeComparisonStrategyContextFlag timeStrategy);

}