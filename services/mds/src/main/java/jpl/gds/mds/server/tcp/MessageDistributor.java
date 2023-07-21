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

package jpl.gds.mds.server.tcp;

import com.lmax.disruptor.EventHandler;
import jpl.gds.mds.server.disruptor.IMessageEventConsumer;
import jpl.gds.mds.server.disruptor.MessageEvent;
import jpl.gds.mds.server.tcp.IClientConnectionManager;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

import java.util.ArrayList;
import java.util.List;

/**
 * This class takes the messages off of the Ring Buffer and distributes each
 * message to all of the registered Client Connection Managers
 */
public class MessageDistributor implements IMessageEventConsumer {

    private final List<IClientConnectionManager> connectedClients = new ArrayList<>();
    private long                                 count            = 0;

    private static final int MESSAGE_COUNT = 10000;

    private final Tracer logger = TraceManager.getTracer(Loggers.MDS);

    @Override
    public void addSocketConnection(final IClientConnectionManager clientConnectionManager) {
        connectedClients.add(clientConnectionManager);
    }

    @Override
    public void removeSocketConnection(final IClientConnectionManager clientConnectionManager) {
        connectedClients.remove(clientConnectionManager);
    }

    /**
     * Distribute message
     * @param messageEvent MessageEvent
     */
    public synchronized void distributeMessage(MessageEvent messageEvent) {
        count++;
        if (count % MESSAGE_COUNT == 0) {
            logger.info("MessageDistributor: Count=" + count + " MessageEventSequence=" + messageEvent.getSequence());
        }
        for (final IClientConnectionManager clientConnectionManager : connectedClients) {
            clientConnectionManager.handleMessage(messageEvent);
        }
    }

    @Override
    public EventHandler<MessageEvent> getEventHandler() {
        return (event, sequence, endOfBatch) -> distributeMessage(event);
    }

    @Override
    public long getTotalCount() {
        return count;
    }
}
