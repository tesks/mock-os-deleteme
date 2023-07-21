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
import jpl.gds.db.api.types.IDbSessionInfoProvider;

public interface ISessionStore extends IDbSqlStore {
    /**
     * Store Identification for LDI Inserters
     */
    public static final StoreIdentifier STORE_IDENTIFIER = StoreIdentifier.None;
    
    /**
     * Insert a test configuration into the test session table in the database.
     *
     * Connection is not closed because we do not control it.
     *
     * @param tc        Test configuration whose information will be inserted
     * @param sessionId Greater than zero if a new fragment is needed
     *
     * @return The unique numeric ID of this test configuration (an
     *         auto-increment field from the database).
     *         Zero will be returned if database usage is turned off or this
     *         store is currently stopped.
     *
     * @throws DatabaseException If the insertion or ID retrieval fails
     */
    long insertTestConfig(IContextConfiguration tc, long sessionId) throws DatabaseException;

    /**
     * Insert a test configuration into the test session table in the database.
     *
     * @param tc Test configuration whose information will be inserted
     *
     * @return The unique numeric ID of this test configuration (an
     *         auto-increment field from the database).
     *         Zero will be returned if database usage is turned off or this
     *         store is currently stopped.
     *
     * @throws DatabaseException If the insertion or ID retrieval fails
     */
    long insertTestConfig(IContextConfiguration tc) throws DatabaseException;

    /**
     * Write LDI file to export directory. It will not be used for LDI here,
     * but will be picked up by the remote. We will force the id, which is
     * otherwise autoincrement.
     *
     * In order to synchronize with the import process, we actually write to
     * the export directory with a prepended "." in the file name, hard link to
     * the export directory without the ".", and then delete the original file.
     * That makes sure that the importing process sees only completed files in
     * the export directory.
     *
     * @param tc Test configuration WITH unique id field.
     *
     * @throws DatabaseException Throws SQLException
     */
    void writeLDI(IContextConfiguration tc) throws DatabaseException;

    /**
     * Update the output directory in the database for the given test
     * configuration
     *
     * @param tc The test configuration whose end time needs to be updated (the
     *            end time should be set on the input test configuration)
     * @param oldDir the old output directory to be changed
     * @param newDir the output directory to set in the database
     * @throws DatabaseException If there is an error updating the end time for the
     *             test session
     */
    void updateOutputDirectory(IDbSessionInfoProvider tc, String oldDir, String newDir) throws DatabaseException;
}