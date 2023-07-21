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
 * a field that can be converted to a timestamp for channel samples.
 * 
 *
 */
public interface IChannelTimestampSupport {

    /**
     * Indicates if the field can be interpreted as a channel timestamp.
     * 
     * @return true if the field is a timestamp, false if not
     */
    public boolean isChannelTimestamp();
    
    /**
     * Retrieves the enumerated value indicating whether this field contains a time used in 
     * product channelization, and what type of timestamp it is.
     * @return TimestampType, or null if this field is not a time used in product channelization
     */
    public abstract ProductDecomTimestampType getChannelTimeType();

    /**
     * Sets the enumerated value indicating whether this field contains a time used in 
     * product channelization, and what type of timestamp it is.
     * @param type the timestamp type, or null if this field is not a time used in product channelization
     */
    public abstract void setChannelTimeType(final ProductDecomTimestampType type);

}