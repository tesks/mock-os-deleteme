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
package jpl.gds.shared.holders;


/**
 * Runtime exception for holder classes. Used when we do not want to have to
 * catch an exception.
 *
 */
public class HolderRuntimeException extends RuntimeException
{
	private static final long serialVersionUID = 1L;


	/**
     * Constructor.
     *
     * @param message Message text
     * @param cause   Cause
     */
    public HolderRuntimeException(final String    message,
                                  final Throwable cause)
    {
        super(message, cause);
    }


    /**
     * Constructor.
     *
     * @param message Message text
     */
    public HolderRuntimeException(final String message)
    {
        super(message);
    }


    /**
     * Constructor.
     *
     * @param cause Cause
     */
    public HolderRuntimeException(final Throwable cause)
    {
        super(cause);
    }


    /**
     * Constructor.
     */
    public HolderRuntimeException()
    {
        super();
    }
}
