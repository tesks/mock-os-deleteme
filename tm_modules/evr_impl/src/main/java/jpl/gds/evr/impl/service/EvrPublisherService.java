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
package jpl.gds.evr.impl.service;

import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.RealtimeRecordedConfiguration;
import jpl.gds.common.types.EhaBool;
import jpl.gds.common.types.RecordedBool;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.evr.api.IEvr;
import jpl.gds.evr.api.message.IEvrMessage;
import jpl.gds.evr.api.message.IEvrMessageFactory;
import jpl.gds.evr.api.service.IEvrPublisherService;
import jpl.gds.evr.api.service.extractor.EvrExtractorException;
import jpl.gds.evr.api.service.extractor.IEvrExtractor;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.spring.bootstrap.SharedSpringBootstrap;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * EvrPublisher subscribes to PacketMessages, invokes the proper EVR adaptor to
 * extract the EVR from the packet (if it is an EVR packet), and publishes EVR
 * messages.
 * 
 */
public class EvrPublisherService implements IEvrPublisherService {
    /**
     * The Tracer logger
     */
    private final Tracer                        log;
    /**
     * The message context.
     */
    protected IMessagePublicationBus messageContext;
    /**
     * Mission adapter.
     */
    protected IEvrExtractor missionAdapter;
    /**
     * Evr task.
     */
    protected String evrTask;
    /**
     * Number of Evr Packets.
     */
    protected long evrPackets;
    /**
     * APID dictionary.
     */
    protected IApidDefinitionProvider apidDefs;

    /**
     * EHA and EVR R/T recorded configuration.
     */
    private final RealtimeRecordedConfiguration rtRecConfig;

    private MessageSubscriber subscriber;

    /**
     * The mission-specific Evr apids.
     */
    private SortedSet<Integer> evrApids;
    
    private final ApplicationContext serviceContext;
    
    private final IEvrMessageFactory messageFactory;

    private final IStatusMessageFactory statusMessageFactory;
    private final IContextConfiguration contextConfig;
    

    /**
     * Creates an EVR publisher.
     * 
     * @param context
     *            the current application context
     */
    public EvrPublisherService(final ApplicationContext context)  {
        
        serviceContext = context;
        messageContext = context.getBean(SharedSpringBootstrap.PUBLICATION_BUS, IMessagePublicationBus.class);
        messageFactory = context.getBean(IEvrMessageFactory.class);
        statusMessageFactory = context.getBean(IStatusMessageFactory.class);
        apidDefs = context.getBean(IApidDefinitionProvider.class);
        loadApids();
        rtRecConfig = context.getBean(RealtimeRecordedConfiguration.class);
        log = TraceManager.getTracer(context, Loggers.TLM_EVR);
        contextConfig = context.getBean(IContextConfiguration.class);
        log.debug("Evr Publisher has started");
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.interfaces.IService#startService()
     */
    @Override
    public boolean startService() {
        subscriber = new EvrMessageSubscriber();
        try
        {
            missionAdapter = serviceContext.getBean(IEvrExtractor.class);
        }
        catch (final Exception e)
        {
            log.error("EVR extractor configuration error: " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Processes an IPacketMessage and produces EVR messages.
     * 
     * @param pm
     *            the IPacketMessage just received
     */
    private void consume(final ITelemetryPacketMessage pm) {
        IEvr evr = null;
        if (!contextConfig.accept(pm) || pm.getPacketInfo().isFill()
                || !isEvrApid(pm.getPacketInfo().getApid())) {

            return;
        }
        evrPackets++;

        try {
            evr = missionAdapter.extractEvr(pm);
            if (evr == null) {
                return; // not an EVR
            }


        } catch (final EvrExtractorException e) {
            sendInvalidPacketMessage(pm, e.getMessage() == null ? e.toString()
                    : e.getMessage());
            return;
        }

        final IEvrMessage evrMessage = messageFactory.createEvrMessage(evr);
        evr.setFromSse(pm.getPacketInfo().isFromSse());
        /* Set EVR RCT */
        evr.setRct(new AccurateDateTime());
        evrMessage.setFromSse(evr.isFromSse());


        final ITelemetryPacketInfo pi = pm.getPacketInfo();
        
        final RecordedBool state =
            rtRecConfig.getState(EhaBool.EVR, pi.getApid(), pi.getVcid(), pi.isFromSse());

        evrMessage.getEvr().setRealtime(! state.get());
        
        /*  Do not convert the EVR to a string unless in TRACE mode. */
        if (log.isEnabledFor(TraceSeverity.TRACE)) {
            log.trace("EVR in publisher is " + evr.toString());
        }
        messageContext.publish(evrMessage);
    }


    /**
     * Send invalid packet data message, currently just a pair of log messages.
     * 
     * @param causeMessage
     *            the original error message that resulted in this call
     */
    private void sendInvalidPacketMessage(final ITelemetryPacketMessage pm,
            final String causeMessage) {
        IPublishableLogMessage logm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.ERROR,
                causeMessage, LogMessageType.GENERAL);
        messageContext.publish(logm);
        log.log(logm);
        logm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.ERROR,
                "Discarded invalid EVR packet: apid="
                        + pm.getPacketInfo().getApid() + ", seq="
                        + pm.getPacketInfo().getSeqCount() + ", length="
                        + pm.getPacketInfo().getSize(),
                LogMessageType.INVALID_PKT_DATA);
        log.log(logm);
        messageContext.publish(logm);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.interfaces.IService#stopService()
     */
    @Override
    public void stopService() {
        if (subscriber != null) {
            messageContext.unsubscribeAll(subscriber);
        }
        log.debug("Evr Publisher has shut down");
    }

    /**
     * 
     * EvrMessageSubscriber is the listener for internal packet messages.
     * 
     *
     */
    private class EvrMessageSubscriber extends BaseMessageHandler {

        /**
         * Creates an instance of EvrMessageSubscriber.
         */
        public EvrMessageSubscriber() {
            messageContext.subscribe(TmServiceMessageType.TelemetryPacket, this);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * jpl.gds.message.BaseMessageHandler#handleMessage(jpl.gds.core
         * .message.Message)
         */
        @Override
        public void handleMessage(final IMessage m) {
            consume((ITelemetryPacketMessage) m);
        }
    }

    /**
     * Returns a flag indicating whether the given apid is an Evr apid.
     * 
     * @param apid
     *            the apid to check
     * @return true if the apid is an Evr apid
     */
    private boolean isEvrApid(final int apid) {
        if (evrApids == null) {
            return false;
        }
        return evrApids.contains(Integer.valueOf(apid));
    }

    /**
     * Loads Evr apids from the ApidReference for the current mission.
     */
    private void loadApids() {
        evrApids = null;
        try {
            if (apidDefs != null) {
                evrApids = apidDefs.getEvrApids();
            } else {
                // Failure to load Apid dictionary will have already been logged
                evrApids = new TreeSet<>();
            }
        } catch (final Exception e) {
            e.printStackTrace();
            final IPublishableLogMessage lm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.FATAL,
                    "Unable to load mission adaptation or APID dictionary: "
                            + e.toString());
            messageContext.publish(lm);
        }
    }

}
