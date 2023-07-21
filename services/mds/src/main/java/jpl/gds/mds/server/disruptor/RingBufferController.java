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

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;

import jpl.gds.mds.server.config.MdsProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadFactory;

/**
 * This class is a ring buffer controller
 */
public class RingBufferController {

    private static final int RING_BUFFER_SIZE = 16384;
    private static final int TIMER_PERIOD = 2000;

    private final WaitStrategy waitStrategy;
    private final ProducerType producerType;
    private Timer timer;

    private final Tracer logger = TraceManager.getTracer(Loggers.MDS);

    @Autowired
    private IMessageEventConsumer messageEventConsumer;

    @Autowired
    private IMessageEventProducer messageEventProducer;

    @Autowired
    private MdsProperties mdsProperties;

    /**
     * Constructor
     */
    public RingBufferController() {
        this.producerType = ProducerType.SINGLE;
        this.waitStrategy = new BlockingWaitStrategy();
    }

    /**
     * Initalize
     */
    public void init() {
        logger.info("Initializing the Ring Buffer, UDP Port ", mdsProperties.getClientPort());
        final ThreadFactory threadFactory = DaemonThreadFactory.INSTANCE;
        final Disruptor<MessageEvent> disruptor = new Disruptor<>(MessageEvent.EVENT_FACTORY, RING_BUFFER_SIZE,
                                                                  threadFactory, producerType, waitStrategy);
        disruptor.handleEventsWith(messageEventConsumer.getEventHandler());

        final RingBuffer<MessageEvent> ringBuffer = disruptor.start();
        messageEventProducer.setRingBuffer(ringBuffer);

        final RingBufferMetricTask metricTask = new RingBufferMetricTask(ringBuffer, logger, messageEventProducer, messageEventConsumer);
        timer = new Timer(true);
        timer.scheduleAtFixedRate(metricTask, 0, TIMER_PERIOD);
    }

    /**
     * Cleanup
     */
    public void cleanup(){
        timer.cancel();
    }

    static class RingBufferMetricTask extends TimerTask {

        private final Tracer logger;
        private final IMessageEventProducer messageEventProducer;
        private final IMessageEventConsumer messageEventConsumer;
        private RingBuffer<MessageEvent> ringBuffer;

        RingBufferMetricTask(final RingBuffer<MessageEvent> ringBuffer,
                             final Tracer logger,
                             final IMessageEventProducer messageEventProducer,
                             final IMessageEventConsumer messageEventConsumer) {
            this.ringBuffer = ringBuffer;
            this.logger = logger;
            this.messageEventProducer = messageEventProducer;
            this.messageEventConsumer = messageEventConsumer;
        }

        @Override
        public void run() {
            long remainingCapacity = ringBuffer.remainingCapacity();
            long bufferSize = ringBuffer.getBufferSize();
            long inputCnt = messageEventProducer.getTotalCount();
            long outputCnt = messageEventConsumer.getTotalCount();
            long delta = inputCnt - outputCnt;
            logger.debug( "RingBuffer InputCnt : " + inputCnt
                    + ", TotalSize: " + bufferSize
                    + ", OutputCnt : " + outputCnt
                    + ", Delta : " + delta
                    + ", Cursor : " + ringBuffer.getCursor()
                    + ", MinGatingSeq : " + ringBuffer.getMinimumGatingSequence()
                    + ", toString() : " + ringBuffer.toString()
                    + ", remaining capacity: " + remainingCapacity
                    + ", queue size: " + (bufferSize - remainingCapacity));
        }
    }
}
