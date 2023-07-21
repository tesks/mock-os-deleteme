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

package jpl.gds.tc.api;

import com.bluecast.io.FileFormatException;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.config.ExecutionStringType;
import jpl.gds.tc.api.config.FileLoadParseException;
import jpl.gds.tc.api.config.VirtualChannelType;
import jpl.gds.tc.api.exception.*;

import java.io.IOException;
import java.util.List;

/**
 * Interface for TEW utilities.
 *
 */
public interface ITewUtility {

    /**
     * Translate and validate the command mnemonic. When using MPSA CTS "Gold Standard", string arguments must be in
     * quotes.
     *
     * @param command flight command object
     * @return translated command bytes
     * @throws BlockException        block exception if the command could not be translated into bytes
     * @throws CommandParseException if an error occurs parsing the command
     */
    byte[] validateAndTranslateCommand(final IFlightCommand command) throws BlockException, CommandParseException, CommandFileParseException;

    /**
     * Wrap a command byte array into packet.
     * <p>
     * NOTE: 2019-04-25 - Not supported yet in AMPCS
     *
     * @param commandBytes translated command bytes
     * @param apid         application process id
     * @return the telecommand packet
     * @throws FrameWrapUnwrapException frame format exception
     */
    ITelecommandPacket wrapBytesToPacket(final byte[] commandBytes, final int apid) throws FrameWrapUnwrapException;

    /**
     * Wrap an IFlightCommand into a frame
     *
     * @param command the flight command
     * @return the TC transfer frame
     * @throws FrameWrapUnwrapException an error was encountered while making a frame from the command
     */
    ITcTransferFrame wrapCommandToFrame(IFlightCommand command) throws FrameWrapUnwrapException;

    /**
     * Wrap an IFlightCommand into the full sequence of frames
     * <br>
     * This includes any delimiter frames
     * @param command the flight command
     * @return the TC transfer frames
     * @throws FrameWrapUnwrapException
     */
    List<ITcTransferFrame> wrapCommandToFrames(IFlightCommand command) throws FrameWrapUnwrapException;

    /**
     * Wrap bytes into a frame
     *
     * @param bytes byte payload
     * @param vcid virtual channel ID
     * @return the TC transfer frame
     */
    ITcTransferFrame wrapBytesToFrame(byte[] bytes, int vcid) throws FrameWrapUnwrapException;


    /**
     * Wrap PDU bytes into a frame
     *
     * @param bytes byte payload
     * @param vcid virtual channel ID
     * @param fsn the frame sequence number to set
     * @return the TC transfer frame
     */
    ITcTransferFrame wrapBytesToFrame(final byte[] bytes, final int vcid, final int fsn) throws FrameWrapUnwrapException;

    /**
     * Wrap telecommand frame bytes into a CLTU
     *
     * @param commandBytes translated command bytes
     * @param vcid         virtual channel id, 3-bit execution string + 3-bit vc number converted to int
     * @return a CLTU
     * @throws CommandParseException if an error occurs parsing command bytes
     * @throws CltuEndecException    if an error occurs formatting the CLTU
     */
    ICltu wrapBytesToCltu(final byte[] commandBytes, final int vcid) throws CommandParseException, CltuEndecException;

    /**
     * Wrap a telecommand frame into a CLTU
     *
     * @param frame the telecommand frame to be turned into a CLTU
     * @return the CLTU
     */
    ICltu wrapFrameToCltu(final ITcTransferFrame frame) throws CltuEndecException;

    /**
     * Wrap telecommand frame bytes into a CLTU
     *
     * @param commandBytes translated command bytes
     * @param esType       execution string type
     * @param vcType       virtual channel type
     * @return a CLTU
     */
    ICltu wrapBytesToCltu(final byte[] commandBytes, final ExecutionStringType esType,
                          final VirtualChannelType vcType) throws CommandParseException, CltuEndecException;

    /**
     * Wrap the list of text files into command file
     *
     * @param info file load information
     * @return a list of command file loads
     * @throws FileFormatException    file format exception
     * @throws FileLoadParseException If there's an issue interpreting the user input file load info
     */
    List<ICommandFileLoad> wrapFileToFileLoads(final IFileLoadInfo info) throws
                                                                         FileFormatException,
                                                                         FileLoadParseException;

    /**
     * Wrap the file into a list of telecommand frames
     *
     * @param load command file load
     * @return a list of telecommand frames
     * @throws FrameWrapUnwrapException frame format exception
     */
    List<ITcTransferFrame> wrapFileLoadToFrames(final ICommandFileLoad load) throws FrameWrapUnwrapException;

    /**
     * Wrap a list of CLTUs into a list of PLOP CLTUs
     *
     * @param cltus list of CLTUs to be wrapped with PLOP acquisition and tail sequences
     * @return a list of CLTUs
     * @throws CltuEndecException CLTU format exception
     */
    List<ICltu> createPlopCltus(final List<ICltu> cltus) throws CltuEndecException;

    /**
     * Reverse SCMF bytes into IScmf
     *
     * @param scmf scmf bytes
     * @return an SCMF
     */
    IScmf reverseScmf(final byte[] scmf) throws ScmfParseException, ScmfWrapUnwrapException, IOException;

    /**
     * Reverse an SCMF from file path
     *
     * @param scmfFilePath file path to SCMF
     * @return an SCMF
     */
    IScmf reverseScmf(final String scmfFilePath) throws ScmfWrapUnwrapException, IOException, ScmfParseException;

    /**
     * Unwrap a CLTU to telecommand frames
     *
     * @param cltu CLTU
     * @return a list of telecommand frames
     */
    List<ITcTransferFrame> unwrapCltuToFrames(final ICltu cltu) throws CltuEndecException, FrameWrapUnwrapException;

    /**
     * Unwrap a telecommand frame to packets NOTE: 2019-04-25 - not currently supported by AMPCS
     *
     * @param frame input telecommand frame
     * @return a list of telecommand packets
     */
    List<ITelecommandPacket> unwrapFrameToPackets(ITcTransferFrame frame);

    /**
     * Reverse translate a telecommand frame to a flight command
     *
     * @param frame a telecommand frame
     * @return a flight command
     * @throws UnblockException If there is an error reversing the bit string back into a command object
     */
    IFlightCommand reverseFrameToCommand(ITcTransferFrame frame) throws UnblockException;

    /**
     * Reverse translate a telecommand packet to a flight command NOTE: 2019-04-25 - not currently supported by AMPCS
     *
     * @param packet a telecommand packet
     * @return a flight command
     */
    IFlightCommand reversePacketToCommand(ITelecommandPacket packet) throws UnblockException;

    /**
     * Reverse a telecommand frame to a command file load
     *
     * @param frames the telecommand frames containing a file laod
     * @return a command file load
     */
    ICommandFileLoad reverseFramesToFileLoad(List<ITcTransferFrame> frames);

    /**
     * Reverse a telecommand packet to a command file load
     *
     * @param packets the telecommand packets containing a file load
     * @return a command file load
     */
    ICommandFileLoad reversePacketsToFileLoad(List<ITelecommandPacket> packets);

    /**
     * Re-wrap a (modified) telecommand packet NOTE: 2019-04-25 - not supported by AMPCS
     *
     * @param packet input telecommand packet
     * @return telecommand packet
     */
    ITelecommandPacket rewrapPacket(ITelecommandPacket packet);

    /**
     * Re-wrap a modified CLTU
     *
     * @param cltu a CLTU
     * @return a CLTU
     */
    ICltu rewrapCltu(ICltu cltu) throws CltuEndecException;

}
