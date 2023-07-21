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
package jpl.gds.shared.sfdu;

/**
 * 
 * This is the base exception class for all CHDO and SFDU-related exceptions.
 * 
 *
 */
@SuppressWarnings("serial")
public class SfduException extends Exception
{
	/**
	 * Creates an instance of SfduException.
	 */
	public SfduException()
	{
		super();
	}

	/**
	 * Creates an instance of SfduException.
	 * @param message the detailed error message
	 * @param cause the Throwable that triggered this exception
	 */
	public SfduException(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * Creates an instance of SfduException.
	 * @param message the detailed error message
	 */
	public SfduException(String message)
	{
		super(message);
	}

	/**
	 * Creates an instance of SfduException.
	 * @param cause the Throwable that triggered this exception
	 */
	public SfduException(Throwable cause)
	{
		super(cause);
	}
}