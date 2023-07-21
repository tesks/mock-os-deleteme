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


import java.text.ParseException;

import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.CoarseFineEncoding;
import jpl.gds.shared.time.CoarseFineExtractor;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.ISclkExtractor;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import jpl.gds.shared.time.SclkScetUtility;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.shared.types.AmpcsStringBuffer;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.exception.ArgumentParseException;
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.UnblockException;

/**
 * Convert a standard time value to/from a bit string
 */
public class LegacyBaseTimeArgumentTranslator implements ICommandArgumentTranslator {
    protected final int scid;
    protected final SclkFormatter sclkFmt = TimeProperties.getInstance().getSclkFormatter();

    public LegacyBaseTimeArgumentTranslator(final int scid) {
        if(scid < 0) {
            throw new IllegalArgumentException("Supplied SCID must be valid");
        }
        this.scid = scid;
    }

    @Override
    public String parseFromBitString(final ICommandArgumentDefinition def, final AmpcsStringBuffer bitString) throws UnblockException {

        /*
         * MPCS-5607. Fix out of bounds error. This was a < rather than
         * <=, causing fall-through to the else and a string index out of bounds
         * exception.
         */
        /*
         * 1/13/14 - MPCS-4802. Logic below was bogus. Offset was not
         * accounted for in the if block, was accounted for in the else block,
         * and then accounted for in the toBytesFromBin call. Both halves of the
         * if/else now account for offset, and toBytesFromBin can then ignore
         * it.
         */
        final int bitLength = def.getBitLength();

        String binString = getValueBits(bitString, bitLength);


        ISclk sclk = binToSclk(binString, bitLength);

        return sclkToString(sclk);
    }

    /**
     * sub function that pulls out the bits
     * @param bitString the full bit string
     * @param bitLength the length of the argument in bits
     * @return the bit string of just the specified argument
     */
    protected String getValueBits(final AmpcsStringBuffer bitString, final int bitLength) {
        String binString = bitString.read(bitLength);

        binString = GDR.fillStr(binString, bitLength, '0');

        return binString;
    }

    /**
     * sub function that converts the binary string to a SCLK value
     * @param binString the binary string of the SCLK
     * @param bitLength the length of the SCLK in the definition
     * @return the SCLK value
     */
    protected ISclk binToSclk(final String binString, final int bitLength) {
        // create a sclk from the byte array
        // MPCS-8197 - 06/09/2016: This implementation of time arguments only works
        // for coarse-fine times.  Future missions should encode each segment of a time argument as
        // a separate data field.
        ISclk sclk;
        final CoarseFineEncoding canonicalConfig = TimeProperties.getInstance().getCanonicalEncoding();
        final int canonicalCoarseLen = canonicalConfig.getCoarseBits();

        // convert to a byte array
        final byte[] bytes = BinOctHexUtility.toBytesFromBin(binString);

        if (bitLength == canonicalCoarseLen) {
            final ISclkExtractor sclkExtract = new CoarseFineExtractor(new CoarseFineEncoding(canonicalCoarseLen, 0));
            sclk = sclkExtract.getValueFromBytes(bytes, 0);
        } else if (bitLength == canonicalConfig.getBitLength()) {
            final ISclkExtractor sclkExtract = TimeProperties.getInstance().getCanonicalExtractor();
            sclk = sclkExtract.getValueFromBytes(bytes, 0);
        } else {
            throw new IllegalStateException("Time argument could not be interpreted as a coarse fine clock");
        }

        return sclk;
    }

    /**
     * sub function that converts the sclk to a string
     * @param sclk the SCLK to be converted to a string
     * @return the string representation of the SCLK
     */
    protected String sclkToString(final ISclk sclk) {
        return sclk.toString();
    }



    @Override
    public String toBitString(final ICommandArgumentDefinition def, final String argValue) throws BlockException {

        if(argValue == null) {
            throw new IllegalStateException("The supplied argument value is null");
        }

        String bits;
        ISclk sclk;
        try {
            sclk = getValueAsSclk(argValue);
        } catch (final ArgumentParseException | IllegalArgumentException e) {
            throw new BlockException(e);
        }

        final int bitLength = def.getBitLength();

        // see if the bit string should be coarse and fine or just coarse

        final CoarseFineEncoding cfConfig = TimeProperties.getInstance().getCanonicalEncoding();
        if (bitLength == cfConfig.getCoarseBits()) {
            bits = BinOctHexUtility.toBinFromBytes(sclk.getCoarseBytes());
        } else if (bitLength == cfConfig.getBitLength()) {
            bits = BinOctHexUtility.toBinFromBytes(sclk.getBytes());
        } else {
            throw new BlockException("Unusable bit length of " + bitLength
                    + " found for Time type argument "
                    + "with dictionary name "
                    + def.getDictionaryName());
        }

        return (bits);
    }

    /**
     * Return the input argument value as a SCLK object according to the rules
     * of this particular argument
     *
     * @param inputVal
     *            The string value to be converted to a SCLK
     *
     * @return The SCLK object corresponding to the input string value
     *
     * @throws ArgumentParseException
     *             If the input string value cannot be converted to a SCLK
     */
    protected ISclk getValueAsSclk(final String inputVal)
            throws ArgumentParseException {

        if(scid < 0 ) {
            throw new IllegalArgumentException("SCID has not been set for this translator");
        }

        if (inputVal == null) {
            throw new IllegalArgumentException("Null input value");
        }

        ISclk sclk;

        if (sclkFmt.matches(inputVal)) {
            sclk = sclkFmt.valueOf(inputVal);
        } else {
            final IAccurateDateTime scet = getValueAsScet(inputVal);
            if (scet != null) {
                sclk = SclkScetUtility.getSclk(scet, null,
                        scid, TraceManager.getDefaultTracer());
                if (sclk == null) {
                    throw new ArgumentParseException(
                            "Error reading SCLK/SCET correlation file for Spacecraft ID "
                                    + scid);
                }
            } else {
                throw new ArgumentParseException("Could not parse input value "
                        + inputVal);
            }
        }

        return (sclk);
    }

    /**
     * Return the input argument value as a SCET (IAccurateDateTime) object
     * according to the rules of this particular argument.
     *
     * @param inputVal
     *            The input value to be converted to a SCET
     *
     * @return The IAccurateDateTime according to the SCET value of the input
     *         value
     *
     * @throws ArgumentParseException
     *             If the input value cannot be converted to a SCET
     */
    protected IAccurateDateTime getValueAsScet(final String inputVal)
            throws ArgumentParseException {

        IAccurateDateTime d;
        try {
            d = new AccurateDateTime(inputVal);
        } catch (final ParseException pe) {
            throw new ArgumentParseException(pe);
        }

        return (d);
    }
}
