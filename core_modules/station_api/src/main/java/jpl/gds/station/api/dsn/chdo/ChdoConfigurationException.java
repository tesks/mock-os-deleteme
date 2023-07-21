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
 * Class ChdoConfigurationException
 *
 * An exception thrown where there are issues with the CHDO SFDU configuration.
 * 
 *
 */
@SuppressWarnings("serial")
public class ChdoConfigurationException extends SfduException
{
	private final int chdoType;
	
	/**
	 * Creates an instance of ChdoPropertyException.
	 */
	public ChdoConfigurationException()
	{
		super();

		this.chdoType = 0;
	}

	/**
	 * Creates an instance of ChdoConfigurationException.
	 * @param message the detailed error message
	 * @param cause the Throwable that triggered this exception
	 */
	public ChdoConfigurationException(final String message, final Throwable cause)
	{
		super(message, cause);
		
		this.chdoType = 0;
	}

	/**
	 * Creates an instance of ChdoConfigurationException.
	 * @param message the detailed error message
	 */
	public ChdoConfigurationException(final String message)
	{
		super(message);
		
		this.chdoType = 0;
	}

	/**
	 * Creates an instance of ChdoConfigurationException.
	 * @param cause the Throwable that triggered this exception
	 */
	public ChdoConfigurationException(final Throwable cause)
	{
		super(cause);

		this.chdoType = 0;
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