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
package jpl.gds.product.automation;


/**
 * AutomationException is a checked exception for use within automation
 * associated classes, primarily post-downlink product processing
 * 
 * 06/15/16 - MPCS-8179 - Added to AMPCS, updated from original version in MPCS for MSL G9.
 *
 */
@SuppressWarnings("serial")
public class AutomationException extends Exception {
	 /**
     * Creates an instance of AutomationException.
     */
    public AutomationException() {
        super();
    }

    /**
     * Creates an instance of AutomationException with the given detail text.
     * @param message the exception message
     */
    public AutomationException(String message) {
        super(message);
    }

    /**
     * Creates an instance of AutomationException with the given detail text and triggering
     * Throwable.
     * @param message the exception message
     * @param rootCause the exception that Trigger this one
     */
    public AutomationException(String message, Throwable rootCause) {
        super(message, rootCause);
    }
}
