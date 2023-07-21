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
package jpl.gds.tc.api.exception;

/**
 * 
 * Exception thrown when a particular usage of uplink causes the number of telecommand frames
 * in an uplink session to exceed the maximum allowable number of frames in an uplink session.
 * 
 * Generally the max # of frames is restricted by the bit length of the sequence counter field
 * in the header of the telecommand frame (usually 8 bits = 256 frames).
 *
 *
 */
public class SessionOverflowException extends Exception
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates an instance of SessionOverflowException.
	 */
	public SessionOverflowException()
	{
		super();
	}

	/**
	 * Creates an instance of SessionOverflowException.
	 * 
	 * @param message The associated error message
	 */
	public SessionOverflowException(String message)
	{
		super(message);
	}

	/**
	 * Creates an instance of SessionOverflowException.
	 * 
	 * @param cause The throwable that caused the exception
	 */
	public SessionOverflowException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * Creates an instance of SessionOverflowException.
	 * 
	 * @param message The associated error message
	 * @param cause The throwable that caused the exception
	 */
	public SessionOverflowException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
