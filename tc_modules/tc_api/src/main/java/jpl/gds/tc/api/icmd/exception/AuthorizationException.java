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
package jpl.gds.tc.api.icmd.exception;

/**
 * Exception for errors that arise when the user is not authorized to perform a
 * certain action
 * 
 * @since AMPCS R3
 */
@SuppressWarnings("serial")
public class AuthorizationException extends ICmdException {
	/**
	 * Basic constructor.
	 */
	public AuthorizationException() {
		super();
	}

	/**
	 * Constructs a CpdException with the given message and cause.
	 * 
	 * @param message the detailed error message
	 * @param cause the Throwable that triggered this exception
	 */
	public AuthorizationException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs a CpdException with the given message.
	 * 
	 * @param message the detailed error message
	 */
	public AuthorizationException(String message) {
		super(message);
	}

	/**
	 * Constructs a CpdException with the given cause.
	 * 
	 * @param cause the Throwable that triggered this exception
	 */
	public AuthorizationException(Throwable cause) {
		super(cause);
	}
}
