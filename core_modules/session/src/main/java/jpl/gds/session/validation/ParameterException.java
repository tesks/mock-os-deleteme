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
package jpl.gds.session.validation;

import jpl.gds.shared.exceptions.AmpcsException;
import jpl.gds.shared.exceptions.AmpcsTracing.Marker;
import jpl.gds.shared.exceptions.AmpcsTracing.MarkerEnum;


/**
 * Exception for use with parameter processing.
 *
 */
public class ParameterException extends AmpcsException
{
    private static final long serialVersionUID = 0L;

    private static final Marker MARKER = new Marker(MarkerEnum.CONTROL);


    /**
     * Constructor.
     *
     * @param message Message text
     */
    public ParameterException(final String message)
    {
        super(message, MARKER);
    }


    /**
     * Constructor.
     *
     * @param message   Message text
     * @param rootCause Underlying cause
     */
    public ParameterException(final String    message,
                              final Throwable rootCause)
    {
        super(message, rootCause, MARKER);
    }


    /**
     * Constructor.
     *
     * @param message Message text
     * @param marker  Marker or null
     */
    public ParameterException(final String message,
                              final Marker marker)
    {
        super(message, marker);
    }


    /**
     * Constructor.
     *
     * @param message   Message text
     * @param rootCause Underlying cause
     * @param marker    Marker or null
     */
    public ParameterException(final String    message,
                              final Throwable rootCause,
                              final Marker    marker)
    {
        super(message, rootCause, marker);
    }
}
