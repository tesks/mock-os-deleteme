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
package jpl.gds.shared.checksum;

import java.util.Arrays;

import jpl.gds.shared.annotation.CustomerAccessible;
import jpl.gds.shared.gdr.GDR;

/**
 * An interface for all checksum computations.
 * 
 *
 */
@CustomerAccessible(immutable = true)
public interface IChecksumCalculator {

    /**
     * Calculates the Checksum of a byte array.
     * 
     * @param buffer
     *            the array of bytes to checksum. If the checksum bytes are
     *            included in the array, they should be cleared before
     *            performing the computation.
     * @param offset
     *            the starting offset of the data to checksum in the byte array
     * @param dataLength
     *            the number of bytes to compute checksum across
     * 
     * @return the checksum value, 1's complemented and ready to write into the
     *         data
     */
    long calculateChecksum(byte[] buffer, int offset, int dataLength);

    /**
     * Verifies the checksum of the given byte buffer. The checksum bytes should 
     * immediately follow the buffer to be checked
     * 
     * @param buffer
     *            the array of bytes to checksum, including the checksum.
     * @param offset
     *            the starting offset of the data to checksum in the byte array
     * @param dataLength
     *            the number of bytes to compute checksum across
     * @return true if the checksum validates, false if not
     * 
     */
    default boolean validateChecksum(final byte[] buffer, final int offset, final int dataLength) {
        return validateChecksum(buffer, offset, dataLength, expectedChecksum(buffer, dataLength));
    }
    
    /**
     * Verifies the checksum of the given byte buffer. The checksum bytes
     * should not be included in the buffer to be checked. 
     * 
     * @param buffer
     *            the array of bytes to checksum, including the checksum.
     * @param offset
     *            the starting offset of the data to checksum in the byte array
     * @param dataLength
     *            the number of bytes to compute checksum across
     * @param checkCrc the expected CRC          
     * @return true if the checksum validates, false if not
     * 
     */
    default boolean validateChecksum(final byte[] buffer, final int offset, final int dataLength,
            final long checkCrc) {

        final long checksum = calculateChecksum(buffer, offset, dataLength);

        return checksum == checkCrc;
    }
    
    /**
     * Parses out the expected checksum from the input data.
     * 
     * @param buff
     *            bytes in the frame that contain the checksum
     * @param offset
     *            number of bytes to skip over to read the checksum value
     * 
     * @return integer value of the checksum stored in the frame
     * 
     */
    default long expectedChecksum(final byte[] buff, final int offset) {
        return (long)GDR.get_u16(buff, offset) & 0x0000ffff ;
    }
}