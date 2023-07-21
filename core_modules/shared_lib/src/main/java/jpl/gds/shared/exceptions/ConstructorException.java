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
 * Nested exception used for reflection constructor errors.
 *
 */
public class ConstructorException extends NestedException
{
    private static final long serialVersionUID = 0L;


    /**
     * Constructor.
     */
    public ConstructorException() {
        super();
    }

    /**
     * Constructor.
     *
     * @param message Message text
     */
    public ConstructorException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param message   Message text
     * @param rootCause Underlying cause
     */
    public ConstructorException(String message, Throwable rootCause) {
        super(message, rootCause);
    }
}
