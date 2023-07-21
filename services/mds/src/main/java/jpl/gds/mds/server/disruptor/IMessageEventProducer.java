/*
 * Copyright 2006-2020. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.mds.server.disruptor;

import com.lmax.disruptor.RingBuffer;
import org.springframework.messaging.Message;

/**
 * Interface for a Message Event Producer
 */
public interface IMessageEventProducer extends IMessageHandler {
    /**
     * Set the ring buffer used for exchanging message events
     *
     * @param ringBuffer Ring buffer of MessageEvent
     */
    void setRingBuffer(final RingBuffer<MessageEvent> ringBuffer);

    /**
     * Process the provided message
     *
     * @param message Message
     */
    void handleMessage(final Message message);
}
