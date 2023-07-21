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
package jpl.gds.tc.api;

public interface IUplinkMetadata {

    /**
     * Get the AMCPS session ID
     * 
     * @return the AMCPS session ID
     */
    public long getSessionId();

    /**
     * Get the AMPCS host ID
     * 
     * @return the AMPCS host ID
     */
    public int getHostId();

    /**
     * Get the AMPCS messaging topic name
     * 
     * @return the AMPCS topic name
     */
    public String getMessageServiceTopicName();

    /**
     * Set the AMPCS messaging topic name
     * 
     * @param topic topic name
     */
    public void setMessageServiceTopicName(String topic);

    /**
     * Get the spacecraft ID
     * 
     * @return the spacecraft ID
     */
    public int getScid();
    
    /**
     * Generates a metadata string that is parsable by the constructor of this
     * class
     * 
     * @return a metadata string that is parsable by the constructor of this
     *         class;
     */
    public String toMetadataString();

}