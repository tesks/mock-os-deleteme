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
package jpl.gds.tc.legacy.impl.args;

import jpl.gds.shared.gdr.GDR;

/**
 * Convert an unsigned integer argument to/from a bit string
 *
 * This includes any unsigned non-float numeric (byte, short, int long, etc.)
 *
 */
public class LegacyBaseUnsignedArgumentTranslator extends LegacyBaseIntegerArgumentTranslator {

    //TODO: maybe change to use guava UnsignedLong ? Then we could handle unsigned 64 bit values...

    @Override
    protected String padParsedValueBits(String valueBits, final int bitLength) {
        String retBits = valueBits;
        // in order to pad out an unsigned argument to the proper length, we
        // left-append
        // '0' characters
        if (valueBits.length() < Long.SIZE) {
            retBits = GDR.fillStr(valueBits, Long.SIZE, '0');
        }

        return retBits;
    }

    @Override
    protected String toArgumentValue(byte[] valueBytes) {
        return (Long.valueOf(GDR.get_u64(valueBytes, 0))).toString();
    }

    @Override
    protected String extendBitString(final String bitString, final int bitLength) {
        return GDR.fillStr(bitString, bitLength, '0');
    }


}
