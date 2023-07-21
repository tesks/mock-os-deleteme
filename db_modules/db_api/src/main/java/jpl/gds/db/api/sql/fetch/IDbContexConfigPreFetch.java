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

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.types.IDbContextInfoProvider;

import java.io.Closeable;

/**
 * Pre-fetch interface for context configuration
 *
 */
public interface IDbContexConfigPreFetch extends IDbSqlFetch, Closeable {
    /**
     * Retrieve all of the sessions based on the search information given in
     * the test session info.
     *
     * @throws DatabaseException If there is a problem
     */
    void get() throws DatabaseException;

    /**
     * Retrieve all of the contextx based on the search information given in
     * the context info.
     *
     * @param tsi The search information used to find sessions
     *
     * @throws DatabaseException If there is a problem
     */
    void get(IDbContextInfoProvider tsi) throws DatabaseException;

    /**
     * Compute where clause corresponding to the fetched id and host pairs.
     *
     * Each host aggregate builds its own sub-where clause, and we do the
     * outer parentheses. If there is only one host aggregate, we can skip a
     * parenthesis set.
     *
     * @param abbrev Table abbreviation
     *
     * @return String
     */
    String getIdHostWhereClause(String abbrev);

    /**
     * Lookup host name corresponding to hostId.
     *
     * @param hostId Host id
     *
     * @return Host name or empty string if none.
     */
    String lookupHost(int hostId);

}