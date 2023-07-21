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
package jpl.gds.eha.api.service.channel;



/**
 * EhaAdapterException is thrown when errors related to IEhaAdapter
 * creation or processing.
 * <p>
 * <b>MULTI-MISSION CORE ADAPTATION CLASS
 * <p>
 * This is a controlled class. It may not be updated without applicable change
 * requests being filed, and approval of project management. A new version tag
 * must be added below with each revision, and both ECR number and author must
 * be included with the version number.</b>
 * <p>
 * EhaAdapterException is thrown when errors related to IEhaAdapter
 * creation or processing. No other exception should be thrown by an IEhaAdapter
 * implementation.
 * <p>
 *
 * @see IPrechannelizedAdapter
 */
public class PrechannelizedAdapterException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an EhaAdapterException with the given detail message.
     * 
     * @param message the detailed message text
     */
    public PrechannelizedAdapterException(final String message) {
        super(message);
    }

    /**
     * Creates an EhaAdapterException with the given triggering Throwable.
     * 
     * @param cause the root exception of the error
     */
    public PrechannelizedAdapterException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates an EhaAdapterException with the given detail message and triggering
     * Throwable.
     * 
     * @param message the detailed message text
     * @param cause the root exception of the error
     */
    public PrechannelizedAdapterException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
