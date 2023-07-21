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

import jpl.gds.shared.holders.SessionFragmentHolder;
import jpl.gds.shared.time.IAccurateDateTime;


public interface IDbSessionInfoProvider extends IDbContextInfoProvider {

	/**
	 * Get description pattern list.
	 *
	 * @return Returns the descriptionPatternList.
	 */
	List<String> getDescriptionPatternList();

	/**
	 * Get downlink stream id list.
	 *
	 * @return Returns the downlinkStreamIdList.
	 */
	List<String> getDownlinkStreamIdList();

	/**
	 * Get FSW version pattern list.
	 *
	 * @return Returns the fswVersionPatternList.
	 */
	List<String> getFswVersionPatternList();

	/**
	 * Get SSE version pattern list.
	 *
	 * @return Returns the sseVersionPatternList.
	 */
	List<String> getSseVersionPatternList();

	/**
	 * Get test start time lower-bound.
	 *
	 * @return Returns the testStartTimeLowerBound.
	 */
    IAccurateDateTime getStartTimeLowerBound();

	/**
	 * Get start time upper-bound.
	 *
	 * @return Returns the testStartTimeUpperBound.
	 */
    IAccurateDateTime getStartTimeUpperBound();

	/**
	 * Gets the session fragment
	 * @return Session fragment holder
	 */
	SessionFragmentHolder getSessionFragment();
}