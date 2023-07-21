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
/**
 * 
 */
package jpl.gds.product.api.decom;

import java.io.IOException;

import jpl.gds.product.api.decom.formatter.IDecomOutputFormatter;
import jpl.gds.shared.types.ByteArraySlice;

/**
 * The IBitField interface is implemented by decom
 * definitions that have values that can be extracted at a bit, as opposed to
 * byte, offset.
 * 
 *
 */
public interface IBitField extends IPrimitiveField {

    /**
     * Prints the formatted field value to an output stream and returns the
     * number of bytes read/used. This method effectively consumes the field
     * bytes from the given input data. The field data is assumed to start at
     * the given offset within the ByteArraySlice.
     * 
     * @param data the ByteArraySlice containing the field value
     * @param bitoffset the starting offset of the field value within the
     *            ByteArraySlice
     * @param out the OutputFormatter to send the formatted field value to
     * @param depth depth of indentation for formatted output
     * @return the number of bits in the BitField
     * @throws IOException if there is a problem printing the value
     */
    public int printValue(ByteArraySlice data, int bitoffset,
            IDecomOutputFormatter out, int depth) throws IOException;
}
