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
package jpl.gds.shared.sfdu;

/**
 * SfduVersionException is thrown when the SFDU/CHDO processing encounters a version
 * of the SFDU label that it cannot process.
 * 
 */
@SuppressWarnings("serial")
public class SfduVersionException extends SfduException
{
	private final int versionNumber;
	private final String sfduLabel;
	
	/**
	 * Constructs an SFDUVersionException.
	 * 
	 * @param message the detailed error message
	 * @param versionNumber the SFDU version encountered
	 * @param sfduLabel the offending SFDU label
	 */
	public SfduVersionException(String message,int versionNumber,String sfduLabel)
	{
		super(message);
		
		this.versionNumber = versionNumber;
		this.sfduLabel = sfduLabel;
	}

	/**
	 * Constructs an SFDUVersionException.
	 * 
	 * @param cause the Throwable that triggered this exception
	 * @param versionNumber the SFDU version encountered
	 * @param sfduLabel the offending SFDU label
	 */
	public SfduVersionException(Throwable cause,int versionNumber,String sfduLabel)
	{
		super(cause);
		
		this.versionNumber = versionNumber;
		this.sfduLabel = sfduLabel;
	}

	/**
	 * Constructs an SFDUVersionException.
     * @param message the detailed error message
	 * @param cause the Throwable that triggered this exception
	 * @param versionNumber the SFDU version encountered
	 * @param sfduLabel the offending SFDU label
	 */
	public SfduVersionException(String message, Throwable cause,int versionNumber,String sfduLabel)
	{
		super(message, cause);
		
		this.versionNumber = versionNumber;
		this.sfduLabel = sfduLabel;
	}
	
	/**
	 * Gets the SFDU version number for this exception.
	 * 
	 * @return version number
	 */
	public int getVersionNumber()
	{
		return(this.versionNumber);
	}
	
	/**
	 * Gets the SFDU label for this exception.
	 * 
	 * @return SFDU label text
	 */
	public String getSfduLabel()
	{
		return(this.sfduLabel);
	}
}
