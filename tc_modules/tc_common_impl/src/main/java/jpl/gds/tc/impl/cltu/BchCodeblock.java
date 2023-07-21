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

package jpl.gds.tc.impl.cltu;

import jpl.gds.tc.api.IBchCodeblock;
import jpl.gds.tc.api.cltu.IBchCodeBlockBuilder;
import jpl.gds.tc.impl.cltu.parsers.BchCodeBlockBuilder;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * A holder for BCH code blocks provided from MPS CTS. Consider using the Builder or an IBchParser to create instances.
 *
 */
public class BchCodeblock implements IBchCodeblock {

    private byte[] data = new byte[0];
    private byte[] edac = new byte[0];

    @Override
    public byte[] getBytes() {
        if (data == null) {
            throw new IllegalStateException("BCH code block data is null");
        }

        if (edac == null) {
            throw new IllegalStateException("BCH code block EDAC is null.");
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length + edac.length);

        baos.write(data, 0, data.length);
        baos.write(edac, 0, edac.length);

        return baos.toByteArray();
    }

    @Override
    public int parseFromBytes(final byte[] codeblock, final int offset) {
        throw new UnsupportedOperationException("Parsing from bytes not supported. Use the appropriate parser.");
    }

    @Override
    public byte[] getEdac() {
        return edac;
    }

    @Override
    public void setEdac(final byte[] edac) {
        if (edac == null) {
            throw new IllegalArgumentException("EDAC must not be null.");
        }
        this.edac = edac;
    }

    @Override
    public byte[] getData() {
        return data;
    }

    @Override
    public void setData(final byte[] inData) {
        if (inData == null) {
            throw new IllegalArgumentException("Data must not be null.");
        }
        this.data = inData;
    }

    @Override
    public IBchCodeblock copy() {
        final IBchCodeBlockBuilder builder = new BchCodeBlockBuilder();
        return builder.setData(Arrays.copyOf(data, data.length))
                .setDataLength(data.length)
                .setEdac(Arrays.copyOf(edac, edac.length))
                .build();
    }


}
