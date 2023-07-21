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
package jpl.gds.eha.api.message;

import com.google.protobuf.InvalidProtocolBufferException;
import jpl.gds.eha.api.channel.IClientChannelValue;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaAggregatedChannelValueMessage;
import jpl.gds.eha.api.message.aggregation.IAlarmChangeMessage;
import jpl.gds.eha.api.message.aggregation.IEhaChannelGroupMetadata;
import jpl.gds.message.api.IStreamMessage;

/**
 * An interface to be implemented by factories that create messages for the EHA
 * projects.
 * 
 * @since R8
 */
public interface IEhaMessageFactory {

    /**
     * Creates an internal (raw) channel value message.
     * 
     * @param val
     *            the channel value for the message
     * @return new message instance
     */
    public IChannelValueMessage createInternalChannelMessage(IServiceChannelValue val);

    /**
     * Creates an end channel processing messages, indicating the end of a
     * related stream of channel values.
     * 
     * @param streamId
     *            the stream ID for the stream of channel messages this one
     *            concludes
     * 
     * @return new message instance
     */
    public IStreamMessage createEndChannelProcMessage(String streamId);

    /**
     * Creates a start channel processing message, indicating the start of a
     * related stream of channel values.
     * 
     * @param streamId
     *            the stream ID for the stream of channel messages this one
     *            begins
     * 
     * @return new message instance
     */
    public IStreamMessage createStartChannelProcMessage(String streamId);

    /**
     * Creates an external (alarmed) channel value message.
     * 
     * @param val
     *            the channel value for the message
     * @return new message instance
     */
    public IAlarmedChannelValueMessage createAlarmedChannelMessage(IClientChannelValue val);

    /**
     * Creates an external (alarmed) channel value message from a raw channel
     * message.
     * 
     * @param message
     *            the raw channel value message
     * @return new message instance
     */
    public IAlarmedChannelValueMessage createAlarmedChannelMessage(IChannelValueMessage message);


    /**
     * Creates an external in-alarm channel value message from an alarmed channel message.
     *
     * @param message
     *              the alarmed channel value message
     * @param previouslyInAlarm
     *              previous alarm state
     * @param currentlyInAlarm
     *              current alarm state
     * @return new message instance
     */
    public IAlarmChangeMessage createAlarmChangeMessage(IAlarmedChannelValueMessage message, boolean previouslyInAlarm, boolean currentlyInAlarm);

    /**
     * Creates a grouped EHA channel message.
     * 
     * @param md
     *            input channel group metadata
     * 
     * @return new message instance
     */
    public IEhaGroupedChannelValueMessage createGroupedChannelMessage(IEhaChannelGroupMetadata md);


    /**
     * Creates a grouped EHA channel message from its binary serialization. Sets event time
     * to the current time.
     * 
     * @param blob a byte array containing the serialized protobuf binary form of the message content

     * @return new message instance
     * @throws InvalidProtocolBufferException  if there is a problem creating the object from the input stream
     */
    public IEhaGroupedChannelValueMessage createGroupedChannelMessage(Proto3EhaAggregatedChannelValueMessage aggMsg) throws InvalidProtocolBufferException;
    
    /**
     * Creates a suspect channels message, based upon the content of the given
     * suspect channels table.
     * 
     * @param table
     *            the current suspect channel table
     * @return new message instance
     */
	ISuspectChannelsMessage createSuspectChannelsMessage(ISuspectChannelTable table);

    /**
     * Creates an empty suspect channels message.
     * 
     * @return new message instance
     */
    public ISuspectChannelsMessage createSuspectChannelsMessage();

}