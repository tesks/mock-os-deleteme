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
 * Thrown when a user input value for a command argument
 * cannot be parsed properly.
 * 
 *
 */
@SuppressWarnings("serial")
public class ArgumentParseException extends CommandParseException
{
	/**
	 * Creates an instance of ArgumentParseException.
	 */
	public ArgumentParseException()
	{
		super();
	}

	/**
	 * Creates an instance of ArgumentParseException.
	 * 
	 * @param arg0  An explanation of what happened.
	 * @param arg1 The exception that caused this one.
	 */
	public ArgumentParseException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

	/**
	 * Creates an instance of ArgumentParseException
	 * 
	 * @param arg0 An explanation of what happened.
	 */
	public ArgumentParseException(String arg0)
	{
		super(arg0);
	}

	/**
	 * Creates an instance of ArgumentParseException.
	 * 
	 * @param arg0 The exception that caused this one.
	 */
	public ArgumentParseException(Throwable arg0)
	{
		super(arg0);
	}
}
