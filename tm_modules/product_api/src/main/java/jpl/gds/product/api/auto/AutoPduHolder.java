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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * "PDU Holder" to streamline JSON serialization for the AUTO CFDP Proxy
 * 
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AutoPduHolder implements IPduHolder {

    private byte[]              pduData;
    private long                destinationEntityId;
    private Map<String, String> metadata;

    /**
     * No-arg constructor required for serialization
     */
    public AutoPduHolder() {

    }

    /**
     * Constructor for the PDU holder object
     * 
     * @param pduData
     *            PDU Data
     * @param destinationEntityId
     *            Destination Entity ID
     */
    public AutoPduHolder(final byte[] pduData, final long destinationEntityId) {
        this(pduData, destinationEntityId, new HashMap<>());
    }

    /**
     * Constructor for the PDU holder object
     * 
     * @param pduData
     *            PDU Data
     * @param destinationEntityId
     *            Destination Entity ID
     * @param metadata
     *            key value metadata associated with the PDU
     */
    public AutoPduHolder(final byte[] pduData, final long destinationEntityId, final Map<String, String> metadata) {
        this.pduData = pduData;
        this.destinationEntityId = destinationEntityId;
        this.metadata = metadata;
    }


    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void setPduData(final byte[] pduData) {
        this.pduData = pduData;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void setDestinationEntityId(final long destinationEntityId) {
        this.destinationEntityId = destinationEntityId;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void setMetadata(final Map<String, String> metadata) {
        this.metadata = metadata;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public byte[] getPduData() {
        return pduData;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public long getDestinationEntityId() {
        return destinationEntityId;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

}
