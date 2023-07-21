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
package jpl.gds.product.impl.decom;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;

import jpl.gds.product.api.decom.IProductDecomField;
import jpl.gds.product.api.decom.ProductDecomFieldType;
import jpl.gds.product.api.decom.formatter.IDecomOutputFormatter;
import jpl.gds.shared.types.ByteArraySlice;
import jpl.gds.shared.types.ByteStream;

/**
 * BitArray represents a decom definition that describes a field that is an
 * array of BitFields.
 *
 *
 * @see BitField
 */
public class BitArray extends AbstractFieldContainer {

    private final int bitlength;
    private int bitoffset;

    /**
     * Creates an instance of BitArray.
     * 
     * @param name the name of the BitArray field
     * @param bitlength the length of the field
     */
    @Deprecated
    public BitArray(final String name, final int bitlength) {
        super(ProductDecomFieldType.BIT_ARRAY_FIELD);
        this.name = name;
        this.bitlength = bitlength;
        bitoffset = 0;
    }

  
    @Override
    public int printValue(final ByteStream stream, final IDecomOutputFormatter out, final int depth)
                                                                            throws IOException {
        if (name.equalsIgnoreCase("fill")) {
            long len = bitlength / 8;
            if (len == -1) {
                len = stream.remainingBytes();
            }
            stream.skip(len);
            return (int) len;
        }

        BitField element;
        int totalBits = 0;
        for (final IProductDecomField iProductDecomField : elements) {
            element = (BitField) iProductDecomField;
            totalBits += element.getBitLength();
        }
        int bytes = totalBits / 8;
        if ((totalBits % 8) != 0) {
            ++bytes;
        }
        ByteArraySlice slice = stream.read(bytes);

        bitoffset = 0;
        for (final IProductDecomField iProductDecomField : elements) {
            element = (BitField) iProductDecomField;
            bitoffset += element.printValue(slice, bitoffset, out, depth);
        }
        return bitoffset / 8;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.impl.decom.AbstractDecomField#printType(java.io.PrintStream, int)
     */
    @Override
    public void printType(final PrintStream out, final int depth) throws IOException {
        printIndent(out, depth);
        out.println("(BitArray of " + bitlength + ") " + name);
        for (final IProductDecomField element : elements) {
            element.printType(out, depth + 1);
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.impl.decom.AbstractFieldContainer#getValueSize()
     */
    @Override
    public int getValueSize() {
        return bitlength / 8;
    }
}
