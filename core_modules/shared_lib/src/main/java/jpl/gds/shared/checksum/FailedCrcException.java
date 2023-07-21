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
package jpl.gds.shared.checksum;

/**
 * This exception should be thrown whenever a CRC check is failed.
 * 
 *
 */
@SuppressWarnings("serial")
public class FailedCrcException extends Exception
{
	/**
	 * 
	 * Creates an instance of FailedCrcException.
	 */
	public FailedCrcException()
	{
		super();
	}

	/**
	 * 
	 * Creates an instance of FailedCrcException.
	 * 
	 * @param message The message associated with the exception
	 */
	public FailedCrcException(String message)
	{
		super(message);
	}

	/**
	 * 
	 * Creates an instance of FailedCrcException.
	 * 
	 * @param cause The cause of this exception
	 */
	public FailedCrcException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * 
	 * Creates an instance of FailedCrcException.
	 * 
	 * @param message The message associated with the exception
	 * 
	 * @param cause The cause of this exception
	 */
	public FailedCrcException(String message, Throwable cause)
	{
		super(message, cause);
	}
}