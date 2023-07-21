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
package jpl.gds.tc.impl.echo;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

import org.springframework.context.ApplicationContext;

import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.shared.util.HostPortUtility;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.echo.ICommandEchoMessage;

/**
 * The ServerSocketCommandEchoInput retrieves command echo data through a socket
 * and is the server end of the server-client relationship. It establishes a
 * socket server and waits for a client to establish a connection. Only one
 * client is supported at any time and is only disconnected when the connection
 * is lost, the client disconnects, or an end of file flag is received
 * 
 *
 */
public class ServerSocketCommandEchoInput extends AbstractCommandEchoInput {
    
    private Thread listenerThread;
    private ConnectionListenerWorker listenerWorker;
    private volatile Socket socket;
    private volatile DataInputStream dis;
    private int port;
    
    int retries = appContext.getBean(CommandProperties.class).getEchoSocketRetryCount();
    int interval = appContext.getBean(CommandProperties.class).getEchoSocketRetryInterval();
    
    /**
     * @param appContext
     * @param host
     * @param port
     */
    public ServerSocketCommandEchoInput(ApplicationContext appContext, int port){
        super(appContext);
        this.port = port;
    }
    
    /**
     * Starts the connection listener thread. It waits for a client to request a
     * connection to the server through the specified port and when one is
     * available, the Socket and DataInputStream are attached. Any additional
     * connections are rejected while this connection is still established
     */
    private void startConnectionListener() {
        trace.debug("Starting connection listener");

        trace.trace("stopping previous connection listener (if it iexists)");
        stopConnectionListener();

        trace.debug("Creating new listener worker (port=" + port + ")");
        listenerWorker = new ConnectionListenerWorker(this);
        /* MPCS-7135 - 3/17/15. Name the thread. */
        listenerThread = new Thread(listenerWorker, "Connection Listener Worker");
        listenerThread.setDaemon(true);

        trace.debug("Starting listener thread");
        listenerThread.start();
    }
    
    private void stopConnectionListener() {
        trace.debug("Stopping connection listener");

        if (listenerThread != null) {
            trace.trace("Interrupting listenerThread");
            listenerThread.interrupt();
            trace.debug("Stopping listenerWorker's ServerSocket");
            listenerWorker.stopServer();

            while (listenerThread.isAlive()) {

                if (interval > 0) {
                    /*
                     * Here, we're reusing the SocketRetryInterval value even
                     * though that config property is not specifically meant for
                     * this purpose. I see no harm of doing it, and it avoids
                     * adding yet another config property to the bloated GDS
                     * config.
                     */
                    trace.trace("ListenerThread is still alive; sleep "
                            + interval + "ms");
                    SleepUtilities.fullSleep(interval, trace,
                            "ServerSocketInput.stopConnectionListener sleep error on retry");
                }

            }
            
            trace.trace("ListenerThread now being set to null");
            listenerThread = null;
            
        } else {
            trace.debug("ListenerThread is already null");
            
        }
        
    }
    
    private void waitForInputStream() {
        trace.debug("Waiting for input stream...");
        
        while (dis == null && !stopping.get()) {

            if (interval > 0) {
                trace.trace("Input stream still null and not stopping yet; sleep "
                        + interval
                        + "ms");
                SleepUtilities
                        .fullSleep(interval, trace,
                                "ServerSocketInput.waitForInputStream sleep error on retry");
            }

        }

        if (dis != null) {
            trace.debug("Input stream obtained (" + dis + ")");
        } else if (stopping.get()) {
            trace.debug("Terminating wait for input stream");
        }

    }
    
    @Override
    public boolean connect() {

        if (!HostPortUtility.isPortValid(port)) {
            throw new IllegalArgumentException("Invalid port number " + port
                    + "given");
        }

        /*
         * Start the connection listening thread.
         */
        startConnectionListener();

        /*
         * Wait around for a DataInputStream to be obtained. 
         */
        waitForInputStream();

        /*
         * Below should return true.
         */
        return dis != null;
    }

    @Override
    public boolean isConnected() {
        /*
         * The client may not show as immediately disconnected, so there could
         * be some time in which it appears "connected", but isn't. Not too big
         * of a deal for command echo
        */
        if (getSocket() == null) {
            trace.trace("isConnected() is false, no socket available");
            return false;
        }

        trace.trace("isConnected() is " + getSocket().isConnected());
        return getSocket().isConnected();
    }

    @Override
    public boolean isStopping() {
        return stopping.get();
    }

