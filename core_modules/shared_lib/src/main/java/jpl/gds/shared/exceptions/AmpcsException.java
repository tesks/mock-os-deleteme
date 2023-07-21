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
package jpl.gds.shared.exceptions;

import jpl.gds.shared.exceptions.AmpcsTracing.Marker;


/**
 * Base class for AMPCS exceptions that can automatically produce a traceback.
 *
 * We are public so that the users can catch the whole hierarchy, but the
 * constructors are protected so that the users must subclass to create
 * tracing exceptions.
 *
 */
public class AmpcsException extends Exception implements IAmpcsTracing
{
    private static final long serialVersionUID = 0L;


    /**
     * Constructor.
     */
    protected AmpcsException()
    {
        super();

        AmpcsTracing.performTracebackIfDesired(this, null);
    }


    /**
     * Constructor.
     *
     * @param message Message text
     */
    protected AmpcsException(final String message)
    {
        super(message);

        AmpcsTracing.performTracebackIfDesired(this, null);
    }


    /**
     * Constructor.
     *
     * @param rootCause Underlying cause
     */
    protected AmpcsException(final Throwable rootCause)
    {
        super(rootCause);

        AmpcsTracing.performTracebackIfDesired(this, null);
    }


    /**
     * Constructor.
     *
     * @param message   Message text
     * @param rootCause Underlying cause
     */
    protected AmpcsException(final String    message,
                             final Throwable rootCause)
    {
        super(message, rootCause);

        AmpcsTracing.performTracebackIfDesired(this, null);
    }


    /**
     * Constructor.
     *
     * @param message Message text
     * @param marker  Marker or null
     */
    protected AmpcsException(final String message,
                             final Marker marker)
    {
        super(message);

        AmpcsTracing.performTracebackIfDesired(this, marker);
    }


    /**
     * Constructor.
     *
     * @param rootCause Underlying cause
     * @param marker    Marker or null
     */
    protected AmpcsException(final Throwable rootCause,
                             final Marker    marker)
    {
        super(rootCause);

        AmpcsTracing.performTracebackIfDesired(this, marker);
    }


    /**
     * Constructor.
     *
     * @param message   Message text
     * @param rootCause Underlying cause
     * @param marker    Marker or null
     */
    protected AmpcsException(final String    message,
                             final Throwable rootCause,
                             final Marker    marker)
    {
        super(message, rootCause);

        AmpcsTracing.performTracebackIfDesired(this, marker);
    }
}
