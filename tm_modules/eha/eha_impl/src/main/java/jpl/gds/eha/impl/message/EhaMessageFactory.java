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
package jpl.gds.eha.impl.message;

import com.google.protobuf.InvalidProtocolBufferException;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.eha.api.channel.IClientChannelValue;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaAggregatedChannelValueMessage;
import jpl.gds.eha.api.message.IAlarmedChannelValueMessage;
import jpl.gds.eha.api.message.IChannelValueMessage;
import jpl.gds.eha.api.message.IEhaGroupedChannelValueMessage;
import jpl.gds.eha.api.message.IEhaMessageFactory;
import jpl.gds.eha.api.message.ISuspectChannelTable;
import jpl.gds.eha.api.message.ISuspectChannelsMessage;
import jpl.gds.eha.api.message.aggregation.IAlarmChangeMessage;
import jpl.gds.eha.api.message.aggregation.IEhaChannelGroupMetadata;
import jpl.gds.message.api.IStreamMessage;

/**
 * Factory for creating messages in the EHA projects.
 * 
 * @since R8
 */
public class EhaMessageFactory implements IEhaMessageFactory {

	private final MissionProperties missionProperties;

	public EhaMessageFactory(final MissionProperties missionProperties) {
		this.missionProperties = missionProperties;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.api.message.IEhaMessageFactory#createInternalChannelMessage(jpl.gds.eha.api.channel.IServiceChannelValue)
	 */
	@Override
	public IChannelValueMessage createInternalChannelMessage(final IServiceChannelValue val) {
        return new InternalChannelValueMessage(val, missionProperties);
    }

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.api.message.IEhaMessageFactory#createEndChannelProcMessage(java.lang.String)
	 */
	@Override
	public IStreamMessage createEndChannelProcMessage(
            final String streamId) {
        return new EndChannelProcMessage(streamId);
    }

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.api.message.IEhaMessageFactory#createStartChannelProcMessage(java.lang.String)
	 */
	@Override
	public IStreamMessage createStartChannelProcMessage(
            final String streamId) {
        return new StartChannelProcMessage(streamId);
    }

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.api.message.IEhaMessageFactory#createAlarmedChannelMessage(jpl.gds.eha.api.channel.IClientChannelValue)
	 */
	@Override
	public IAlarmedChannelValueMessage createAlarmedChannelMessage(final IClientChannelValue val) {
		return new AlarmedChannelValueMessage(val, missionProperties);
    }
    
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.api.message.IEhaMessageFactory#createAlarmedChannelMessage(jpl.gds.eha.api.message.IChannelValueMessage)
	 */
	@Override
	public IAlarmedChannelValueMessage createAlarmedChannelMessage(
            final IChannelValueMessage message) {
		return new AlarmedChannelValueMessage(message, missionProperties);
    }

	@Override
	public IAlarmChangeMessage createAlarmChangeMessage(IAlarmedChannelValueMessage message, boolean previouslyInAlarm, boolean currentlyInAlarm) {
		return new AlarmChangeMessage(message, previouslyInAlarm, currentlyInAlarm, missionProperties);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.api.message.IEhaMessageFactory#createGroupedChannelMessage(jpl.gds.eha.api.message.aggregation.IEhaChannelGroupMetadata)
	 */
	@Override
	public IEhaGroupedChannelValueMessage createGroupedChannelMessage(final IEhaChannelGroupMetadata md) {
		return new EhaGroupedChannelValueMessage(md, missionProperties);
    }
	
	@Override
	public IEhaGroupedChannelValueMessage createGroupedChannelMessage(final Proto3EhaAggregatedChannelValueMessage aggMsg) throws InvalidProtocolBufferException {
		return new EhaGroupedChannelValueMessage(aggMsg, missionProperties);
	}

    /**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.api.message.IEhaMessageFactory#createSuspectChannelsMessage(jpl.gds.eha.api.message.ISuspectChannelTable)
	 */
	@Override
	public ISuspectChannelsMessage createSuspectChannelsMessage(
            final ISuspectChannelTable table) {
        return new SuspectChannelsMessage(table);
    }

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.api.message.IEhaMessageFactory#createSuspectChannelsMessage()
	 */
	@Override
	public ISuspectChannelsMessage createSuspectChannelsMessage() {
        return new SuspectChannelsMessage();
    }
}
