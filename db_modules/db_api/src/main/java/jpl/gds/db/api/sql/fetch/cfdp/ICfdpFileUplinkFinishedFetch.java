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
package jpl.gds.db.api.sql.fetch.cfdp;

import jpl.gds.db.api.sql.fetch.IDbSqlFetch;

public interface ICfdpFileUplinkFinishedFetch extends IDbSqlFetch {

    /**
     * The abbreviation that should as a variable name in an SQL statement using
     * the CFDP File Uplink Finished table
     */
    String DB_CFDP_FILE_UPLINK_FINISHED_TABLE_ABBREV = "cfuf";

}