/*
 * Copyright 2006-2020. California Institute of Technology.
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
package jpl.gds.tcapp.app.reverse.pdu;

import jpl.gds.ccsds.api.cfdp.ICfdpPdu;

import java.util.List;

/**
 * Interface for result of PDU parsing
 */
public interface IPduParserResult {

    /**
     * Getter for pdus
     * @return list of PDUs parsed
     */
    List<ICfdpPdu> getPdus();

    /**
     * Setter for PDUs
     * @param pdus list of PDUs parsed
     */
    void setPdus(List<ICfdpPdu> pdus);

    /**
     * Add a single PDU
     * @param pdu a PDU to add to the list of parsed PDUs
     */
    void addPdu(ICfdpPdu pdu);

    /**
     * Getter for the exception
     * @return the exception thrown during PDU parsing
     */
    Exception getException();

    /**
     * Setter for the exception
     * @param exception an exception that was thrown during PDU parsing
     */
    void setException(Exception exception);
}
