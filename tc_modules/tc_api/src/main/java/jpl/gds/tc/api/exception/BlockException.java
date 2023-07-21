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

import jpl.gds.tc.api.config.UplinkParseException;

/**
 * 
 * This exception is thrown when there is an error translating user input
 * command information into a binary representation that can be placed
 * in the data section of a telecommand frame.
 * 
 *
 */
@SuppressWarnings("serial")
public class BlockException extends UplinkParseException
{
	/**
	 * Creates an instance of BlockException.
	 */
	public BlockException()
	{
		super();
	}

	/**
	 * Creates an instance of BlockException.
	 * @param message An explanation of what happened.
	 * @param cause The exception that caused this one.
	 */
	public BlockException(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * Creates an instance of BlockException.
	 * @param message An explanation of what happened.
	 */
	public BlockException(String message)
	{
		super(message);
	}

	/**
	 * Creates an instance of BlockException.
	 * @param cause The exception that caused this one.
	 */ 
	public BlockException(Throwable cause)
	{
		super(cause);
	}
}