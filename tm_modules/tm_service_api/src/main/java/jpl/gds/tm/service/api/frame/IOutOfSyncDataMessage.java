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
package jpl.gds.tm.service.api.frame;


/**
 * An interface to be implemented by out-of-sync data frame event messages.
 * 
 * @since R8
 */
public interface IOutOfSyncDataMessage extends IFrameEventMessage {

    /**
     * This gets the out of sync bytes data length.
     * 
     * @return the data length
     */
    long getOutOfSyncBytesLength();
    
    /**
     * This gets the out of sync data.
     * 
     * @return the buffer containing the out of sync data
     */
    byte[] getOutOfSyncData();


}