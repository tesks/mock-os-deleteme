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
package jpl.gds.message.api.external;

import java.net.Socket;

import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * Utility class to check for the presence of the message service broker. We
 * have abandoned the special ActiveMQ method in favor of the generic method.
 */
public class CheckMessageService extends Object {
    /**
     * Check if the message service broker is up or down. Do NOT call too often. Inherently
     * slow.
     * @param conf the current message service configuration
     * @param host The host or null to get from config
     * @param port The port or negative or zero to get from config
     * @param trace A Tracer to use for logging
     * @param fatal True if trace logged as fatal, else error
     * @param echo true if message service status should be echoed to the console; false if not
     * @return True if up; false if down or error
     */
    public static boolean checkMessageServiceRunning(MessageServiceConfiguration conf, final String host, final int port,
            final Tracer trace, final boolean fatal, final boolean echo) {
        final Tracer log =
                (trace != null) ? trace : TraceManager.getDefaultTracer();

        String jmsHost = null;
        int jmsPort = 0;

        if (host != null) {
            jmsHost = host;
        } else {
            jmsHost = conf.getMessageServerHost();

            if (jmsHost == null) {
                jmsHost = "localhost";
            }
        }

        if (port > 0) {
            jmsPort = port;
        } else {
            jmsPort = conf.getMessageServerPort();

            if (jmsPort <= 0) {
                final int defaultJmsPort = 61614;
                jmsPort = defaultJmsPort;
            }
        }

        boolean status = true;
        Socket socket = null;

        try {
            socket = new Socket(jmsHost, jmsPort);
        } catch (final Exception e) {
            status = false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (final Exception e) {
                    // Makes FindBugs happy
                    socket = null;
                }
            }
        }

        if (echo)
        {
            if (status) {

                log.info("The message service is running on " + jmsHost + ":" + jmsPort);
            } else {
                final String message =
                        "The message service does not appear to be " + "running on " + jmsHost
                        + "/" + jmsPort;
                if (fatal) {
                    log.fatal(message);
                } else {
                    log.error(message);
                }
            }
        }  
        return status;
    }

    /**
     * Determines if the message service is active at its specified port. Does not determine
     * if it accepts publications -- merely whether it responds to telnet.
     * 
     * @param conf the current message service configuration
     * @param host the message service host name. If null, will be fetched from the 
     *        current global configuration
     * @param port the message service port. If 0, will be fetched from the current
     *        global configuration
     * @param logger the Tracer to use for logging of messages
     * @param fatal true if the lack of message service should be logged as a FATAL
     *        message
     * @return true if the message service is up, false if not
     */
    public static boolean checkMessageServiceRunning(MessageServiceConfiguration conf, final String host, final int port,
            final Tracer logger, final boolean fatal )
    {
        return checkMessageServiceRunning(conf, host, port, logger, fatal, true );
    }
}
