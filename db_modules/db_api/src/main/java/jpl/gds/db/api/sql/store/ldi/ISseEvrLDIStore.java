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
package jpl.gds.db.api.sql.store.ldi;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.evr.api.IEvr;

public interface ISseEvrLDIStore extends IEvrLDIStore {
    /**
     * Store Identification for LDI Inserters
     */
    public static final StoreIdentifier STORE_IDENTIFIER = StoreIdentifier.SseEvr;
    
    /** Table fields as CSV */
    String DB_SSE_EVR_DATA_FIELDS = IEvrLDIStore.FIELDS_COMMON;
    
    /**
     * Insert an SSE EVR and its metadata into the database
     *
     * @param evr EVR from message
     *
     * @throws DatabaseException SQL exception
     */
    public void insertSseEvr(IEvr evr) throws DatabaseException;
}