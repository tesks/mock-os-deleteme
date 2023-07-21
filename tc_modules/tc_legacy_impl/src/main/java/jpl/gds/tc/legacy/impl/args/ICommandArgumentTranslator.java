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
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.UnblockException;

/**
 * The ICommandArgumentTranslator interface details the functions required for any TC command argument translator.
 * The translator must be able to convert a TC command argument value to/from a binary string.
 * (A string of composed only of "1" and "0").
 *
 */
public interface ICommandArgumentTranslator {

    /**
     * Get a command argument value from the supplied bit string at the specified offset
     * @param def the ICommandArgumentDefinition of the next argument value
     * @param bitString the bit string containing the argument values
     * @return the String representation of the argument value
     * @throws UnblockException if there's an error parsing the argument value from the bit string
     */
    String parseFromBitString(final ICommandArgumentDefinition def, final AmpcsStringBuffer bitString) throws UnblockException;

    /**
     * Convert the specified argument value into a bit string
     * @param def the ICommandArgumentDefinition of the argument
     * @param argValue the String representation of the argument value
     * @return the bitString of the argument value
     * @throws BlockException if there's an error converting the argument value into a bit string
     */
    String toBitString(final ICommandArgumentDefinition def, final String argValue) throws BlockException;
}
