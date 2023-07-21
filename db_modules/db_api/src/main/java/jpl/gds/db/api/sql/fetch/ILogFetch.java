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

import jpl.gds.db.api.sql.store.ldi.ILogMessageLDIStore;

public interface ILogFetch extends IDbSqlFetch {
    /** Database table name */
    public final String DB_LOG_MESSAGE_TABLE_NAME = ILogMessageLDIStore.DB_LOG_MESSAGE_DATA_TABLE_NAME;

    /**
     * The abbreviation that should as a variable name in an SQL statement using
     * the log message table
     */
    public final String DB_LOG_MESSAGE_TABLE_ABBREV = "lm";
}