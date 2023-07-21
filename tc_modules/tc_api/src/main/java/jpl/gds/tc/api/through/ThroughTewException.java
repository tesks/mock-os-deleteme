/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.tc.api.through;

import jpl.gds.tc.api.config.UplinkParseException;

/**
 * Thrown when an error occurs while performing a through-translation, wrapping, and encoding via the MPSA UplinkUtils.
 * 
 * @since 8.2.0
 */
public class ThroughTewException extends UplinkParseException
{
	private static final long serialVersionUID = 1L;

	/**
	 * Creates an instance of ThroughTewException.
	 */
	public ThroughTewException()
	{
		super();
	}

	/**
	 * Creates an instance of ThroughTewException.
	 * @param message An explanation of what happened.
	 * @param cause The exception that caused this one.
	 */
	public ThroughTewException(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * Creates an instance of ThroughTewException.
	 * @param message An explanation of what happened.
	 */
	public ThroughTewException(String message)
	{
		super(message);
	}

	/**
	 * Creates an instance of ThroughTewException.
	 * @param cause The exception that caused this one.
	 */
	public ThroughTewException(Throwable cause)
	{
		super(cause);
	}

}