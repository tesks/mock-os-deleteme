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
 * This exception is thrown by chill_send_scmf.  It is raised when the command dictionary
 * version stored within the SCMF file being transmitted does not match the command dictionary
 * version for the current session. 
 * 
 *
 */
public class ScmfVersionMismatchException extends ScmfParseException 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Creates an instance of ScmfVersionMismatchException.
	 */
	public ScmfVersionMismatchException()
	{
		super();
	}

	/**
	 * Creates an instance of ScmfVersionMismatchException.
	 * 
	 * @param message An explanation of what happened.
	 * @param cause The exception that caused this one.
	 */
	public ScmfVersionMismatchException(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * Creates an instance of ScmfVersionMismatchException.
	 * 
	 * @param arg0  An explanation of what happened.
	 */
	public ScmfVersionMismatchException(String arg0)
	{
		super(arg0);
	}

	/**
	 * Creates an instance of ScmfVersionMismatchException.
	 * 
	 * @param cause The exception that caused this one.
	 */
	public ScmfVersionMismatchException(Throwable cause)
	{
		super(cause);
	}
}
