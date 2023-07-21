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

import jpl.gds.dictionary.api.alarm.IAlarmDefinition;
import jpl.gds.eha.api.channel.IAlarmValue;
import jpl.gds.eha.api.channel.IServiceChannelValue;

/**
 * An interface to be implemented by classes that calculate channel alarm
 * states.
 * 
 */
public interface IChannelAlarm {

    /**
     * Returns the definition object corresponding to this run-time alarm
     * object.
     *
     * @return the IAlarmDefinition object
     */
    public IAlarmDefinition getDefinition();

    /**
     * Checks a channel value using the alarm definition and returns an
     * IAlarmValue if the alarm is triggered.
     * 
     * @param history
     *            the alarm history provider
     * @param value
     *            the channel value to check for alarm
     * @return AlarmValue, or null if no alarm is triggered
     */
    public IAlarmValue check(IAlarmHistoryProvider history, IServiceChannelValue value);

}