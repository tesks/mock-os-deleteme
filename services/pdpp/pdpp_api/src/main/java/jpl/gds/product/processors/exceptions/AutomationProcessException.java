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
package jpl.gds.product.processors.exceptions;

import jpl.gds.product.automation.AutomationException;

/**
 * Exception to be used by PDPP during processing
 *
 */
public class AutomationProcessException extends AutomationException {
    private static final long serialVersionUID = 3731866336661858368L;

    /**
     *
     */
    public AutomationProcessException() {
        super();
    }

    /**
     * @param message
     * @param rootCause
     */
    public AutomationProcessException(final String message, final Throwable rootCause) {
        super(message, rootCause);
    }

    /**
     * @param message
     */
    public AutomationProcessException(final String message) {
        super(message);
    }
}

