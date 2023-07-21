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
import jpl.gds.shared.types.AmpcsStringBuffer;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.UnblockException;

/**
 * Convert an enumerated argument to/from a bit string
 *
 */
public class LegacyBaseFillerArgumentTranslator implements ICommandArgumentTranslator {

    @Override
    public String parseFromBitString(final ICommandArgumentDefinition def, final AmpcsStringBuffer bitString) throws UnblockException
    {
        try
        {
            return bitString.read(def.getBitLength());
        }
        catch(final IllegalArgumentException | IndexOutOfBoundsException iae)
        {
            throw new UnblockException("Could not interpret the input bytes " + BinOctHexUtility.HEX_STRING_PREFIX1 + BinOctHexUtility.toHexFromBin(bitString.getBuffer()) + " for the fill argument: " + iae.getMessage(),iae);
        }
    }

    @Override
    public String toBitString(final ICommandArgumentDefinition def, final String argValue) throws BlockException {
        if(argValue == null) {
            throw new BlockException("The value of a fill argument must not be null");
        }

        String bitString = argValue;


        if(BinOctHexUtility.isValidBin(argValue) || BinOctHexUtility.hasBinaryPrefix(argValue)) {
            bitString = BinOctHexUtility.stripBinaryPrefix(argValue);
        } else if(BinOctHexUtility.isValidHex(argValue) || BinOctHexUtility.hasHexPrefix(argValue)) {
            try {
                bitString = BinOctHexUtility.toBinFromHex(argValue);
            } catch (Exception e) {
                throw new BlockException("The value of a fill argument must be a binary or hex string, but this string was found: \"" + argValue + "\"");
            }
        }

        if(!BinOctHexUtility.isValidBin(bitString)) {
            throw new BlockException("The value of a fill argument must be a binary or hex string, but this string was found: \"" + argValue + "\"");
        }
        final int bitLength = def.getBitLength();
        int fillLength = bitString.length();

        final StringBuilder fillValueBuffer = new StringBuilder(bitLength);
        fillValueBuffer.append(bitString);

        if(fillLength <= 0)
        {
            throw new BlockException("Invalid fill length...empty or negative length fill value encountered. (fill bits = " + argValue + ")");
        }
        else if(fillLength > bitLength)
        {
            throw new IllegalStateException("Can't have more fill bits than length of argument. (fill bits = " + argValue +
                    ", argument bit length = " + bitLength + ")");
        }
        else if((bitLength % fillLength) != 0)
        {
            throw new BlockException("The fill bits must divide evenly into the argument bit length. (fill bits = " + argValue +
                    ", argument bit length = " + bitLength + ")");
        }

        // repeat the fill value to fill the entire length of the argument
        while(fillLength != bitLength)
        {
            fillValueBuffer.append(bitString);
            fillLength += bitString.length();
        }

        // get the fill value bit string and add it to the command bit set
        bitString = fillValueBuffer.toString();

        return(bitString);

    }

}
