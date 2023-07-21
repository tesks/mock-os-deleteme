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

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.container.GlobalLadUtilities;
import jpl.gds.globallad.disruptor.IDisruptorProducer;
import jpl.gds.globallad.disruptor.JmsDataEvent;
import jpl.gds.globallad.io.MessageToGladDataConverter;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.message.api.external.IExternalMessageUtility;
import jpl.gds.shared.log.Tracer;

/**
 * JMS subscriber disruptor. Manages a disruptor, accepts external messages, and publishes them to the disruptor ring
 * buffer.
 */
public class JmsSubscriberDisruptor {

    private final IExternalMessageUtility            msgUtil;
    private final IDisruptorProducer<IGlobalLADData> dataProducer;
    private final JmsDataSourceMetrics               metrics;
    private final Tracer                             tracer;
    private final MessageToGladDataConverter         messageTranslator;
    private       RingBuffer<JmsDataEvent>           ringBuffer;
    private       Disruptor<JmsDataEvent>            disruptor;


    /**
     * Constructor
     *
     * @param msgUtil           external message utility
     * @param messageTranslator jms message to glad data translator
     * @param dataProducer      glad data producer
     * @param metrics           metrics tracker
     * @param tracer            log tracer
     */
    public JmsSubscriberDisruptor(IExternalMessageUtility msgUtil, MessageToGladDataConverter messageTranslator,
                                  IDisruptorProducer<IGlobalLADData> dataProducer, JmsDataSourceMetrics metrics,
                                  Tracer tracer) {
        this.msgUtil = msgUtil;
        this.messageTranslator = messageTranslator;
        this.dataProducer = dataProducer;
        this.metrics = metrics;
        this.tracer = tracer;
    }

    /**
     * Start the disruptor
     */
    public void startDisruptor() {
        WaitStrategy inserterWaitStrategy;

        switch (GlobalLadProperties.getGlobalInstance().getClientWaitStrategy()) {
            case SLEEP:
                inserterWaitStrategy = new SleepingWaitStrategy();
                break;
            case SPIN:
                inserterWaitStrategy = new BusySpinWaitStrategy();
                break;
            case YIELD:
                inserterWaitStrategy = new YieldingWaitStrategy();
                break;
            case BLOCK:
            default:
                inserterWaitStrategy = new BlockingWaitStrategy();
                break;
        }

        disruptor = new Disruptor<>(JmsDataEvent.DATA_EVENT_FACTORY, // eventFactory,
                GlobalLadProperties.getGlobalInstance().getGlobalLadRingBufferSize(), // ringBufferSize,
                GlobalLadUtilities.createThreadFactory("glad-jms-worker-%d"), // thread factory
                ProducerType.MULTI, // jms writes with multiple threads
                inserterWaitStrategy
        );

        final EventHandler<JmsDataEvent> handler = new JmsEventHandler(msgUtil, messageTranslator, dataProducer, metrics,
                tracer);

        disruptor.handleEventsWith(handler);

        ringBuffer = disruptor.start();
    }

    /**
     * Publish JMS external message to disruptor
     *
     * @param message
     */
    public void publishEvent(final IExternalMessage message) {
        ringBuffer.publishEvent(JmsDataEvent.DATA_TRANSLATOR, message);
    }

    /**
     * Get the disruptor's remaining capacity
     *
     * @return
     */
    public long remainingCapacity() {
        return ringBuffer.remainingCapacity();
    }

    /**
     * Get the disruptor's configured buffer size
     *
     * @return
     */
    public int getBufferSize() {
        return ringBuffer.getBufferSize();
    }

    /**
     * Shutdown
     */
    public void shutdown() {
        disruptor.shutdown();
    }
}
