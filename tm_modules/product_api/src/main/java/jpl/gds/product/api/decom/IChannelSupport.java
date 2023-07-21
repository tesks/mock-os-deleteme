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
package jpl.gds.product.api.decom;

/**
 * An interface to be implemented by simple decom field classes that represent
 * a field that can be converted to a channel value.
 * 
 *
 */
public interface IChannelSupport {
    
	 /**
     * Sets the flag indicating if the element is a channel in the channel dictionary this 
     * attribute is true, and the values for the dn-eu conversion, lookup, and alarm are
     * taken from the channel dictionary
     * 
     * @param chan true if the element is a channel; false otherwise
     */
    public abstract void setIsChannel(boolean chan);
    
    /**
     * If the element is a channel in the channel dictionary this attribute is
     * true, and the values for the dn-eu conversion, lookup, and alarm are
     * taken from the channel dictionary
     * 
     * @return true if the element is a channel; false otherwise
     */
    public abstract boolean isChannel();
    
    /**
     * Retrieves the channel ID associated with this element, from the channel
     * dictionary.
     * 
     * @return the channelId as a String
     */
    public abstract String getChannelId();
    
    /**
     * Sets the channel ID associated with this element, from the channel
     * dictionary.
     * 
     * @param id the channelId as a String
     */
    public abstract void setChannelId(String id);
}
