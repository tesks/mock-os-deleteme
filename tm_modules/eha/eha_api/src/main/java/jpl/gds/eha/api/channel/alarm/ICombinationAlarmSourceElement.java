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

import jpl.gds.dictionary.api.alarm.AlarmDefinitionFactory;
import jpl.gds.dictionary.api.alarm.IAlarmDefinition;
import jpl.gds.dictionary.api.alarm.IAlarmDictionary;

/**
 * The <code>ICombinationAlarmSourceElement</code> interface is to be
 * implemented by basic source alarm proxy classes and logic groups that combine
 * those proxy classes. The implementing classes are not alarms by themselves.
 * The proxy alarm classes serve as "samplers" for the overall combination
 * alarm. Each "sampler" proxy alarm will have an underlying, actual simple
 * alarm definition object that will do the actual calculation of whether or not
 * it is in alarm. If the implementing class is a logic group, it is an abstract
 * entity composed of one or more proxy alarms or nested logic groups.
 * <p>
 * This interface is very different from its previous incarnation. In its
 * previous incarnation, the interface was only implemented by a single class,
 * which was then called the CombinationAlarmTriggerProxy. Hence, the interface
 * specifically defined methods that would suit the "trigger proxy" class, which
 * is a basic source alarm proxy class by nature (in other words, its purpose is
 * limited to representing a single underlying real alarm, and so can be treated
 * as a single alarm on a channel). However, this interface now also represents
 * logic groups of those types of basic alarms. Consequently, this interface no
 * longer extends the <code>IAlarmDefinition</code> interface. The supporting
 * object model has been changed accordingly.
 *
 * 
 * @see IAlarmDictionary
 * @see AlarmDefinitionFactory
 * @see IAlarmDefinition
 */
public interface ICombinationAlarmSourceElement {

	/**
	 * Returns the AlarmState value of the source element for realtime EHA.
	 * 
	 * @return AlarmState value of the source element for realtime EHA
	 */
	AlarmState getRealtimeAlarmState();

	/**
	 * Returns the AlarmState value of the source element for recorded EHA.
	 * 
	 * @return AlarmState value of the source element for recorded EHA
	 */
	AlarmState getRecordedAlarmState();
}
