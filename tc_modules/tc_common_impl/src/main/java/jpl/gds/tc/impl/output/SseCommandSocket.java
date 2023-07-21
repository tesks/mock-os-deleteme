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
package jpl.gds.tc.impl.output;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.connection.IConnection;
import jpl.gds.common.config.connection.INetworkConnection;
import jpl.gds.shared.annotation.Singleton;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.tc.api.output.ISseCommandSocket;


/**
 * Singleton to hold output socket for use with SSE commanding.
 *
 * No need for lazy create of the instance since there is no need to
 * reference this class unless to call one of the methods.
 *
 * Note that the atomic can eliminate some of the synchronization
 * but not all.
 *
 *
 */
@Singleton
public final class SseCommandSocket implements ISseCommandSocket
{
    private final Tracer trace;
    private String           host;
    private int              port;
    private String           dest;

    private final AtomicBoolean ALLOW_SSE_SOCKET = new AtomicBoolean(false);

    private Socket      _sseSocket = null;
    private PrintWriter _pw        = null;


    /**
     * Private constructor.
     * @param appContext 
     */
    public SseCommandSocket(final ApplicationContext appContext)
    {
        super();
        trace = TraceManager.getDefaultTracer(appContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void transmitSseCommand(final String commandString)
        throws IOException
    {
        if (! ALLOW_SSE_SOCKET.get())
        {
            throw new IllegalArgumentException("SSE command socket disabled");
        }

        if (commandString == null)
        {
            throw new IllegalArgumentException("Input SSE command was null");
        }

        boolean keepTrying = true;

        while (keepTrying)
        {
            if (_sseSocket == null)
            {
                keepTrying = false;

                try
                {
                    _sseSocket = new Socket(host, port);
                }
                catch (final IOException ioe)
                {
                    trace.error("Error connecting SSE socket to ", dest, ": ",
                            ioe.getMessage(), ioe.getCause());

                    closeEverything();

                    throw ioe;
                }

                try
                {
                    // Input side is not used

                    _sseSocket.shutdownInput();
                }
                catch (final RuntimeException rte)
                {
                    throw rte;
                }
                catch (final Exception e)
                {
                    SystemUtilities.doNothing();
                }

                try
                {
                    _pw = new PrintWriter(_sseSocket.getOutputStream());
                }
                catch (final IOException ioe)
                {
                    trace.error("Error connecting SSE socket writer to ", dest,
                            ": ",
                            ioe.getMessage(), ioe.getCause());

                    closeEverything();

                    throw ioe;
                }

                trace.debug("Created SSE socket to ", dest);
            }

            // _sseSocket and _pw are not null if we get here

            _pw.write(commandString);
            _pw.flush();

            if (! _pw.checkError()      &&
                ! _sseSocket.isClosed() &&
                _sseSocket.isConnected())
            {
                trace.trace("Sent SSE command '", commandString.replaceAll("\n", ""), "' to ",
                               dest);

                // We made it, exit the loop

                keepTrying = false;
            }
            else
            {
                // Error reported, so close everything.
                // If we have already tried recreating the socket
                // then bail out. Otherwise stay in the loop.

                closeEverything();

                final String message = "Error writing to SSE socket to " + dest;

                trace.error(message);

                if (! keepTrying)
                {
                    throw new IOException(message);
                }
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void enableSseSocket(final IConnection cc)
    {
    	
    	if (!(cc instanceof INetworkConnection)) {
    		throw new IllegalArgumentException("Connection configuration must be a network configuration");
    	}
    	
    	host     = ((INetworkConnection)cc).getHost();
    	port     = ((INetworkConnection)cc).getPort();
        dest     = host + ":" + port;

        if (ALLOW_SSE_SOCKET.compareAndSet(false, true))
        {
            trace.debug("Enabled SSE command socket");
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void disableSseSocket()
    {
        if (ALLOW_SSE_SOCKET.compareAndSet(true, false))
        {
            synchronized (this)
            {
                closeEverything();
            }

            trace.debug("Disabled SSE command socket");
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSseSocketEnabled()
    {
        return ALLOW_SSE_SOCKET.get();
    }


    /**
     * Close everything, ignoring exceptions.
     * Cautious but safe.
     * Must be called synchronized.
     */
    private void closeEverything()
    {
        if (_sseSocket != null)
        {
            try
            {
                _sseSocket.close();
            }
            catch (final RuntimeException rte)
            {
                throw rte;
            }
            catch (final Exception e)
            {
                SystemUtilities.doNothing();
            }

            _sseSocket = null;
        }

        if (_pw != null)
        {
            try
            {
                _pw.close();
            }
            catch (final RuntimeException rte)
            {
                throw rte;
            }
            catch (final Exception e)
            {
                SystemUtilities.doNothing();
            }

            _pw = null;
        }
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }
}
