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
package jpl.gds.decom.exception;

/**
 * This is the base class for all exceptions thrown during decom processing. This exception
 * and its subclasses should be used as a single checked exception type to signal callers
 * of a decom processor that a non-recoverable error was encountered.
 *
 */
public class DecomException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Create an exception instance.
	 * @param message the error message describing the nature of the exception.
	 * @param cause the nested cause of the exception
	 */
	public DecomException(String message, Exception cause) {
		super(message, cause);
	}

	/**
	 * Create an exception instance.
	 * @param message the error message describing the nature of the exception.
	 */
	public DecomException(String message) {
		super(message);
	}

}
