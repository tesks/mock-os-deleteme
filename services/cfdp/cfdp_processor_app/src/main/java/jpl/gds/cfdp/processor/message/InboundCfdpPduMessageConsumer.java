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

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import jpl.gds.ccsds.api.cfdp.CfdpPduDirection;
import jpl.gds.ccsds.api.cfdp.ICfdpPdu;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.cfdp.processor.in.disruptor.InboundPduEvent;
import jpl.gds.cfdp.processor.in.disruptor.InboundPduRingBufferManager;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.message.api.external.IExternalMessageUtility;
import jpl.gds.message.api.external.IMessageServiceListener;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.metadata.ISerializableMetadata;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.cfdp.ICfdpPduMessage;

@Service
@DependsOn("configurationManager")
public class InboundCfdpPduMessageConsumer implements IMessageServiceListener {

    private Tracer log;

    @Autowired
    private ApplicationContext appContext;

    long receiveCount;

    @Autowired
    private IExternalMessageUtility externalMessageUtility;

    @Autowired
    private InboundPduRingBufferManager inboundPduRingBufferManager;

    @Autowired
    private ConfigurationManager configurationManager;

    long localCfdpEntityId;

    @PostConstruct
    private void init() {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);
        // MPCS-9792: Prep to filter only for PDUs destined for this local entity
        localCfdpEntityId = Long.parseUnsignedLong(configurationManager.getLocalCfdpEntityId());
    }

    @Override
    public void onMessage(final IExternalMessage message) {

        try {

            // Increment the total message count
            receiveCount++;

            // Get the message type and process the message if it is a type we are
            // interested in
            final IMessageType messageType = externalMessageUtility.getInternalType(message);

            if (TmServiceMessageType.CfdpPdu.getSubscriptionTag() != messageType.getSubscriptionTag()) {
                return;
            }

            final IMessage[] internalMessages = externalMessageUtility.instantiateMessages(message);

            /* MPCS-10000: If PDUs were corrupt, message instantiator would have failed (logging exception, stack trace,
            etc.). But the method would have returned, with null. Check for null. */

            if (internalMessages == null) {
                log.warn("Message instantiation failed - PDU(s) in the message may have been corrupt");
                return;
            }

            for (final IMessage internalMessage : internalMessages) {

                final ICfdpPduMessage cfdpPduMessage = (ICfdpPduMessage) internalMessage;

                // MPCS-10002: Filter only for PDUs from either chill_down or LinkSim, as configured
                if (cfdpPduMessage.fromSimulator() == configurationManager.inboundPduShouldBeFromLinkSim()) {
                    final ICfdpPdu cfdpPdu = cfdpPduMessage.getPdu();

                    // MPCS-9792: Filter only for PDUs destined for this local entity
                    if ((cfdpPdu.getHeader().getDirection() == CfdpPduDirection.TOWARD_RECEIVER && cfdpPdu.getHeader().getDestinationEntityId() == localCfdpEntityId)
                            || (cfdpPdu.getHeader().getDirection() == CfdpPduDirection.TOWARD_SENDER && cfdpPdu.getHeader().getSourceEntityId() == localCfdpEntityId)) {

                        // CFDP R3 - Obtain AMPCS context information
                        final ISerializableMetadata metadataHeader = cfdpPduMessage.getMetadataHeader();

                        inboundPduRingBufferManager.getRingBuffer().publishEvent(InboundPduEvent::translate,
                                cfdpPdu.getData(), cfdpPdu.getData().length, cfdpPduMessage.getOutputDir(),
                                cfdpPduMessage.getSclk() != null ? cfdpPduMessage.getSclk().toString() : null,
                                cfdpPduMessage.getScet() != null ? cfdpPduMessage.getScet().getFormattedScet(true) : null,
                                cfdpPduMessage.getLst() != null ? cfdpPduMessage.getLst().getFormattedSol(true) : null,
                                cfdpPduMessage.getErt() != null ? cfdpPduMessage.getErt().getFormattedErt(true) : null,
                                cfdpPduMessage.getSequenceCount(),
                                cfdpPduMessage.getSessionId(),
                                cfdpPduMessage.getSessionName(),
                                cfdpPduMessage.getFswDictionaryDir(),
                                cfdpPduMessage.getFswVersion(),
                                cfdpPduMessage.getVenueType(),
                                cfdpPduMessage.getTestbedName(),
                                cfdpPduMessage.getUser(),
                                cfdpPduMessage.getHost(),
                                cfdpPduMessage.getScid(),
                                cfdpPduMessage.getApid(),
                                cfdpPduMessage.getApidName(),
                                cfdpPduMessage.getVcid(),
                                0, // Sequence ID not provided by CFDP PDU message
                                0, // Sequence Version not provided by CFDP PDU message
                                0, // Command Number not provided by CFDP PDU message
                                cfdpPduMessage.getRelayScid() // MPCS-9817: Add relay SCID
                        );
                    } else {
                        log.trace("Discard PDU destined for entity " + (cfdpPdu.getHeader().getDirection()
                                == CfdpPduDirection.TOWARD_RECEIVER
                                ? Long.toUnsignedString(cfdpPdu.getHeader().getDestinationEntityId())
                                : Long.toUnsignedString(cfdpPdu.getHeader().getSourceEntityId())));
                    }

                } else {
                    log.trace("Discard PDU from " + (cfdpPduMessage.fromSimulator() ? "LinkSim" : "chill_down"));
                }

            }

            // Write the data to the capture file if so configured

        } catch (final MessageServiceException e) {
            log.error("Error receiving message: " + ExceptionTools.getMessage(e));
        }

    }

}