    @Override
    protected void disconnect(){
        super.disconnect();
        try{
            trace.trace("Disconnecting ServerSocketCommandEchoInput");

            if (dis != null) {
                trace.trace("Closing input stream and setting it to null");
                dis.close();
                this.dis = null;
            } else {
                trace.trace("Input stream is already null");
            }

            if (socket != null) {
                trace.info("Disconnecting from data source "
                        + ":localPort="
                        + socket.getLocalPort()
                        + ":clientIP="
                        + socket.getInetAddress().toString()
                        + ":clientPort="
                        + socket.getPort());
                trace.trace("Closing socket and setting it to null");
                socket.close();
                setSocketAndDis(null);
            } else {
                trace.trace("Socket is already null");
            }
        } catch (IOException e) {
            trace.debug("Encountered an issue while attepting to stop current connection", e);
            //no need to do anything else, connection should be stopped.
        }
    }
    
    @Override
    public void ingestData() {
        byte[] data;
        int len;
        
        try{
            while(isConnected() && !stopping.get()){
                data = new byte[64];

                len = getDis().read(data);

                trace.trace("Read bytes: len = " + len);

                if(len > 0){
                    ICommandEchoMessage message = msgFactory.createCommandEchoMessage(data, 0, len);
                    msgBus.publish(message);
                } else if (len == -1){
                    break;
                }
            }
        } catch (IOException e1) {
            trace.warn("Exception encountered while retrieving data from the socket.", e1);
        }

        finally{
            if(stopping.get()){
                stopSource();
            }
            else {
                disconnect();
            }
        }

    }
    
    /**
     * Get the current Socket object for the current connection
     * @return the current Socket
     */
    protected Socket getSocket(){
        return this.socket;
    }
    
    /**
     * Set the current Socket and DataInputStream for the current connection
     * @param socket the new Socket
     * @throws IOException if an I/O error occurs while updating the associated DataInputStream 
     */
    protected void setSocketAndDis(Socket socket) throws IOException{
        this.socket = socket;
        
        if(this.socket != null){
            trace.trace("opening a new DataInputStream from the new connection");
            this.dis = new DataInputStream(this.socket.getInputStream());
        }
    }
    
    /**
     * Get the current DataInputStream.
     * @return the current DataInputStream
     */
    protected DataInputStream getDis(){
        return this.dis;
    }
    
    /**
     * Class that maintains the connection listening ServerSocket. Runs in a
     * thread.
     * 
     * Sep 28, 2017 - based largely upon
     *          ServerSocketInputConnection.ConnectionListenerWorker
     * @since AMPCS R8
     */
    private class ConnectionListenerWorker implements Runnable
    {

        private ServerSocket server;

        private final ServerSocketCommandEchoInput sscei;

        
        /**
         * Default constructor.
         * 
         * @param port
         *            Port number of the listening socket.
         * @param sscei
         *            The ServerSocketCommandEchoInput object using this worker.
         */
        ConnectionListenerWorker(ServerSocketCommandEchoInput sscei) { 
            this.sscei = sscei;
        }

        
        private void stopServer() {
            trace.debug("Stopping connection listening server");

            if (server != null) {
                
                try {
                    trace.debug("listening server is not null; closing it");
                    server.close();
                    server = null;
                } catch (IOException e) {
                    trace.error("Closing ServerSocket produced exception: " + e.getMessage());
                }
                
            } else {
                trace.trace("listening server is already null");
            }

        }

        
        /**
         * {@inheritDoc}
         *
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            trace.debug("CLW: run()");

            try {
                server = new ServerSocket(port);
                trace.info("Server socket input listening server active. Listening for connections on port " + port);

            } catch (IOException e) {
                trace.error("Creating ServerSocket on port " + port
                        + " produced exception: " + e.getMessage());

                                return;
            }

            trace.trace("Entering connection accepting while loop");

            while(!Thread.interrupted()) {
                Socket newSocket = null;
                
                try {
                    newSocket = server.accept();
                } catch (IOException e) {
                    trace.debug("IOException on server.accept(): " + e.getMessage());
                    return;
                }

                trace.debug("Accepted a new inbound connection");
                
                try {
                    
                    if (sscei.getSocket() != null) {
                        trace.warn("Rejected additional connection to echo source "
                                + ":localPort="
                                + sscei.getSocket().getLocalPort()
                                + ":clientIP="
                                + newSocket.getInetAddress().toString()
                                + ":clientPort="
                                + newSocket.getPort()
                                + " at "
                                + TimeUtility.getFormatter().format(new Date()));
                        newSocket.close();
                    } else {
                        trace.info("Accepting new connection "
                                + ":clientIP="
                                + newSocket.getInetAddress().toString()
                                + ":clientPort="
                                + newSocket.getPort()
                                + " at "
                                + TimeUtility.getFormatter().format(new Date()));
                        sscei.setSocketAndDis(newSocket);
                    }

                } catch (IOException e) {
                    trace.warn("Exception with new socket: " + e.getMessage());
                }

            }
            
            trace.debug("Exited connection accepting while loop");
            trace.info("Closing Socket listening server");
            
            try {
                server.close();
            } catch (IOException e) {
                trace.warn("Exception trying to close connection listening server: " + e.getMessage());
            }
        }
    }

}
