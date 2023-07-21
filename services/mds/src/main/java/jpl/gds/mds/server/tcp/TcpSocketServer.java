/*
 * Copyright 2006-2020. California Institute of Technology.
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

package jpl.gds.mds.server.tcp;

import jpl.gds.mds.server.MonitorDataService;
import jpl.gds.mds.server.disruptor.IMessageEventConsumer;
import jpl.gds.mds.server.disruptor.RingBufferController;
import jpl.gds.mds.server.spring.ClientConnectionManagerProvider;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class functions as the TCP server socket component. Listens for incoming TCP socket connections and creates a
 * new ClientConnectionManager for each new client connection request
 */
public class TcpSocketServer {
    private final Tracer                          logger          = TraceManager.getTracer(Loggers.MDS);
    private final IMessageEventConsumer           messageDistributor;
    private final RingBufferController            ringBufferController;
    private final ClientConnectionManagerProvider clientConnectionManagerProvider;
    private final ServerSocketFactory             socketFactory;
    private       ExecutorService                 executorService = Executors.newCachedThreadPool();
    private       ServerSocket                    serverSocket;

    /**
     * Constructor
     *
     * @param messageDistributor
     * @param ringBufferController
     * @param clientConnectionManagerProvider
     * @param socketFactory
     */
    public TcpSocketServer(final IMessageEventConsumer messageDistributor,
                           final RingBufferController ringBufferController,
                           final ClientConnectionManagerProvider clientConnectionManagerProvider,
                           final ServerSocketFactory socketFactory) {
        this.messageDistributor = messageDistributor;
        this.ringBufferController = ringBufferController;
        this.clientConnectionManagerProvider = clientConnectionManagerProvider;
        this.socketFactory = socketFactory;
    }

    /**
     * Start TCP Socket server
     *
     * @param port Socket port
     */
    public void start(int port) {
        ringBufferController.init();
        final boolean secure     = socketFactory instanceof SSLServerSocketFactory;
        final String  serverType = secure ? "(Secure TLS)" : "(Unsecure)";
        try {
            logger.info("TCP Server ", serverType, " listening on port ", port);
            serverSocket = socketFactory.createServerSocket(port);
            while (!MonitorDataService.exiting.get()) {
                //blocks until a client connects
                final Socket socket = serverSocket.accept();
                final Runnable clientConnectionManager = (Runnable)
                        clientConnectionManagerProvider.getClientConnectionManager(messageDistributor, socket);
                executorService.submit(clientConnectionManager);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            stop();
        }

    }

    /**
     * Stop TCP Socket Server
     */
    public void stop() {
        ringBufferController.cleanup();
        executorService.shutdownNow();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
