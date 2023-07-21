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
 * An interface to be implemented by CFDP PDU factories.
 * 
 *
 * @since R8
 * 
 */
public interface ICfdpPduFactory {

    /**
     * Creates a CFDP PDU object.
     * 
     * @param header the header object for the PDU
     * @param pduData the PDU data
     * @return new ICfdpPdu instance
     */
    ICfdpPdu createPdu(ICfdpPduHeader header, byte[] pduData);

    /**
     * Creates a CFDP PDU object
     * 
     * @param pduData
     *            a single byte array containing the entire CFDP PDU message
     * @return new ICfdpPdu instance
     */
    ICfdpPdu createPdu(final byte[] pduData);

    /**
     * Creates a new CFDP PDU header object.
     * 
     * @return new ICfdpPduHeader instance
     */
    public ICfdpPduHeader createPduHeader();
    
}