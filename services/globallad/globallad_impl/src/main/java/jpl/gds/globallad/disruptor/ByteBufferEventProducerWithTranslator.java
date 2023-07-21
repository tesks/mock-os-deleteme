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
package jpl.gds.globallad.disruptor;

import java.nio.ByteBuffer;

import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;

/**
 * Disruptor producer to create byte buffer events.
 */
public class ByteBufferEventProducerWithTranslator implements IDisruptorProducer<ByteBuffer>
{
    private final RingBuffer<ByteBufferEvent> ringBuffer;

    /**
     * @param ringBuffer 
     */
    public ByteBufferEventProducerWithTranslator(RingBuffer<ByteBufferEvent> ringBuffer)
    {
        this.ringBuffer = ringBuffer;
    }

    private static final EventTranslatorOneArg<ByteBufferEvent, ByteBuffer> TRANSLATOR =
        new EventTranslatorOneArg<ByteBufferEvent, ByteBuffer>()
        {
            public void translateTo(ByteBufferEvent event, long sequence, ByteBuffer bb)
            {
                event.set(bb);
            }
        };

    /* (non-Javadoc)
     * @see jpl.gds.globallad.disruptor.IDisruptorProducer#onData(java.lang.Object)
     */
    public void onData(ByteBuffer bb)
    {
        ringBuffer.publishEvent(TRANSLATOR, bb);
    }
}


