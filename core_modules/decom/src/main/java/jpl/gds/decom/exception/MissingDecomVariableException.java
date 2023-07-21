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
 * This exception is used to represent an error in decommutation stemming from a variable that
 * is referenced, but not known by the decom processor.
 *
 */
public class MissingDecomVariableException extends DecomException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Create an exception instance.
	 * @param message the error message describing the nature of the exception.
	 * @param cause the nested cause of the exception
	 */
	public MissingDecomVariableException(String message, Exception cause) {
		super(message, cause);
	}

	/**
	 * Create an exception instance.
	 * @param message the error message describing the nature of the exception.
	 */
	public MissingDecomVariableException(String message) {
		super(message);
	}
}
