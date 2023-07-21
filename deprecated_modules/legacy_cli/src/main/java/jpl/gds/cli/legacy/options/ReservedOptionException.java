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

package jpl.gds.cli.legacy.options;

/**
 * When application create arguments, each argument that did not come from the
 * reserved command line options class should be checked to make sure that it is
 * not reserved.  This exception is thrown when an application command line argument
 * attempts to use a short or long option name that is reserved.
 * 
 * This exception is thrown as a RuntimeException because it's mostly a check that is done
 * in development.  Having to declare a "throws" on every application that creates options and
 * having to implement a try/catch is burdensome and unnecessary.
 * 
 */
public class ReservedOptionException extends RuntimeException
{
    private static final long serialVersionUID = 0L;


    /**
     * Constructor.
     */	
	public ReservedOptionException()
	{
		super();
	}


    /**
     * Constructor.
     *
     * @param message Message text
     */	
	public ReservedOptionException(String message)
	{
		super(message);
	}
}
