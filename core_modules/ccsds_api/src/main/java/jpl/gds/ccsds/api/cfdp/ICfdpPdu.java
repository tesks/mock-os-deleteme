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
package jpl.gds.ccsds.api.cfdp;

/**
 * An interface to be implemented by all CFDP PDUs.
 * 
 *
 * @since R8
 */
public interface ICfdpPdu {
    
    /**
     * Gets the PDU header.
     * 
     * @return ICfdpPduHeader object
     */
    public ICfdpPduHeader getHeader();
    
    /**
     * Gets the PDU data.
     * 
     * @return byte array of PDU content, excluding header
     */
    public byte[] getData();

    /**
     * Sets the PDU data. Only sets the data without altering the existing header data in the object. Useful for
     * creating corrupted CFDP data object.
     *
     * @param pduData PDU data to set
     */
    public void setData(byte[] pduData);
}
