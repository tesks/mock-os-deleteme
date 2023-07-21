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

import java.io.Closeable;
import java.util.List;

import jpl.gds.common.config.types.VenueType;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.api.types.IDbQueryable;


public interface IDbSessionPreFetch extends IDbSqlFetch, Closeable {
    /**
     * Retrieve all of the sessions based on the search information given in
     * the test session info.
     *
     * NEW METHOD: Does not take tsi parameter, but retrieves it from the
     *             
     *
     * @throws DatabaseException If there is a problem
     */
    void get() throws DatabaseException;

    /**
     * Retrieve all of the sessions based on the search information given in
     * the test session info.
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
     * Compute where clause corresponding to the fetched id and host pairs.
     *
     * Each host aggregate builds its own sub-where clause, and we do the
     * outer parentheses. If there is only one host aggregate, we can skip a
     * parenthesis set.
     *
     * @param abbrev  Table abbreviation
     * @param abbrev2 Metatable abbreviation or null
     *
     * @return String
     */
    String getIdHostWhereClause(String abbrev, String abbrev2);

    /**
     * Compute where clause corresponding to the fetched id and host pairs.
     *
     * Each host aggregate builds its own sub-where clause, and we do the
     * outer parentheses. If there is only one host aggregate, we can skip a
     * parenthesis set.
     *
     * @param abbrev     Table abbreviation
     * @param abbrev2    Metatable abbreviation or null
     * @param abbrevHost Host table abbreviation
     *
     * @return String
     */
    String getIdHostWhereClause(String abbrev, String abbrev2, String abbrevHost);

    /**
     * Not used.
     *
     * @return List<DatabaseSession>
     *
     * @throws DatabaseException
     *             on database error
     */
    @Override
    List<IDbQueryable> getNextResultBatch() throws DatabaseException;

    /**
     * Lookup host name corresponding to hostId.
     *
     * @param hostId Host id
     *
     * @return Host name or empty string if none.
     */
    String lookupHost(int hostId);

    /**
     * Lookup SCID corresponding to host id and id. You won't find it
     * unless GET_SCID was specified.
     *
     * @param hostId Host id
     * @param id     Session id
     *
     * @return Spacecraft id or zero if none found.
     */
    int lookupSCID(Integer hostId, Long id);

    /**
     * Lookup OD corresponding to host id and id. You won't find it
     * unless GET_OD was specified.
     *
     * @param hostId Host id
     * @param id     Session id
     *
     * @return Output directory or empty string if none found.
     */
    String lookupOD(Integer hostId, Long id);

    /**
     * Lookup dssId corresponding to host id and id.
     * 
     * @param hostId Host Id
     * @param id     Session id
     * 
     * @return dssId or zero if none found.
     */
    int lookupDss(Integer hostId, Long id);

    /**
     * Lookup VT corresponding to host id and id. 
     *
     * @param hostId Host id
     * @param id     Session id
     *
     * @return VenueType
     */
    VenueType lookupVT(Integer hostId, Long id);

    /**
     * Look up venue type corresponding to the ids.
     *
     * @param hostId    Host id
     * @param sessionId Session id
     *
     * @return Venue type corresponding to the ids.
     */
    VenueType lookupVenue(Integer hostId, Long sessionId);
}