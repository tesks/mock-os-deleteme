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
 * This exception is thrown when an attempt is made to access a non-existent property
 * within a CHDO.
 * 
 */
@SuppressWarnings("serial")
public class ChdoPropertyException extends SfduException
{
	private final int chdoType;
	private final String property;
	
	/**
	 * Creates an instance of ChdoPropertyException.
	 */
	public ChdoPropertyException()
	{
		super();
		
		this.property = null;
		this.chdoType = 0;
	}

	/**
	 * Creates an instance of ChdoPropertyException.
	 * @param message the detailed error message
	 * @param cause the Throwable that triggered this exception
	 */
	public ChdoPropertyException(String message, Throwable cause)
	{
		super(message, cause);
		
		this.property = null;
		this.chdoType = 0;
	}

	/**
	 * Creates an instance of ChdoPropertyException.
	 * @param message the detailed error message
	 */
	public ChdoPropertyException(String message)
	{
		super(message);
		
		this.property = null;
		this.chdoType = 0;
	}

	/**
	 * Creates an instance of ChdoPropertyException.
	 * @param cause the Throwable that triggered this exception
	 */
	public ChdoPropertyException(Throwable cause)
	{
		super(cause);
		
		this.property = null;
		this.chdoType = 0;
	}   
	
	/**
	 * Creates an instance of ChdoPropertyException.
	 * @param message the detailed error message
	 * @param cause the Throwable that triggered this exception
	 * @param property the name of the CHDO property being accessed at time of the exception
	 * @param chdoType the type of CHDO being accessed at time of the exception
	 */
	public ChdoPropertyException(String message, Throwable cause, String property, int chdoType)
	{
		super(message, cause);
		
		this.property = property;
		this.chdoType = chdoType;
	}

	/**
	 * Creates an instance of UnknownChdoPropertyException.
	 * @param message the detailed error message
	 * @param property the name of the CHDO property being accessed at time of the exception
	 */
	public ChdoPropertyException(String message, String property)
	{
		super(message);
		
		this.property = property;
		this.chdoType = 0;
	}

	/**
	 * Creates an instance of ChdoPropertyException.
	 * @param message the detailed error message
	 * @param property the name of the CHDO property being accessed at time of the exception
	 * @param chdoType the type of CHDO being accessed at time of the exception
	 */
	public ChdoPropertyException(String message,String property,int chdoType)
	{
		super(message);
		
		this.property = property;
		this.chdoType = chdoType;
	}

	/**
	 * Creates an instance of UnknownChdoPropertyException.
	 * @param cause the Throwable that triggered this exception
	 * @param property the name of the CHDO property being accessed at time of the exception
	 * @param chdoType the type of CHDO being accessed at time of the exception
	 */
	public ChdoPropertyException(Throwable cause,String property,int chdoType)
	{
		super(cause);
		
		this.property = property;
		this.chdoType = chdoType;
	}   
	
	/**
	 * Gets the CHDO property name for this exception.
	 * @return CHDO property name, or null if none set
	 */
	public String getProperty()
	{
		return(this.property);
	}
	
	
	/**
	 * Gets the CHDO type for this exception.
	 * @return CHDO type, or 0 if none set
	 */
	public int getChdoType()
	{
		return(this.chdoType);
	}
}