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
package jpl.gds.tc.api.frame;

import java.util.List;

import jpl.gds.tc.api.ICommandFileLoad;
import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.ITelecommandPacket;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.CommandFileParseException;
import jpl.gds.tc.api.exception.CommandParseException;
import jpl.gds.tc.api.exception.FrameWrapUnwrapException;

/**
 * Interface for the TelecommandFrameBuilder
 * 
 *
 */
public interface ITcTransferFrameFactory {

    /**
     * Create a TelecommandFrame from a TelecommandPacket
     * 
     * @param packet
     *            The TelecommandPacket
     * @param scid
     *            spacecraft id
     * @param vcid
     *            vcid
     * @return ITcTransferFrame containing the packet data
     */
    ITcTransferFrame createTelecommandFrameFromPacket(final ITelecommandPacket packet, final int scid,
                                                             final int vcid);

    /**
     * Create a Telecommand frame from PDU data
     * 
     * @param pdu
     *            The PDU
     * 
     * @param scid
     *            The spacecraft id
     * @param vcid
     *            The vcid
     * @return ITcTransferFrame
     */
    ITcTransferFrame createTelecommandFrameFromPdu(byte[] pdu, int scid, int vcid);

    /**
     * Get the set of frames that contain the input file load
     * 
     * @param load
     *            The file load to wrap in a set of telecommand frames
     *
     * @return A set of telecommand frames containing the input file load
     */
    List<ITcTransferFrame> createFileLoadFrames(final ICommandFileLoad load) throws FrameWrapUnwrapException;

    /**
     * Get a frame that contains a single command
     * @param command
     * @return
     * @throws BlockException
     * @throws CommandParseException
     * @throws CommandFileParseException
     */
    ITcTransferFrame createCommandFrame(IFlightCommand command) throws BlockException, CommandParseException, CommandFileParseException;

    /**
     * Create a beginning delimiter frame.
     *
     * @return a delimiter telecommand frame
     */
     ITcTransferFrame createBeginDelimiterFrame();

    /**
     * Create an ending delimiter frame.
     *
     * @return a delimiter telecommand frame for placement at the end of a
     */
    ITcTransferFrame createEndDelimiterFrame();

}
