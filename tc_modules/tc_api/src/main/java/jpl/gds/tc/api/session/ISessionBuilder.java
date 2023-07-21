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
package jpl.gds.tc.api.session;

import java.util.List;

import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.exception.*;
import jpl.gds.tc.api.frame.ITcTransferFrameSerializer;

/**
 * An interface for the builder for command sessions.
 * 
 *
 * @since R8
 * MPCS-9390 - 1/24/18 - Added interface
 */
public interface ISessionBuilder {

    /**
     * The maximum size of a session (in data frames) is determined by the amount of bits that can be used to set the
     * sequence number in the telecommand frame header (as of this writing, that's 8 bits).
     */
    int MAX_SESSION_SIZE = Math.round((float) Math.pow(2, ITcTransferFrameSerializer.SEQUENCE_NUMBER_BIT_LENGTH));

    /**
     * Add a data frame to the uplink session.
     *
     * @param inputFrame The data frame to add to the uplink session.
     */
    void addFrame(ITcTransferFrame inputFrame);

    /**
     * Add a command to this uplink session
     * 
     * @param command The flight command to add to the session
     * 
     * @throws BlockException If the command cannot be serialized into the body of a frame
     */
    void addCommand(IFlightCommand command) throws BlockException, FrameWrapUnwrapException, CommandParseException, CommandFileParseException;

    /**
     * Add an entire set of frames to this session.
     * 
     * @param frames The frames to add to this uplink session.
     */
    void addFrames(List<ITcTransferFrame> frames);

    /**
     * Build the uplink session from the existing set of data frames.  The uplink session is built by
     * adding delimiter frames before/after particular data frames based on configuration settings.  In addition,
     * the sequence numbers of the data frames are set here because they are dependent on the structure of the
     * uplink session.  Finally, the uplink session repeat count is used to determine how many duplicate copies of the session
     * should be transmitted
     *
     * @return The complete list of data and delimiter telecommand frames
     * 
     * @throws SessionOverflowException If there are more frames in the result than are allowed in an uplink session 
     */
    List<ITcTransferFrame> getSessionFrames() throws SessionOverflowException;

    /**
     * Empty out the current list of data frames for this session.
     * 
     * Useful for reusing this object.
     */
    void clear();

    /**
     * Get the command list.
     * @return The list of commands used to build this session
     */
    List<IFlightCommand> getCommandList();

    /**
     * Test whether or not there are any frames in this session.
     * 
     * @return True if the session contains frames, false otherwise.
     */
    boolean isEmpty();

}