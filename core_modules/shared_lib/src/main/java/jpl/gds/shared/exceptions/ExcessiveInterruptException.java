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
 * Exception for excessive interrupts detected.
 *
 */
public class ExcessiveInterruptException extends Exception
{
    private static final long serialVersionUID = 0L;

    /** Maximum number of times we allow a thread to be interrupted
     * while sleeping
     */
    public static final int MAX_INTERRUPT = 100;


    /**
     * Constructor.
     */
    public ExcessiveInterruptException()
    {
        super();
    }


    /**
     * Constructor.
     *
     * @param message Message text
     */
    public ExcessiveInterruptException(final String message)
    {
        super(message);
    }


    /**
     * Constructor.
     *
     * @param cause Underlying cause
     */
    public ExcessiveInterruptException(final Throwable cause)
    {
        super(cause);
    }


    /**
     * Constructor.
     *
     * @param message Message text
     * @param cause   Underlying cause
     */
    public ExcessiveInterruptException(final String    message,
                                       final Throwable cause)
    {
        super(message, cause);
    }


    /**
     * Constructs with a typical message.
     *
     * @param who Module complaining
     *
     * @return ExcessiveInterruptException
     */
    public static ExcessiveInterruptException constructTypical(final String who)
    {
        return new ExcessiveInterruptException(
                       who                                      +
                       " aborted due to excessive interrupts (" +
                       MAX_INTERRUPT                            +
                       ")");
    }
}
