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

import jpl.gds.context.api.ISimpleContextConfiguration;
import jpl.gds.db.api.DatabaseException;

public interface IHostFetch extends IDbSqlFetch  {
    /**
     * Returns the hostId for the DatabaseSession. If there is no hostId for the
     * DatabaseSession one is created and returned.
     *
     * We take advantage of the situation to do some error checking on the Host
     * table rows. The hostOffset rows should all match (the local offset).
     * There should be at least one row whose hostId has the local offset.
     * If the hostName is present at the local offset, there should be just one
     * copy.
     *
     * Note especially that we look for the presence of the hostName ONLY at
     * the local offset. Other offsets must be due to merges.
     *
     * While we are at it, we remember the maximum hostId ordinal so we can
     * pass it to HostStore so he can create the entry.
     * 
     * @param hn Name of the host to query for
     *
     * @return Host id of the DatabaseSession.
     *
     * @throws DatabaseException SQL exception
     */
    int get(String hn) throws DatabaseException;

    /**
     * Get host id corresponding to host name and set in configuration.
     * 
     * @param contextConfig the Simple Context Configuration
     *
     * @throws DatabaseException
     *             SQL exception
     */
    void getHostId(ISimpleContextConfiguration contextConfig) throws DatabaseException;
}