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

package jpl.gds.globallad.io.socket;

import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.io.AbstractBinaryLoadHandler;
import jpl.gds.globallad.io.IBinaryLoadHandler;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Insertion of binary data dump via Socket connection
 */
public class SocketBinaryLoadHandler extends AbstractBinaryLoadHandler implements IBinaryLoadHandler {
    private final GlobalLadProperties config = GlobalLadProperties.getGlobalInstance();
    private       OutputStream        outputStream;

    /**
     * Constructor
     *
     * @param input Client input stream, binary LAD data
     */
    public SocketBinaryLoadHandler(InputStream input) {
        super(input);
    }

    @Override
    public void execute() throws Exception {
        try (Socket client = new Socket(config.getServerHost(), config.getSocketServerPort())) {
            final OutputStream op = client.getOutputStream();
            setOutputStream(op);

            // execute the abstract byte reading behavior
            super.execute();

            op.flush();
            outputStream = null;
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    @Override
    protected void handleBytes(final byte[] bytes, final int bytesRead) throws Exception {
        outputStream.write(bytes, 0, bytesRead);
    }

    private void setOutputStream(final OutputStream outputStream) {
        this.outputStream = outputStream;
    }
}
