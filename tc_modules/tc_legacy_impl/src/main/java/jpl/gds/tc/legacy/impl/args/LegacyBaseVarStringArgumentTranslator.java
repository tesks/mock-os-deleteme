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
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.UnblockException;

/**
 * Convert a variable length character string to/from a bit string
 *
 */
public class LegacyBaseVarStringArgumentTranslator extends LegacyBaseStringArgumentTranslator {

    protected int prefixBitLength;

    @Override
    protected void setNames(final ICommandArgumentDefinition def) {

        super.setNames(def);

        prefixBitLength = def.getPrefixBitLength();
    }

    @Override
    protected String getValueBits(final AmpcsStringBuffer bitString, final int bitLength) throws UnblockException {
        String lengthBits;

        if (bitString.available() < prefixBitLength) {
            throw new UnblockException(
                    "There were not enough bits in the input binary value of argument \""
                            + dictName
                            + "\" to determine the length field of the variable length string argument.");
        }

        lengthBits = bitString.read(prefixBitLength);

        /*
         * 1/15/14 - MPCS-5602. Previous code only worked if the prefix
         * bit length was 16. Now supports 8, 16, and 32 bit prefixes.
         */
        long valueByteLength;
        switch (prefixBitLength) {
            case 8:
                valueByteLength = GDR.get_u8(
                        BinOctHexUtility.toBytesFromBin(lengthBits), 0);
                break;
            case 16:
                valueByteLength = GDR.get_u16(
                        BinOctHexUtility.toBytesFromBin(lengthBits), 0);
                break;
            case 32:
                valueByteLength = GDR.get_u32(
                        BinOctHexUtility.toBytesFromBin(lengthBits), 0);
                break;
            default:
                throw new UnblockException("Unsupported prefix bit length ("
                        + prefixBitLength + ") in var string command argument "
                        + dictName);
        }

        if (bitString.available() < (valueByteLength * 8)) {
            throw new UnblockException(
                    "There were not enough bits in the input binary value of argument \""
                            + dictName
                            + "\" to read the value of the string. The expected length is "
                            + (valueByteLength * 8)
                            + " bits, but only " + (bitString.available())
                            + " bits were found.");
        }

        String valueBits = bitString.read( (int) valueByteLength * 8);

        // strip off any 0 bits that prevent this bit string from being
        // byte-aligned
        while ((valueBits.length() % 8) != 0) {
            valueBits = valueBits.substring(0, valueBits.length() - 2);
        }

        return valueBits;
    }

    @Override
    public String toBitString(final ICommandArgumentDefinition def, String argValue) throws BlockException {

        setNames(def);

        verifyValue(argValue);

        argValue = cleanString(argValue);

        String bitString = getPrefixBits(argValue.length(), def.getPrefixBitLength());

        bitString += toBaseBitString(argValue);

        return bitString;
    }

    @Override
    protected void verifyValue(final String argValue) {
        if (argValue == null) {
            throw new IllegalStateException(
                    "Cannot get the bit represenation of the argument (dictionary name = \""
                            + dictName
                            + "\", FSW name = \""
                            + fswName
                            + "\") because its value is NULL.");
        }
    }

    /**
     * sub function to get the bits in the prefix, which represent the string length
     * @param stringLength the length of the string to be encoded
     * @param prefixBitLength the number of bits allowed for the prefix
     * @return the binary string of the prefix
     * @throws BlockException if there's an error encountered while creating the prefix
     */
    protected String getPrefixBits(final int stringLength, final int prefixBitLength) throws BlockException {

        /*
         * 1/15/14 - MPCS-5602. Previous code only worked if the prefix
         * bit length was 16. Now supports 8, 16, and 32 bit prefixes.
         */
        final byte[] lengthBytes = new byte[prefixBitLength / 8];
        switch (prefixBitLength) {
            case 8:
                GDR.set_u8(lengthBytes, 0, stringLength);
                break;
            case 16:
                GDR.set_u16(lengthBytes, 0, stringLength);
                break;
            case 32:
                GDR.set_u32(lengthBytes, 0, stringLength);
                break;
            default:
                throw new BlockException("Unsupported prefix bit length ("
                        + prefixBitLength + ") in var string command argument "
                        + dictName);
        }

        return BinOctHexUtility.toBinFromBytes(lengthBytes);
    }

}
