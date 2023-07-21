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

import jpl.gds.context.api.ISimpleContextConfiguration;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.types.IDbContextInfoUpdater;

/**
 * Interface for Context Configuration Store
 *
 */
public interface IContextConfigStore extends IDbSqlStore{
    /**
     * Store Identification for LDI Inserters
     */
    public static final StoreIdentifier STORE_IDENTIFIER = StoreIdentifier.ContextConfig;

    /**
     * Insert a context configuration into the context config table in the database.
     * Connection is not closed because we do not control it.
     *
     * @param config        Context configuration whose information will be inserted
     *
     * @return The unique numeric ID of this context configuration (an auto-increment
     *         field from the database). Zero will be returned if database usage is
     *         turned off or this store is currently stopped.
     *
     * @throws DatabaseException If the insertion or ID retrieval fails
     */
    long insertContext(ISimpleContextConfiguration config) throws DatabaseException;

    /**
     * Insert a context configuration into the context config table in the database.
     * Connection is not closed because we do not control it.
     *
     * @param config        Context configuration whose information will be inserted
     * @param sessionId Session ID - optional; pass zero if not used
     *
     * @return The unique numeric ID of this context configuration (an auto-increment
     *         field from the database). Zero will be returned if database usage is
     *         turned off or this store is currently stopped.
     *
     * @throws DatabaseException If the insertion or ID retrieval fails
     */
    long insertContext(ISimpleContextConfiguration config, long sessionId) throws DatabaseException;

    /**
     * Update the session IDs for the given context configuration
     *
     * @param updater The  configuration info whose session ID needs to be updated
     * @param sessionIds the session IDs to set in the database
     * @throws DatabaseException If there is an error updating database
     */
    void updateSessionId(IDbContextInfoUpdater updater, String sessionIds) throws DatabaseException;

    /**
     * Write LDI files (context + metadata) to export directory. They will not be used
     * for LDI here, but will be picked up by the remote.
     * We will force the id, which is otherwise autoincrement.
     *
     * In order to synchronize with the import process, we actually write to the export
     * directory with a prepended ".: in the file name, hard link to the export directory
     * without the ".", and then delete the original file.
     * That makes sure that the importing process sees only completed files in the export directory.
     *
     * @param contextConfig Simple Context configuration WITH unique id field.
     *
     * @throws DatabaseException Throws SQLException
     */
    void writeLDI(ISimpleContextConfiguration contextConfig) throws DatabaseException;
}
