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
package jpl.gds.eha.api.message.aggregation;

import java.util.List;

import jpl.gds.eha.api.channel.IClientChannelValue;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaAggregatedGroup;
import jpl.gds.eha.api.message.IAlarmedChannelValueMessage;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;

public interface IEhaChannelGroupMetadata {

    /**
     * Updates this GroupedMetadata object with the values to that contained
     * within the IAlarmedChannelValueMessage
     * 
     * @param eventTime
     *            the event time from the original IAlarmedChannelValueMessage object
     * @param m
     *            the currently message being processed
     * @return If no triggering change in metadata, return the same
     *         GroupedMetadata object if a triggering change in metadata,
     *         return a newly initialized GroupedMetadata object
     */
    public void updateMetadata(IAccurateDateTime eventTime, IClientChannelValue value);

    /**
     * Determine the number of channels contained in this group
     * 
     * @return the number of group member channels contained in this group
     */
    public int size();

    /**
     * @return the discriminatorKey
     */
    public IEhaChannelGroupDiscriminator getDiscriminatorKey();
    
    
    public AggregateMessageType getAggregateMessageType();
    
    /**
     * Returns a copy of the group member list
     * 
     * @return the groupMembers
     */
    public List<IEhaChannelGroupMember> getGroupMembers();

    /**
     * Returns a copy of the list of Channel IDs contained in this group
     * 
     * @return the channelIds
     */
    public List<String> getChannelIds();
    
    /**
     * @return the number of unique channel IDs contained in this group
     */
    public int getUniqueChannelIdCount();

    /**
     * @return the Low and High SCLK values in the current IEhaChannelGroup
     */
    public List<ISclk> getSclkRange();
    
    /**
     * @return the number of unique SCLKs contained in this group
     */
    public int getUniqueSclkCount();

    /**
     * @return the Protocol Buffer that represents this EHA Group Metadata
     */
    public Proto3EhaAggregatedGroup build();
    
    /**
     * This method comes from java.util.TimerTask
     * 
     * @see #java.util.TimerTask.cancel() for further details.
     * 
     * @return true if this task is scheduled for one-time execution and has not
     *         yet run, or this task is scheduled for repeated execution.
     *         Returns false if the task was scheduled for one-time execution
     *         and has already run, or if the task was never scheduled, or if
     *         the task was already cancelled. (Loosely speaking, this method
     *         returns <tt>true</tt> if it prevents one or more scheduled
     *         executions from taking place.)
     */
    public boolean cancel();

	/**
	 * @return a list of IAlarmedChannelValueMessages representing the aggregated messages
	 * contained in this group.
	 */
	public List<IAlarmedChannelValueMessage> unpackAlarmedChannelValueMessages();
}