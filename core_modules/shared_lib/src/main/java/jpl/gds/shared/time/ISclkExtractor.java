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
package jpl.gds.shared.time;

import java.util.Map;

import jpl.gds.shared.algorithm.IGenericAlgorithm;
import jpl.gds.shared.annotation.CustomerAccessible;
import jpl.gds.shared.types.BitBuffer;

/**
 * An interface to be implemented by classes that can extract ISclk from a byte array.
 * 
 *
 */
@CustomerAccessible(immutable = true)
public interface ISclkExtractor extends IGenericAlgorithm {

    /**
     * Extract SCLK from given byte array starting at offset.
     * 
     * @param buff
     *            byte array containing SCLK
     * @param startingOffset
     *            starting offset into the array
     * @return ISclk the extracted SCLK value
     */
    public ISclk getValueFromBytes(final byte[] buff, final int startingOffset);

    /**
     * Determine whether the given buffer has enough bytes in order to extract
     * a SCLK
     * 
     * @param buff
     *            byte array that may contain a SCLK
     * @param startingOffset
     *            starting offset into the array
     * @return true if enough bytes are present in the array
     *         to extract a SCLK
     */
    public boolean hasEnoughBytes(final byte[] buff, final int startingOffset);

    /**
     * Extract SCLK from the give bit buffer.
     * 
     * @param buffer
     *            the buffer containing the SCLK to extract
     * @param args
     *            the runtime arguments to the algorithm
     * @return the extracted SCLK value
     */
    public ISclk getValueFromBits(final BitBuffer buffer, final Map<String, Object> args);

}
