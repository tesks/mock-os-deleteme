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
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.UnblockException;

/**
 * Convert a floating point value (both single and double precision) to/from a bit string
 *
 */
public class LegacyBaseFloatArgumentTranslator implements ICommandArgumentTranslator {


    @Override
    public String parseFromBitString(final ICommandArgumentDefinition def, final AmpcsStringBuffer bitString)
            throws UnblockException {


        final int bitLength = def.getBitLength();

        if (bitString.available() < bitLength) {
            throw new UnblockException(
                    "Could not interpret the binary value for argument "
                            + def.getDictionaryName()
                            + " because the argument has a length of "
                            + bitLength + " bits, but only "
                            + (bitString.available())
                            + " bits were found.");
        }

        final String argBits = bitString.read(bitLength);

        Number value;
        switch (bitLength) {
            case Float.SIZE:
                value = Float.valueOf(GDR.getFloatFromBits(argBits));
                break;

            case Double.SIZE:
                value = Double.valueOf(GDR.getDoubleFromBits(argBits));
                break;

            default:

                throw new UnblockException("Invalid bit length of " + bitLength
                        + " found for " + "float argument (dictionary name = "
                        + def.getDictionaryName() + ")");
        }

        return value.toString();
    }

    @Override
    public String toBitString(final ICommandArgumentDefinition def, final String argValue) throws BlockException {

        if(argValue == null) {
            throw new BlockException("supplied argument value must not be null");
        }

        if(argValue.isEmpty()) {
            throw new BlockException("supplied argument value must not be empty");
        }


        String bitString;
        boolean positiveNumber;

        final int bitLength = def.getBitLength();

        switch (bitLength) {
            case Float.SIZE:

                final float floatVal = Float.parseFloat(argValue);
                positiveNumber = floatVal >= 0;
                bitString = Integer.toBinaryString(Float.floatToIntBits(floatVal));

                break;

            case Double.SIZE:

                final double doubleVal = Double.parseDouble(argValue);
                positiveNumber = doubleVal >= 0;
                bitString = Long.toBinaryString(Double.doubleToLongBits(doubleVal));

                break;

            default:

                throw new BlockException("Invalid bit length of " + bitLength
                        + " found for " + "float argument (dictionary name = "
                        + def.getDictionaryName()
                        + ", fsw name = " + def.getFswName() + ")");
        }

        if (bitString.length() < bitLength) {
            bitString = GDR.fillStr(bitString, bitLength, positiveNumber ? '0'
                    : '1');
        } else if (bitLength < bitString.length()) {
            final int difference = bitString.length() - bitLength;
            bitString = bitString.substring(difference);
        }

        return (bitString);
    }

}
