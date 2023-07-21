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
package jpl.gds.db.api.types;

import java.util.List;

/**
 * Context config info provider
 * Manipulates DatabaseContextInfo objects
 *
 */
public interface IDbContextInfoProvider extends IDbInfoProvider {

	/**
	 * Check that at least one piece of information in this
	 * object has been given a non-null value
	 *
	 * @return True if at least one test-related value in this
	 * class is non-null, false otherwise
	 */
	boolean isSearchCriteriaSet();

	/**
	 * Check that at least one piece of information in this
	 * object has been given a non-null value
	 *
	 * @return True if at least one test-related value in this
	 * class is non-null, false otherwise
	 */
	boolean isIdHostSearchCriteriaOnlySet();

	/**
	 * This function generates a templated string of SQL that can be used in the
	 * WHERE clause of a prepared SQL statement.  The word "templated" implies
	 * that ? characters have been inserted in place of actual values.
	 *
	 * <span style="font-weight:bold">IMPORTANT</span>: Once this function is
	 * called, do not make any changes to this class before calling
	 * "fillInSqlTemplate" or unpredictable behavior may occur.
	 *
	 * @param tablePrefix Table abbreviation
	 *
	 * @return The templated SQL string for use in an SQL WHERE clause.
	 *         There are no leading or trailing SQL connector keywords
	 *         (no preceding or trailing ANDs/ORs)
	 */
	String getSqlTemplate(String tablePrefix);

	/**
	 * Get test key list.
	 *
	 * @return Returns the testKeyList.
	 */
	List<Long> getSessionKeyList();

	/**
	 * Get parent key list.
	 *
	 * @return Returns the testKeyList.
	 */
	List<Long> getParentKeyList();

	/**
	 * Get type pattern list.
	 *
	 * @return Returns the typePatternList.
	 */
	List<String> getTypePatternList();

	/**
	 * Get user pattern list.
	 *
	 * @return Returns the userPatternList.
	 */
	List<String> getUserPatternList();

	/**
	 * Get host pattern list.
	 *
	 * @return Returns the hostPatternList.
	 */
	List<String> getHostPatternList();

	/**
	 * Get name pattern list.
	 *
	 * @return Returns the namePatternList.
	 */
	List<String> getNamePatternList();
}