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
package jpl.gds.dictionary.api;

import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.util.BinOctHexUtility;


/**
 * A static utility class for manipulating opcodes.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 *
 */
public class OpcodeUtil extends Object
{
    public static final int NO_OPCODE = 0;
    private final int opcodeBitLength;
    private final int opcodeDigitsLength;
    private final int opcodeByteLength;
    private final boolean hideOpCode;
    private static final Tracer log       = TraceManager.getTracer(Loggers.UTIL);

    /**
     * Constructor
     * 
     * @param dictConfig the current DictionaryConfiguration
     */
    public OpcodeUtil(final DictionaryProperties dictConfig)
    {
    	opcodeBitLength    = dictConfig.getOpcodeBitLength();
    	opcodeDigitsLength = (opcodeBitLength + 3) / 4;
    	opcodeByteLength   = (opcodeBitLength + Byte.SIZE - 1) / Byte.SIZE;

    	hideOpCode = dictConfig.getHideOpcode();
    }

    /**
     * Returns whether or not the opcode should be shown in logs
     *
     * @return boolean true if opcodes should be shown.
     */
    public boolean hideOpCode() {
        return hideOpCode;
    }


    /**
     * Checks an input opcode, verifying that it is specified in hex and is of
     * the proper length for current mission.
     *
     * @param val opcode string in hex (which may or may not begin with
     *            BinOctHexUtility.HEX_STRING_PREFIX1 or BinOctHexUtility.HEX_STRING_PREFIX2) or
     *            binary (must begin with 0b)
     *
     * @param nullOpcode NULL opcode
     * 
     * @param opcodeBitLength the opcode bit length in the current mission/dictionary
     *        context 
     *
     * @return string representing the opcode converted to hex and padded to the
     *         length required for the mission.
     */
    public static String checkAndConvertOpcode(final String val,
                                               final String nullOpcode,
                                               final int opcodeBitLength)
    {
        if (val == null) {
            throw new IllegalArgumentException("Null input");
        }

        else if (nullOpcode.equalsIgnoreCase(val) || val.isEmpty())
        {
            return nullOpcode;
        }

        /*
         * Checking for valid hex actually has the side effect of checking for
         * valid binary also...any binary string with or without the prefix
         * validates as valid hex (e.g. 00110101, 0b0101011, 0B10101011)
         */
        else if (!BinOctHexUtility.isValidHex(val)) {
            throw new IllegalArgumentException("Opcode \"" + val
                    + "\" is not a valid binary or hexadecimal number.");
        }

        /*
         * Turn the input value into hex with no prefix
         */
        final StringBuilder hexValue = new StringBuilder(64);
        if (BinOctHexUtility.hasHexPrefix(val)) {
            hexValue.append(BinOctHexUtility.stripHexPrefix(val));
        } else if (BinOctHexUtility.hasBinaryPrefix(val)) {
            hexValue.append(BinOctHexUtility.toHexFromBin(val));
        } else {
            hexValue.append(val);
        }

        int bitLength = hexValue.length() * 4;

        if (bitLength < opcodeBitLength) {
            while (bitLength < opcodeBitLength) {
                hexValue.insert(0, '0');
                bitLength = hexValue.length() * 4;
            }
        } else if (bitLength > opcodeBitLength) {
            throw new IllegalArgumentException(
                          "The input opcode 0x" + hexValue                 +
                          " has a bit length of "                          +
                          bitLength                                        +
                          " which is greater than the expected length of " +
                          opcodeBitLength                                  +
                          " bits.");
        }

        return hexValue.toString().toLowerCase();
    }


    /**
     * Format opcode as hex string with zero padding on the left.
     * The bit length can be anything from 1 to 64 and it will work
     * properly.
     *
     * @param opcode    Opcode as a long
     * @param addHeader Add the hex header
     *
     * @return Opcode as string
     *
     */
    public String formatOpcode(final long    opcode,
                                      final boolean addHeader)
    {
        final StringBuilder sb =
            new StringBuilder(addHeader ? BinOctHexUtility.HEX_STRING_PREFIX1
                                        : "");

        final String hex = Long.toHexString(validateOpcodeAndTrace(opcode));

        // Pad out with zeroes as necessary

        for (int i = hex.length(); i < opcodeDigitsLength; ++i)
        {
            sb.append('0');
        }

        sb.append(hex);

        return sb.toString();
    }


    /**
     * Format opcode as hex string with zero padding on the left.
     * The bit length can be anything from 1 to 64 and it will work
     * properly.
     *
     * @param opcode    Opcode as an unsigned int
     * @param addHeader Add the hex header
     *
     * @return Opcode as string
     *
     */
    public String formatOpcode(final int     opcode,
                                      final boolean addHeader)
    {
        return formatOpcode(Integer.toUnsignedLong(opcode), addHeader);
    }


    /**
     * Remove hex prefix.
     *
     * @param opcode String to strip
     *
     * @return Stripped string
     *
     */
    public static String stripHexPrefix(final String opcode)
    {
        return BinOctHexUtility.stripHexPrefix(opcode);
    }


