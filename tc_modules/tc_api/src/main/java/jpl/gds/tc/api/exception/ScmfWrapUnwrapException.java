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
 * This exception is thrown when a problem occurs parsing or writing an SCMF file
 * and either the file has a formatting error or the output code has reached a state that
 * would cause a formatting error in the generated file.
 * 
 */
@SuppressWarnings("serial")
public class ScmfWrapUnwrapException extends UplinkParseException
{
	/**
	 * Creates an instance of ScmfWrapUnwrapException.
	 */
	public ScmfWrapUnwrapException()
	{
		super();
	}

	/**
	 * Creates an instance of ScmfWrapUnwrapException.
	 * @param message An explanation of what happened.
	 * @param cause The exception that caused this one.
	 */
	public ScmfWrapUnwrapException(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * Creates an instance of ScmfWrapUnwrapException.
	 * @param message An explanation of what happened.
	 */
	public ScmfWrapUnwrapException(String message)
	{
		super(message);
	}

	/**
	 * Creates an instance of ScmfWrapUnwrapException.
	 * @param cause The exception that caused this one.
	 */
	public ScmfWrapUnwrapException(Throwable cause)
	{
		super(cause);
	}
}