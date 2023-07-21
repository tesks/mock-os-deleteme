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

import com.lmax.disruptor.EventHandler;
import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.evr.api.message.EvrMessageType;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.disruptor.IDisruptorProducer;
import jpl.gds.globallad.disruptor.JmsDataEvent;
import jpl.gds.globallad.io.MessageToGladDataConverter;
import jpl.gds.globallad.service.GlobalLadConversionException;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.message.api.external.IExternalMessageUtility;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;

import java.util.Arrays;

/**
 * Disruptor JMS Event Handler. Takes IExternalMessages off of the disruptor queue, extracts the individual EHA and EVR
 * messages, and submits them to the GLAD.
 */
public class JmsEventHandler implements EventHandler<JmsDataEvent> {
    private final IExternalMessageUtility            msgUtil;
    private final MessageToGladDataConverter         messageTranslator;
    private final IDisruptorProducer<IGlobalLADData> dataProducer;
    private final JmsDataSourceMetrics               metrics;
    private final Tracer                             tracer;

    /**
     * Constructor
     *
     * @param msgUtil           external message utility
     * @param messageTranslator message translator
     * @param dataProducer      global lad data producer
     * @param metrics           metrics gatherer
     * @param tracer            log tracer
     */
    public JmsEventHandler(final IExternalMessageUtility msgUtil, final MessageToGladDataConverter messageTranslator,
                           final IDisruptorProducer<IGlobalLADData> dataProducer, final JmsDataSourceMetrics metrics,
                           final Tracer tracer) {
        this.msgUtil = msgUtil;
        this.messageTranslator = messageTranslator;
        this.dataProducer = dataProducer;
        this.metrics = metrics;
        this.tracer = tracer;
    }

    @Override
    public void onEvent(final JmsDataEvent event, final long sequence, final boolean endOfBatch) throws Exception {
        final IExternalMessage externalMessage = event.data;

        final long queueTimeNs = System.nanoTime() - event.timestamp;
        metrics.addJmsQueueWaitTime(queueTimeNs / 1_000_000_000.0);

        try {
            final IMessage[] messages = msgUtil.instantiateMessages(externalMessage);
            metrics.addTlmRx(messages.length);

            final int    scid  = msgUtil.getIntHeaderProperty(externalMessage, "SPACECRAFT_ID");
            final String venue = msgUtil.getStringHeaderProperty(externalMessage, "VENUE_TYPE").intern();

            Arrays.stream(messages).forEach(msg -> {
                if (msg.getType().equals(EhaMessageType.AlarmedEhaChannel) || msg.getType()
                        .equals(EvrMessageType.Evr)) {
                    try {
                        final IGlobalLADData data = messageTranslator.convert(msg, scid, venue);
                        if (data != null) {
                            dataProducer.onData(data);
                            if (msg.getType().equals(EhaMessageType.AlarmedEhaChannel)) {
                                metrics.incEhaRx();
                            } else if (msg.getType().equals(EvrMessageType.Evr)) {
                                metrics.incEvrRx();
                            }
                        } else {
                            tracer.warn("No data from message: ", msg.getType().getSubscriptionTag());
                        }
                    } catch (GlobalLadConversionException e) {
                        tracer.error("An error occurred adding data to the global LAD: ", e.getMessage());
                    }
                }
            });

            metrics.incJmsProc();
        } catch (MessageServiceException e) {
            tracer.error("An error occurred processing JMS messages: ", e);
        } finally {
            // this is important for disruptor handlers, see
            // https://lmax-exchange.github.io/disruptor/user-guide/index.html#_clearing_objects_from_the_ring_buffer
            event.clear();
        }
    }
}
