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
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.types.AmpcsStringBuffer;
import jpl.gds.tc.api.exception.ArgumentParseException;
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.UnblockException;

/**
 * Converts a time value represented as a floating point value to/from a bit string
 *
 */
public class LegacyFloatTimeArgumentTranslator extends LegacyBaseTimeArgumentTranslator {

    public LegacyFloatTimeArgumentTranslator(int scid) {
        super(scid);
    }

    @Override
    public String parseFromBitString(final ICommandArgumentDefinition def, final AmpcsStringBuffer bitString) throws UnblockException {

        if(def.getBitLength() != Double.SIZE) {
            throw new UnblockException("Invalid bit length of " + def.getBitLength()
                    + " found for " + "time argument (dictionary name = "
                    + def.getDictionaryName()
                    + ", fsw name = " + def.getFswName() + ")");
        }

        return super.parseFromBitString(def, bitString);
    }

    @Override
    protected  ISclk binToSclk(final String binString, final int bitLength) {
        return sclkFmt.valueOf((new Double(GDR.getDoubleFromBits(binString))).toString());
    }

    @Override
    protected String sclkToString(final ISclk sclk) {
        return sclk.toDecimalString();
    }

    @Override
    public String toBitString(final ICommandArgumentDefinition def, final String argValue) throws BlockException {

        if (argValue == null) {
            throw new IllegalStateException(
                    "The current argument value is null");
        }

        String bits;
        try {
            getValueAsSclk(argValue);
        } catch (final ArgumentParseException e) {
            throw new BlockException(e);
        }

        final int bitLength = def.getBitLength();
        if (bitLength == Double.SIZE) {
            final double doubleVal = Double.parseDouble(argValue);
            final boolean positiveNumber = doubleVal >= 0;
            bits = Long.toBinaryString(Double.doubleToLongBits(doubleVal));

            if (bits.length() < bitLength) {
                bits = GDR.fillStr(bits, bitLength, positiveNumber ? '0' : '1');
            } else if (bitLength < bits.length()) {
                final int difference = bits.length() - bitLength;
                bits = bits.substring(difference);
            }
        } else {
            throw new BlockException("Unusable bit length of " + bitLength
                    + " found for Time type argument "
                    + "with dictionary name "
                    + def.getDictionaryName());
        }

        return bits;
    }
}
