/*
 * Copyright 2006-2017. California Institute of Technology.
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
package jpl.gds.product.impl.checksum;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.zip.CRC32;

import jpl.gds.product.api.checksum.IProductDataChecksum;
import jpl.gds.product.api.checksum.ProductDataChecksumException;
import jpl.gds.shared.types.ByteArraySlice;

/**
 * Calculates the CRC32 checksum of data using the CRC32 that is part of the Gzip (IETF RFC 1952)
 * file standard.
 * 
 *
 */
public class Crc32ProductDataChecksum implements IProductDataChecksum {

    @Override
    public long computeChecksum(final byte[] bytes, final int offset, final int length)
            throws ProductDataChecksumException {
        final CRC32 calculator = new CRC32();
        calculator.update(bytes, offset, length);
        return calculator.getValue();
    }

    @Override
    public long computeChecksum(final List<ByteArraySlice> bytes) throws ProductDataChecksumException {
        final CRC32 calculator = new CRC32();
        for (final ByteArraySlice slice : bytes) {
            calculator.update(slice.array);
        }
        return calculator.getValue();
    }

    @Override
    public long computeChecksum(final byte[] bytes) throws ProductDataChecksumException {
        final CRC32 calculator = new CRC32();
        calculator.update(bytes);
        return calculator.getValue();
    }

    @Override
    public long computeChecksum(final File file, final int offset, final int length)
            throws ProductDataChecksumException {
        if (file == null) {
            throw new ProductDataChecksumException("ProductDataChecksum.computeChecksum Null file");
        }

        if (offset < 0) {
            throw new ProductDataChecksumException("ProductDataChecksum.computeChecksum " + "Negative offset");
        }

        // Because the length arg is an integer, there has to be a way to sum
        // the whole file. If length is -1, choose the length of the file, which
        // is a long value.
        final long actualLength = length == -1 ? file.length() - offset : Integer.toUnsignedLong(length);
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(file);
        }
        catch (final FileNotFoundException fnfe) {
            throw new ProductDataChecksumException("ProductDataChecksum.computeChecksum " + "File could not be opened: "
                    + file.getPath(), fnfe);
        }
        final CRC32 calculator = new CRC32();
        try {
            final FileChannel fileChannel = fis.getChannel();
            fileChannel.position(offset);
            final ByteBuffer buffer = ByteBuffer.allocate(1024);
            long i = 0;
            for (; i < actualLength; i += buffer.position()) {
                buffer.clear();
                fileChannel.read(buffer);
                buffer.flip();
                calculator.update(buffer);
            }

        }
        catch (final IOException e) {
            try {
                fis.close();
            }
            catch (final IOException e2) {
                // ignore
            }
            throw new ProductDataChecksumException("ProductDataChecksum.computeChecksum " + "encountered IOException",
                                                   e);
        }

        try {
            fis.close();
        }
        catch (final IOException e) {
            // Ignore
        }
        return calculator.getValue();
    }

    @Override
    public long computeChecksum(final File file) throws ProductDataChecksumException {
        return computeChecksum(file, 0, -1);
    }

}
