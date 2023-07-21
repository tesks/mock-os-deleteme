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
package jpl.gds.tc.api;

import java.io.PrintWriter;

import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.UnblockException;

public interface ICommandWriteUtility {

    /** String consisting of only two newline characters */
    String DOUBLE_LINE        = "\n\n";
    /** Formatting for hex output.  The number of hex characters that go on a single line */
    int    HEX_CHARS_PER_LINE = 48;

    /**
     * Write out a CLTU
     * @param pw The PrintWriter to write results to
     * @param cltu The CLTU to be written
     * @param cltuNumber The number of the CLTU in the file
     * 
     * @throws BlockException if there is an error translating the command to the required telecommand data bytes
     * @throws UnblockException if there is an error translating the telecommand data bytes to commands
     */
    void writeCltu(PrintWriter pw, ICltu cltu, int cltuNumber) throws UnblockException, BlockException;

    /**
     * Write out a telecommand frame.
     * @param pw The PrintWriter to write results to
     * @param frame The frame to display
     * @param frameNumber The number of the frame in the CLTU
     * 
     * @throws UnblockException if there is an error translating the telecommand data bytes to commands
     * @throws BlockException if there is an error translating the command to the required telecommand data bytes
     */
    void writeFrame(PrintWriter pw, ITcTransferFrame frame, int frameNumber) throws UnblockException, BlockException;

    /**
     * Write out a CLTU that could not be interpreted
     * 
     * @param pw The PrintWriter to write results to
     * @param bytes The bytes that were unable to be interpreted as a CLTU
     * @param cltuNumber The number of the CLTU in the file
     */
    void writeBadCltu(PrintWriter pw, byte[] bytes, int cltuNumber);

    /**
     * Write out a command that could not be interpreted
     * @param pw The PrintWriter to write results to
     * @param frameDataBits The data that could not be interpreted as a command
     */
    void writeBadCommand(PrintWriter pw, String frameDataBits);

    /**
     * Write out a command
     * 
     * @param pw The PrintWriter to write results to
     * @param command The command to display
     * @throws BlockException if there is an error translating the command to the required telecommand data bytes
     */
    void writeCommand(PrintWriter pw, IFlightCommand command) throws BlockException;

    /**
     * Write out a command argument
     * 
     * @param pw
     *            The PrintWriter to write results to
     * @param cmd
     *            The command containing the argument
     * @param index
     *            The index of the argument to be displayed
     * @throws BlockException
     *             if there is an error translating the command to the required
     *             telecommand data bytes
     * MPCS-10473 - 04/05/19 - Arguments now permanently contained within the owning IFlightCommand
     *          Updated function arguments
     */
    void writeCommandArgument(final PrintWriter pw, final IFlightCommand cmd, final int index) throws BlockException;
    
    /**
     * Write out a command argument
     * 
     * @param pw
     *            The PrintWriter to write results to
     * @param cmd
     *            The command argument to display
     * @throws BlockException
     *             if there is an error translating the command to the required
     *             telecommand data bytes
     * MPCS-10473 - 04/05/19 - Arguments now permanently contained within the owning IFlightCommand.
     *          Added this second version to print repeat command arguments.
     */
    void writeCommandArgument(final PrintWriter pw, final IFlightCommand cmd, final int index, final int subIndex) throws BlockException;

    /**
     * Write out the beginning of a file load.  This is treated differently because the first chunk of a file load
     * contains all of the metadata for the file load.
     * 
     * @param pw The PrintWriter to write results to
     * @param load The file load to display
     */
    void writeFirstFileLoad(PrintWriter pw, ICommandFileLoad load);

    /**
     * Write out a piece of a file load (that's not the first piece).
     * 
     * @param pw The PrintWriter to write results to
     * @param data The file load data to display
     */
    void writeFileLoad(PrintWriter pw, byte[] data);

}