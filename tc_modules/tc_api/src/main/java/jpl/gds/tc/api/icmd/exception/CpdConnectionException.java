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
 * Exception for errors that arise AMPCS fails to contact CPD
 * 
 * @since AMPCS R5
 */
@SuppressWarnings("serial")
public class CpdConnectionException extends ICmdException {
	/**
	 * Basic constructor.
	 */
	public CpdConnectionException() {
		super();
	}

	/**
	 * Constructs a CpdConnectionException with the given message and cause.
	 * 
	 * @param message the detailed error message
	 * @param cause the Throwable that triggered this exception
	 */
	public CpdConnectionException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs a CpdConnectionException with the given message.
	 * 
	 * @param message the detailed error message
	 */
	public CpdConnectionException(String message) {
		super(message);
	}

	/**
	 * Constructs a CpdConnectionException with the given cause.
	 * 
	 * @param cause the Throwable that triggered this exception
	 */
	public CpdConnectionException(Throwable cause) {
		super(cause);
	}
}
