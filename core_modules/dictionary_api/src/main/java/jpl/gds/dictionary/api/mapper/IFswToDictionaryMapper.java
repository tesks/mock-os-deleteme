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

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.shared.annotation.CustomerAccessible;
import jpl.gds.shared.annotation.Mutator;

/**
 * The IFswToDictionaryMapper interface is to be implemented by all FswToDicitonaryMapper
 * adaptation classes.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * <p>
 * The FswToDictionaryMapper is utilized to represent the many-to-many
 * relationship of FSW build IDs to dictionary versions. It uses a mapping file
 * specified in a DictionaryConfiguration object. This file is in XML format,
 * and is read in ONE time upon initialization of chill_down, and may
 * subsequently be queried for a String flight/ground dictionary version number
 * based upon a long FSW build ID.
 * 
 *
 */
@CustomerAccessible(immutable = false)
public interface IFswToDictionaryMapper {
	
	/**
	 * Reload the mapper file.
	 * @throws DictionaryException 
	 * @return true if a reload was done false otherwise
	 */
	public boolean reload() throws DictionaryException;
	
	/**
	 * @return map of FSW build IDs to Dictionary version number Strings.
	 */
	public Map<Long, String> getFswIdToDictVersionMap();
	
	/**
	 * Returns the correct full Dictionary Version for the provided FSW build ID.
	 *
	 * @param fswId FSW build ID for which a dictionary is being requested
	 * @return the correct full Dictionary Version for the provided FSW build ID.
	 *
	 */
	public IFswToDictMapping getDictionary(final long fswId);


	
	/**
	 * Returns the correct Flight Release Version for the provided
	 * FSW build ID. This value is truncated and does not contain the Ground Revision
	 * (_01, _02, _03, etc.).
	 * 
	 * @param fswId
	 *            FSW build ID for which a dictionary is being requested
	 * @return the correct Flight Release Dictionary Version for the provided
	 *         FSW build ID.
	 */
	public String getReleaseVersionId(final long fswId);
	
	/**
	 * Returns the FSW build IDs for the given full Dictionary Version. 
	 * 
	 * @param dict
	 *            The full dictionary version (name only, not path) for which FSW build IDs are being requested
	 * @return the sorted set of FSW build IDs mapped to the given Dictionary
	 *         Version.
	 * 
	 * @see IFswToDictionaryMapper#getDictionary(long)
	 */
	public SortedSet<Long> getBuildVersionIds(final String dict);
	
	/**
	 * Determine if the given full Dictionary Version has a map entry in the table
	 * 
	 * @param dict
	 *            the full Dictionary Version (name only, not path) being checked
	 * @return true if this mapping table contains a mapping for the specified
	 *         dictionary, false if not.
	 */
	public boolean isMapped(final String dict);
	
	/**
	 * Determine if the given FSW build ID has a map entry in the table
	 * 
	 * @param fswId
	 *            FSW build ID being checked
	 * @return true if this mapping table contains a mapping for the specified
	 *         FSW build ID, false if not.
	 */
	public boolean isMapped(final long fswId);
	
	/**
	 * Determine if two FSW build IDs represent the same Flight Dictionary
	 * Release.
	 * 
	 * @param fswIds1 first ID
	 * @param fswIds2 second ID
	 * @return true if provided FSW Build Ids represent the same Flight
	 *         Dictionary Release Version (ignoring ground revisions), and false
	 *         if not.
	 */
	public boolean isSameFlightDictionary(final Set<Long> fswIds1, final Set<Long> fswIds2);
	
	/**
	 * Determine if two FSW Build Ids represent the same Flight Dictionary
	 * Release.
	 * 
	 * @param fswId1 first ID
	 * @param fswId2 second ID
	 * @return true if provided FSW Build Ids represent the same Flight
	 *         Dictionary Release Version (ignoring ground revisions), and false
	 *         if not.
	 */
	public boolean isSameFlightDictionary(final long fswId1, final long fswId2);
	
	/**
	 * Determines whether the provided Flight Build ID and the provided
	 * full Dictionary Version have the same FSW release version.
	 * 
	 * @param fswId
	 *            the FSW Build ID
	 * @param dict
	 *            the full Dictionary Version
	 * @return true if the dictionary version is from the same FSW Build version, false if not.
	 */
	public boolean isSameFlightDictionary(final long fswId, final String dict);
	
	/**
	 * Returns the correct Flight Release Dictionary Version ID for the provided
	 * Full Dictionary version. This value does not contain the
	 * Ground Revision.
	 * 
	 * @param dict
	 *            the Dictionary Version being checked
	 * @return the correct full Dictionary Version ID for the provided Flight
	 *         Released Dictionary version.
	 */
	public String getReleaseVersionId(final String dict);
	
	/**
	 * Returns the complete Set of all supported FSW Build IDs.
	 * 
	 * @return a Set of supported Software Versions.
	 */
	public Set<Long> getSupportedBuildVersionIds();
	
	/**
	 * Returns the String representing an unmatched or unknown dictionary version. Because this is not standardized this
	 * may vary from mission to missison.
	 * @return A String representing an unmatched or unknown dictionary version
	 */
	public String getUnmatchedDictionaryVersion();
	
	/**
	 * Adds an entry to the dictionary mapping tables in the same manner an entry line in the dictionary mapping file is added.
	 * @param fswBuildVersion FSW build version
	 * @param fswReleaseVersion FSW release version
	 * @param dictionaryVersion dictionary version (sub-directory)
	 * @param groundRevision ground dictionary version
	 * @param dictionaryDirectory dictionary directory path
	 * @param customer customer name
	 * @param timeStamp timestamp for this entry
	 * @param mpduSize byte size of the product MPDU for this dictionary version
	 * 
	 */
    @Mutator
	public void addFswToDictMapping(Long fswBuildVersion, String fswReleaseVersion, String dictionaryVersion, Integer groundRevision, String dictionaryDirectory, String customer, String timeStamp, Integer mpduSize);
	
	
//	/**
//	 * Added a central way to check if a fsw version number
//	 * is the same as the loaded dictionary.
//	 * 
//	 * Checks to see if the dictionary versions loaded in the dictionary factories are the same as the given FSW version.
//	 * 
//	 * @param fswVersion
//	 * @return TRUE if the loaded dictionary and the FSW dictionary are the same, FALSE otherwise
//	 * 
//	 */
//	public boolean dictionaryVersionMatched(final long fswVersion);
	
	/**
	 * Returns the options mpdu_size attribute for the dictionary mapped to fswId.
	 * 
	 * @param fswId id to get the size.
	 * @return the mpdu size from the dictionary mapper file, -1 if no dictionary was found mapped by fswId.
	 */
	public int getMpduSize(final long fswId);
}
