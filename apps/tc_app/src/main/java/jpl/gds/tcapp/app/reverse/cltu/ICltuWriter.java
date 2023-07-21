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
package jpl.gds.tcapp.app.reverse.cltu;

import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tcapp.app.reverse.IDataWriter;

import java.util.List;

/**
 * Interface for CLTU Writer. Like other writers, it has one entry point used to pass down objects
 * to be written.
 *
 */
public interface ICltuWriter extends IDataWriter {
    /**
     * Write method to be implemented. Takes a list of CLTUs and unpacks their contents.
     *
     * @param cltus
     *          List of CLTUs to write
     * @return
     *      a list of byte arrays containing the PDU data that was unpacked from the CLTUs
     */
    List<byte[]> doReverseCltus(final List<ICltu> cltus);

    /**
     * Close up any resources that were opened for writing
     */
    void cleanUp();
}
