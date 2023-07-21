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
package jpl.gds.db.api.sql.fetch;

import java.util.Map;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.shared.time.DatabaseTimeRange;

public interface IEvrFetch extends IDbSqlFetch {
    /**
     * Return a summary of the number of EVR rows for each EVR level
     *
     * @param testSession the TestSessionInfo for the test to query
     * @return A hashmap with (key,value) pairs of the form (level,count)
     *
     * @throws DatabaseException If there is a problem executing the query
     */
    public Map<String, Integer> countEvrsByLevel(IDbContextInfoProvider testSession) throws DatabaseException;

    /**
     * Turn time-range into where-clause segment.
     *
     * @param range Time range
     * @param fsw   True if FSW
     * @param extended True if extended tables are in use
     *
     * @return Segment as string
     *
     * @version MPCS-8384 Extended support
     */
	public String getTimeRangeClause(DatabaseTimeRange range, boolean fsw, boolean extended);
}