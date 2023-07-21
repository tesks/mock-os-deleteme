/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.message.api.spill;


/**
 * Only exception thrown by SpillProcessor (on purpose, anyway)
 */
public final class SpillProcessorException extends Exception {
    /**
     * Default serial ID for serializable exception.
     */
    private static final long serialVersionUID = 1L;


    /**
     * Constructor.
     *
     * @param message Message text
     */
    public SpillProcessorException(final String message) {
        super(message);
    }


    /**
     * Constructor.
     *
     * @param message Message text
     * @param cause   Underlying cause
     */
    public SpillProcessorException(final String message,
                                   final Throwable cause) {
        super(message, cause);
    }


    /**
     * Constructor.
     *
     * @param cause Underlying cause
     */
    public SpillProcessorException(final Throwable cause) {
        super(cause);
    }
}
