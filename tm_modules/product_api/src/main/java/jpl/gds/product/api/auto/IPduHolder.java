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
package jpl.gds.product.api.auto;

import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Interface for the AUTO CFDP PDU holder
 * 
 * @since R8
 *
 */
@JsonSerialize
public interface IPduHolder {

    /**
     * Set the PDU data
     * 
     * @param pduData
     *            PDU
     */
    public void setPduData(final byte[] pduData);

    /**
     * Get the PDU data
     * 
     * @return byte[] data
     */
    public byte[] getPduData();

    /**
     * Set the Destination Entity ID
     * 
     * @param destinationEntityId
     *            destination
     */
    public void setDestinationEntityId(final long destinationEntityId);

    /**
     * Get the Destination Entity ID
     * 
     * @return destination
     */
    public long getDestinationEntityId();

    /**
     * Set meta data to associate with the PDU
     * 
     * @param metadata
     *            key value
     */
    public void setMetadata(final Map<String, String> metadata);

    /**
     * Get the PDU meta data
     * 
     * @return key value
     */
    public Map<String, String> getMetadata();
}
