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
 * This is an exception thrown by the raw output adapters when difficulties 
 * initializing the adapter or sending output are encountered.
 * 
 *
 */
@SuppressWarnings("serial")
public class RawOutputException extends Exception
{
	/**
	 * Basic constructor.
	 */
	public RawOutputException()
	{
		super();
	}

	/**
	 * Constructs a RawOutputException with the given message and cause.
	 * 
	 * @param message the detailed error message
	 * @param cause the Throwable that triggered this exception
	 */
	public RawOutputException(String message, Throwable cause)
	{
		super(message, cause);
	}
	/**
	 * Constructs a RawOutputException with the given message.
	 * 
	 * @param message the detailed error message
	 */
	public RawOutputException(String message)
	{
		super(message);
	}
	
	/**
	 * Constructs a RawOutputException with the given cause.
	 * 
	 * @param cause the Throwable that triggered this exception
	 */
	public RawOutputException(Throwable cause)
	{
		super(cause);
	}
}
