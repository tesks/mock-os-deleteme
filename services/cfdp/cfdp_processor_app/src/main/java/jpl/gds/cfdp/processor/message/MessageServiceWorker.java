/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.cfdp.processor.message;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.LifecycleAware;
import jpl.gds.cfdp.processor.CfdpProcessorApp;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.cfdp.processor.message.disruptor.MessageEvent;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.message.api.MessageApiBeans;
import jpl.gds.message.api.handler.IQueuingMessageHandler;
import jpl.gds.message.api.portal.IMessagePortal;
import jpl.gds.message.api.util.MessageFilterMaker;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.spring.bootstrap.SharedSpringBootstrap;
import jpl.gds.tm.service.api.TmServiceMessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Arrays;

import static jpl.gds.context.api.TopicNameToken.APPLICATION_PDU;

@Service
@DependsOn("configurationManager")
public class MessageServiceWorker implements EventHandler<MessageEvent>, LifecycleAware {

    private Tracer log;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    private InboundCfdpPduMessageConsumer inboundCfdpPduMessageConsumer;

    @Autowired
    private IVenueConfiguration venueConfiguration;

    @Autowired
    private IGeneralContextInformation generalContextInfo;

    @Autowired
    private MissionProperties missionConfig;

    @Autowired
    private IContextIdentification contextId;

    private IMessagePortal messagePortal;

    private IMessagePublicationBus messagePublicationBus;

    private IQueuingMessageHandler queuingMessageHandler;

    @PostConstruct
    public void init() {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);
    }

    @Override
    public void onStart() {
        messagePortal = appContext.getBean(MessageApiBeans.MESSAGE_SERVICE_PORTAL, IMessagePortal.class);
        messagePublicationBus = appContext.getBean(SharedSpringBootstrap.PUBLICATION_BUS, IMessagePublicationBus.class);
        queuingMessageHandler = appContext.getBean(IQueuingMessageHandler.class,
                configurationManager.getMessageServiceInboundPduHandlerQueueSizeProperty());

        // TODO
        // final String filter = MessageFilterMaker.createSubscriptionFilter(scid, messageTypes, dssFilter, vcFilter);
        final String filter = MessageFilterMaker
                .createFilterForMessageTypes(Arrays.asList(TmServiceMessageType.CfdpPdu));

        String[] rootTopics = configurationManager.getMessageServiceInboundPduRootTopicsOverride();

        if (rootTopics.length == 0 || (rootTopics.length == 1 && "".equalsIgnoreCase(rootTopics[0]))) {
            // No override topic provided, so use context default
            rootTopics = new String[1];


            // 5/2022 MCSECLIV-992 -> MPCS-12390: Set user provided arguments into config objects
            // NOTE: this should really be done in the commandlinehandler's configure() but is not possible
            //      due to how CFDP CLI parsing was implemented (doesn't follow same pattern as everything else)
            // @see AmpcsStyleCommandLineHandler; unfortunately it is only used in static contexts, so we can't pass in
            //      config objects or the application context without refactoring where and how it's used
            if (CfdpProcessorApp.commandLineHandler.getDownlinkStreamType() != null) {
                venueConfiguration.setDownlinkStreamId(CfdpProcessorApp.commandLineHandler.getDownlinkStreamType());
            }
            if (CfdpProcessorApp.commandLineHandler.getVenueType() != null) {
                venueConfiguration.setVenueType(CfdpProcessorApp.commandLineHandler.getVenueType());
            } else {
                venueConfiguration.setVenueType(missionConfig.getDefaultVenueType());
            }
            if (CfdpProcessorApp.commandLineHandler.getTestbedName() != null) {
                venueConfiguration.setTestbedName(CfdpProcessorApp.commandLineHandler.getTestbedName());
            } else {
                venueConfiguration.setTestbedName(missionConfig.getDefaultTestbedName(venueConfiguration.getVenueType(), contextId.getHost()));
            }
            if (CfdpProcessorApp.commandLineHandler.getSubtopic() != null) {
                generalContextInfo.setSubtopic(CfdpProcessorApp.commandLineHandler.getSubtopic());
            }

            rootTopics[0] = appContext.getBean(IGeneralContextInformation.class).getRootPublicationTopic();
        }

        try {

            for (final String topic : rootTopics) {
                String qualifiedTopic = topic + "." + APPLICATION_PDU.getTopicNameComponent();
                log.info("Subscribing to topic " + qualifiedTopic + " with filter '" + filter + "'");
                queuingMessageHandler.setSubscription(qualifiedTopic, filter, false);
                queuingMessageHandler.addListener(inboundCfdpPduMessageConsumer);
                queuingMessageHandler.start();
            }

        } catch (final Exception e) {
            log.error("Unexpected error in initialization: " + ExceptionTools.getMessage(e), e);
        }

        messagePortal.enableImmediateFlush(true);
        // MPCS-9827 - For R8.1, only enable the the CFDP topic
        // MPCS-10925 - enable CFDP products on the product topic (see comments in MPCS-9827 too)
        // MPCS-10879 - Disabling enableSpecificTopics call below, due to discovering bug MPCS-11184
        //messagePortal.enableSpecificTopics(new TopicNameToken[]{APPLICATION_PRODUCT, APPLICATION_CFDP});
        appContext.getBean(IGeneralContextInformation.class).setRootPublicationTopic(configurationManager.getMessageServicePublishingRootTopicOverride());
        messagePortal.startService();
    }

    @Override
    public void onShutdown() {
        queuingMessageHandler.shutdown(false, true);
        messagePortal.stopService();

        // TODO flush anything that remains to be published
    }

    @Override
    public void onEvent(final MessageEvent event, final long sequence, final boolean endOfBatch) throws Exception {
        final IMessage msg = event.getMessage();

        // MPCS-10094 1/6/2019 Check if session is set
        if (msg.getContextKey().getNumber() == null) {
            // Session is not set, so use application's context when publishing (a.k.a. "parent" context)
            messagePublicationBus.publish(msg);
        } else {
            // Session is set, so don't overwrite the message's context with application's parent context
            messagePublicationBus.publish(msg, true);
        }

    }

}