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
package jpl.gds.cfdp.linksim.out;

import jpl.gds.ccsds.api.cfdp.CfdpPduDirection;
import jpl.gds.ccsds.api.cfdp.ICfdpPdu;
import jpl.gds.cfdp.linksim.CfdpLinkSimApp;
import jpl.gds.cfdp.linksim.datastructures.ReceivedPduContainer;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.context.api.TopicNameToken;
import jpl.gds.message.api.MessageApiBeans;
import jpl.gds.message.api.portal.IMessagePortal;
import jpl.gds.shared.holders.FrameIdHolder;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.spring.bootstrap.SharedSpringBootstrap;
import jpl.gds.tm.service.api.TmServiceApiBeans;
import jpl.gds.tm.service.api.cfdp.ICfdpMessageFactory;
import jpl.gds.tm.service.api.cfdp.ICfdpPduMessage;
import jpl.gds.tm.service.api.packet.IPacketMessageFactory;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfoFactory;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static jpl.gds.context.api.TopicNameToken.APPLICATION_PDU;

/**
 * {@code CfdpLinkSimOutboundPduJmsSink} is a publisher for outgoing PDUs from the link simulator.
 *
 */
@Service
// Below needed for message publication bus to publish correctly
@DependsOn("tmServiceSpringBootstrap")
public class CfdpLinkSimOutboundPduJmsSink implements ICfdpLinkSimOutboundPduSink {

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private Environment env;

    private Tracer log;

    private IMessagePortal messagePortal;
    private IMessagePublicationBus messagePublicationBus;
    private ICfdpMessageFactory cfdpPduMessageFactory;

    @PostConstruct
    private void init() {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);

        messagePortal = appContext.getBean(MessageApiBeans.MESSAGE_SERVICE_PORTAL, IMessagePortal.class);
        messagePublicationBus = appContext.getBean(SharedSpringBootstrap.PUBLICATION_BUS, IMessagePublicationBus.class);
        messagePortal.enableImmediateFlush(true);

        // MPCS-10301 - Add selective topic
        messagePortal.enableSpecificTopics(new TopicNameToken[]{APPLICATION_PDU});

        // MPCS-10524 - If user provided a JMS root topic for publishing, use it
        String outRootTopic = env.getProperty(CfdpLinkSimApp.MESSAGE_SERVICE_OUTBOUND_PDU_ROOT_TOPIC);

        if (outRootTopic != null && !"null".equalsIgnoreCase(outRootTopic)) {
            appContext.getBean(IGeneralContextInformation.class).setRootPublicationTopic(outRootTopic);
        }

        messagePortal.startService();

        cfdpPduMessageFactory = appContext.getBean(TmServiceApiBeans.CFDP_MESSAGE_FACTORY, ICfdpMessageFactory.class);
    }

    @Override
    public void send(final ICfdpPdu outPdu, final ReceivedPduContainer originalPduContainer) {
        log.info("Publishing PDU toward entity ", outPdu.getHeader().getDirection()
                        == CfdpPduDirection.TOWARD_RECEIVER
                        ? Long.toUnsignedString(outPdu.getHeader().getDestinationEntityId())
                        : Long.toUnsignedString(outPdu.getHeader().getSourceEntityId()),
                ": ",
                outPdu);

        ICfdpPduMessage pduMessage = null;

        if (originalPduContainer.getOriginalPduMessage() != null) {
            pduMessage = originalPduContainer.getOriginalPduMessage();
            pduMessage.setPdu(outPdu);
            pduMessage.setFromSimulator(true);
        } else {
            final ITelemetryPacketMessage pm = appContext.getBean(IPacketMessageFactory.class).createTelemetryPacketMessage(
                    appContext.getBean(ITelemetryPacketInfoFactory.class).create(),
                    PacketIdHolder.getNextFswPacketId(), null, null, FrameIdHolder.getNextFrameId());
            pduMessage = cfdpPduMessageFactory.createSimulatorPduMessage(pm,
                    outPdu, appContext.getBean(IContextConfiguration.class));
        }

        messagePublicationBus.publish(pduMessage);
    }

    @PreDestroy
    private void shutdown() {
        /*
         * May have been stopped by CfdpLinkSimInboundPduJmsSource, all depends on what order Spring is destroying the
         * beans.
         */
        messagePortal.stopService();
    }

}