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

import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.types.AmpcsStringBuffer;
import jpl.gds.shared.util.BinOctHexUtility;

/**
 * Convert a signed integer argument to/from a bit string.
 *
 * This includes any signed non-float numeric (byte, short, int, long, etc)
 *
 */
public class LegacyBaseIntegerArgumentTranslator implements ICommandArgumentTranslator {

    @Override
    public String parseFromBitString(final ICommandArgumentDefinition def, final AmpcsStringBuffer bitString) {


        int bitLength = def.getBitLength();

        String valueBits = getValueBits(bitString, bitLength);

        valueBits = padParsedValueBits(valueBits, bitLength);

        byte[] valueBytes = BinOctHexUtility.toBytesFromBin(valueBits);

        return toArgumentValue(valueBytes);
    }

    /**
     * sub function gets the bits needed from the bit string
     * @param bitString the full bit string
     * @param bitLength the length of the argument
     * @return the bit string of just the argument
     */
    protected String getValueBits(final AmpcsStringBuffer bitString, final int bitLength) {
        return bitString.read(bitLength);
    }

    /**
     * sub function that adjusts the parsed bits to the long size (64 bits)
     * @param valueBits the bits of the specified argument
     * @param bitLength the length of the specified argument
     * @return the extended bit string
     */
    protected String padParsedValueBits(String valueBits, final int bitLength) {
        String retBits = valueBits;

        /*
         * If the bits are already at the size of a long, they're already signed.
         * If they're not, the leading bit is the sign bit.
         */
        if(valueBits.length() < Long.SIZE) {

            if (valueBits.length() < bitLength) {
                retBits = GDR.fillStr(valueBits,Long.SIZE,'0');
            } else {
                retBits = GDR.fillStr(valueBits,Long.SIZE,valueBits.charAt(0));
            }

        }

        return retBits;
    }

    /**
     * sub function that converts the Long bytes to a value
     * @param valueBytes the long bits converted to a byte array
     * @return the string representation of the numeric value
     */
    protected String toArgumentValue(byte[] valueBytes) {
        return (Long.valueOf(GDR.get_i64(valueBytes,0))).toString();
    }


    @Override
    public String toBitString(final ICommandArgumentDefinition def, final String argValue) {


        String bitString = toBaseBitString(argValue);

        return adjustToBitStringLength(bitString, def.getBitLength());
    }

    /**
     * sub function that converts the string numeric value to a binary string
     * @param argValue the string representation of a number
     * @return the binary string of the supplied value
     */
    protected String toBaseBitString(final String argValue) {
        final long value = GDR.parse_long(argValue);
        return Long.toBinaryString(value);
    }

    /**
     * sub function that adjusts the binary string to the final length
     * @param bitString the binary string of the argument
     * @param bitLength the length of the argument as it will be transmitted
     * @return the adjusted binary string length
     */
    protected String adjustToBitStringLength(final String bitString, final int bitLength) {
        if(bitString.length() == bitLength) { //just right, most likely case
            return bitString;

        } else if(bitString.length() < bitLength) { //to short
            return extendBitString(bitString, bitLength);

        } else { //if (bitString.length() > bitLength) - too long
            return shortenBitString(bitString, bitLength);
        }

    }

    /**
     * sub function that extends the binary string of a number
     * @param bitString the binary string of the argument
     * @param bitLength the length of the argument as it will be transmitted
     * @return the adjusted binary string
     */
    protected String extendBitString(final String bitString, final int bitLength) {
        //the only way for a signed int to take up the full thing is for it to be negative!
        boolean fullSize = bitString.length() == Long.SIZE;
        return GDR.fillStr(bitString, bitLength, fullSize ? '1' : '0');
    }

    /**
     * sub function that shortens the binary string of a number
     * @param bitString the binary string of the argument
     * @param bitLength the length of the argument as it will be transmitted
     * @return the adjusted binary string
     */
    protected String shortenBitString(final String bitString, final int bitLength) {
        final int difference = bitString.length() - bitLength;
        return bitString.substring(difference);
    }

}
