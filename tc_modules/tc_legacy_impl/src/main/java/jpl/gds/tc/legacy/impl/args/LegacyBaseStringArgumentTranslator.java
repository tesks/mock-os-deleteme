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

import java.io.UnsupportedEncodingException;

import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.types.AmpcsStringBuffer;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.UnblockException;

/**
 * Convert a fixed length character string to/from a bit string
 *
 */
public class LegacyBaseStringArgumentTranslator implements ICommandArgumentTranslator {

    /** The string encoding to use when parsing string args from binary */
    protected static final String ENCODING = "US-ASCII";

    /** The bit representation of a null character */
    protected static final String NULL_BYTE_BIT_STRING = "00000000";

    protected String dictName;
    protected String fswName;

    @Override
    public String parseFromBitString(final ICommandArgumentDefinition def, final AmpcsStringBuffer bitString) throws UnblockException {

        setNames(def);

        String binString = getValueBits(bitString, def.getBitLength());

        return bitsToString(binString);
    }

    /**
     * utility function for names that are used often in thrown exceptions
     * @param def the ICommandArgumentDefinition of the current argument being translated
     */
    protected void setNames(final ICommandArgumentDefinition def) {
        dictName = def.getDictionaryName();
        fswName = def.getFswName();
    }

    /**
     * sub function that extracts JUST the bits that are needed from the bit string
     * @param bitString the bit string containing the argument
     * @param bitLength the length of the argument
     * @return the bit string of the argument
     * @throws UnblockException
     */
    protected String getValueBits(final AmpcsStringBuffer bitString, final int bitLength) throws UnblockException {
        String binString = bitString.read(bitLength);

        // strip off any 0 bits that prevent this bit string from being
        // byte-aligned
        while ((binString.length() % 8) != 0) {
            /*
             * MPCS-5607. Fix out of bounds exception. Second argument was
             * subtracting 2 rather than one from the length. The point is to
             * remove one bit at a time until total is modulo 8.
             */
            binString = binString.substring(0, binString.length() - 1);
        }

        // remove all null characters from the end of the string (byte =
        // 0b00000000)...
        // this strips off any padding that was applied
        while (binString.endsWith(NULL_BYTE_BIT_STRING)) {
            binString = binString.substring(0, binString.length()
                    - NULL_BYTE_BIT_STRING.length());
        }

        return binString;
    }

    /**
     * sub function that converts the supplied bit string to ASCII characters
     * @param binString the binary string to be converted
     * @return the converted string
     * @throws UnblockException if an error is encountered converting from binary to string
     */
    protected String bitsToString(final String binString) throws UnblockException {

        // decode the string using the ASCII char set
        final byte[] argBytes = BinOctHexUtility.toBytesFromBin(binString);
        String argVal;
        try {
            argVal = new String(argBytes, ENCODING);
        } catch (final UnsupportedEncodingException e) {
            throw new UnblockException("Attempted to interpret byte value \"0x"
                    + BinOctHexUtility.toHexFromBin(binString)
                    + "\" for StringArgument (dictionary name = "
                    + dictName
                    + ", fsw name = " + fswName
                    + "), but the current platform does not support "
                    + ENCODING + " encoding: " + e.getMessage(), e);
        }

        return argVal;
    }


    @Override
    public String toBitString(final ICommandArgumentDefinition def, String argValue) throws BlockException {

        setNames(def);

        verifyValue(argValue);

        argValue = cleanString(argValue);

        String bitString = toBaseBitString(argValue);

        try {
            bitString = adjustToBitStringLength(bitString, def.getBitLength());
        } catch (BlockException e) {
            throw new BlockException (
                    "The value \""
                            + argValue
                            + "\" given to "
                            + "String Argument (dictionary name = "
                            + dictName
                            + ", fsw name = "
                            + fswName
                            + ")"
                            + " has a bit length of "
                            + bitString.length()
                            + ", but it is only permitted to have a bit length of up to "
                            + def.getBitLength() + " bits.");
        }

        return bitString;
    }

    /**
     * sub function that executes checks needed before performing the binary conversion
     * @param argValue the argument value being checked
     * @throws BlockException if there will be an error converting the string to a bit string
     */
    protected void verifyValue(final String argValue) throws BlockException {
        if (argValue == null) {
            throw new IllegalStateException(
                    "Cannot get the bit represenation of the argument (dictionary name = \""
                            + dictName
                            + "\", FSW name = \""
                            + fswName
                            + "\") because its value is NULL.");
        }
        if (argValue.length() == 0) {
            throw new BlockException(
                    "Empty string input received for string argument (dictionary name = "
                            + dictName
                            + ", fsw name = "
                            + fswName
                            + ").  Empty string values are illegal");
        }
    }

    /**
     * sub function that just converts the string characters to a binary string
     * @param argValue the String of chracters to be converted
     * @return the binary string of the supplied argument value
     * @throws BlockException if there's an error encountered while converting the character to a binary string
     */
    protected String toBaseBitString(final String argValue) throws BlockException {

        byte[] stringBytes;

        // characters in a string are always 1 byte or 2 bytes, so the string
        // binary value should always be byte aligned
        try {
            stringBytes = argValue.getBytes(ENCODING);
        } catch (final UnsupportedEncodingException e) {
            throw new BlockException("Attempted to interpret string value \""
                    + argValue + "\" for StringArgument (dictionary name = "
                    + dictName
                    + ", fsw name = " + fswName
                    + "), but the current platform does not support "
                    + ENCODING + " encoding: " + e.getMessage(), e);
        }

        return BinOctHexUtility.toBinFromBytes(stringBytes);
    }

    /**
     * sub function that adjusts the bit string to the appropriate length for transmission
     * @param bitString the base binary string
     * @param bitLength the length of the argument as it needs to be for transmission
     * @return the adjusted binary string
     * @throws BlockException if the supplied bit string is longer than the argument's binary length
     */
    protected String adjustToBitStringLength(String bitString, final int bitLength) throws BlockException {
        if (bitString.length() < bitLength) {
            bitString = GDR.leftFillStr(bitString, bitLength, '0');
        } else if (bitString.length() > bitLength) {
            throw new BlockException("bitString cannot fit");
        }

        return bitString;
    }

    //unsure if it's been unquoted at this point. let's just assume it is
    protected String cleanString(final String argValue) {
        String cleanString = argValue;

        if(StringUtil.isQuoted(cleanString)) {
            cleanString = StringUtil.removeQuotes(cleanString);
        }

        return cleanString;
    }
}
