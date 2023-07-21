/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.tc.api.cltu;

import jpl.gds.tc.api.exception.CltuEndecException;

/**
 * Interface for CLTU parsing and extraction in MPS
 *
 */
public interface ICltuParser {

    /**
     * Parse a CLTU from bytes, BCH codewords will be validated
     *
     * @param cltuBytes CLTU bytes
     * @return CLTU object
     */
    ICltu parse(byte[] cltuBytes) throws CltuEndecException;

    /**
     * Parse a CLTU from bytes, optionally validating BCH codewords
     *
     * @param cltuBytes   CLTU bytes
     * @param validateBch true to validate BCH codewords, false to skip validation
     * @return CLTU object
     */
    ICltu parse(byte[] cltuBytes, final boolean validateBch) throws CltuEndecException;

    /**
     * Parse a CLTU from bytes, using custom start and tail sequences
     *
     * @param cltuBytes     CLTU bytes
     * @param startSequence start sequence
     * @param tailSequence  tail sequence
     * @param validateBch   true to validate BCH codewords, false to skip validation
     * @return CLTU object
     * @throws CltuEndecException
     */
    ICltu parse(final byte[] cltuBytes, final byte[] startSequence, final byte[] tailSequence,
                final boolean validateBch) throws CltuEndecException;

}
