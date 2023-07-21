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

import jpl.gds.db.api.sql.store.IEndSessionStore;
import jpl.gds.db.api.sql.store.ISessionStore;

public interface ISessionFetch extends IDbSqlFetch {
    /** Database table name */
    public final String DB_SESSION_TABLE_NAME = ISessionStore.DB_SESSION_DATA_TABLE_NAME;

    /** Database table name */
    public final String DB_END_SESSION_TABLE_NAME = IEndSessionStore.DB_END_SESSION_DATA_TABLE_NAME;

    /** The abbreviation for a session table */
    public final String DB_SESSION_TABLE_ABBREV = "ts";

    /** The abbreviation for a end session table */
    public final String DB_END_SESSION_TABLE_ABBREV = "tes";

    /** table field for sessionDssId */
    public final String DSS_ID                      = "dssId";

    /** table field for sessionVcid */
    public final String VCID                        = "vcid";
}