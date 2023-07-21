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
package jpl.gds.telem.input.impl.connection;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;

import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.util.HostPortUtility;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.stream.IRawInputStream;
import jpl.gds.telem.input.impl.stream.BufferedRawInputStream;
import jpl.gds.telem.input.impl.stream.RawInputStream;


/**
 * This class is responsible for establishing a client socket connection for
 * telemetry acquisition.
 *
 */
public class ClientSocketInputConnection
    extends AbstractRawInputConnection
{
	/* 03/10/2014 - MPCS-5963: Removed INTER_RETRY_DELAY_MS */
	/* MPCS-5135 08/02/13 Changed parent class */

    private DataInputStream dis = null;
    private Socket socket = null;
    
    /**
     * Constructor.
     * 
     * @param context the current ApplicationContext
     */
    public ClientSocketInputConnection(final ApplicationContext context)  {
        super(context);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.telem.input.api.connection.IRawInputConnection#openConnection()
     */
    @Override
    public boolean openConnection()
        throws RawInputException
    {
        this.stopping = false;

        if (connectHost == null) {
            throw new IllegalArgumentException("Null input host");
        } else if (!HostPortUtility.isPortValid(connectPort)) {
            throw new IllegalArgumentException("Invalid port number " + connectPort
                    + "given");
        }

        this.dis = getDataInputStream(connectHost, connectPort);

        boolean ok = (dis != null);

        // MPCS-5135 08/02/13 Redo of if


        return ok;

        /** END MPCS-5013 */
    }


    /**
     * {@inheritDoc}
     *
     * @see
     * jpl.gds.telem.input.api.connection.IRawInputConnection#closeConnection()
     */
    @Override
    public void closeConnection() throws IOException
    {
        /** MPCS-5013 07/17/13 Whole thing */

        super.closeConnection();

        if (dis != null)
        {
            try
            {
                dis.close();
            }
            catch (final IOException ioe)
            {
                // Don't let this minor error stop procesing
            }
        }

    }


    /**
     * {@inheritDoc}
     *
     * @see
     * jpl.gds.telem.input.api.connection.IRawInputConnection#getConnectionString()
     */
    @Override
    public String getConnectionString()
    {
        /** MPCS-5013 07/17/13 Whole thing */

        final StringBuilder sb = new StringBuilder();

        sb.append(this.rawInputType);
        sb.append(" with server at ");
        sb.append(connectHost).append(':').append(connectPort);

        if ((socket != null) && socket.isConnected())
        {
            sb.append('(').append(socket.getInetAddress().getHostAddress()).append(')');
            sb.append(" on local ").append(socket.getLocalAddress().getHostAddress());
            sb.append(':').append(socket.getLocalPort());
        }
        else
        {
            sb.append("(no connection)");
        }

        /*
         * 11/25/13 - MPCS-5552. Do not add station to message if there
         * isn't one defined.
         */
        if (station != StationIdHolder.UNSPECIFIED_VALUE) {
            sb.append(" from station ").append(station);
        }

        return sb.toString();
    }


    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.telem.input.api.connection.IRawInputConnection#reconnect()
     */
    @Override
    public void reconnect() throws RawInputException
    {
        // MPCS-5013 07/23/13 Refactor
        // MPCS-5135 08/02/13 Use functions in abstract
    	
    	logger.debug("CSIC: reconnect()");

        if (socket != null)
        {
            try
            {
                socket.close();
            }
            catch (final IOException e)
            {
                throw new RawInputException("Error closing socket", e);
            }

            messenger.sendDisconnectMessage(getConnectionString());
        }

        // MPCS-5135 08/02/13 Use functions in abstract


        this.dis = getDataInputStream(connectHost, connectPort);

    }


    private DataInputStream getDataInputStream(final String host, final int port) {
        int retryCount = this.rawConfig.getSocketRetries();
        boolean closed = true;
        /** BEGIN MPCS-5013 07/16/13 */
        boolean reported = false;


        final SocketAddress sa            = new InetSocketAddress(host, port);
        final int           retryInterval =
            (int) Math.max(Math.min(this.rawConfig.getSocketRetryInterval(),
                                    Integer.MAX_VALUE),
                           1L);

        // loop and keep trying to open the socket for as many
        // times as specified by the retry count
        while (closed && !stopping) {
            try {
                // if retryCount < 0; retry forever
                if (retryCount > 0) {
                    retryCount--;
                }

                /** Try to connect the socket MPCS-5860 02/25/14 */

                /* MPCS-5860 - (rework) 03/05/2014
                 * Moved following line into while() loop from above.
                 * The socket must always be recreated! 
                 */
                socket = new Socket();
                socket.connect(sa, retryInterval);

                logger.debug(ClientSocketInputConnection.class.getName() +
                             " has created client socket to "            +
                             host                                        +
                             ":"                                         +
                             port);

                // get the data input stream
                dis = new DataInputStream(socket.getInputStream());
                logger.debug(ClientSocketInputConnection.class.getName()
                        + " has opened input stream from socket");

                // If we got this far the socket is open
                closed = false;
            }
            catch (final IOException ie)
            {
                // This will catch SocketTimeoutException as well

                // if we've finished our number of retries
                if (retryCount == 0)
                {
                    stopping = true;

                    logger.warn("IOException "                +
                                ie.getLocalizedMessage()      +
                                " connecting to socket port " +
                                port                          +
                                " on host "                   +
                                host);
                }
                else if ((retryCount < 0) && ! reported)
                {
                    // Report a problem at least once

                    reported = true;

                    logger.warn("IOException "                 +
                                 ie.getLocalizedMessage()      +
                                 " connecting to socket port " +
                                 port                          +
                                 " on host "                   +
                                 host);
                }
                /** END MPCS-5013 */
                
                /* MPCS-5860 - 03/05/2014 - introduce a small delay before retrying.
                 */
                try {
					socket.close();
				} 
                catch (final Exception e) {
					logger.debug("Failure closing socket after error and before retry.", e);
				}
                
            	/* 03/10/2014 - MPCS-5963: Restore ability  to abort with ^C during
            	 */
                if (this.rawConfig.getSocketRetryInterval() > 0) {
                	SleepUtilities.checkedSleep(this.rawConfig.getSocketRetryInterval());
                }
            }
        }

        return dis;
    }


    /**
     * {@inheritDoc}
     * @see jpl.gds.telem.input.api.connection.IRawInputConnection#getRawInputStream()
     */
    @Override
    public IRawInputStream getRawInputStream()
    {
    	// MPCS-5013 07/23/13 Redo

    	logger.debug("CSIC: getRawInputStream()");

    	if (dis == null) {
    		logger.debug("CSIC:  - dis == null, so returning null");
    		return null;
    	}

    	// MPCS-7766  12/29/15 - Update to use buffered input stream only when specified
        if (this.rawConfig.isBufferedInputAllowed(connectionType, sseFlag)) {

    		// MPCS-7449 DE 10/07/15 changed to BufferedRawInputStream
    		return ((dis != null) ? new BufferedRawInputStream(this.appContext, dis, this.bufferDir) : null);
    	}
    	else{
    		return ((dis != null) ? new RawInputStream(dis) : null);
    	}
    }


    /**
     * {@inheritDoc}
     * @see jpl.gds.telem.input.api.connection.IRawInputConnection#loadData(java.util.concurrent.atomic.AtomicBoolean)
     */
    @Override
    public boolean loadData(final AtomicBoolean handlerStopping)
        throws IOException
    {
        // MPCS-5131 08/01/13 Add atomic

        // MPCS-5013 07/18/13

        // The initial (and only) DataInputStream is loaded in openConnection

        return (this.dis != null);
    }


    /**
     * {@inheritDoc}
     *
     * @see
     * jpl.gds.telem.input.api.connection.IRawInputConnection#isConnected()
     */
    @Override
    public boolean isConnected() {
        return this.socket.isConnected();
    }


    /**
     * {@inheritDoc}
     *
     * @see
     * jpl.gds.telem.input.api.connection.IRawInputConnection#isDataStream()
     */
    @Override
    public boolean isDataStream() {
        return true;
    }


    /**
     * We do need the status, set in the stream processor.
     *
     * @return True if EOF on stream
     */
    @Override
    public boolean needsEofOnStreamStatus()
    {
        return true;
    }
}
