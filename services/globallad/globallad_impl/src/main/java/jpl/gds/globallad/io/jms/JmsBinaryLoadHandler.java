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

package jpl.gds.globallad.io.jms;

import jpl.gds.globallad.data.factory.IGlobalLadDataFactory;
import jpl.gds.globallad.data.storage.DataInsertionManager;
import jpl.gds.globallad.disruptor.ByteBufferEvent;
import jpl.gds.globallad.disruptor.ByteBufferEventFactory;
import jpl.gds.globallad.io.AbstractBinaryLoadHandler;
import jpl.gds.globallad.io.GlobalLadDataMessageConstructor;
import jpl.gds.globallad.io.IBinaryLoadHandler;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Insertion of binary data dump into JMS LAD
 */
public class JmsBinaryLoadHandler extends AbstractBinaryLoadHandler implements IBinaryLoadHandler {
    private final GlobalLadDataMessageConstructor messageConstructor;
    private final ByteBufferEventFactory          byteBufferEventFactory = new ByteBufferEventFactory();
    private       long                            sequence               = 0;

    /**
     * Constructor
     *
     * @param input        Client input stream, binary LAD data
     * @param dataFactory  GLAD data factory
     * @param dataProducer GLAD data insertion manager
     */
    @Autowired
    public JmsBinaryLoadHandler(final InputStream input, final IGlobalLadDataFactory dataFactory,
                                DataInsertionManager dataProducer) {
        super(input);
        this.messageConstructor = new GlobalLadDataMessageConstructor(dataFactory,
                dataProducer);
    }

    @Override
    protected void handleBytes(final byte[] buffer, final int bytesRead) throws Exception {
        final ByteBuffer      byteBuffer = ByteBuffer.wrap(Arrays.copyOf(buffer, bytesRead), 0, bytesRead);
        final ByteBufferEvent event      = byteBufferEventFactory.newInstance();
        event.set(byteBuffer);

        messageConstructor.onEvent(event, sequence++, false);
    }
}
