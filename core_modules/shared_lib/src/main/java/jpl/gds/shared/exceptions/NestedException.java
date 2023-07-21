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

import java.io.PrintStream;
import java.io.PrintWriter;


/**
 * Exception class that holds a primary underlying exception.
 *
 */
public class NestedException extends Exception
{
    private static final long serialVersionUID = 0L;


    private final Throwable rootCause;


    /**
     * Constructor.
     */
    public NestedException() {
        super();

        this.rootCause = null;
    }

    /**
     * Constructor.
     *
     * @param message Message text
     */
    public NestedException(String message) {
        super(message);

        this.rootCause = null;
    }

    /**
     * Constructor.
     *
     * @param message   Message text
     * @param rootCause Underlying cause
     */
    public NestedException(String message, Throwable rootCause) {
        super(message);

        this.rootCause = rootCause;
    }


    /**
     * Get underlying cause of this exception.
     *
     * @return Root cause
     */
    public Throwable getRootCause() {
        return rootCause;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printStackTrace() {
        super.printStackTrace();
        if (rootCause != null) {
            rootCause.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printStackTrace(PrintStream out) {
        super.printStackTrace(out);
        if (rootCause != null) {
            rootCause.printStackTrace(out);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printStackTrace(PrintWriter out) {
        super.printStackTrace(out);
        if (rootCause != null) {
            rootCause.printStackTrace(out);
        }
    }
}
