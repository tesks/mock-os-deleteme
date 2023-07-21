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

import java.util.List;

import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.alarm.ICombinationAlarmDefinition;

/**
 * An interface to be implemented by classes that calculate combination channel
 * alarms.
 * 
 * @since R8
 */
public interface ICombinationAlarm {

    /**
     * Returns the unique identifier string of this combination alarm.
     * 
     * @return the alarmId unique identifier string
     */
    public String getAlarmId();

    /**
     * Returns the alarm level of this combination alarm.
     *
     * @return the alarmLevel the alarm level
     */
    public AlarmLevel getAlarmLevel();

    /**
     * Returns the source proxy list.
     * 
     * @return list of source alarm proxies
     */
    public List<ICombinationAlarmSourceProxy> getSourceProxies();

    /**
     * Returns the target proxy list.
     * 
     * @return list of target alarm proxies
     */
    public List<ICombinationAlarmTargetProxy> getTargetProxies();

    /**
     * Returns the alarm state of the combination alarm.
     * 
     * @param forRealtime
     *            if true, will return the alarm state of the realtime
     *            telemetry; if false, will return the alarm state of the
     *            recorded telemetry
     * @return alarm state of the combination alarm
     */
    public AlarmState getAlarmState(boolean forRealtime);

    /**
     * Gets the definition of this alarm.
     * 
     * @return the combination alarm definition object
     * 
     */
    public ICombinationAlarmDefinition getDefinition();

    /**
     * Gets the top-level boolean logic group of source alarms for this
     * combination.
     * 
     * @return CombinationAlarmSourceBooleanLogicGroup object
     * 
     */
    public ICombinationAlarmBooleanGroup getSourceGroup();

}