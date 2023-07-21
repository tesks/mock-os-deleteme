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

import java.sql.PreparedStatement;
import java.util.List;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.shared.holders.SessionFragmentHolder;
import jpl.gds.shared.time.IAccurateDateTime;

public interface IDbSessionInfoUpdater extends IDbSessionInfoProvider, IDbContextInfoUpdater {

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
	 * @param early_start Early-start status
	 *
	 * @return Updated index value
	 *
	 * @throws DatabaseException If the statement cannot be filled in properly
	 */
	int fillInSqlTemplate(int index, PreparedStatement statement, boolean early_start) throws DatabaseException;

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
	 * Add a description pattern
	 *
	 * @param descPattern Description pattern
	 */
	void addDescriptionPattern(String descPattern);

	/**
	 * Sets the descriptionPatternList
	 *
	 * @param descriptionPatternList The descriptionPatternList to set.
	 */
	void setDescriptionPatternList(List<String> descriptionPatternList);

	/**
	 * Add a downlink stream ID
	 *
	 * @param downlinkStreamId Downlink stream id
	 */
	void addDownlinkStreamId(String downlinkStreamId);

	/**
	 * Sets the downlinkStreamIdList
	 *
	 * @param downlinkStreamIdList The downlinkStreamIdList to set.
	 */
	void setDownlinkStreamIdList(List<String> downlinkStreamIdList);

	/**
	 * Add an FSW version pattern
	 *
	 * @param fswVersionPattern FSW version pattern
	 */
	void addFswVersionPattern(String fswVersionPattern);

	/**
	 * Sets the fswVersionPatternList
	 *
	 * @param fswVersionPatternList The fswVersionPatternList to set.
	 */
	void setFswVersionPatternList(List<String> fswVersionPatternList);

	/**
	 * Add an SSE version pattern
	 *
	 * @param sseVersionPattern SSE version pattern
	 */
	void addSseVersionPattern(String sseVersionPattern);

	/**
	 * Sets the sseVersionPatternList
	 *
	 * @param sseVersionPatternList The sseVersionPatternList to set.
	 */
	void setSseVersionPatternList(List<String> sseVersionPatternList);

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
	 * Add a test key range
	 *
	 * @param testKeyStart Test key start
	 * @param testKeyEnd   Test key end
	 */
	void addSessionKeyRange(Long testKeyStart, Long testKeyEnd);

	/**
	 * Sets the testStartTimeLowerBound
	 *
	 * @param testStartTimeLowerBound The testStartTimeLowerBound to set.
	 */
	void setStartTimeLowerBound(IAccurateDateTime testStartTimeLowerBound);

	/**
	 * Sets the testStartTimeUpperBound
	 *
	 * @param startTimeUpperBound The testStartTimeUpperBound to set.
	 */
	void setStartTimeUpperBound(IAccurateDateTime startTimeUpperBound);

	/**
	 * Set the session fragment
	 * @param fragment Session fragment holder
	 */
	void setSessionFragment(SessionFragmentHolder fragment);
}