/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.cfdp.common;

/**
 * Class CfdpPduConstants
 *
 */
public class CfdpPduConstants {

    /**
     * Theoretical maximum PDU size calculation (based on CCSDS 727.0-B-4):
     *
     *   4 bytes fixed header
     * + 8 bytes source entity ID
     * + 8 bytes tx seq num
     * + 8 bytes destination entity ID
     * + 65536 bytes data
     * -------------------------------
     * = 65564 total bytes
     */
    public static final int THEORETICAL_MAX_PDU_SIZE_IN_BYTES = 65564;

}
