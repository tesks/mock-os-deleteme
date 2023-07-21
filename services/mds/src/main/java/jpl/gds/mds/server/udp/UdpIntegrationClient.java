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

package jpl.gds.mds.server.udp;

import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.ip.udp.UnicastSendingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * Implementation for UDP CLient
 */
@Service
public class UdpIntegrationClient implements IUdpClient {
    private final Tracer tracer;
    private final UnicastSendingMessageHandler udpSendingAdapter;

    /**
     * Constructor
     * @param udpSendingAdapter UnicastSendingMessageHandler
     */
    @Autowired
    public UdpIntegrationClient(final UnicastSendingMessageHandler udpSendingAdapter) {
        this.udpSendingAdapter = udpSendingAdapter;
        tracer = TraceManager.getTracer(Loggers.MDS);
    }

    @Override
    public void sendMessage(final byte[] message) {
        if(!udpSendingAdapter.getHost().isEmpty()) {
            udpSendingAdapter.handleMessage(MessageBuilder.withPayload(message).build());
            tracer.info("Forwarding UDP bytes, MsgSize=", message.length);
        }
    }
}