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
package jpl.gds.db.api.sql.store;

import jpl.gds.db.api.DatabaseException;

public interface ICommandUpdateStore extends IDbSqlStore {
    /**
     * Store Identification for LDI Inserters
     */
    public static final StoreIdentifier STORE_IDENTIFIER = StoreIdentifier.CommandUpdate;
    
    /**
     * MPCS-9908
     * Database table fields as CSV.
     */
    static final String DB_COMMAND_MESSAGE_FIELDS = SESSION_ID + "," + HOST_ID + "," + FRAGMENT_ID + ","
                                                        + "requestId" + "," + "finalized";

    /**
     * Get last query issued.
     *
     * @return Last query
     */
    String getLastQuery();

    /**
     * Stop the store.
     */
    void stopUpdateStore();

    /**
     * Update the finalized column to true (1). Note that a row count of zero
     * is tolerated so that an error is not generated when the finalized status
     * is written multiple times or unnecessarily.
     *
     * Note that warnings are bogus because we are not overloading the system
     * finalize (which has no parameters).
     *
     * @param hostId          Host id
     * @param sessionId       Session id
     * @param sessionFragment Session fragment
     * @param requestId       Request id
     *
     * @throws DatabaseException Throws SQLException
     */
    void finalize(int hostId, long sessionId, int sessionFragment, String requestId) throws DatabaseException;
}
