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
package jpl.gds.telem.input.api;

import jpl.gds.shared.log.LogMessageType;

/**
 * This is an exception thrown by the raw input adapters when difficulties 
 * initializing the adapter or processing input are encountered.
 * 
 *
 */
@SuppressWarnings("serial")
public class RawInputException extends Exception
{
	private LogMessageType logMsg;
	
	/**
	 * Basic constructor.
	 */
	public RawInputException()
	{
		super();
	}

	/**
	 * Constructs a RawInputException with the given message.
	 * 
	 * @param message the detailed error message
	 */
	public RawInputException(String message)
	{
		super(message);
	}
	
	/**
	 * Constructs a RawInputException with the given message and LogMessageType.
	 * @param message the detailed error message
	 * @param logMsg the log message type
	 */
	public RawInputException(String message, LogMessageType logMsg) {
		super(message);
		this.logMsg = logMsg;
	}
	
	/**
	 * Get the log message type
	 * @return the log message type
	 */
	public LogMessageType getLogMessageType() {
		return this.logMsg;
	}

	/**
	 * Constructs a RawInputException with the given cause.
	 * 
	 * @param cause the Throwable that triggered this exception
	 */
	public RawInputException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * Constructs a RawInputException with the given message and cause.
	 * 
	 * @param message the detailed error message
	 * @param cause the Throwable that triggered this exception
	 */
	public RawInputException(String message, Throwable cause)
	{
		super(message, cause);
	}
}