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

import jpl.gds.db.api.DatabaseException;

import java.sql.PreparedStatement;
import java.util.List;

/**
 * Context config info updater
 * Manipulates DatabaseContextInfo objects
 */
public interface IDbContextInfoUpdater extends IDbContextInfoProvider{
    /**
     * <span style="font-weight:bold">IMPORTANT</span>: This function should
     * only be called if no changes have been made to this object since the
     * getSqlTemplate() method was called. Otherwise unpredictable behavior may
     * occur.
     *
     * The input index value should be pointing at the first spot in the
     * prepared statement that should be filled in by this method (the first ?
     * that was in the string returned from the getSqlTemplate() method)
     *
     * Early_start is set to force the start-time (if specified) to be zero.
     * This is needed for channel-value-change processing when all values are
     * required.
     *
     * @param index       Index
     * @param statement   Prepared statement
     * @param earlyStart Early-start status
     *
     * @return Updated index value
     *
     * @throws DatabaseException If the statement cannot be filled in properly
     */
    int fillInSqlTemplate(int index, PreparedStatement statement, boolean earlyStart) throws DatabaseException;

    /**
     * See three-argument form. Here we just set early_start to false.
     *
     * @param index     Index
     * @param statement Prepared statement
     *
     * @return Updated value of index
     *
     * @throws DatabaseException SQL exception
     */
    int fillInSqlTemplate(int index, PreparedStatement statement) throws DatabaseException;

    /**
     * Add a test key
     *
     * @param testKey Test key
     */
    void addSessionKey(Long testKey);

    /**
     * Set a test key
     *
     * @param testKey Test key
     */
    void setSessionKey(Long testKey);

    /**
     * Set a test key
     *
     * @param testKey Test key
     */
    void addParentKey(Long testKey);

    /**
     * Sets the testKeyList
     *
     * @param testKeyList The testKeyList to set.
     */
    void setSessionKeyList(List<Long> testKeyList);

    /**
     * Add a type pattern
     *
     * @param typePattern Type pattern
     */
    void addTypePattern(String typePattern);

    /**
     * Sets the typePatternList
     *
     * @param typePatternList The typePatternList to set.
     */
    void setTypePatternList(List<String> typePatternList);

    /**
     * Add a user pattern
     *
     * @param userPattern User pattern
     */
    void addUserPattern(String userPattern);

    /**
     * Add a host pattern
     *
     * @param hostPattern Host pattern
     */
    void addHostPattern(String hostPattern);

    /**
     * Set a host pattern
     *
     * @param hostPattern Host pattern
     */
    void setHostPattern(String hostPattern);

    /**
     * Sets the hostPatternList
     *
     * @param hostPatternList The hostPatternList to set.
     */
    void setHostPatternList(List<String> hostPatternList);

    /**
     * Add a name pattern
     *
     * @param namePattern Name pattern
     */
    void addNamePattern(String namePattern);

    /**
     * Sets the namePatternList
     *
     * @param namePatternList The namePatternList to set.
     */
    void setNamePatternList(List<String> namePatternList);
}
