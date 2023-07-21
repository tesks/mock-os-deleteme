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

import jpl.gds.db.api.sql.store.IContextConfigStore;

/**
 * Interface for Context Configuration fetch
 *
 */

public interface IContextConfigFetch extends IDbSqlFetch {
    /** Database table name */
    public final String DB_CONTEXT_TABLE_NAME = IContextConfigStore.DB_CONTEXT_CONFIG_TABLE_NAME;

    /** Database table name */
    public final String DB_CONTEXT_KEYVAL_TABLE_NAME = IContextConfigStore.DB_CONTEXT_CONFIG_KEYVAL_TABLE_NAME;

    /** The abbreviation for a context config table */
    public final String DB_CONTEXT_TABLE_ABBREV = "cc";

    /** The abbreviation for a context config table */
    public final String DB_CONTEXT_KEYVAL_TABLE_ABBREV = "kv";
}