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
 * {@code ICfdpDvtExtractor} is an interface for objects that extract the Data Validity Time (DVT) from products,
 * product parts, and/or their metadata. Where in the product metadata the DVT can be found is mission-dependent, as
 * well as the format.
 *
 * @since AMPCS CFDP Release 3
 */
public interface ICfdpDvtExtractor {

    /**
     * Extract the Data Validity Time coarse ticks from CFDP transaction metadata.
     *
     * @param status transaction metadata structure
     * @return DVT coarse ticks
     * @throws IllegalStateException thrown if the extractor runs into an issue while parsing the transaction metadata
     * @throws IllegalArgumentException thrown if transaction metadata is missing the required info to extract the DVT
     */
    long extractDvtCoarse(TransStatus status);

    /**
     * Extract the Data Validity Time fine ticks from CFDP transaction metadata.
     *
     * @param status transaction metadata structure
     * @return DVT fine ticks
     * @throws IllegalStateException thrown if the extractor runs into an issue while parsing the transaction metadata
     * @throws IllegalArgumentException thrown if transaction metadata is missing the required info to extract the DVT
     */
    long extractDvtFine(TransStatus status);

}