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

import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.eha.api.channel.ChannelCategoryEnum;
import jpl.gds.eha.api.channel.ChannelValueFilter;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaAggregatedGroup;
import jpl.gds.message.api.IStreamMessage;

/**
 * Interface IEhaGroupedChannelValueMessage
 */
public interface IEhaGroupedChannelValueMessage extends IStreamMessage {
    /**
     * @return
     */
    default public List<IServiceChannelValue> getChannelValueGroup(final ApplicationContext appContext) {
        return getChannelValueGroup(null, appContext);
    }
    
    /**
     * @param filter
     * @return
     */
    public List<IServiceChannelValue> getChannelValueGroup(ChannelValueFilter filter, ApplicationContext appContext);
    
    /**
     * Get realtime state.
     *
     * @return True if realtime
     */
    public boolean isRealtime();


    /**
     * Get channel category.
     *
     * @return Channel category
     */
    public ChannelCategoryEnum getChannelCategory();


    /**
     * Get APID
     *
     * @return APID
     */
    public Integer getApid();


    /**
     * Get VCID
     *
     * @return VCID
     */
    public Integer getVcid();

    /**
     * Get DSSID
     *
     * @return DSSID
     */
    public Integer getDssId();
    
    /**
     * Get channel ids.
     *
     * @return List of channel ids.
     */
    public List<String> getChannelIds();


    /**
     * Get SCLK range state.
     *
     * @return True if SCLK range populated
     */
    public boolean hasErtRange();


    /**
     * Get ERT minimum.
     *
     * @return ERT minimum
     */
    public long getErtMinimumRange();


    /**
     * Get ERT maximum.
     *
     * @return ERT maximum
     */
    public long getErtMaximumRange();

    
    /**
     * Get SCLK range state.
     *
     * @return True if SCLK range populated
     */
    public boolean hasSclkRange();


    /**
     * Get SCLK minimum.
     *
     * @return SCLK minimum
     */
    public long getSclkMinimumRange();


    /**
     * Get SCLK maximum.
     *
     * @return SCLK maximum
     */
    public long getSclkMaximumRange();


    /**
     * Get number of values.
     *
     * @return Count of values
     */
    public int getValuesCount();
    
    
    public int numChannelValues();
    
    
    /**
     * Convert to binary without context headers
     *
     * @return byte[] 
     */
    public byte[] toBinaryWithoutContextHeaders();
    
    
    /**
     * Get the aggregated group.
     *
     * @return Proto3EhaAggregatedGroup group
     */
    public Proto3EhaAggregatedGroup getEhaAggregatedGroup();
    
    
    /**
     * Get SCET maximum.
     *
     * @return SCET maximum
     */
	public long getScetMaximumRange();

    /**
     * Get SCET minimum.
     *
     * @return SCET minimum
     */
	public long getScetMinimumRange();
	
    /**
     * Get SCET range state.
     *
     * @return True if SCLK range populated
     */
	public boolean hasScetRange();

    /**
     * Get RCT range state.
     *
     * @return True if RCT range populated
     */	
    public boolean hasRctRange();

    /**
     * Get RCT minimum.
     *
     * @return RCT minimum
     */    
    public long getRctMinimumRange();

    /**
     * Get RCT maximum.
     *
     * @return RCT maximum
     */
    public long getRctMaximumRange();
}
