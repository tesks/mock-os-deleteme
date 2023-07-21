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

import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.alarm.AlarmType;
import jpl.gds.dictionary.api.alarm.IAlarmDefinition;
import jpl.gds.eha.api.channel.IAlarmValue;

/**
 * An interface to be implemented by channel alarm factories.
 * 
 * @since R8
 */
public interface IAlarmValueFactory {

	    /**
     * Creates an empty IAlarmValue object.
     * 
     * @return new IAlarmValue
     */
	IAlarmValue create();

    /**
     * Creates an IAlarmValue object from an IAlarmDefinition and sets alarm
     * state.
     * 
     * @param def
     *            the IAlarmDefinition object for the parent alarm definition in
     *            the alarm dictionary
     * @param inAlarm
     *            true if alarm is set, false if cleared
     * @param displayState
     *            display string for the current alarm state - usually includes
     *            alarm type
     * @return new IAlarmValue
     */
	IAlarmValue create(IAlarmDefinition def, boolean inAlarm, String displayState);

	/**
	 * Creates an IAlarmValue object from an IAlarmDefinition and sets alarm state.
	 * 
	 * @param type the definition type of the alarm
	 * @param level the level of the alarm
	 * @param onEu true if alarm is on channel EU, false if on DN
	 * @param inAlarm true if alarm is set, false if cleared
	 * @param displayState display string for the current alarm state - usually includes alarm type
	 * @param alarmId the alarm def id
	 * @return new IAlarmValue
	 */
	IAlarmValue create(AlarmType type, AlarmLevel level, boolean onEu, boolean inAlarm, String displayState, String alarmId);

}