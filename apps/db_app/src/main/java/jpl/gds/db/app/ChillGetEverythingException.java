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
package jpl.gds.db.app;

/**
 * Thrown when Chill Get Everything catches an exceptiona and cannot continue.
 *
 */
public class ChillGetEverythingException extends RuntimeException {
    /**
     * The serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates an instance of ChillGetEverythingException
     * @param message a detail message
     */
    public ChillGetEverythingException(final String message){
        super(message);
    }

    /**
     * Creates an instance of ChillGetEverythingException
     * @param message a detail message
     * @param rootCause the causal exception
     */
    public ChillGetEverythingException(final String message, 
            final Throwable rootCause){
        super(message, rootCause);
    }
}
