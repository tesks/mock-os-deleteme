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
package jpl.gds.db.app.ctab;


/**
 * General exception for all CTAB use.
 *
 */
public class CtabException extends Exception
{
    /** Serial version */
	private static final long serialVersionUID = 1L;


	/**
     * Constructor.
     *
     * @param message Message
     * @param cause   Underlying cause
     */
    public CtabException(final String    message,
                         final Throwable cause)
    {
        super(message, cause);
    }


    /**
     * Constructor.
     *
     * @param message Message
     */
    public CtabException(final String message)
    {
        super(message);
    }
}
