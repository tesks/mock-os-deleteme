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
 * An interface to be implemented by decom field classes that can support
 * channelization of the block of data they contain.
 * 
 *
 */
public interface IChannelBlockSupport {
    /**
     * Indicates whether this definition element contains channelized data. Should
     * be overridden by base classes that support this.
     * @return true to perform channelization, false otherwise
     */
    public abstract boolean hasChannels();
    
    /**
     * Indicates if the field should be channelized.
     * 
     * @return true if should generate channels from the data, false if not
     */
    public boolean isChannelize();

    /**
     * Sets the flag indicating if the field should be channelized.
     * 
     * @param channelize true if should generate channels from the data, false if not
     */
    public void setChannelize(final boolean channelize);
    
}
