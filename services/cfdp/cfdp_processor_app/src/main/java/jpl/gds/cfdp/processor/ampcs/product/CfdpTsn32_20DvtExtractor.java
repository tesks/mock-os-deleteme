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
package jpl.gds.cfdp.processor.ampcs.product;

import cfdp.engine.TransStatus;

/**
 * {@code CfdpTsn32_20DvtExtractor} extracts the Data Validity Time from the CFDP transaction's sequence number, where
 * the first 32 bits are parsed as seconds and the following 20 bits are parsed as sub-seconds.
 *
 * @since CFDP Release 3
 */
public class CfdpTsn32_20DvtExtractor implements ICfdpDvtExtractor {

    @Override
    public long extractDvtCoarse(final TransStatus status) {
        return status.getTransID().getNumber() >>> 32;
    }

    @Override
    public long extractDvtFine(final TransStatus status) {
        return (status.getTransID().getNumber() >>> 12) & 0xFFFFF;
    }

}