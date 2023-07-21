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
import jpl.gds.tcapp.app.reverse.IDataWriter;

import java.util.List;

/**
 * Interface for PDU Writer. Like other writers, it has one entry point used to pass down objects
 * to be written.
 *
 */
public interface IPduWriter extends IDataWriter {

    /**
     * Write method to be implemented. Takes a list of PDUs and unpacks their contents.
     *
     * @param pdus
     *          List of PDUs to write
     */
    void doReversePdus(final List<ICfdpPdu> pdus);
}
