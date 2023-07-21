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


/**
 * Used with static methods and initializations that are NOT expected to fail,
 * but that technically need to catch certain exceptions.
 * 
 */
public class StaticException extends RuntimeException
{
    private static final long serialVersionUID = 0L;

    /**
     * Constructor.
     */
    public StaticException()
    {
        super();
    }

    /**
     * Constructor.
     * 
     * @param message detail message for this exception
     */
    public StaticException(final String message)
    {
        super(message);
    }

    /**
     * Constructor.
     * 
     * @param cause the Throwable that triggered this exception
     */
    public StaticException(final Throwable cause)
    {
        super(cause);
    }

    /**
     * Constructor.
     * 
     * @param message the detail message for this exception
     * @param cause the Throwable that triggered this exception
     */
    public StaticException(final String    message,
                           final Throwable cause)
    {
        super(message, cause);
    }
}
