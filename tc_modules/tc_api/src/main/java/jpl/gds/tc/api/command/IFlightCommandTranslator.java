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
package jpl.gds.tc.api.command;

import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.UnblockException;

/**
 * The IFlightCommandTranslator interface is to be used to house an IFlightCommand and be able to set and get argument
 * values through bit strings and return the command as a bit string using internal conversion objects.
 *
 */
public interface IFlightCommandTranslator {

    /**
     * Allows the user to set or change the command used by this translator.
     *
     * @param command the Flight Command to be translated
     */
    IFlightCommandTranslator setCommand(final IFlightCommand command);

    /**
     * Allows the ICommandArgument at the specified index to parse the input binary string and use the values contained
     * therein to set the corresponding fields on this object. This is the logical inverse of the "toBitString" function.
     *
     * @param index the index for the command arguments with a value(s) to be set.
     *
     * @param bitString The bit string to parse data from (only contains '1' and '0' characters)
     *
     * @param offset The offset into the bit string where parsing should begin
     *
     * @return An integer indicating the number of bits that were read.
     *
     * @throws UnblockException If a translation error occurs.
     */
    int parseAndSetArgumentValueFromBitString(int index, String bitString, int offset) throws UnblockException;

    /**
     * Allows the ICommandArgument at the specified index within the specified repeat argument to parse the input binary string and use the values contained
     * therein to set the corresponding fields on this object. This is the logical inverse of the "toBitString" function.
     *
     * @param index the index for the repeat command argument with a command argument(s) to be set.
     *
     * @param subIndex the index for the command argument with an argument(s) to be set
     *
     * @param bitString The bit string to parse data from (only contains '1' and '0' characters)
     *
     * @param offset The offset into the bit string where parsing should begin
     *
     * @return An integer indicating the number of bits that were read.
     *
     * @throws UnblockException If a translation error occurs.
     */
    int parseAndSetArgumentValueFromBitString(int index, int subIndex, String bitString, int offset) throws UnblockException;

    /**
     * Get the specified ICommandArguemnt value in a binary representation.
     *  This is the logical inverse of the "parseAndSetArgumentValueFromBitString" function.
     *
     * @param index the index of the command argument to be queried
     *
     * @return A bit string representation of the queried command argument
     * (the string only contains '1' and '0' characters).
     *
     * @throws BlockException If a translation error occurs.
     */
    String getArgumentBitString(int index) throws BlockException;

    /**
     * Get the specified ICommandArguemnt value in a binary representation.
     *  This is the logical inverse of the "parseAndSetArgumentValueFromBitString" function.
     *
     * @param index the index of the repeat command argument holding the argument to be queried
     *
     * @param subIndex the index of the command argument to be queried
     *
     * @return A bit string representation of the queried command argument
     * (the string only contains '1' and '0' characters).
     *
     * @throws BlockException If a translation error occurs.
     */
    String getArgumentBitString (int index, int subIndex) throws BlockException;

    /**
     * Translate this command into a binary representation.  This is the logical
     * inverse of the "parseFromBitString" function.
     *
     * @return A bit string representation of this command
     * (the string only contains '1' and '0' characters).
     *
     * @throws BlockException If a translation error occurs.
     *
     * MPCS-9581 4/2/18 Remove BinaryRepresentable interface
     *
     */
    String toBitString() throws BlockException;

    /**
     * Retrieve the opcode value as a bit string
     *
     * @return A String containing all '0' and '1' characters representing the
     *         opcode of this command
     *
     * @throws BlockException
     *             If there is an error translating this command's opcode into
     *             binary
     *
     * MPCS-7725 01/20/16 Use OpcodeUtil
     */
    String getOpcodeBits() throws BlockException;

    /**
     * Given the binary representation of a command as a bit string, de-serialize
     * the bit string back into a command object. This function will only parse
     * the opcode portion of the command, it will not parse any associated
     * argument values.
     *
     * @param bitString
     *            The bit string to parse the command out of
     * @param offset
     *            The offset into the bit string where parsing should begin
     * @return The FlightCommand object corresponding to the input bit string
     *
     * @throws UnblockException
     *             If there is an error reversing the bit string back into a
     *             command object
     */
    IFlightCommand parseFromBitString(final String bitString, final int offset) throws UnblockException;
}
