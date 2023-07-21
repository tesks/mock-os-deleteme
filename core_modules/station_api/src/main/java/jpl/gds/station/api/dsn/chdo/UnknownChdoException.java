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
package jpl.gds.station.api.dsn.chdo;

import jpl.gds.shared.sfdu.SfduException;

/**
 * UnknownChdoException is thrown by the ChdoDefinition SFDU class when a caller
 * attempts to access ChdoDefinition data for an unknown ChdoDefinition.
 *
 *
 */
@SuppressWarnings("serial")
public class UnknownChdoException extends SfduException
{
	private final int type;
	
	/**
	 * Creates an instance of UnknownChdoException.
	 */
	public UnknownChdoException()
	{
		super();
		
		this.type = -1;
	}
	
	/**
	 * Creates an instance of UnknownChdoException.
	 * @param type the type of CHDO being accessed at the time of this exception
	 */
	public UnknownChdoException(int type)
	{
		super();
		
		this.type = type;
	}

	/**
	 * Creates an instance of UnknownChdoException.
	 * @param message the detailed error message
	 * @param cause the Throwable that triggered this exception
	 */
	public UnknownChdoException(String message, Throwable cause)
	{
		super(message, cause);
		
		this.type = -1;
	}
	
	/**
	 * Creates an instance of UnknownChdoException.
	 * @param message the detailed error message
	 * @param cause the Throwable that triggered this exception
	 * @param type the type of CHDO being accessed at the time of this exception
	 */
	public UnknownChdoException(String message, Throwable cause, int type)
	{
		super(message, cause);
		
		this.type = type;
	}

	/**
	 * Creates an instance of UnknownChdoException.
	 * @param message the detailed error message
	 */
	public UnknownChdoException(String message)
	{
		super(message);
		
		this.type = -1;
	}
	
	/**
	 * Creates an instance of UnknownChdoException.
	 * @param message the detailed error message
	 * @param chdoType the type of CHDO being accessed at the time of this exception
	 */
	public UnknownChdoException(String message,int chdoType)
	{
		super(message);
		
		this.type = chdoType;
	}

	/**
	 * Creates an instance of UnknownChdoException.
	 * @param cause the Throwable that triggered this exception
	 */
	public UnknownChdoException(Throwable cause)
	{
		super(cause);
		
		this.type = -1;
	}
	
	/**
	 * Creates an instance of UnknownChdoException.
	 * @param cause the Throwable that triggered this exception
	 * @param type the type of CHDO being accessed at the time of this exception
	 */
	public UnknownChdoException(Throwable cause,int type)
	{
		super(cause);
		
		this.type = type;
	}

	/**
	 * Gets the CHDO type from this exception.
	 * 
	 * @return the CHDO type
	 */
	public int getType() {
		return type;
	}
}