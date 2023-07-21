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
package jpl.gds.dictionary.api.channel;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import jpl.gds.dictionary.api.IDefinitionProviderLoadStatus;

/**
 * An interface to be implemented by classes that serve up channel definitions.
 * 
 *
 * @since R8
 */
public interface IChannelDefinitionProvider extends IDefinitionProviderLoadStatus {
    
    /**
     * Gets the channel definition corresponding to a specified channel ID.
     * 
     * @param id channel ID
     * 
     * @return IChannelDefinition object for the channel, or null if not found
     */
    public IChannelDefinition getDefinitionFromChannelId(final String id);
    
    /**
     * Gets a map of channel ID to channel definition objects.
     * 
     * @return Map of string channel ID to IChannelDefinition
     */
    public Map<String, IChannelDefinition> getChannelDefinitionMap();
    
    /**
     * Retrieves a list channel derivations definitions.
     * 
     * @return list of IChannelDerivation objects; list will be empty (not null)
     *         if no derivations exist
     */
    public List<IChannelDerivation> getChannelDerivations();
    
    /**
     * Retrieves a sorted set of all channel IDs.
     * 
     * @return Set of channel ID strings
     */
    public SortedSet<String> getChanIds();
    
    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.dictionary.api.IDefinitionProviderLoadStatus#isLoaded()
     */
    @Override
    default public boolean isLoaded() {
        return true;
    }
      
}
