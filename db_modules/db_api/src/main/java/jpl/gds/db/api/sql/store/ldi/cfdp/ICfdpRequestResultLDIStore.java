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
package jpl.gds.db.api.sql.store.ldi.cfdp;

import jpl.gds.cfdp.message.api.ICfdpRequestResultMessage;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.db.api.sql.store.ldi.ILDIStore;

/**
 * {@code ICfdpRequestResultLDIStore} is the interface for LDI store implementation for the CFDP Request Result data
 * type.
 */
public interface ICfdpRequestResultLDIStore extends ILDIStore {
    StoreIdentifier STORE_IDENTIFIER = StoreIdentifier.CfdpRequestResult;

    /**
     * Database table fields as CSV.
     */
    String DB_CFDP_REQUEST_RESULT_FIELDS =
            SESSION_ID + "," +
                    HOST_ID + "," +
                    FRAGMENT_ID + "," +
                    "eventTimeCoarse" + "," +
                    "eventTimeFine" + "," +
                    "cfdpProcessorInstanceId" + "," +
                    "requestId" + "," +
                    "rejected" + "," +
                    "resultContent" + "," +
                    CONTEXT_ID + "," +
                    CONTEXT_HOST_ID;


    /**
     * Insert a CFDP Request Result event into the database
     *
     * @param m The CFDP Request Result message
     * @throws DatabaseException throws SQLException
     */
    void insertMessage(ICfdpRequestResultMessage m) throws DatabaseException;
}