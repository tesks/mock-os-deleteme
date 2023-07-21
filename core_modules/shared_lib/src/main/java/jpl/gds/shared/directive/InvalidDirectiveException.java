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
package jpl.gds.shared.directive;

/**
 * An exception thrown for invalid process control directives.
 * 
 * @since R8
 *
 */
public class InvalidDirectiveException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
     * Constructor.
     * 
     * @param message detailed message string
     */
    public InvalidDirectiveException(String message) {
        super(message);
    }

    /**
     * Constructor.
     * 
     * @param cause Throwable that caused this exception
     */
    public InvalidDirectiveException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor.
     * 
     * @param message detailed message string
     * @param cause Throwable that caused this exception
     */
    public InvalidDirectiveException(String message, Throwable cause) {
        super(message, cause);
    }

}
