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

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.db.api.DatabaseException;

public interface IHostStore extends IDbSqlStore {
    /**
     * Store Identification for LDI Inserters
     */
    public static final StoreIdentifier STORE_IDENTIFIER = StoreIdentifier.None;

    /**
     * Alternate form of DB table name
     */
    String ALT_HOST_STORE_DB_TABLE_NAME = "." + DB_HOST_STORE_TABLE_NAME;

    /**
     * Inserts a unique id number based on the test configuration host name.
     * 
     * @param hostName   Host name
     * @param hostId     Host id
     * @param hostOffset Host offset
     *
     * @throws DatabaseException Throws SQLException
     */
    void insertHostName(String hostName, int hostId, int hostOffset) throws DatabaseException;

    /**
     * Write LDI file to export directory. It will not be used for LDI here, but
     * will be picked up by the remote.
     *
     * In order to synchronize with the import process, we actually write to the
     * export directory with a prepended "." in the file name, hard link to the
     * export directory without the ".", and then delete the original file. That
     * makes sure that the importing process sees only completed files in the
     * export directory.
     * 
     * @param contextConfigs
     *            the Context Configuration
     * @param tc
     *            Test configuration WITH unique id field.
     *
     * @throws DatabaseException
     *             Throws SQLException
     */
    void writeLDI(IContextConfiguration contextConfigs) throws DatabaseException;
}