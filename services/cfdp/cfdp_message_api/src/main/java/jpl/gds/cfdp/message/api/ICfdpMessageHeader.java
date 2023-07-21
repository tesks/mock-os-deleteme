/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.cfdp.message.api;
/**
 * {@code ICfdpMessageHeader} represents the header object of every CFDP message.
 *
 * @since R8
 */
public interface ICfdpMessageHeader {

    /**
     * Return the CFDP Processor instance ID string
     *
     * @return CFDP Processor instance ID
     */
    String getCfdpProcessorInstanceId();

    /**
     * Set the CFDP Processor instance ID string
     *
     * @param instanceId processor instance ID to set
     */
    void setCfdpProcessorInstanceId(String instanceId);

}