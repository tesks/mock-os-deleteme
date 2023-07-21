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
package jpl.gds.shared.metadata;

/**
 * An exception to be thrown when metadata is found to be invalid for
 * usage y whatever is attempting to use it.
 * 
 *
 */
public class InvalidMetadataException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     * 
     * @param message the detailed exception message
     */
    public InvalidMetadataException(String message) {
        super(message);
    }

    /**
     * Constructor
     * @param cause the underlying Throwable that resulted in this exception.
     */
    public InvalidMetadataException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor
     * 
     * @param message the detailed exception message
     * @param cause the underlying Throwable that resulted in this exception.
     */
    public InvalidMetadataException(String message, Throwable cause) {
        super(message, cause);
    }
}
