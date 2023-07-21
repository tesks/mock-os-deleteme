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
package jpl.gds.eha.impl.service.channel.alarm;

import java.util.ArrayList;
import java.util.List;

import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.shared.channel.ChannelListRange;
import jpl.gds.shared.channel.ChannelListRangeException;

/**
 * An alarm trigger class based upon a list of channel IDs.
 * 
 */
public class ChannelListTrigger extends AbstractAlarmTrigger
{
	private final List<String> channelIds;

    /**
     * Constructor.
     */
	public ChannelListTrigger()
	{
		super();

		channelIds = new ArrayList<String>(16);
	}

    /**
     * List of channel IDs in this trigger.
     * 
     * @return list of channel ID strings
     */
	public List<String> getChannelIds()
	{
		return(channelIds);
	}

    /**
     * Adds a channel ID to this trigger.
     * 
     * @param channelId
     *            ID to add
     */
	public void addChannelId(final String channelId)
	{
		if(channelIds.contains(channelId) == false)
		{
			channelIds.add(channelId);
		}
	}
	
    /**
     * Adds a range of channel IDs to this trigger.
     * 
     * @param startId
     *            starting channel ID
     * @param endId
     *            ending channel ID
     */
	public void addChannelRange(final String startId,final String endId)
	{
		String[] ids = new String[0];
		try
		{
			final ChannelListRange aChannelListSet = new ChannelListRange();
			ids = aChannelListSet.genChannelListFromRange(new String[] { startId + ".." + endId });
		}
		catch (final ChannelListRangeException e)
		{
			trace.error("Error adding the channel list range " + startId + ".." + endId);
		}
		
		for(final String id : ids)
		{
			if(channelIds.contains(id) == false)
			{
				channelIds.add(id);
			}
		}
	}
	
    /**
     * {@inheritDoc}
     */
	@Override
    public boolean matches(final IServiceChannelValue value)
	{
		final String inputId = value.getChanId();
		for(final String channelId : channelIds)
		{
			if(channelId.equalsIgnoreCase(inputId))
			{
				return(true);
			}
		}
		
		return(false);
	}
}
