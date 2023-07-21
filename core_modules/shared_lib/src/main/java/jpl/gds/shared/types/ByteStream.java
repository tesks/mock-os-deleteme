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
package jpl.gds.shared.types;

import java.io.IOException;
import java.io.OutputStream;

import jpl.gds.shared.annotation.CustomerExtensible;


/**
 * Basic implementation of a byte stream.
 *
 */
@CustomerExtensible(immutable = true)
public abstract class ByteStream {

    /** number of bytes in each block written by writeFully() **/
    private static final int BLOCK_SIZE = 64 * 1024;


    /**
     * Reset
     */
    public abstract void reset();


    /**
     * Read some bytes.
     *
     * @param bytes Count
     *
     * @return Byte array slice
     */
    public abstract ByteArraySlice read(int bytes);


    /**
     * Skip some bytes.
     *
     * @param bytes Count
     */
    public abstract void skip(long bytes);


    /**
     * Get length.
     *
     * @return Length
     */
    public abstract long getLength();


    /**
     * Get offset.
     *
     * @return Offset
     */
    public abstract long getOffset();


    /**
     * Get non-empty status.
     *
     * @return True if not empty
     */
    public boolean hasMore() {
        return getOffset() < getLength();
    }


    /**
     * Get count of remaining bytes.
     *
     * @return Count
     *
     * @throws IOException I/O error
     */
    public long remainingBytes() throws IOException {
        return getLength() - getOffset();
    }


    /**
     * Write all bytes to output stream.
     *
     * @param out
     *            Stream
     *
     * @throws IOException
     *             I/O error
     */
    public void writeFully(final OutputStream out) throws IOException {
        int bufsize = BLOCK_SIZE;

        reset();
        long remaining = getLength();
        while (remaining > 0) {
            if (remaining < BLOCK_SIZE) {
                bufsize = (int) remaining;
            }
            final ByteArraySlice slice = read(bufsize);
            out.write(slice.array, slice.offset, slice.length);
            remaining -= bufsize;
        }
    }

    /**
     * Write bytes to output stream.
     *
     * @param out
     *            Stream
     * @param bytes
     *            Count
     *
     * @throws IOException
     *             I/O error
     */
    public void write(final OutputStream out, final long bytes) throws IOException {
        if (bytes > remainingBytes()) {
            throw new IOException("Tried to read " + bytes + " of stream with only " + remainingBytes() + " remaining");
        }
        int bufsize = BLOCK_SIZE;

        long remaining = bytes;
        while (remaining > 0) {
            if (remaining < BLOCK_SIZE) {
                bufsize = (int) remaining;
            }
            final ByteArraySlice slice = read(bufsize);
            out.write(slice.array, slice.offset, slice.length);
            remaining -= bufsize;
        }
    }
    
    /**
     * Read data from the stream and return it as a new byte array.
     *
     * @param bytes Count
     *
     * @return Byte array
     *
     * @throws IOException I/O error
     */
    public byte[] readIntoByteArray(final int bytes) throws IOException {
        final ByteArraySlice readSlice = read(bytes);
        if (readSlice.length == 0) {
            throw new IOException("Error reading " + bytes + " bytes from offset "
                    + getOffset());
        }
        final byte[] result = new byte[bytes];
        System.arraycopy(readSlice.array, readSlice.offset, result, 0, readSlice.length);
        return result;
    }
}
