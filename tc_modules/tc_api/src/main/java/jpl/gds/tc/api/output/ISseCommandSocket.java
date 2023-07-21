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
package jpl.gds.tc.api.output;

import java.io.IOException;

import jpl.gds.common.config.connection.IConnection;

public interface ISseCommandSocket {

    /**
     * Transmit SSE command.
     *
     * There are three levels of validation: creating the socket,
     * getting the print writer, and writing the data.
     *
     * If the socket does not exist, we try just one time to do all three.
     * If the socket already exists, and one of the second two fail, the
     * socket is recreated, but just one attempt is made to do all three.
     *
     * The reason to try to recreate at all is to take care of the case where
     * the receiving application drops. We pick that up and subsequent sends may
     * succeed.
     *
     * If all is well, just one socket is ever created and used over and over.
     *
     * If an IOException is thrown, the socket and print writer are closed and null.
     *
     * @param commandString Command string
     *
     * @throws IOException On I/O error
     */
    void transmitSseCommand(String commandString) throws IOException;

    /**
     * Enable SSE socket.
     */
    void enableSseSocket(IConnection cc);

    /**
     * Close and remove SSE socket.
     *
     * Must be synchronized in case transmitSseCommand
     * is active. We can go ahead and clear the flag,
     * though.
     */
    void disableSseSocket();

    /**
     * Get SSE socket enable state.
     *
     * @return True if enabled
     */
    boolean isSseSocketEnabled();

    /**
     * Gets the configured SSE command socket host
     * 
     * @return destination host
     */
    public String getHost();

    /**
     * Gets the configured SSE command socket port
     * 
     * @return destination port
     */
    public int getPort();

}