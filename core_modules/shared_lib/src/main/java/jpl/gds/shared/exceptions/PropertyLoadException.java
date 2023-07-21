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
 * Exception object to throw when loading configuration properties
 * 
 * @since R8
 *
 */
public class PropertyLoadException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * @param message
     *            the error message
     * @param throwable
     *            the throwable exception
     */
    public PropertyLoadException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

    /**
     * @param message
     *            the error message
     */
    public PropertyLoadException(final String message) {
        this(message, null);
    }

}
