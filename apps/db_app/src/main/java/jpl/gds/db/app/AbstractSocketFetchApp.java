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
package jpl.gds.db.app;

import java.io.OutputStream;
import java.io.PrintWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;

import jpl.gds.db.api.io.ServerSocketException;
import jpl.gds.db.app.util.ServerSocketWriter;
import jpl.gds.shared.holders.HostNameHolder;
import jpl.gds.shared.holders.PortHolder;
import jpl.gds.shared.sys.FlushBool;


/**
 * AbstractSocketFetchApp is the base class for all command line applications
 * that query the database to a socket.
 *
 */
public abstract class AbstractSocketFetchApp extends AbstractFetchApp
{

    /** Long socket host option */
    public static final String SOCKET_HOST_LONG = "socketHost";

    /** Long socket port option */
    public static final String SOCKET_PORT_LONG = "socketPort";

    /** The host with the socket */
    private HostNameHolder _socketHost = HostNameHolder.UNSUPPORTED;

    /** The port of the socket */
    private PortHolder _socketPort = PortHolder.UNSUPPORTED;

    /** Handles the actual writing to the socket */
    private ServerSocketWriter _ssw = null;


    /**
     * Creates an instance of AbstractSocketFetchApp
     *
     * @param tableName Name of the database table
     * @param appName   Application name
     * @param app       CSV application type
     */
    protected AbstractSocketFetchApp(final String tableName,
                                     final String appName,
                                     final String app)
    {
        super(tableName, appName, app);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void addAppOptions()
    {
        super.addAppOptions();

        addOption(null, SOCKET_HOST_LONG, "string",  "Socket host");
        addOption(null, SOCKET_PORT_LONG, "integer", "Socket port");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void configureApp(final CommandLine cmdline)
        throws ParseException
    {
        super.configureApp(cmdline);

        // Test individually before we test together (helps JUnit)

        final boolean hasSocketHost = cmdline.hasOption(SOCKET_HOST_LONG);
        final boolean hasSocketPort = cmdline.hasOption(SOCKET_PORT_LONG);

        if (hasSocketHost)
        {
            _socketHost = HostNameHolder.getFromOption(SOCKET_HOST_LONG,
                                                       cmdline,
                                                       false);
        }

        if (hasSocketPort)
        {
            _socketPort = PortHolder.getFromOption(SOCKET_PORT_LONG,
                                                   cmdline,
                                                   false);
        }

        if ((hasSocketHost || hasSocketPort) &&
            (hasSocketHost != hasSocketPort))
        {
            throw new MissingOptionException("--"                  +
                                             SOCKET_HOST_LONG      +
                                             " and --"             +
                                             SOCKET_PORT_LONG      +
                                             " must be specified " +
                                             "together");
        }

        if (! hasSocketHost)
        {
            // We have neither host nor port

            return;
        }

        // At this point we have a proper host and port

        if (cmdline.hasOption(REPORT_LONG))
        {
            throw new ParseException("--"                             +
                                     SOCKET_HOST_LONG                 +
                                     " must not be specified with --" +
                                     REPORT_LONG);
        }

        if (cmdline.hasOption(SHOW_COLUMNS_LONG))
        {
            throw new ParseException("--"                             +
                                     SOCKET_HOST_LONG                 +
                                     " must not be specified with --" +
                                     SHOW_COLUMNS_LONG);
        }
    }


    /**
     * Overridden to perform application-specific
     * startup activities.
     *
     * @throws ServerSocketException If problem starting
     */
    @Override
    protected void specificStartup() throws ServerSocketException
    {

        super.specificStartup();

        if (! _socketHost.isUnsupported())
        {
            _ssw = new ServerSocketWriter(_socketHost, _socketPort, dbProperties.getQuerySocketTimeoutMs());
        }
    }


    /**
     * Overridden to perform application-specific
     * shutdown activities.
     */
    @Override
    protected void specificShutdown()
    {

        super.specificShutdown();

        if (_ssw != null)
        {
            _ssw.close();
        }
    }


    /**
     * Get output stream attached to socket.
     *
     * @return Output stream
     */
    @Override
    protected OutputStream getOverridingOutputStream()
    {

        if (! _socketHost.isUnsupported())
        {
            return _ssw.getOutputStream();
        }

        return null;
    }


    /**
     * Get print writer attached to socket.
     *
     * @param autoFlush True if we want to auto-flush
     *
     * @return Print writer
     */
    @Override
    protected PrintWriter getOverridingPrintWriter(final FlushBool autoFlush)
    {

        if (! _socketHost.isUnsupported())
        {
            return _ssw.getPrintWriter(autoFlush);
        }

        return null;
    }


    /**
     * Perform checks against OUTPUT_FORMAT_LONG.
     *
     * @param cmdline Command line
     *
     * @throws ParseException If any parameter errors found
     */
    protected void checkOutputFormat(final CommandLine cmdline)
        throws ParseException
    {
        if (cmdline.hasOption(OUTPUT_FORMAT_LONG) &&
            cmdline.hasOption(SOCKET_HOST_LONG))
        {
            throw new ParseException("--"                             +
                                     SOCKET_HOST_LONG                 +
                                     " must not be specified with --" +
                                     OUTPUT_FORMAT_LONG);
        }
    }


    /**
     * Perform checks against FILE_LONG.
     *
     * @param cmdline Command line
     * @param desired True if is mandatory without socket
     *
     * @throws ParseException If any parameter errors found
     */
    protected void checkFilename(final CommandLine cmdline,
                                 final boolean     desired)
        throws ParseException
    {
        final boolean haveFile = cmdline.hasOption(FILE_LONG);

        if (cmdline.hasOption(SOCKET_HOST_LONG))
        {
            if (haveFile)
            {
                throw new ParseException("--"                             +
                                         SOCKET_HOST_LONG                 +
                                         " must not be specified with --" +
                                         FILE_LONG);
            }
        }
        else if (desired && ! haveFile)
        {
            throw new MissingOptionException("--"      +
                                             FILE_LONG +
                                             " is required");
        }
    }


    /**
     * Supply extra usage information.
     *
     * @return Usage information as a string
     */
    protected static String getExtraUsage()
    {
        return ("--"             +
                SOCKET_HOST_LONG +
                " <string> "     +
                "--"             +
                SOCKET_PORT_LONG +
                " <int>");
    }


    /**
     * Return true if we are sending to a socket.
     *
     * @return State
     */
    protected boolean usingSocket()
    {
        return (_ssw != null);
    }
    

    /**
     * Get the name of the host
     * 
     * @return host name
     */
    protected String getHost() {
    	return _socketHost.getValue();
    }
    
    /**
     * Get the port number
     * 
     * @return port number
     */
    protected Integer getPort() {
    	return _socketPort.getValue();
    }

    /**
     * Write log statements to tracer
     * 
     * @param noOutput true if fetch returned nothing, false if fetch returned 
     * results
     */
    @Override
    protected void writeToLog(boolean noOutput) {
        if(usingSocket()) {
        	if(noOutput) {
            	trace.info("Socket query returned no data.");
            } else {
            	trace.info("Sending query data over socket to host \"" + 
            			getHost() + "\" and port \"" + 
            			getPort() + "\"");
            }
        }
    }
}
