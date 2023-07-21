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

import jpl.gds.mds.server.udp.IUdpClient;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import org.springframework.messaging.Message;

/**
 * This class handles the publication of messages to the ring buffer.
 */
public class RingBufferPublisher implements IMessageEventProducer {

    private final Tracer                   logger = TraceManager.getTracer(Loggers.MDS);
    private       RingBuffer<MessageEvent> ringBuffer;
    private       long                     count  = 0;
    private       final IUdpClient udpClient;

    /**
     * Constructor
     * @param udpClient IUdpClient
     */
    public RingBufferPublisher(final IUdpClient udpClient) {
        this.udpClient = udpClient;
    }

    @Override
    public void setRingBuffer(RingBuffer<MessageEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    @Override
    public synchronized void handleMessage(final Message message) {
        count++;
        ringBuffer.publishEvent(MessageEvent.DATA_TRANSLATOR, message);
        logger.info("RingBufferPublisher: Count= " + count + " Cursor= " + ringBuffer
                .getCursor() + " MsgSize=" + ((byte[]) message.getPayload()).length);
        //forward UDP message
        udpClient.sendMessage((byte[]) message.getPayload());
    }

    @Override
    public long getTotalCount() {
        return count;
    }
}
