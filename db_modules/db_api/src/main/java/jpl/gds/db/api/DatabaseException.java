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
/**
 * 
 */
package jpl.gds.db.api;


public class DatabaseException extends Exception {
    private static final long serialVersionUID = 8541236968080983677L;

	/**
     * Basic constructor.
     */
    public DatabaseException() {
        super();
    }

    /**
     * Constructs a DatabaseException with the given message.
     * 
     * @param message the detailed error message
     */
    public DatabaseException(final String message) {
        super(message);
    }

    /**
     * Constructs a DatabaseException with the given cause.
     * 
     * @param cause the Throwable that triggered this exception
     */
    public DatabaseException(final Throwable cause) {
        super(cause.getLocalizedMessage(), cause);
    }

    /**
     * Constructs a DatabaseException with the given message and cause.
     * 
     * @param message the detailed error message
     * @param cause the Throwable that triggered this exception
     */
    public DatabaseException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public DatabaseException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
