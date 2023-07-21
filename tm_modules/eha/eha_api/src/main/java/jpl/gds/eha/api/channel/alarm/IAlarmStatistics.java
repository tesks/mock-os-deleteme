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

import jpl.gds.alarm.serialization.AlarmStatistics.Proto3AlarmStatistics;
import jpl.gds.eha.api.channel.IAlarmValue;


/**
 * An interface to be implemented by channel alarm statistics objects.
 * 
 * @since R8
 */
public interface IAlarmStatistics {

    /**
     * Updates statistics according to the given IAlarmValue.
     * 
     * @param value IAlarmValue to use for update
     */
    public void updateCounts(IAlarmValue value);

    /**
     * Resets statistics.
     */
    public void resetCounts();

    /**
     * Retrieves the consecutive out of alarm count.
     * 
     * @return the count
     */
    public int getConsecutiveNoAlarms();

    /**
     * Retrieves the consecutive red alarm count.
     * 
     * @return the count
     */
    public int getConsecutiveRedAlarms();

    /**
     * Retrieves the consecutive yellow alarm count.
     * 
     * @return the count
     */
    public int getConsecutiveYellowAlarms();

    /**
     * Gets the latest IAlarmValue.
     * 
     * @return IAlarmValue object, or null if non set
     */
    public IAlarmValue getCurrentValue();

    /**
     * Sets the latest IAlarmValue.
     * 
     * @param value the IAlarmValue object to set
     */
    public void setCurrentValue(IAlarmValue value);

    /**
     * Writes the statistics to a protobuf message
     * 
     * @return the protobuf message containing this channel value
     */
    public Proto3AlarmStatistics build();
    
    /**
     * Populated the statistics from a protobuf message, which should have been created using
     * build().
     * 
     * @param msg the protobuf message containing the channel value
     */
    public void load(Proto3AlarmStatistics msg);

}