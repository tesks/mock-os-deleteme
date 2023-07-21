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
package jpl.gds.automation.auto;

import org.springframework.http.HttpStatus;

/**
 * An exception class use for errors encountered in the AUTO proxy
 *  @since AMPCS R6
 */
@SuppressWarnings("serial")
public class AutoProxyException extends RuntimeException {
    /**
     * 
     */
    public AutoProxyException() {
        super();
    }

    /**
     * @param message
     *            error message
     * @param cause
     *            throwable cause
     * @param enableSuppression
     *            if the exception should be suppressed
     * @param writableStackTrace
     *            write the stack trace
     */
    public AutoProxyException(final String message, final Throwable cause, final boolean enableSuppression,
            final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * @param message
     *            error message
     * @param cause
     *            throwable cause
     */
    public AutoProxyException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     *            error message
     */
    public AutoProxyException(final String message) {
        super(message);
    }

    /**
     * @param cause
     *            throwable cause
     */
    public AutoProxyException(final Throwable cause) {
        super(cause);
    }

    /**
     * @param status
     * @param message
     */
    public AutoProxyException(final HttpStatus status, final String message) {
        this("<" + status.name() + ": " + status.value() + "> " + message + "\n" + status.getReasonPhrase());
    }

}
