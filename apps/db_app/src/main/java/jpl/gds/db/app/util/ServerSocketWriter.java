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
package jpl.gds.db.app.util;

import java.io.Closeable;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import jpl.gds.db.api.io.ServerSocketException;
import jpl.gds.shared.annotation.AlwaysThrows;
import jpl.gds.shared.holders.HostNameHolder;
import jpl.gds.shared.holders.PortHolder;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.sys.FlushBool;
import jpl.gds.shared.sys.SystemUtilities;


/**
 * This class provides support for sending database queries over a socket.
 *
 * NB: Do not do any logging at INFO level because this class is used with
 * fetch classes that write data rows to the console.
 *
 */
public class ServerSocketWriter extends Object implements Closeable
{

    private static final String ME = "ServerSocketWriter: ";

    private final Socket       _socket = new Socket();
    private final OutputStream _os;
    private final Tracer   _log = TraceManager.getDefaultTracer();


    /**
     * Constructor. Connect socket and get output stream.
     *
     * Watch the order of the catches; must be specific
     * before general.
     *
     * @param host Host
     * @param port Port
     * @param timeout milliseconds before the attempted 
     *                port connection times out
     *
     * @throws ServerSocketException On any problem
     */
    public ServerSocketWriter(final HostNameHolder host,
                              final PortHolder     port, final int timeout)
        throws ServerSocketException
    {
        super();
        _log.setPrefix(ME);
        try
        {
            _socket.setReuseAddress(true);
        }
        catch (final SocketException se)
        {
            SystemUtilities.doNothing();
        }

        try
        {
            _socket.setKeepAlive(true);
        }
        catch (final SocketException se)
        {
            SystemUtilities.doNothing();
        }

        try
        {
            _socket.connect(
                new InetSocketAddress(host.getValue(), port.getValue()),
                timeout);
        }
        catch (final ConnectException ce)
        {
            close();

            logAndThrow("Could not connect (probably refused): ", ce);
        }
        catch (final NoRouteToHostException nrthe)
        {
            close();

            logAndThrow("Cannot get to host: ", nrthe);
        }
        catch (final SocketException se)
        {
            close();

            logAndThrow("Socket problem connecting: ", se);
        }
        catch (final SocketTimeoutException ste)
        {
            close();

            logAndThrow("Socket timeout connecting: ", ste);
        }
        catch (final IOException ioe)
        {
            close();

            logAndThrow("General problem connecting: ", ioe);
        }

        try
        {
            // We don't need the input side

            _socket.shutdownInput();
        }
        catch (final IOException ioe)
        {
            SystemUtilities.doNothing();
        }

        OutputStream os = null;

        try
        {
            os = _socket.getOutputStream();
        }
        catch (final IOException ioe)
        {
            close();

            logAndThrow("Problem getting output stream: ", ioe);
        }

        _os = os;

        final InetAddress remote = _socket.getInetAddress();
        final InetAddress local  = _socket.getLocalAddress();

        _log.debug("Connected to ", remote.getCanonicalHostName(), "(", remote.getHostAddress(), "):",
                _socket.getPort(), " from ", local.getCanonicalHostName(), "(", local.getHostAddress(), "):",
                _socket.getLocalPort(), " with timeout ",
                      timeout);
    }


    /**
     * Get data output stream attached to socket.
     *
     * @return Data output stream
     */
    public OutputStream getOutputStream()
    {
        return new LocalOutputStream(_os);
    }


    /**
     * Get print writer attached to socket.
     *
     * @param autoFlush True if we want to auto-flush
     *
     * @return Print writer
     */
    public PrintWriter getPrintWriter(final FlushBool autoFlush)
    {
        return new LocalPrintWriter(_os, autoFlush);
    }


    /**
     * Close the socket if not closed already.
     * Closes the output stream.
     */
    @Override
    public void close()
    {
        if (! _socket.isClosed())
        {
            try
            {
                _socket.close();
            }
            catch (final IOException ioe)
            {
                SystemUtilities.doNothing();
            }
        }
    }


    /**
     * Log error and throw exception with cause.
     *
     * @param message Text of message
     * @param cause   Exception cause
     */
    @AlwaysThrows
    private void logAndThrow(final String    message,
                             final Exception cause)
        throws ServerSocketException
    {
        _log.error(message, cause);

        throw new ServerSocketException(message, cause);
    }


    /** Subclass to handle logging */
    private class LocalOutputStream extends FilterOutputStream
    {
        /**
         * Methods write/3, write/1, flush, and close are called.
         * There is no need to override flush, close, or write/1.
         * (write/1 is documented to call write/3.)
         */


        /**
         * Constructor.
         *
         * @param os Output stream
         */
        public LocalOutputStream(final OutputStream os)
        {
            super(os);
        }


        /**
         * Write bytes. Trap errors and log and rethrow.
         *
         * @param bytes  Byte array
         * @param offset Offset into byte arrey
         * @param length Number of bytes to write
         *
         * @throws IOException On any I/O problem
         */
        @Override
        public void write(final byte[] bytes,
                          final int    offset,
                          final int    length) throws IOException
        {
            try
            {
                super.write(bytes, offset, length);
            }
            catch (final IOException ioe)
            {
                close();

                _log.error("Error writing to socket; underlying cause: " + ioe.getMessage(), 
                              ioe.getCause());
                throw ioe;
            }

            try
            {
                flush();
            }
            catch (final IOException ioe)
            {
                SystemUtilities.doNothing();
            }
        }
    }


    /** Subclass to handle logging */
    private class LocalPrintWriter extends PrintWriter
    {
        /**
         * Methods write/1, flush, and close are called.
         * There is no need to override flush and close.
         */


        /**
         * Constructor.
         *
         * @param os        Output stream
         * @param autoFlush True for automatic flushing
         */
        public LocalPrintWriter(final OutputStream os,
                                final FlushBool    autoFlush)
        {
            super(os, autoFlush.get());
        }


        /**
         * Write a string. Trap errors and log. We cannot throw because the
         * super does not throw.
         *
         * @param s String
         */
        @Override
        public void write(final String s)
        {
            super.write(s);

            if (checkError())
            {
                _log.error("Error printing to socket");
            }
        }
    }
}
