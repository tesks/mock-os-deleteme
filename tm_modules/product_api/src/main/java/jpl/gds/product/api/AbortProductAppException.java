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
package jpl.gds.product.api;

import jpl.gds.shared.exceptions.NestedException;

/**
 * AbortProductAppException is throw by certain product applications when a fatal error occurs.
 * 
 *
 */

@SuppressWarnings("serial")
public class AbortProductAppException extends NestedException 
{	
	/**
	 * Creates an AbortProductAppException.
	 * 
	 * @param message a detailed exception message
	 */
	public AbortProductAppException(final String message)
	{
         super(message); 
    }

	/**
	 * Creates an AbortProductAppException.
	 * 
	 * @param message a detailed exception message
	 * @param rootCause the Throwable that triggered this exception
	 */
    public AbortProductAppException(final String message, final Throwable rootCause)
    {
        super(message, rootCause);
    }
}