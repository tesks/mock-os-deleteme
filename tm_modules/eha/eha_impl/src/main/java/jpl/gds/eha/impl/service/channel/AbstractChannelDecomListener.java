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
package jpl.gds.eha.impl.service.channel;

import java.util.Map;
import java.util.Stack;

import jpl.gds.decom.IDecomListener;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.decom.IDecomMapDefinition;
import jpl.gds.dictionary.api.decom.types.IChannelizableDataDefinition;
import jpl.gds.dictionary.api.decom.types.IDecomMapReference;

/**
 * Common IDecomListener used to manage the channel mapping stack and channel lookup table
 * for an IDecomMapReference
 * 
 * This class can be used to find an IChannelDefinition for IChannelizableDataDefinition 
 * objects without a channel id by searching the lookup table and stack.
 * 
 *
 */
public class AbstractChannelDecomListener implements IDecomListener {
	
    /**
     * map of channel ID to channel definition.
     */
	protected final Map<String, IChannelDefinition> channelLookup;

    /**
     * TODO
     */
	protected final Stack<Map<String, String>> channelMappingStack = new Stack<>();
	
    /**
     * Constructor.
     * 
     * @param channelMap
     *            map of channel ID to channel definition
     */
	public AbstractChannelDecomListener(final Map<String, IChannelDefinition> channelMap) { 
		this.channelLookup = channelMap;
	}
	
	    /**
     * Returns an IChannelDefinition from the channelLookup map using the given
     * IChannelizableDataDefinition The channelMappingStack is iterated on to
     * find a Channel ID if the given definition has an empty channel Id
     * 
     * @param def
     *            Channelizable Data Definition for the Channel
     * @return IChannelDefinition
     */
	protected IChannelDefinition findChannelValue(final IChannelizableDataDefinition def) {
		if(def.shouldChannelize()) {
			String channelId = def.getChannelId();
			if (channelId.isEmpty()) {
				for (final Map<String, String> channelMapping : channelMappingStack) {
					if (channelMapping.containsKey(def.getName())) {
						channelId = channelMapping.get(def.getName());
						break;
					}
				}
			}
			return channelLookup.get(channelId);
		}
		return null;
	}
	
	
	@Override
	public void onMapReference(final IDecomMapReference statement) {
		channelMappingStack.push(statement.getNameToChannelMap());
	}

	@Override
	public void onMapEnd(final IDecomMapDefinition statement) {
		if (!channelMappingStack.isEmpty()) {
			channelMappingStack.pop();
		}
	}

}
