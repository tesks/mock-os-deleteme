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
 * This class is a superclass for all the various types of exceptions
 * that can occur when processing and parsing uplink information.
 *
 *
 */
public class UplinkParseException extends Exception
{

	private static final long serialVersionUID = 1L;

	/**
	 * Creates an instance of UplinkParseException.
	 */
	public UplinkParseException()
	{
		super();
	}

	/**
	 * Creates an instance of UplinkParseException.
	 * @param message An explanation of what happened.
	 * @param cause The exception that caused this one.
	 */
	public UplinkParseException(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * Creates an instance of UplinkParseException.
	 * @param message An explanation of what happened.
	 */
	public UplinkParseException(String message)
	{
		super(message);
	}

	/**
	 * Creates an instance of UplinkParseException.
	 * @param cause The exception that caused this one.
	 */
	public UplinkParseException(Throwable cause)
	{
		super(cause);
	}

}
