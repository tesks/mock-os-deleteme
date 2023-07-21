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
package jpl.gds.shared.log;

/**
 * A general interface that can be used by any TraceNotifier or Log4J Appender for
 * registration and notification of listeners to trace log messages.
 * 
 */
public interface TraceListener {
    /**
     * Notifies the current object that a log message has been received on a
     * given context key
     * 
     * @param message
     *            the internal AmpcsLog4jMessage object to pass to registered
     *            trace listeners
     */
    public void notifyTraceListeners(AmpcsLog4jMessage message);
}
