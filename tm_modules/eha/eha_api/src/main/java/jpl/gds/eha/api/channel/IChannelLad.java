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
package jpl.gds.eha.api.channel;

import java.io.Writer;
import java.util.List;

import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.eha.channel.api.ILatestSampleProvider;
import jpl.gds.shared.types.Pair;

/**
 * An interface to be implemented by internal latest channel data tables.
 * 
 * @wince R8
 */
public interface IChannelLad extends ILatestSampleProvider {

    /**
     * Adds a new (most recent) value for a channel
     * 
     * @param val
     *            The new value to add to the table
     * @throws IllegalArgumentException
     *             if there is a problem with the incoming channel value
     */
    public void addNewValue(IServiceChannelValue val) throws IllegalArgumentException;

    /**
     * Gets a list of all channel IDs in the LAD.
     * @param realtime true if the channel IDs desired are realtime, false for recorded
     * @return List of ChannelId object
     */
    public List<String> getAllChannelIds(boolean realtime);

    /**
     * Gets a list of all channel IDs and station pairs in the LAD.
     * @param realtime true if the pairs desired are realtime, false for recorded
     * @return List of Pair object
     */
    public List<Pair<String, Integer>> getAllChannelIdAndStationPairs(
            boolean realtime);

    /**
     * Gets a list of all the channel values in the LAD.
     * 
     * @return list of channel values
     */
    public List<IServiceChannelValue> getChannelValueList();

    /** 
     * Clears all values from the LAD. It is important that this and all methods that
     * modify the hash maps are synchronized. The return value indicates the system time
     * when the last channel was added. This is important in the global LAD.
     * 
     * @return the last time the LAd was modified prior to the clear
     */
    public long clearAll();
    
    /**
     * Gets the channel definition provider in use by this LAD.
     * 
     * @return channel definition provider
     */
    public IChannelDefinitionProvider getDefinitionProvider();
    
    /**
     * Sets the channel definition provider for use by this LAD.
     * 
     * @param toSet
     *            channel definition provider
     */
    public void setDefinitionProvider(IChannelDefinitionProvider toSet);
    
    /**
     * Writes the content of the LAD to the given writer as CSV.
     * 
     * @param writer
     *            output writer
     * @return true if the operation was successful.
     * 
     */
    public boolean writeCsv(Writer writer);

}