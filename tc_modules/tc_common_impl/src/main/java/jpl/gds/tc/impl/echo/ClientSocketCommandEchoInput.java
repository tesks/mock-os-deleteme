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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.springframework.context.ApplicationContext;

import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.echo.ICommandEchoMessage;

/**
 * The ClientSocketCommandEchoInput retrieves command echo data through a socket
 * and is on the client end of the server-client relationship. Will stay connected to the
 * socket client until either the connection is terminated or an end of file signal has been
 * received.
 * 
 *
 */
public class ClientSocketCommandEchoInput extends AbstractCommandEchoInput {

    private String host;
    private int port;
    
    private Socket socket;
    
    private boolean reported = false;
    
    /**
     * Constructor. Configures ClientSocketCommandEchoInput instance variables.
     * @param appContext the current ApplicationContext
     * @param host the hostname of the server socket to be contected
     * @param port the port number the server socket is utilizing
     */
    public ClientSocketCommandEchoInput(ApplicationContext appContext, String host, int port) {
        super(appContext);
        this.host = host;
        this.port = port;
    }
    
    
    @Override
    public boolean connect() {
        
        int retries = appContext.getBean(CommandProperties.class).getEchoSocketRetryCount();
        int interval = appContext.getBean(CommandProperties.class).getEchoSocketRetryInterval();
        
        final SocketAddress sa = new InetSocketAddress(this.host, this.port);
        
        while(!isConnected() && !stopping.get()){
            //if retry < 0, retry FOREVER
            if(retries > 0){
                retries--;
            }
            
            try {
                socket = new Socket();
                socket.connect(sa);
                
                trace.info("Connected to socket host");
                
            } catch (IOException e) {
                if(retries == 0){
                    stopping.set(true);
                    
                    trace.warn("IOException: " + e.getLocalizedMessage() +
                               " while attempting to connect to " + this.host +
                               ":" + this.port);
                } else if (retries < 0 && !reported){
                    reported = true;
                    trace.warn("IOException: " + e.getLocalizedMessage() +
                            " while attempting to connect to " + this.host +
                            ":" + this.port);
                    trace.warn("Attempting to reconect every " + (interval/1000.0) + " seconds.");
                    trace.warn("Additional connection errors will not be reported.");
                }
                
                try{
                    socket.close();
                } catch (Exception e1){
                    trace.debug("Exception encountered while closing client socket after IOException.", e1);
                }
                
                if(interval > 0){
                    SleepUtilities.checkedSleep(interval);
                }
            }
            
        }
        
        return isConnected();
    }

    @Override
    public boolean isConnected() {
        return (this.socket != null && this.socket.isConnected());
    }
    
    @Override
    protected void disconnect(){
        super.disconnect();
        trace.trace("Closing ClientSocketCommandEchoInput");
        if(socket != null){
            try {
                trace.trace("Closing socket and setting it to null");
                socket.close();
            } catch (IOException e) {
                trace.debug("Encountered an issue while attempting to close the socket", e);
                //doesn't matter
            }
            socket = null;
        }
        else {
            trace.trace("Socket is already null");
        }
    }
    
    @Override
    public void ingestData() {
        byte[] data;
        int len;
        
        InputStream is;
        try {
            is = socket.getInputStream();

            while(isConnected() && !stopping.get()){
                data = new byte[64];
                
                len = is.read(data);

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
            stopSource();
        }
    }

}
