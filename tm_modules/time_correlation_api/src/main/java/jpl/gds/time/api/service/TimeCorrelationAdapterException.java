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
package jpl.gds.time.api.service;

/**
 * This is an exception thrown by various time correlation operations.
 * 
 *
 */
@SuppressWarnings("serial")
public class TimeCorrelationAdapterException extends Exception
{
	/**
	 * Basic constructor.
	 */
	public TimeCorrelationAdapterException()
	{
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param message detail message text
	 * @param cause the root exception
	 */
	public TimeCorrelationAdapterException(String message, Throwable cause)
	{
		super(message,cause);
	}

	/**
	 * Constructor.
	 * 
	 * @param message detail message text
	 */
	public TimeCorrelationAdapterException(String message)
	{
		super(message);
	}

	/**
	 * Constructor.
	 * 
	 * @param cause the root exception
	 */
	public TimeCorrelationAdapterException(Throwable cause)
	{
		super(cause);
	}
}
