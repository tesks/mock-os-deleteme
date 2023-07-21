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

import java.util.Set;

import jpl.gds.db.api.DatabaseException;

public interface IChannelDataPreFetch extends IDbSqlFetch  {
    /**
     * Retrieve all of the sessions based on the search information given in
     * the session pre-query.
     *
     * @throws DatabaseException If there is a problem
     */
    void get() throws DatabaseException;

    /**
     * Look up FSW channel ids corresponding to master key.
     *
     * @param hostId    Host id
     * @param sessionId Session id
     *
     * @return Set of Channel ids
     */
    Set<String> lookupFsWChannelIds(int hostId, int sessionId);

    /**
     * Look up monitor channel ids corresponding to master key.
     *
     * @param hostId    Host id
     * @param sessionId Session id
     *
     * @return Set of Channel ids
     */
    Set<String> lookupMonitorChannelIds(int hostId, int sessionId);

    /**
     * Look up header channel ids corresponding to master key.
     *
     * @param hostId    Host id
     * @param sessionId Session id
     *
     * @return Set of Channel ids
     */
    Set<String> lookupHeaderChannelIds(int hostId, int sessionId);

    /**
     * Look up SSE channel ids corresponding to master key.
     *
     * @param hostId    Host id
     * @param sessionId Session id
     *
     * @return Set of Channel ids
     */
    Set<String> lookupSseChannelIds(int hostId, int sessionId);

    /**
     * Look up SSE header channel ids corresponding to master key.
     *
     * @param hostId    Host id
     * @param sessionId Session id
     *
     * @return Set of Channel ids
     */
    Set<String> lookupSseHeaderChannelIds(int hostId, int sessionId);

    /**
     * Look up FSW channel ids.
     *
     * @return Set of Channel ids
     */
    Set<String> lookupFswChannelIds();

    /**
     * Look up monitor channel ids.
     *
     * @return Set of Channel ids
     */
    Set<String> lookupMonitorChannelIds();

    /**
     * Look up header channel ids.
     *
     * @return Set of Channel ids
     */
    Set<String> lookupHeaderChannelIds();

    /**
     * Look up SSE channel ids.
     *
     * @return Set of Channel ids
     */
    Set<String> lookupSseChannelIds();

    /**
     * Look up SSE header channel ids.
     *
     * @return Set of Channel ids
     */
    Set<String> lookupSseHeaderChannelIds();

    /**
     * Look up precalculated CRC32 of channel-id.
     *
     * @param channelId Channel id
     *
     * @return CRC32 or null if not available
     */
    Long lookupCRC32(String channelId);

}