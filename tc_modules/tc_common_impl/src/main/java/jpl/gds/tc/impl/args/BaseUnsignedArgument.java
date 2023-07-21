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
package jpl.gds.tc.impl.args;

import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.command.args.ICommandArgument;
import jpl.gds.tc.api.exception.ArgumentParseException;
import org.springframework.context.ApplicationContext;

import java.math.BigInteger;

/**
 * 
 * The base class implementation of an unsigned command argument.
 * <p>
 * An unsigned argument represents a single unsigned integer value such as 25 or
 * 251. If an unsigned argument is defined to be 8 bits long, its allowable
 * values fall in the range 0 to 255 (unless otherwise restricted by the
 * mission).
 * 
 *
 * 11/8/13 - MPCS-5521. Some methods and members moved to
 *          superclass. General cleanup, javadoc, static analysis changes.
 * 6/22/14 - MPCS-6304. Dictionary-related members throughout
 *          removed to CommandArgumentDefinition class. This class has been
 *          stripped down to support runtime commanding capabilities only.
 * 71/119 - MPCS-10745 - removed toBitString, parseFromBitString
 * 
 */
public class BaseUnsignedArgument extends BaseIntegerArgument {
    /**
     * Creates an instance of BaseUnsignedArgument.
     *
     * @param appContext App context
     * @param def
     *            the command argument definition object for this argument.
     * 
     */
    public BaseUnsignedArgument(ApplicationContext appContext, ICommandArgumentDefinition def) {

        super(appContext, def);
    }


    @Override
    public ICommandArgument copy() {

        final BaseUnsignedArgument ua = new BaseUnsignedArgument(appContext, this.getDefinition());
        setSharedValues(ua);
        return (ua);
    }


    /**
     * Override for unsigned arguments. Converts a binary, hexadecimal, or decimal value into a BigInteger,
     * then into a Number.
     * JFWagner - 7/20/2020 - MPCS-11829: replaced dependency on GDR with logic that uses BigInteger instead
     * of long, since long is not big enough to hold a U64 value
     * @param inputVal the value to be converted
     * @return Number representing the unsigned decimal value of the input value
     * @throws ArgumentParseException
     *                  if the input value cannot be parsed
     */
    @Override
    protected Number formatValueAsComparable(final String inputVal)
            throws ArgumentParseException {

        try {
            String bitString;
            try {
                if (BinOctHexUtility.hasBinaryPrefix(inputVal)) {
                    bitString = BinOctHexUtility.stripBinaryPrefix(inputVal);
                } else if (BinOctHexUtility.hasHexPrefix(inputVal)) {
                    final String hexString = BinOctHexUtility.stripHexPrefix(inputVal);
                    bitString = BinOctHexUtility.toBinFromHex(hexString);
                } else {
                    bitString = BinOctHexUtility.toBinFromBytes((new BigInteger(inputVal)).toByteArray());
                }

                return new BigInteger(BinOctHexUtility.toBytesFromBin(bitString));
            } catch (final Exception e) {
                throw new ArgumentParseException("Could not interpret input value "
                        + inputVal, e);
            }
        } catch (final Exception e) {
            throw new ArgumentParseException("Could not interpret input value "
                    + inputVal, e);
        }
    }
}
