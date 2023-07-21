/*
 * Copyright 2006-2021. California Institute of Technology.
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

package jpl.gds.globallad.io;

import java.io.InputStream;

/**
 * Abstract GLAD binary load handler. Handles reading bytes from a provided input stream, and delegates byte handling to
 * either a Socket or JMS handler.
 */
public abstract class AbstractBinaryLoadHandler implements IBinaryLoadHandler {

    protected static final int BUFFER_SIZE = 4096;
    protected final InputStream input;

    /**
     * Constructor
     *
     * @param input input stream
     */
    protected AbstractBinaryLoadHandler(final InputStream input) {
        this.input = input;
    }

    @Override
    public void execute() throws Exception {
        int    bytesRead = 0;
        byte[] buffer    = new byte[BUFFER_SIZE];

        do {
            bytesRead = input.read(buffer);

            if (bytesRead > 0) {
                handleBytes(buffer, bytesRead);
            }

        } while (bytesRead > 0);
    }

    /**
     * Handle bytes read from the provided input stream
     *
     * @param bytes
     * @param bytesRead
     * @throws Exception
     */
    protected abstract void handleBytes(final byte[] bytes, final int bytesRead) throws Exception;
}
