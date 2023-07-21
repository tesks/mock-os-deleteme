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

import jpl.gds.alarm.serialization.Proto3AlarmHistory;
import jpl.gds.eha.api.channel.IServiceChannelValue;

/**
 * An interface to be implemented by classes that provide a history of EHA alarm
 * states.
 * 
 * @since R8
 */
public interface IAlarmHistoryProvider {
	
	/**
	 * Merge the information stored in history into this implementation.
	 * 
	 */
	public void merge(IAlarmHistoryProvider history);

    /**
     * Add a new (most recent) alarm value for a channel.
     * 
     * @param alarm
     *            the alarm object the value is being added for
     * @param val
     *            The new value to add to the table
     */
    public void addNewValue(IChannelAlarm alarm, IServiceChannelValue val);

    /**
     * Updates statistics for the given alarm object.
     * 
     * @param alarm
     *            the owning alarm object
     * @param stats
     *            the alarm statistics object to store
     * @param isRealtime
     *            true to update the realtime statistics, false for recorded
     * @param isMonitor
     *            true to update station monitor data statistics (if true,
     *            isRealtime is ignored), false to update based on isRealtime
     *            only
     * @param dss
     *            station number of the station monitor data (requires that
     *            isMonitor is true), ignored if isMonitor is false
     */
    public void updateStatistics(IChannelAlarm alarm, IAlarmStatistics stats,
            boolean isRealtime, boolean isMonitor, Integer dss);

    /**
     * Gets the latest statistics for the given alarm object.
     * 
     * @param alarm
     *            the owning alarm object
     * @param isRealtime
     *            true to retrieve the realtime statistics, false for recorded
     * @param isMonitor
     *            true to retrieve the station monitor data statistics (f true,
     *            isRealtime is ignored), false to retrieve based on isRealtime
     *            only
     * @param dss
     *            station number of the station monitor data statistics to
     *            retrieve (requires that isMonitor is true), ignored if
     *            isMonitor is false
     * @return the AlarmStatistics object, or null if no statistics yet recorded
     *         or alarm definition not found
     */
    public IAlarmStatistics getStatistics(IChannelAlarm alarm,
            boolean isRealtime, boolean isMonitor, Integer dss);

    /**
     * Get the most recent value for a channel owned by a specific alarm object.
     * 
     * @param alarm
     *            The owning alarm whose channel's most recent value is wanted
     * @param isRealtime
     *            true if realtime value desired, false if recorded
     * @param isMonitor
     *            true if station monitor data desired (isRealtime will be
     *            ignored), false if retrieval based on isRealtime
     * @param dss
     *            station number of the station monitor data to retrieve
     *            (requires that isMonitor is true), ignored if isMonitor is
     *            false
     * @return The most recent value of the channel
     */
    public IServiceChannelValue getMostRecentValue(IChannelAlarm alarm,
            boolean isRealtime, boolean isMonitor, Integer dss);

    /**
     * Get the history of the last N values for a particular channel owned by
     * the given alarm object.
     * 
     * @param alarm
     *            The owning alarm whose channel's value history is wanted
     * @param isRealtime
     *            true if realtime value desired, false if recorded
     * @param isMonitor
     *            true if station monitor data desired (isRealtime will be
     *            ignored), false if retrieval based on isRealtime
     * @param dss
     *            station number of the station monitor data to retrieve
     *            (requires that isMonitor is true), ignored if isMonitor is
     *            false
     * @return The List of values for the channel. The first (0th) index of the
     *         List is the most recent value and the last (N-1) index of the
     *         List is the oldest value.
     */
    public List<IServiceChannelValue> getValueHistory(IChannelAlarm alarm,
            boolean isRealtime, boolean isMonitor, Integer dss);

    /**
     * Clears all the history values for a single channel owned by the specified
     * alarm object.
     * 
     * Station monitor data history for all DSS IDs for the channel will be
     * cleared.
     * 
     * @param alarm
     *            The owning alarm whose channel to clear
     */
    public void clearValues(IChannelAlarm alarm);

    /**
     * Clears all history values for all alarms.
     */
    public void clearValues();

    /**
     * Sets the channel value that was last used to update the statistics for
     * the specified alarm object.
     * 
     * @param alarm
     *            the owning Alarm object
     * @param isRealtime
     *            true to update the last realtime channel value, false for
     *            recorded
     * @param isMonitor
     *            true to update the last station monitor channel value (if
     *            true, isRealtime is ignored), false to update based on
     *            isRealtime only
     * @param dss
     *            station number of the station monitor data (requires that
     *            isMonitor is true), ignored if isMonitor is false
     * @param value
     *            the channel value to store
     */
    public void setLastValueInStatistics(IChannelAlarm alarm,
            boolean isRealtime, boolean isMonitor, Integer dss,
            IServiceChannelValue value);

    /**
     * Retrieves the last channel value used to update the statistics for the
     * specified alarm object.
     * 
     * @param alarm
     *            the owning alarm object
     * @param isRealtime
     *            true to fetch the last realtime channel value, false for
     *            recorded
     * @param isMonitor
     *            true to fetch the last station monitor channel value (if true,
     *            isRealtime is ignored), false to fetch based on isRealtime
     *            only
     * @param dss
     *            station number of the station monitor data (requires that
     *            isMonitor is true), ignored if isMonitor is false
     * @return channel value
     */
    public IServiceChannelValue getLastValueInStatistics(IChannelAlarm alarm,
            boolean isRealtime, boolean isMonitor, Integer dss);


    /**
     * Writes the alarm history to a protobuf message
     * 
     * @return the protobuf message containing this alarm history
     */
    public Proto3AlarmHistory build();
}