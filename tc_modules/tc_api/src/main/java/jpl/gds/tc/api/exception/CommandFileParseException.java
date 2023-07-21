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
 * Thrown when there is an issue parsing a user input command list file. 
 *
 */
public class CommandFileParseException extends UplinkParseException
{

	private static final long serialVersionUID = 1L;

	/**
	 * Creates an instance of CommandFileParseException.
	 */
	public CommandFileParseException()
	{
		super();
	}

	/**
	 * Creates an instance of CommandFileParseException.
	 * @param message An explanation of what happened.
	 * @param cause The exception that caused this one.
	 */
	public CommandFileParseException(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * Creates an instance of CommandFileParseException.
	 * @param message An explanation of what happened.
	 */
	public CommandFileParseException(String message)
	{
		super(message);
	}

	/**
	 * Creates an instance of CommandFileParseException.
	 * @param cause The exception that caused this one.
	 */
	public CommandFileParseException(Throwable cause)
	{
		super(cause);
	}

}
