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

import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.evr.api.message.EvrMessageType;
import jpl.gds.globallad.GlobalLadException;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm;
import jpl.gds.globallad.disruptor.IDisruptorProducer;
import jpl.gds.globallad.io.IGlobalLadDataSource;
import jpl.gds.globallad.io.MessageToGladDataConverter;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.message.api.external.IExternalMessageUtility;
import jpl.gds.message.api.external.IMessageServiceListener;
import jpl.gds.message.api.external.ITopicSubscriber;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessageType;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This data source provides the GLAD with EHA and EVR data from the JMS bus
 */
public class GlobalLadJmsDataSource implements IGlobalLadDataSource, IMessageServiceListener {

    private final Tracer                 tracer;
    private final List<ITopicSubscriber> subscribers;

    private final Object                  waitLock  = new Object();
    private final AtomicBoolean           isRunning = new AtomicBoolean(false);
    private final JmsSubscriberDisruptor  jmsSubscriberDisruptor;
    private final JmsDataSourceMetrics    metrics;
    private final IExternalMessageUtility msgUtil;

    /**
     * Constructor
     *
     * @param subscribers       topic subscribers
     * @param messageTranslator message translator
     * @param msgUtil           external message utility
     * @param dataProducer      global lad data producer
     * @param tracer            log tracer
     */
    public GlobalLadJmsDataSource(final List<ITopicSubscriber> subscribers,
                                  final MessageToGladDataConverter messageTranslator,
                                  final IExternalMessageUtility msgUtil,
                                  final IDisruptorProducer<IGlobalLADData> dataProducer,
                                  final Tracer tracer) {
        this.subscribers = subscribers;
        this.tracer = tracer;
        this.metrics = new JmsDataSourceMetrics();
        this.msgUtil = msgUtil;
        this.jmsSubscriberDisruptor = new JmsSubscriberDisruptor(msgUtil, messageTranslator, dataProducer, this.metrics,
                tracer);

        init();
    }

    /**
     * Initialize thyself
     */
    private void init() {
        tracer.debug("Initializing GlobalLAD JMS Data Source");
        for (ITopicSubscriber sub : this.subscribers) {
            try {
                sub.setMessageListener(this);
            } catch (MessageServiceException e) {
                tracer.error("An error occurred while initializing the JMS data source.", e);
            }
        }
    }

    private void startup() throws GlobalLadException {
        isRunning.set(true);
        tracer.debug("Starting JMS Data Source.");
        jmsSubscriberDisruptor.startDisruptor();
        metrics.start();
        for (ITopicSubscriber sub : subscribers) {
            tracer.info("Listening to JMS topic: ", sub.getTopic());
            try {
                sub.start();
            } catch (MessageServiceException e) {
                throw new GlobalLadException(e);
            }
        }
        tracer.info("JMS Data Source is running");
    }

    @Override
    public void run() {
        try {
            startup();
        } catch (GlobalLadException e) {
            tracer.error("Unable to complete JMS data source startup.", e);
            return;
        }
        while (isRunning.get()) {
            synchronized (waitLock) {
                try {
                    waitLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        shutdown();
    }

    private void shutdown() {
        tracer.info("Shutting down GlobalLAD JMS Data Source");
        for (ITopicSubscriber subscriber : subscribers) {
            subscriber.close();
        }
        tracer.debug("Subscribers closed");
        jmsSubscriberDisruptor.shutdown();
        tracer.debug("Disruptor shutdown");
        metrics.stop();
        isRunning.set(false);
        tracer.info("JMS Data Source is stopped.");
    }

    @Override
    public void close() throws IOException {
        isRunning.set(false);
        synchronized (waitLock) {
            waitLock.notify();
        }
        tracer.info("GlobalLAD JMS Data Source has been shut down.");
    }

    @Override
    public JsonObject getStats() {
        return Json.createObjectBuilder()
                .add("disruptorCapacity", jmsSubscriberDisruptor.getBufferSize())
                .add("disruptorRemainingCapacity", jmsSubscriberDisruptor.remainingCapacity())
                .add("jms", metrics.getJmsMetrics())
                .build();
    }

    @Override
    public JsonObject getMetadata(final IGlobalLadContainerSearchAlgorithm matcher) {
        return getStats();
    }

    @Override
    public String getJsonId() {
        return "GlobalLadJmsListener";
    }

    @Override
    public void onMessage(final IExternalMessage externalMessage) {
        try {
            IMessageType type = msgUtil.getInternalType(externalMessage);
            if (IMessageType.matches(EhaMessageType.AlarmedEhaChannel, type)
                    || IMessageType.matches(EvrMessageType.Evr, type)
                    || IMessageType.matches(EhaMessageType.GroupedEhaChannels, type)
                    || IMessageType.matches(EhaMessageType.ChannelValue, type)) {


                jmsSubscriberDisruptor.publishEvent(externalMessage);
                metrics.incJmsRx();
            }
        } catch (MessageServiceException e) {
            tracer.trace(e.getMessage());
        }
    }

}
