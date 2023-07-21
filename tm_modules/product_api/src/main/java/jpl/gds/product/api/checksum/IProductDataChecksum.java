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
package jpl.gds.product.api.checksum;

import java.io.File;
import java.util.List;

import jpl.gds.shared.types.ByteArraySlice;

/**
 * An interface to be implemented by product checksum classes.
 */
public interface IProductDataChecksum {

    /**
     * Compute a checksum for the supplied bytes.
     *
     * The result is returned as an unsigned short (in an int).
     *
     * @param bytes  the data bytes
     * @param offset the starting offset into the data byte array
     * @param length the number of bytes to check
     *
     * @return int checksum
     *
     * @throws ProductDataChecksumException thrown if there is a problem when 
     *              computing the checksum
     */
    public abstract long computeChecksum(final byte[] bytes,
                                      final int    offset,
                                      final int    length)
        throws ProductDataChecksumException;

    /**
     * Compute a checksum for the given list of byte array slices.
     *
     * The result is returned as an unsigned short (in an int).
     *
     * @param bytes List of ByteArraySlice objects containing the data bytes
     *
     * @return int checksum
     *
     * @throws ProductDataChecksumException thrown if there is a problem when 
     *              computing the checksum
     */
    public abstract long computeChecksum(final List<ByteArraySlice> bytes)
        throws ProductDataChecksumException;


    /**
     * Compute a checksum for the supplied bytes.
     * 
     * @param bytes the data bytes to check
     *
     * @return int checksum
     *
     * @throws ProductDataChecksumException thrown if there is a problem when 
     *              computing the checksum
     */
    public abstract long computeChecksum(final byte[] bytes)
        throws ProductDataChecksumException;


    /**
     * Compute a checksum for a portion of the supplied file.
     *
     * @param file   the file containing the data to be checked
     * @param offset the starting offset
     * @param length the number of bytes to sum
     *
     * @return int checksum
     *
     * @throws ProductDataChecksumException thrown if there is a problem when 
     *              computing the checksum
     */
    public abstract long computeChecksum(final File file,
                                      final int  offset,
                                      final int  length)
        throws ProductDataChecksumException;


    /**
     * Compute a checksum for the supplied file.
     *
     * @param file the file containing the data to be checked
     *
     * @return int checksum
     *
     * @throws ProductDataChecksumException thrown if there is a problem when 
     *              computing the checksum
     */
    public abstract long computeChecksum(final File file)
        throws ProductDataChecksumException;

}
