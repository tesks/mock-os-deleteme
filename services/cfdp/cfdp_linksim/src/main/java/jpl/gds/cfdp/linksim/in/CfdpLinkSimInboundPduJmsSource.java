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

package jpl.gds.cfdp.linksim.in;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import jpl.gds.cfdp.linksim.CfdpLinkSimApp;
import jpl.gds.context.api.IGeneralContextInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import jpl.gds.cfdp.linksim.CfdpLinkSimPduQueueManager;
import jpl.gds.cfdp.linksim.datastructures.ReceivedPduContainer;
import jpl.gds.context.api.ContextTopicNameFactory;
import jpl.gds.context.api.TopicNameToken;
import jpl.gds.message.api.MessageApiBeans;
import jpl.gds.message.api.external.IExternalMessageUtility;
import jpl.gds.message.api.external.IMessageClientFactory;
import jpl.gds.message.api.external.ITopicSubscriber;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.message.api.portal.IMessagePortal;
import jpl.gds.message.api.util.MessageFilterMaker;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.cfdp.ICfdpPduMessage;

@Service
@DependsOn("tmServiceSpringBootstrap")
@ConditionalOnProperty(name = "input.from.jms", havingValue = "true")
public class CfdpLinkSimInboundPduJmsSource {

    private Tracer log;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    CfdpLinkSimPduQueueManager queueManager;

    @Autowired
    private IExternalMessageUtility externalMessageUtility;

    @Autowired
    private Environment env;

    private final List<ITopicSubscriber> topicSubscribers = new LinkedList<>();

    /* Message portal is not needed for the operation of this class, but for shutdown (pre-destroy), message portal
     * needs to be stopped before topic subscribers can be closed. Because of non-deterministic bean destruction
     * ordering, grab a message portal bean and stop it on shutdown.
     */
    @Autowired
    private IMessagePortal messagePortal;

    @PostConstruct
    public void init() {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);
        log.info("PDU input from JMS");

        // MPCS-10524- If user provided a JMS root topic for subscribing, use it
        String inRootTopic = env.getProperty(CfdpLinkSimApp.MESSAGE_SERVICE_INBOUND_PDU_ROOT_TOPIC);

        if (inRootTopic == null || "null".equalsIgnoreCase(inRootTopic)) {
            inRootTopic = ContextTopicNameFactory.getMissionSessionTopic(appContext);
        }

        final IMessageClientFactory clientFactory = appContext.getBean(IMessageClientFactory.class);
        final String topicFilter = MessageFilterMaker.createFilterForMessageTypes(Arrays.asList(TmServiceMessageType.CfdpPdu));

        try {
            this.topicSubscribers.add(
                    clientFactory.getTopicSubscriber(
                            TopicNameToken.APPLICATION_PDU.getApplicationDataTopic(inRootTopic),
                            topicFilter,
                            true));

            for (final ITopicSubscriber sub : this.topicSubscribers) {
                log.info("Setting message listener for topic ", sub.getTopic());
                sub.setMessageListener(message -> {

                    try {

                        final IMessageType messageType = externalMessageUtility.getInternalType(message);

                        if (TmServiceMessageType.CfdpPdu.getSubscriptionTag() != messageType.getSubscriptionTag()) {
                            return;
                        }

                        final IMessage[] internalMessages = externalMessageUtility.instantiateMessages(message);

                        for (final IMessage internalMessage : internalMessages) {
                            final ICfdpPduMessage cfdpPduMessage = (ICfdpPduMessage) internalMessage;

                            if (!cfdpPduMessage.fromSimulator()) {

                                try {

                                    if (queueManager.getQueue().offer(new ReceivedPduContainer(cfdpPduMessage.getPdu().getData(),
                                            cfdpPduMessage), 5, TimeUnit.SECONDS)) {
                                        log.trace("New PDU queued successfully");
                                    } else {
                                        log.error("PDU offer to queue failed");
                                    }

                                } catch (final InterruptedException e) {
                                    log.error("PDU offer to queue threw exception", ExceptionTools.getMessage(e), e);
                                }

                            }

                        }

                    } catch (final MessageServiceException e) {
                        log.error("Failed to instantiate incoming PDU messages", ExceptionTools.getMessage(e), e);
                    }

                });

                sub.start();
            }

        } catch (final MessageServiceException e) {
            log.error("JMS subscription failed", ExceptionTools.getMessage(e), e);
        }

        messagePortal = appContext.getBean(MessageApiBeans.MESSAGE_SERVICE_PORTAL, IMessagePortal.class);
    }

    @PreDestroy
    public void shutdown() {

        try {
            // First, stop message portal
            messagePortal.stopService();

            // Now we can proceed to close the subscribers
            for (final ITopicSubscriber sub : topicSubscribers) {
                final String topic = sub.getTopic();
                sub.close();
                log.info("Successfully closed subscription to ", topic);
            }

        } catch (final Exception e) {
            log.error("Problem closing topic subscribers:", ExceptionTools.getMessage(e), e);
        }

    }

}