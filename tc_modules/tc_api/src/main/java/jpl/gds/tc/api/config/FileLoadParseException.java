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
package jpl.gds.tc.api.config;


/**
 * Thrown when there is an issue either constructing a file load
 * based on user input or attempting to parse a chunk of binary
 * data back into a File Load object.
 *
 *
 */
public class FileLoadParseException extends UplinkParseException
{

	private static final long serialVersionUID = 1L;

	/**
	 * Creates an instance of FileLoadParseException.
	 */
	public FileLoadParseException()
	{
		super();
	}

	/**
	 * Creates an instance of FileLoadParseException.
	 * @param message An explanation of what happened.
	 * @param cause The exception that caused this one.
	 */
	public FileLoadParseException(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * Creates an instance of FileLoadParseException.
	 * @param message An explanation of what happened.
	 */
	public FileLoadParseException(String message)
	{
		super(message);
	}

	/**
	 * Creates an instance of FileLoadParseException.
	 * @param cause The exception that caused this one.
	 */
	public FileLoadParseException(Throwable cause)
	{
		super(cause);
	}

}
