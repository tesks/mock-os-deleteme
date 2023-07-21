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
package jpl.gds.dictionary.api.mapper;

/**
 * Interface IFswToDictMapping
 */
public interface IFswToDictMapping extends Comparable<IFswToDictMapping>{
	
	/**
	 * @return the fswBuildVersion
	 */
	public long getFswBuildVersion();


	/**
	 * @return the fswReleaseVersion
	 */
	public String getFswReleaseVersion();

	/**
	 * @return the dictionaryVersion
	 */
	public String getDictionaryVersion();

	/**
	 * @return the groundRevision
	 */
	public int getGroundRevision();
	
	/**
	 * @return the dictionaryDirectory
	 */
	public String getFswDirectory();

	/**
	 * @return the customer
	 */
	public String getCustomer();

	/**
	 * @return the timeStamp
	 */
	public String getTimeStamp();

	/**
	 * @return the mpdu size attribute value.
	 */
	public int getMpduSize();

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode();

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj);
	
	public String toString();
	
	public int compareTo(IFswToDictMapping o);
	
}