    /**
     * Check opcode for valid hex.
     *
     * @param opcode String to check
     *
     * @return True if OK
     *
     */
    public static boolean isValidHex(final String opcode)
    {
        return BinOctHexUtility.isValidHex(opcode);
    }


    /**
     * Check opcode for hex prefix.
     *
     * @param opcode String to check
     *
     * @return True if prefix is present
     *
     */
    public static boolean hasHexPrefix(final String opcode)
    {
        return BinOctHexUtility.hasHexPrefix(opcode);
    }


    /**
     * Check opcode for binary prefix.
     *
     * @param opcode String to check
     *
     * @return True if prefix is present
     *
     */
    public static boolean hasBinaryPrefix(final String opcode)
    {
        return BinOctHexUtility.hasBinaryPrefix(opcode);
    }


    /**
     * Convert hex string to bin string.
     *
     * @param opcode String to convert
     *
     * @return Converted string
     *
     */
    public static String toBinFromHex(final String opcode)
    {
        return BinOctHexUtility.toBinFromHex(opcode);
    }


    /**
     * Convert bin string to hex string.
     *
     * @param opcode String to convert
     *
     * @return Converted string
     *
     */
    public static String toHexFromBin(final String opcode)
    {
        return BinOctHexUtility.toHexFromBin(opcode);
    }


    /**
     * Convert bytes to hex string.
     *
     * @param opcode String to convert
     *
     * @return Converted string
     *
     */
    public static String toHexFromBytes(final byte[] opcode)
    {
        return BinOctHexUtility.toHexFromBytes(opcode);
    }


    /**
     * Add hex prefix to opcode. Remove old one if present.
     *
     * @param opcode String to convert
     *
     * @return Converted string
     *
     */
    public static String addHexPrefix1(final String opcode)
    {
        return (BinOctHexUtility.HEX_STRING_PREFIX1 +
                BinOctHexUtility.stripHexPrefix(opcode));
    }


    /**
     * Format opcode as hex string.
     *
     * @param opcode          String to format
     * @param hexCharsPerLine Number of hex characters per line
     *
     * @return Formatted string
     *
     */
    public static String formatHexString(final String opcode,
                                         final int    hexCharsPerLine)
    {
        return BinOctHexUtility.formatHexString(opcode, hexCharsPerLine);
    }


    /**
     * Parse opcode from hex string.
     *
     * @param opcode String to parse
     *
     * @return Opcode as unsigned int
     *
     */
    public int parseOpcodeFromHex(final String opcode)
    {
        int result = NO_OPCODE;

        try
        {
            result = Integer.parseUnsignedInt(opcode, 16);
        }
        catch (final NumberFormatException nfe)
        {
            log.error(
                "Found opcode out of bounds for " +
                opcodeBitLength                 +
                " bits: '"                        +
                opcode                            +
                "', using "                       +
                NO_OPCODE                         +
                " instead");

            return result;
        }

        return validateOpcodeAndTrace(result);
    }


    /**
     * Parse opcode from decimal string.
     *
     * @param opcode String to parse
     *
     * @return Opcode as unsigned int
     *
     */
    public int parseOpcodeFromDecimal(final String opcode)
    {
        int result = NO_OPCODE;

        try
        {
            result = Integer.parseUnsignedInt(opcode);
        }
        catch (final NumberFormatException nfe)
        {
            log.error(
                "Found opcode out of bounds for " +
                opcodeBitLength                 +
                " bits: '"                        +
                opcode                            +
                "', using "                       +
                NO_OPCODE                         +
                " instead");

            return result;
        }

        return validateOpcodeAndTrace(result);
    }


    /**
     * Verify that opcode is within bounds and log an error
     * and fix if not.
     *
     * @param opcode Opcode as long
     *
     * @return Opcode or NO_OPCODE if invalid
     *
     */
    public long validateOpcodeAndTrace(final long opcode)
    {
        // Note: non-sign-extending shift
        // Note: Shift CANNOT be greater than long size

        if ((opcodeBitLength < Long.SIZE) &&
            ((opcode >>> opcodeBitLength) != 0L))
        {
            log.error(
                "Found opcode out of bounds for " +
                opcodeBitLength                 +
                " bits: "                         +
                Long.toHexString(opcode)          +
                ", using "                        +
                NO_OPCODE                         +
                " instead");

            return NO_OPCODE;
        }

        return opcode;
    }


    /**
     * Verify that opcode is within bounds and log an error
     * and fix if not.
     *
     * @param opcode Opcode as unsigned integer
     *
     * @return Opcode or NO_OPCODE if invalid
     *
     */
    public int validateOpcodeAndTrace(final int opcode)
    {
        return (int) validateOpcodeAndTrace(Integer.toUnsignedLong(opcode));
    }


    /**
     * Check for opcode of proper length. The size has to be the right
     * size to hold the configured number of bits.
     *
     * @param length Opcode length in bytes
     *
     * @return True if correct
     *
     */
    public boolean validateOpcodeLength(final int length)
    {
        return (length == opcodeByteLength);
    }
}
