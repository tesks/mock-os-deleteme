/*
 * Copyright 2006-2020. California Institute of Technology.
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

package jpl.gds.product.processors.descriptions;

/**
 * Simple description of the LOG PDPP processor, helps to create child product and session names
 * Mnemonic is "logger"
 */
public class LoggerPdppDescription extends AbstractPdppProcessorDescription {

    /**
     * Passthrough to the abstract class constuctor
     */
    public LoggerPdppDescription() {
        super("LOG", "LOGGED from Session");
    }

}
