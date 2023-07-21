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

import java.util.ArrayList;
import java.util.List;

/**
 * Returns the PDUs parsed, and any errors encountered.
 * This way we can return any error that occurred, without losing PDUs that were parsed successfully beforehand.
 *
 */
public class PduParserResult implements IPduParserResult {

    private List<ICfdpPdu> pdus;
    private Exception exception;

    /**
     * Constructor
     */
    public PduParserResult() {
        pdus = new ArrayList<ICfdpPdu>();
    }

    /**
     * Getter for pdus
     * @return list of PDUs parsed
     */
    @Override
    public List<ICfdpPdu> getPdus() {
        return pdus;
    }

    /**
     * Setter for PDUs
     * @param pdus list of PDUs parsed
     */
    @Override
    public void setPdus(final List<ICfdpPdu> pdus) {
        this.pdus = pdus;
    }

    /**
     * Add a single PDU
     * @param pdu a PDU to add to the list of parsed PDUs
     */
    @Override
    public void addPdu(final ICfdpPdu pdu) {
        pdus.add(pdu);
    }

    /**
     * Getter for the exception
     * @return the exception thrown during PDU parsing
     */
    @Override
    public Exception getException() {
        return exception;
    }

    /**
     * Setter for the exception
     * @param exception an exception that was thrown during PDU parsing
     */
    @Override
    public void setException(final Exception exception) {
        this.exception = exception;
    }
}
