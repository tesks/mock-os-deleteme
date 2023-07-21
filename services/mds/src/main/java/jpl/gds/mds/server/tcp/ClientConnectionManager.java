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

import jpl.gds.mds.server.disruptor.IMessageEventConsumer;
import jpl.gds.mds.server.disruptor.MessageEvent;
import jpl.gds.mds.server.spring.SpillProcessorProvider;
import jpl.gds.message.api.spill.ISpillProcessor;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import org.springframework.messaging.Message;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client Connection Manager handles the incoming, queuing and outgoing messages for each TCP socket client connection.
 * Uses a SpillProcessor to handle cases where the connected client cannot keep up with the volume of message flow.
 */
public class ClientConnectionManager extends Thread implements IClientConnectionManager {
    private static final int  QUEUE_CAPACITY     = 10000;
    private static final int  TIMER_INTERVAL     = 2 * 1000;
    private static final int  INCOMING_LIST_SIZE = 1000;
    private static final int  SPILL_QUOTA        = 1000;
    private static final long SPILL_TIMEOUT      = 100L;

    private final Socket                                clientSocket;
    private final AtomicBoolean                         socketOpen = new AtomicBoolean(false);
    private final int                                   clientPort;
    private final IMessageEventConsumer                 messageDistributor;
    private final ISpillProcessor<MessageListContainer> spillProcessor;
    private final Tracer                                logger     = TraceManager.getTracer(Loggers.MDS);
    private final BlockingQueue<MessageListContainer>   clientQueue;
    private       List<Message>                         incomingList;
    private       long                                  totalReceivedCount;
    private       long                                  totalSentCount;

    /**
     * Constructor
     *
     * @param messageDistributor IMessageEventConsumer
     * @param socket             Socket
     */
    public ClientConnectionManager(final IMessageEventConsumer messageDistributor, final Socket socket,
                                   final SpillProcessorProvider spillProcessorProvider) {
        this.messageDistributor = messageDistributor;
        this.clientSocket = socket;
        this.setName("Client:" + clientSocket.getPort());
        this.clientQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        this.spillProcessor = spillProcessorProvider.getSpillProcessor(clientQueue,
                SPILL_QUOTA,
                SPILL_TIMEOUT,
                logger);
        this.clientPort = clientSocket.getPort();
    }

    @Override
    public void run() {
        incomingList = new ArrayList<>();

        spillProcessor.start();
        messageDistributor.addSocketConnection(this);

        logger.info("Received new client connection : " + clientPort);

        final MetricTask metricTask = new MetricTask(this, clientSocket, clientQueue, logger);
        final Timer      timer      = new Timer(true);
        timer.scheduleAtFixedRate(metricTask, 0, TIMER_INTERVAL);

        socketOpen.set(true);
        try {
            while (socketOpen.get()) {
                final MessageListContainer messageListContainer = spillProcessor.poll();
                final List<Message>        messageList          = messageListContainer.getMessageList();
                for (final Message message : messageList) {
                    clientSocket.getOutputStream().write((byte[]) message.getPayload());
                }
                totalSentCount += messageList.size();
            }
        } catch (InterruptedException e) {
            logger.error(clientPort, "Encountered an Error: ", e.getMessage());
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            //Socket has been closed by client
            logger.warn("Client went away, removing connection ", clientPort);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                logger.warn("Encountered an error closing a socket client ", clientPort, ", continuing");
            }
            messageDistributor.removeSocketConnection(this);
            timer.cancel();
        }
    }

    /**
     * Get client socket
     *
     * @return Socket
     */
    public Socket getClientSocket() {
        return clientSocket;
    }

    @Override
    public void handleMessage(MessageEvent message) {
        // This method should return as quickly as possible to minimize the impact
        // of any one client on the rest of the connected clients.
        incomingList.add(message.getMessage());
        totalReceivedCount++;
        if (incomingList.size() >= INCOMING_LIST_SIZE) {
            this.flushBuffer();
        }
    }

    private synchronized void pushBufferToQueue() {
        spillProcessor.put(new MessageListContainer(new ArrayList<>(incomingList)));
        incomingList.clear();
    }

    @Override
    public void flushBuffer() {
        logger.debug(getName() + ": Got Request to flush buffer, size = " + incomingList.size());
        if (!incomingList.isEmpty()) {
            pushBufferToQueue();
        }
    }

    @Override
    public long getTotalReceivedCount() {
        return totalReceivedCount;
    }

    @Override
    public long getTotalSentCount() {
        return totalSentCount;
    }

    /**
     * Metrics Task
     */
    static class MetricTask extends TimerTask {

        private final Socket                              socket;
        private final IClientConnectionManager            clientConnectionManager;
        private final Tracer                              logger;
        private final BlockingQueue<MessageListContainer> clientQueue;

        /**
         * Constructor
         *
         * @param clientConnectionManager
         * @param socket
         * @param clientQueue
         * @param logger
         */
        MetricTask(IClientConnectionManager clientConnectionManager, Socket socket,
                   BlockingQueue<MessageListContainer> clientQueue, Tracer logger) {
            this.socket = socket;
            this.clientQueue = clientQueue;
            this.clientConnectionManager = clientConnectionManager;
            this.logger = logger;
        }

        @Override
        public void run() {
            logger.debug("Client socket : " + socket.getPort()
                    + " QueueSize :" + clientQueue.size()
                    + " received count: " + clientConnectionManager.getTotalReceivedCount()
                    + " sent count: " + clientConnectionManager.getTotalSentCount());
            clientConnectionManager.flushBuffer();
        }
    }
}